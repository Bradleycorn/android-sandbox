package net.bradball.android.sandbox.playback;

import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import net.bradball.android.sandbox.model.Recording;
import net.bradball.android.sandbox.util.LogHelper;

import java.util.List;
import java.util.Queue;

/**
 * Created by bradb on 7/17/16.
 */
public class PlaybackManager implements
        PlayQueue.QueueUpdateListener,
        Playback.PlaybackListener {

    private static final String TAG = LogHelper.makeLogTag(PlaybackManager.class);

    private Playback mPlayback;
    private PlayQueue mQueue;
    private PlaybackEventsListener mListener;


    private String mLastError;
    private MediaMetadataCompat mCurrentItemMetadata;

    public interface PlaybackEventsListener {
        void onQueueCreated(String title, List<MediaSessionCompat.QueueItem> queue);
        void onPlaybackStateChanged(int newState);
        void onMetadataChanged(MediaMetadataCompat metadata);
    }

    public PlaybackManager(Playback playback, PlayQueue queue) {
        mPlayback = playback;
        mPlayback.setPlaybackListener(this);
        mQueue = queue;
        mQueue.setQueueUpdateListener(this);
    }

    public void setPlaybackEventsListener(PlaybackEventsListener listener) {
        mListener = listener;
    }

    public void startPlayback(Recording recording, String mediaId) {
        mQueue.createQueue(recording, mediaId);
        mListener.onQueueCreated(mQueue.getTitle(), mQueue.getItems());
        MediaSessionCompat.QueueItem currentItem = mQueue.getCurrentItem();
        if (currentItem != null) {
            mPlayback.play(currentItem);
        }
    }

    public boolean canPlayMedia(String mediaId) {
        return mQueue.containsMediaId(mediaId);
    }

    public boolean isPlayingMedia() {
        return (mPlayback != null && mPlayback.isPlaying());
    }

    public PlaybackStateCompat getCurrentState() {
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        int state = PlaybackStateCompat.STATE_NONE;

        if (mPlayback != null) {  //&& mPlayback.isConnected()) {
            position = mPlayback.getPlaybackPosition();
            state = mPlayback.getPlaybackState();
        }

        // If there is an error message, send it to the playback state:
        if (state == PlaybackStateCompat.STATE_ERROR) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(mLastError);
        }

        //noinspection ResourceType
        stateBuilder.setActions(getAvailableActions());

        //setCustomAction(stateBuilder);

        //noinspection ResourceType
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        // Set the activeQueueItemId if the current index is valid.
        MediaSessionCompat.QueueItem currentQueueItem = mQueue.getCurrentItem();
        if (currentQueueItem != null) {
            stateBuilder.setActiveQueueItemId(currentQueueItem.getQueueId());
        }

        return stateBuilder.build();
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

    private boolean isListenerRegistered() {
        return (mListener != null);
    }


    // PLAYBACK UPDATE LISTENER IMPLEMENTATION
    // =======================================

    @Override
    public void onLostAudioFocus() {

    }

    @Override
    public void onPlaybackComplete(boolean nextSongQueued) {

    }

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        if (isListenerRegistered()) {
            mListener.onPlaybackStateChanged(playbackState);
        }
    }

    @Override
    public void onError(String error) {
        mLastError = error;
    }

    @Override
    public void onNextStarting(String mediaId) {

    }

    @Override
    public void onBufferUpdate(int bufferAmount) {

    }


    //PLAY QUEUE UPDATE LISTENER IMPLEMENTATION
    //========================================

    @Override
    public void onCurrentItemChanged(MediaSessionCompat.QueueItem item, MediaMetadataCompat metadata) {
        mCurrentItemMetadata = metadata;
        if (isListenerRegistered()) {
            mListener.onMetadataChanged(mCurrentItemMetadata);
        }
    }

}
