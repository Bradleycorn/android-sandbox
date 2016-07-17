package net.bradball.android.sandbox.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;

import net.bradball.android.sandbox.model.Recording;
import net.bradball.android.sandbox.provider.RecordingUrisEnum;
import net.bradball.android.sandbox.provider.RecordingsContract;
import net.bradball.android.sandbox.util.MediaHelper;
import net.bradball.android.sandbox.util.MusicLoader;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by bradb on 4/24/16.
 */
public class MusicHandlerThread extends HandlerThread {

    private static final String TAG = "MusicHandlerThread";

    private static final int MESSAGE_CACHE_TRACKS = 2;
    private static final int MESSAGE_LOAD_CHILDREN = 3;
    private static final int MESSAGE_LOAD_RECORDING = 4;

    private final MusicLoader mMusicLoader;
    private final Resources mResources;

    private Handler mHandler;
    private Handler mResponseHandler;
    private ConcurrentHashMap<MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>, String> mBrowserRequestsMap = new ConcurrentHashMap<>();

    private MediaLoadedCallback mMediaLoadedCallback;

    public interface MediaLoadedCallback {
        void onChildrenLoaded(MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> result, List<MediaBrowserCompat.MediaItem> list, String parentMediaId);
        void onRecordingLoaded(Recording recording, String mediaId, int action);
    }


    public MusicHandlerThread(Context context, Handler responseHandler) {
        super(TAG);
        mMusicLoader = new MusicLoader(context.getApplicationContext());
        mResponseHandler = responseHandler;
        mResources = context.getResources();
    }

    public void setMediaLoadedCallback(MediaLoadedCallback mediaLoadedCallback) {
        mMediaLoadedCallback = mediaLoadedCallback;
    }

    public void loadChildren(MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> result, String parentMediaID) {

        if (parentMediaID == null) {
            mBrowserRequestsMap.remove(result);
        } else {
            mBrowserRequestsMap.put(result, parentMediaID);
            mHandler.obtainMessage(MESSAGE_LOAD_CHILDREN, result).sendToTarget();
        }
    }


    public void cacheTracks(String recordingMediaId) {
        if (recordingMediaId != null) {
            mHandler.obtainMessage(MESSAGE_CACHE_TRACKS, recordingMediaId).sendToTarget();
        }
    }

    public void loadRecording(String mediaId, int action) {
        if (mediaId != null) {
            mHandler.obtainMessage(MESSAGE_LOAD_RECORDING, action, 0, mediaId).sendToTarget();
        }
    }


    public void clearMessageQueue() {
        mHandler.removeMessages(MESSAGE_CACHE_TRACKS);
        mHandler.removeMessages(MESSAGE_LOAD_CHILDREN);
        mHandler.removeMessages(MESSAGE_LOAD_RECORDING);
        mHandler.removeCallbacksAndMessages(null);
    }


    private void returnChildren(final MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> result, final String recordingIdentifier, final List<MediaBrowserCompat.MediaItem> list) {
        mResponseHandler.post(new Runnable() {
            @Override
            public void run() {
                if (result != null && !mBrowserRequestsMap.get(result).equals(recordingIdentifier)) {
                    return;
                }

                if (result != null) {
                    mBrowserRequestsMap.remove(result);
                }

                mMediaLoadedCallback.onChildrenLoaded(result, list, recordingIdentifier);
            }
        });
    }

    private void returnRecording(final Recording recording, final String mediaId, final int action) {
        mResponseHandler.post(new Runnable() {
            @Override
            public void run() {
                mMediaLoadedCallback.onRecordingLoaded(recording, mediaId, action);
            }
        });
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String mediaId;

                switch (msg.what) {
                    case MESSAGE_LOAD_CHILDREN:
                        List<MediaBrowserCompat.MediaItem> music;
                        MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> result;

                        result = (MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>) msg.obj;
                        mediaId = mBrowserRequestsMap.get(result);

                        music = mMusicLoader.getChildren(mediaId, mResources);

                        returnChildren(result, mediaId, music);
                        break;

                    case MESSAGE_CACHE_TRACKS:
                        // To cache tracks, all we need to do is call
                        // the MusicProvider's getChildren method, passing
                        // the recording media id.
                        mediaId = (String) msg.obj;
                        mMusicLoader.getChildren(mediaId, mResources);

                        break;
                    case MESSAGE_LOAD_RECORDING:
                        mediaId = (String) msg.obj;
                        int action = msg.arg1;

                        Uri recordingUri;
                        RecordingUrisEnum mediaType = MediaHelper.getMediaIdType(mediaId);

                        if (mediaType == RecordingUrisEnum.TRACK) {
                           recordingUri = RecordingsContract.Recordings.buildRecordingUri(MediaHelper.extractRecordingIdentifier(Uri.parse(mediaId)));
                        } else {
                            recordingUri = Uri.parse(mediaId);
                        }

                        Recording recording = mMusicLoader.getRecordingWithTracks(recordingUri);

                        returnRecording(recording, mediaId, action);
                }
            }
        };
    }









}
