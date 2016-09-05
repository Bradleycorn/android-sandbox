package net.bradball.android.sandbox.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.media.MediaBrowserCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import net.bradball.android.sandbox.R;
import net.bradball.android.sandbox.ui.fragments.MediaBrowserFragment;
import net.bradball.android.sandbox.util.LogHelper;
import net.bradball.android.sandbox.util.MediaHelper;

public class BrowserActivity extends PlaybackControlsActivity
    implements MediaBrowserFragment.MediaBrowserFragmentListener {
    private static final String TAG = LogHelper.makeLogTag(BrowserActivity.class);

    public static Intent getIntent(Context packageContext) {
        return new Intent(packageContext, BrowserActivity.class);
    }

    @Override
    protected boolean usesNavDrawer() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showFragment(null);
    }

    @Override
    protected void onMediaBrowserConnected() {
        super.onMediaBrowserConnected();

        MediaBrowserFragment browserFragment = getMediaBrowserFragment();
        if (browserFragment != null) {
            browserFragment.onBrowserServiceConnected();
        }
    }

    @Override
    public void onItemChanged(MediaBrowserCompat.MediaItem item) {
        if (item == null) {
            setTitle(R.string.browser_title);
        } else {
            setTitle(item.getDescription().getTitle());
        }
    }

    @Override
    public void onItemSelected(MediaBrowserCompat.MediaItem mediaItem) {

        if (mediaItem.isPlayable()) {
            getSupportMediaController().getTransportControls().playFromMediaId(mediaItem.getMediaId(), null);
        } else if (mediaItem.isBrowsable()) {
            showFragment(mediaItem);
        } else {
            LogHelper.w(TAG, "Selected media item that is not browsable or playable??? ", mediaItem.getMediaId());
        }
    }

    private void showFragment(MediaBrowserCompat.MediaItem mediaItem) {

        String mediaId = (mediaItem != null) ? mediaItem.getMediaId() : null;
        if (mediaItem == null || TextUtils.equals(mediaId, MediaHelper.ROOT_ID)) {
            setTitle(R.string.browser_title);
        } else {
            setTitle(mediaItem.getDescription().getTitle());
        }

        MediaBrowserFragment browserFragment = getMediaBrowserFragment();

        if (browserFragment == null || !TextUtils.equals(browserFragment.getMediaId(), mediaId)) {
            browserFragment = MediaBrowserFragment.newInstance(mediaId);

            FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction()
                    .replace(R.id.container, browserFragment)
                    .commit();
        }
    }

    private MediaBrowserFragment getMediaBrowserFragment() {
        return (MediaBrowserFragment) getSupportFragmentManager().findFragmentById(R.id.container);
    }
}
