package net.bradball.android.sandbox.ui;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import net.bradball.android.sandbox.R;
import net.bradball.android.sandbox.service.MusicService;
import net.bradball.android.sandbox.sync.SyncHelper;
import net.bradball.android.sandbox.ui.fragments.PlaybackControlsFragment;
import net.bradball.android.sandbox.util.LogHelper;

/**
 * Created by bradb on 7/30/16.
 */
public abstract class PlaybackControlsActivity extends BaseActivity implements IMediaBrowser {

    private MediaBrowserCompat mMediaBrowser;

    private final MediaBrowserCompat.ConnectionCallback mConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            onMediaBrowserConnected();
        }
    };

    private final MediaControllerCompat.Callback mMediaControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
        }

        @Override
        public void onQueueTitleChanged(CharSequence title) {
            super.onQueueTitleChanged(title);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Connect a media browser.
        mMediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, MusicService.class), mConnectionCallback, null);

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = getPlaybackControlsFragment();

        if (fragment == null) {
            fragment = createPlaybackControlsFragment();

            fm.beginTransaction()
                    .add(getControlsContainerId(), fragment)
                    .commit();
        }
    }



    @Override
    protected void onStart() {
        super.onStart();
        mMediaBrowser.connect();
    }



    @Override
    protected void onStop() {
        super.onStop();
        mMediaBrowser.disconnect();
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_with_controls;
    }

    @IdRes
    protected int getControlsContainerId() {
        return R.id.playback_controls_container;
    }

    private Fragment getPlaybackControlsFragment() {
        FragmentManager fm = getSupportFragmentManager();
        return fm.findFragmentById(getControlsContainerId());
    }

    private Fragment createPlaybackControlsFragment() {
        return PlaybackControlsFragment.newInstance();
    }

    @Override
    public MediaBrowserCompat getMediaBrowser() {
        return mMediaBrowser;
    }


    //TODO: Use this to hookup a MediaController and setup the playback controls fragment.
    protected void onMediaBrowserConnected() {
        try {
            MediaControllerCompat mediaController = new MediaControllerCompat(this, mMediaBrowser.getSessionToken());
            setSupportMediaController(mediaController);
            mediaController.registerCallback(mMediaControllerCallback);
        } catch(RemoteException ex) {
            LogHelper.e("Could not Connect a MediaController");
        }
    }
}
