package net.bradball.android.sandbox.playback;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import net.bradball.android.sandbox.model.Recording;
import net.bradball.android.sandbox.provider.RecordingUrisEnum;
import net.bradball.android.sandbox.util.MediaHelper;

/**
 * Created by bradb on 5/25/16.
 */
public class PlaybackManager extends MediaSessionCompat.Callback implements LocalPlayback.PlaybackListener {



    private LocalPlayback mLocalPlayback;
    private PlayQueue mPlayQueue;
    private final ServiceCallback mServiceCallback;

    public PlaybackManager(LocalPlayback localPlayback, PlayQueue queue, ServiceCallback serviceCallback) {
        mLocalPlayback = localPlayback;
        mLocalPlayback.setPlaybackListener(this);

        mPlayQueue = queue;

        mServiceCallback = serviceCallback;
    }


    public void createQueue(Recording recording) {


    }

    // MEDIASESSION CALLBACK METHODS
    // =============================


    @Override
    public void onPlay() {
        super.onPlay();
    }

    @Override
    public void onPlayFromMediaId(String mediaId, Bundle extras) {
        //We have a request to play some media (either a track or recording).
        //First, we need to send the info back to the MusicService so
        //it can load the recordings and tracks we need (presumably from in memory cache).
        //When it's done, it'll call our createQueue() method to create
        //a queue, and start playback
        if (mPlayQueue.isMediaQueued(mediaId)) {
            //This media is already in the play queue, we just need
            //to play it.
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
        super.onSeekTo(pos);
    }

    @Override
    public void onSkipToNext() {
        super.onSkipToNext();
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

    }

    @Override
    public void onPlaybackComplete() {

    }

    @Override
    public void onPlaybackStatusChanged(int playbackState) {

    }

    @Override
    public void onError(String error) {

    }

    @Override
    public void currentMediaIdChanged(String mediaId) {

    }





    public interface ServiceCallback {
        void onPlaybackStarting();

        void onPlaybackStopped();

        void onPlaybackStateChanged(PlaybackStateCompat newState);

        void onNotificationRequired();

        void onPlayMediaRequest(String mediaId, Bundle extras);
    }

}
