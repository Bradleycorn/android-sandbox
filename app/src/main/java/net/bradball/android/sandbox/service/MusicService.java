package net.bradball.android.sandbox.service;

import android.app.Notification;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import net.bradball.android.sandbox.model.Recording;
import net.bradball.android.sandbox.playback.MediaSessionManager;
import net.bradball.android.sandbox.playback.PlayQueue;
import net.bradball.android.sandbox.playback.Playback;
import net.bradball.android.sandbox.util.LogHelper;
import net.bradball.android.sandbox.util.MediaHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends MediaBrowserServiceCompat
    implements
        MediaSessionManager.MediaSessionListener,
        MusicHandlerThread.MediaLoadedCallback {

    private static final String TAG = LogHelper.makeLogTag(MusicService.class);

    public static final String INTENT_ACTION_PLAYER_COMMAND = "net.bradball.android.sandbox.playerCommand";
    public static final String CMD_NAME = "CMD_NAME";
    public static final String CMD_STOP_CASTING = "CMD_STOP_CASTING";
    public static final String CMD_PAUSE = "CMD_PAUSE";

    private static final int STOP_DELAY = 30000;
    private static final int MEDIA_LOADED_PLAY = 0;
    private static final int NOTIFICATION_ID = 1;

    private MusicHandlerThread mMusicHandlerThread;
    private Handler mMusicResponseHandler;

    private MediaSessionManager mSessionManager;

    private boolean isForeground;


    private final DelayedStopHandler mDelayedStopHandler = new DelayedStopHandler(this);

    @Override
    public void onCreate() {
        super.onCreate();

        isForeground = false;

        //Create a Music Loader to get music data from the database and API
        mMusicResponseHandler = new Handler();
        mMusicHandlerThread = new MusicHandlerThread(this, mMusicResponseHandler);
        mMusicHandlerThread.setMediaLoadedCallback(this);
        mMusicHandlerThread.start();
        mMusicHandlerThread.getLooper();

        //Setup a MediaSession Manager that's responsible for handling all things
        //related to our media session, including playback, handling of button presses,
        //maintaining the state and metadata for playback, etc.
        mSessionManager = new MediaSessionManager(this);
        mSessionManager.setMediaSessionListener(this);

        //Our MediaSession (owned by the MediaSessionManager) has a token that client's will need
        //in order to connect to it. Luckily the MediaBrowserService we are extending has a handy
        //mechanism to pass this token around to all those clients, so let's take care of that now.
        setSessionToken(mSessionManager.getSessionToken());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            MediaButtonReceiver.handleIntent(mSessionManager.getMediaSession(),intent);
        }

        //Reset the delayed stop hander. And set a message to fire in 30 seconds.
        //If no music is played by then, we'll stop the service.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        //TODO: Kill the notification?
        mMusicHandlerThread.clearMessageQueue();
        mMusicHandlerThread.quit();

        mDelayedStopHandler.removeCallbacksAndMessages(null);

        mSessionManager.destroy();
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot(MediaHelper.ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        LogHelper.d(TAG, "Request for children of: ", parentId);
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
    public void onChildrenChanged(Uri parentMediaUri) {
        LogHelper.d(TAG, "Notifying Connected MediaBrowsers that Children have changed for URI: ", parentMediaUri.toString() );
        notifyChildrenChanged(parentMediaUri.toString());
    }

    @Override
    public void onRecordingLoaded(Recording recording, String playMediaId, int action) {
        switch (action) {
            case MEDIA_LOADED_PLAY:
                mSessionManager.playMedia(recording, playMediaId);
                break;
        }
    }

    private void updateNotification(Notification mediaNotification) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (mediaNotification != null) {
            notificationManager.notify(NOTIFICATION_ID, mediaNotification);
        } else {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    // MEDIASESSION LISTENER IMPLEMENTATION
    // ====================================

    @Override
    public void onQueueMediaId(String mediaId, Bundle extras) {
        //Load the media from storage. the MEDIA_LOADED_PLAY argument will
        //be passed back to this object's onRecordingLoaded method, so we can use
        //that to start playback when the recording is loaded.
        mMusicHandlerThread.loadRecording(mediaId, MEDIA_LOADED_PLAY);
        mDelayedStopHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onPlaybackStarting(Notification mediaNotification) {

        //Clear any existing messages on the delayed stop handler, so it won't kill the service.
        mDelayedStopHandler.removeCallbacksAndMessages(null);

        //Start the service in the foreground (and setup the media notificiation)

        if (!isForeground) {
            isForeground = true;
            startForeground(NOTIFICATION_ID, mediaNotification);
        } else {
            updateNotification(mediaNotification);
        }


        // The following startService call shouldn't be needed. The startForeground()
        // call above should keep the service running.

        // The service needs to continue running even after the bound client (usually a
        // MediaController) disconnects, otherwise the music playback will stop.
        // Calling startService(Intent) will keep the service running until it is explicitly killed.
        //startService(new Intent(getApplicationContext(), MusicService.class));
    }

    @Override
    public void onPlaybackStopped(boolean isPermanent, Notification mediaNotification) {
        //Stop the service from running in the foreground.
        stopForeground(isPermanent);
        updateNotification(mediaNotification);

        //TODO: If the stop is permanent, we probably need to send a message to the delay stop handler to stop the service.
    }

    @Override
    public void onPlaybackMediaChanged(Notification mediaNotification) {
        updateNotification(mediaNotification);
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
            if (service != null) {
                if (service.mSessionManager.isMediaPlaying()) {
                    LogHelper.d(TAG, "Ignoring delayed stop since the media player is in use.");
                    return;
                }
                LogHelper.d(TAG, "Stopping service with delay handler.");
                service.stopSelf();
            }
        }
    }



}
