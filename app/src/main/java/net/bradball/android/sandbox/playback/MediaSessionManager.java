package net.bradball.android.sandbox.playback;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import net.bradball.android.sandbox.model.Recording;
import net.bradball.android.sandbox.util.LogHelper;
import net.bradball.android.sandbox.util.MediaNotificationHelper;

import java.util.List;

/**
 *               |  Created   |   Playing   |    Paused   |   Stopped   | Destroyed |
 * --------------|------------|-------------|-------------|-------------|-----------|
 * MediaPlayer   |    new     |   prepare   |    pause    |    stop     |  release  |
 *               |            |    play     |             |             |           |
 *               |            |             |             |             |
 * --------------|------------|-------------|-------------|-------------|-----------|
 * Audio Focus   |            |request focus|             | clear focus |           |
 * --------------|------------|-------------|-------------|-------------|-----------|
 * Noisy         |            |  register   | unregister  |             |           |
 * --------------|------------|-------------|-------------|-------------|-----------|
 * Media Session |    new     |  setActive  |             |set InActive |  release  |
 *               | set flags  |set metadata |             |             |           |
 *               |set callback|  set state  | set state   |             |           |
 * --------------|------------|-------------|-------------|-------------|-----------|
 * Notification  |            | start FG    |stopFG(false)|stopFG(true) |           |
 *---------------|------------|-------------|-------------|-------------|-----------|
 * Service       |            | load Tracks |             |             |           |
 *               |            | delay stop  |             | delay stop  |           |
 *               |            |   clear     |             |  start      |           |
 *---------------|------------|-------------|-------------|-------------|-----------|
 */

public class MediaSessionManager extends MediaSessionCompat.Callback
    implements
        PlaybackManager.PlaybackEventsListener {

    private static final String TAG = LogHelper.makeLogTag(MediaSessionManager.class);

    private Context mContext;
    private MediaSessionCompat mMediaSession;
    private PlaybackManager mPlaybackManager;
    private Playback mPlayer;
    private PlayQueue mPlayQueue;
    private MediaSessionListener mListener;
    private MediaControllerCallback mMediaControllerCallback;

    //We will store the most recent mediaId that has been requested for playback.
    //This is needed in case multiple requests come in "at the same time"
    //for different media (last one wins).
    private String mCurrentPlaybackRequest;


    public interface MediaSessionListener {
        void onQueueMediaId(String mediaId, Bundle extras);
        void onPlaybackStarting(Notification mediaNotification);
        void onPlaybackStopped(boolean isPermanent, Notification mediaNotification);
        void onPlaybackMediaChanged(Notification mediaNotification);
    }


    public MediaSessionManager(Context context) {
        mContext = context;

        mMediaControllerCallback = new MediaControllerCallback();

        mMediaSession = new MediaSessionCompat(mContext, TAG);
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setCallback(this);
        mMediaSession.getController().registerCallback(mMediaControllerCallback);

        mPlayer = new Playback(mContext);
        mPlayQueue = new PlayQueue();
        mPlaybackManager = new PlaybackManager(mPlayer, mPlayQueue);
        mPlaybackManager.setPlaybackEventsListener(this);

    }

    public void setMediaSessionListener(MediaSessionListener listener) {
        mListener = listener;
    }

    public void destroy() {
        //TODO: Stop playback and kill all mediaPlayers.
        mMediaSession.release();
    }

    /**
     * Return the MediaSessionCompat object.
     * Use this method with caution.
     * The MediaSesion is needed in the Service to setup the MediaButtonReceiver.
     * Otherwise, it should be rare that the MediaSession is needed directly.
     * @return MediaSessionCompat
     */
    public MediaSessionCompat getMediaSession() {
        return mMediaSession;
    }

    public MediaSessionCompat.Token getSessionToken() { return mMediaSession.getSessionToken(); }


    public void playMedia(Recording recording, String mediaId) {
        //Make sure the media to be played is the most recent to be requested.
        if (!TextUtils.equals(mediaId,mCurrentPlaybackRequest)) {
            return;
        }

        mPlaybackManager.startPlayback(recording, mediaId);
    }

    public boolean isMediaPlaying() {
        return mPlaybackManager.isPlayingMedia();
    }

    private void setSessionQueue(String title, List<MediaSessionCompat.QueueItem> queue) {
        mMediaSession.setQueueTitle(title);
        mMediaSession.setQueue(queue);
    }

    private void setSessionActive(boolean isActive) {
        if(mMediaSession.isActive() != isActive) {
            mMediaSession.setActive(isActive);
        }
    }

    private void setSessionState() {

        PlaybackStateCompat newState = mPlaybackManager.getCurrentState();

        mMediaSession.setPlaybackState(newState);


    }

    private void setSessionMetaData(MediaMetadataCompat metaData) {
        mMediaSession.setMetadata(metaData);
    }

    private boolean isListenerRegistered() {
        return (mListener != null);
    }


    // MEDIASESSION CALLBACKS
    // ======================

    @Override
    public void onPlayFromMediaId(String mediaId, Bundle extras) {

        //We have a request to play some media (either a track or recording).
        //First, we need to send the info back to the MusicService so
        //it can load the recordings and tracks we need (presumably from in memory cache).
        //When it's done, it'll call our createQueue() method to create
        //a queue, and start playback
        mCurrentPlaybackRequest = mediaId;
        if (mPlaybackManager.canPlayMedia(mediaId)) {
            //This media is already in the play queue, we just need to play it.
            //TODO: Play the right media.
        } else if (isListenerRegistered()){
            mListener.onQueueMediaId(mediaId, extras);
        }

    }

    // PLAYBACK EVENTS LISTENER IMPLEMENTATION
    // =======================================

    @Override
    public void onQueueCreated(String title, List<MediaSessionCompat.QueueItem> queue) {
      setSessionQueue(title, queue);
    }

    @Override
    public void onPlaybackStateChanged(int newState) {

        if (newState == PlaybackStateCompat.STATE_PLAYING || newState == PlaybackStateCompat.STATE_BUFFERING || newState == PlaybackStateCompat.STATE_STOPPED) {
            setSessionActive(newState == PlaybackStateCompat.STATE_PLAYING || newState == PlaybackStateCompat.STATE_BUFFERING);
        }

        setSessionState();
    }

    @Override
    public void onMetadataChanged(MediaMetadataCompat metadata) {
        setSessionMetaData(metadata);
    }


    // MEDIACONTROLLERCOMPAT.CALLBACK OVERRIDES
    private class MediaControllerCallback extends MediaControllerCompat.Callback {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            if (isListenerRegistered()) {
                Notification mediaNotification = MediaNotificationHelper.createNotification(mContext,mMediaSession);
                switch (state.getState()) {
                    case PlaybackStateCompat.STATE_PLAYING:
                        mListener.onPlaybackStarting(mediaNotification);
                        break;
                    case PlaybackStateCompat.STATE_PAUSED:
                        mListener.onPlaybackStopped(false, mediaNotification);
                        break;
                    case PlaybackStateCompat.STATE_STOPPED:
                        mListener.onPlaybackStopped(true, null);
                        break;
                }
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            mListener.onPlaybackMediaChanged(MediaNotificationHelper.createNotification(mContext,mMediaSession));
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            mMediaSession.getController().unregisterCallback(mMediaControllerCallback);
        }
    }

}
