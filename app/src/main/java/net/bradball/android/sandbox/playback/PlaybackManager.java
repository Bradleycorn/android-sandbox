package net.bradball.android.sandbox.playback;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import net.bradball.android.sandbox.model.Recording;

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
 */public class PlaybackManager extends MediaSessionCompat.Callback implements Playback.PlaybackListener {
    private static final String TAG = "PlaybackManager";


    private Playback mPlayback;
    private PlayQueue mPlayQueue;
    private final ServiceCallback mServiceCallback;

    //We will store the most recent mediaId that has been requested for playback.
    //This is needed in case multiple requests come in "at the same time"
    //for different media (last one wins).
    private String mCurrentPlaybackRequest;

    public PlaybackManager(Playback playback, PlayQueue queue, ServiceCallback serviceCallback) {
        mPlayback = playback;
        mPlayback.setPlaybackListener(this);
        mPlayQueue = queue;
        mServiceCallback = serviceCallback;
    }


    public void handlePlayRequest(Recording recording, String mediaId) {
        //Make sure the media to be played is the most recent to be requested.
        if (!TextUtils.equals(mediaId,mCurrentPlaybackRequest)) {
            return;
        }

        mPlayQueue.createQueue(recording, mediaId);
        playMusic();
    }



    public Playback getPlayback() {
        return mPlayback;
    }

    public void updatePlaybackState(String error) {
        Log.d(TAG, "updatePlaybackState, playback state=" + mPlayback.getPlaybackState());
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        int state = PlaybackStateCompat.STATE_NONE;

        if (mPlayback != null) {  //&& mPlayback.isConnected()) {
            position = mPlayback.getPlaybackPosition();
            state = mPlayback.getPlaybackState();
        }


        //noinspection ResourceType
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder().setActions(getAvailableActions());

        //setCustomAction(stateBuilder);

        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error);
            state = PlaybackStateCompat.STATE_ERROR;
        }
        //noinspection ResourceType
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        // Set the activeQueueItemId if the current index is valid.
        MediaSessionCompat.QueueItem currentQueueItem = mPlayQueue.getCurrentItem();
        if (currentQueueItem != null) {
            stateBuilder.setActiveQueueItemId(currentQueueItem.getQueueId());
        }

        mServiceCallback.onPlaybackStateChanged(stateBuilder.build());

        if (state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_PAUSED) {
            mServiceCallback.onNotificationRequired();
        }
    }

    private long getAvailableActions() {
        long actions =
                PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
                        //PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        if (mPlayback.isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        }
        return actions;
    }

    private void playMusic() {
        MediaSessionCompat.QueueItem currentItem = mPlayQueue.getCurrentItem();
        if (currentItem != null) {
            mServiceCallback.onPlaybackStarting();
            mPlayback.play(currentItem);
        }
    }

    // MEDIASESSION CALLBACK METHODS
    // =============================


    @Override
    public void onPlay() {
        playMusic();
    }

    @Override
    public void onPlayFromMediaId(String mediaId, Bundle extras) {
        //We have a request to play some media (either a track or recording).
        //First, we need to send the info back to the MusicService so
        //it can load the recordings and tracks we need (presumably from in memory cache).
        //When it's done, it'll call our createQueue() method to create
        //a queue, and start playback
        mCurrentPlaybackRequest = mediaId;
        if (mPlayQueue.isMediaQueued(mediaId)) {
            //This media is already in the play queue, we just need to play it.
            //TODO: Play the right media.
        } else {
            mServiceCallback.onPlayMediaRequest(mediaId, extras);
        }
    }

    @Override
    public void onPlayFromSearch(String query, Bundle extras) {
        super.onPlayFromSearch(query, extras);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSeekTo(long pos) {
        mPlayback.seekTo((int) pos);
    }

    @Override
    public void onSkipToNext() {

        if (mPlayQueue.skip(1)) {
            playMusic();
        }
    }

    @Override
    public void onSkipToPrevious() {
        super.onSkipToPrevious();
    }

    @Override
    public void onSkipToQueueItem(long id) {
        super.onSkipToQueueItem(id);
    }



    @Override
    public void onCustomAction(String action, Bundle extras) {
        super.onCustomAction(action, extras);
    }



    // PLAYBACK LISTENER IMPLEMENTATION
    // ================================

    @Override
    public void onLostAudioFocus() {
        //TODO: Kill the whole service
    }

    @Override
    public void onPlaybackComplete(boolean nextTrackBuffering) {
        //TODO: UpdatePlayQueue, setting Next Track as current if necessary.
        if (!nextTrackBuffering) {
            mPlayQueue.skip(1);
            playMusic();
        }
    }

    @Override
    public void onNextStarting(String mediaId) {
        mPlayQueue.setCurrentItem(mediaId);
    }

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        updatePlaybackState(null);
    }

    @Override
    public void onError(String error) {
        updatePlaybackState(error);
    }

    @Override
    public void onBufferUpdate(int bufferAmount) {

    }

    public interface ServiceCallback {
        void onPlaybackStarting();

        void onPlaybackStopped();

        void onPlaybackStateChanged(PlaybackStateCompat newState);

        void onNotificationRequired();

        void onPlayMediaRequest(String mediaId, Bundle extras);
    }

}
