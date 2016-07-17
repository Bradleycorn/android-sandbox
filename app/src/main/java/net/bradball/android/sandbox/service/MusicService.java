package net.bradball.android.sandbox.service;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import net.bradball.android.sandbox.R;
import net.bradball.android.sandbox.model.Recording;
import net.bradball.android.sandbox.model.Show;
import net.bradball.android.sandbox.model.Track;
import net.bradball.android.sandbox.playback.LocalPlayback;
import net.bradball.android.sandbox.playback.PlayQueue;
import net.bradball.android.sandbox.playback.Playback;
import net.bradball.android.sandbox.playback.PlaybackManager;
import net.bradball.android.sandbox.provider.RecordingUriMatcher;
import net.bradball.android.sandbox.provider.RecordingUrisEnum;
import net.bradball.android.sandbox.util.MediaHelper;
import net.bradball.android.sandbox.util.MusicLoader;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class MusicService extends MediaBrowserServiceCompat
    implements PlaybackManager.ServiceCallback,
        MusicHandlerThread.MediaLoadedCallback,
        PlayQueue.QueueUpdateListener {
    private static final String TAG = "MusicService";

    public static final String INTENT_ACTION_PLAYER_COMMAND = "net.bradball.android.sandbox.playerCommand";
    public static final String CMD_NAME = "CMD_NAME";
    public static final String CMD_STOP_CASTING = "CMD_STOP_CASTING";
    public static final String CMD_PAUSE = "CMD_PAUSE";

    private static final int STOP_DELAY = 30000;
    private static final int MEDIA_LOADED_PLAY = 0;


    private MusicHandlerThread mMusicHandlerThread;
    private Handler mMusicResponseHandler;

    private MediaSessionCompat mMediaSession;
    private PlaybackManager mPlaybackManager;
    private PlayQueue mPlayQueue;


    private final DelayedStopHandler mDelayedStopHandler = new DelayedStopHandler(this);

    @Override
    public void onCreate() {
        super.onCreate();

        //Create a Music Loader to get music data from the database and API
        mMusicResponseHandler = new Handler();
        mMusicHandlerThread = new MusicHandlerThread(this, mMusicResponseHandler);
        mMusicHandlerThread.setMediaLoadedCallback(this);
        mMusicHandlerThread.start();
        mMusicHandlerThread.getLooper();

        //Setup a playback manager, which will handle all of the
        //"on play", "on pause", etc requests that will come into the service from
        //all the different devices (the app front end, android wear, etc).
        //Most (all?) of these will come via a MediaController's Transport Controls.
        //But the PlaybackManager only oversee's playback event. The actual player (MediaPlayer)
        //is handled by a LocalPlayback object, which is given to the PlaybackManager.
        //We'll also need a PlayQueue to give to the PlaybackManager.
        mPlayQueue = new PlayQueue(this);
        Playback playback = new Playback(this);
        mPlaybackManager = new PlaybackManager(playback, mPlayQueue, this);


        //Now we can setup a MediaSession. The MediaSession is the brains of this whole
        //operation. It does all the stuff we need to be able to pass commands back and forth
        //between app Activities, Android Wear, and our Player Service.
        //Our PlaybackManager is a MediaSession.Callback object, so we'll set it as the callback
        //and then it will receive and Manage all the playback related commands.
        mMediaSession = new MediaSessionCompat(this, TAG);
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setCallback(mPlaybackManager);

        //Our MediaSession has a token that client's will need in order to connect to it.
        //Luckily the MediaBrowserService we are extending has a handy mechanism to pass
        //this token around to all those clients, so let's take care of that now.
        setSessionToken(mMediaSession.getSessionToken());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            MediaButtonReceiver.handleIntent(mMediaSession,intent);
        }

        //Reset the delayed stop hander. And set a message to fire in 30 seconds.
        //If no music is played by then, we'll stop the service.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        //TODO: Tell the PlaybackManager to Stop playback (and kill it's MediaPlayer).
        //TODO: Kill the notification?
        mMusicHandlerThread.clearMessageQueue();
        mMusicHandlerThread.quit();

        mDelayedStopHandler.removeCallbacksAndMessages(null);

        mMediaSession.release();
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot(MediaHelper.ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        if (!MediaHelper.isBrowsable(parentId)) {
            result.sendResult(new ArrayList<MediaBrowserCompat.MediaItem>());
            return;
        }

        result.detach();
        mMusicHandlerThread.loadChildren(result, parentId);

    }

    @Override
    public void onChildrenLoaded(Result<List<MediaBrowserCompat.MediaItem>> result, List<MediaBrowserCompat.MediaItem> list, String parentMediaId) {
        result.sendResult(list);
    }

    @Override
    public void onRecordingLoaded(Recording recording, String playMediaId, int action) {
        switch (action) {
            case MEDIA_LOADED_PLAY:
                mPlaybackManager.handlePlayRequest(recording, playMediaId);
                break;
        }
    }

    // PlaybackManager.ServiceCallback Implementation
    // ==============================================


    @Override
    public void onPlayMediaRequest(String mediaId, Bundle extras) {
        //Load the media from storage. the MEDIA_LOADED_PLAY argument will
        //be passed back to this object's onRecordingLoaded method, so we can use
        //that to start playback when the recording is loaded.
        mMusicHandlerThread.loadRecording(mediaId, MEDIA_LOADED_PLAY);
    }

    @Override
    public void onPlaybackStarting() {
        //setActive
        //setMetaData
        //setState
        //start notification
        //clear messages from delayedStopHandler


        if (!mMediaSession.isActive()) {
            mMediaSession.setActive(true);
        }

        mDelayedStopHandler.removeCallbacksAndMessages(null);

        // The service needs to continue running even after the bound client (usually a
        // MediaController) disconnects, otherwise the music playback will stop.
        // Calling startService(Intent) will keep the service running until it is explicitly killed.
        startService(new Intent(getApplicationContext(), MusicService.class));
    }

    @Override
    public void onPlaybackStopped() {
        mMediaSession.setActive(false);

        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);

        stopForeground(true);
    }

    @Override
    public void onPlaybackStateChanged(PlaybackStateCompat newState) {
        mMediaSession.setPlaybackState(newState);
    }

    @Override
    public void onNotificationRequired() {
        //TODO: start a notification
    }

    // PlayQueue.QueueUpdateListener Implementation
    // ============================================


    @Override
    public void onMetadataChanged(MediaMetadataCompat metadata) {
        mMediaSession.setMetadata(metadata);
    }

    @Override
    public void onMetadataRetrieveError() {
        mPlaybackManager.updatePlaybackState("Unable to retrieve metadata");
    }

    @Override
    public void onCurrentQueueIndexUpdated(int index) {
    }

    @Override
    public void onQueueUpdated(String title, List<MediaSessionCompat.QueueItem> queue) {
        mMediaSession.setQueue(queue);
        mMediaSession.setQueueTitle(title);
    }

    /**
     * A simple handler that stops the service if playback is not active (playing)
     */
    private static class DelayedStopHandler extends Handler {
        private final WeakReference<MusicService> mWeakReference;

        private DelayedStopHandler(MusicService service) {
            mWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            MusicService service = mWeakReference.get();
            if (service != null && service.mPlaybackManager.getPlayback() != null) {
                if (service.mPlaybackManager.getPlayback().isPlaying()) {
                    Log.d(TAG, "Ignoring delayed stop since the media player is in use.");
                    return;
                }
                Log.d(TAG, "Stopping service with delay handler.");
                service.stopSelf();
            }
        }
    }



}
