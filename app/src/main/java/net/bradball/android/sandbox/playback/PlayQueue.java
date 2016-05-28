package net.bradball.android.sandbox.playback;

import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;

import net.bradball.android.sandbox.model.Recording;
import net.bradball.android.sandbox.model.Track;
import net.bradball.android.sandbox.provider.RecordingsContract;
import net.bradball.android.sandbox.util.MusicLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by bradb on 5/26/16.
 */
public class PlayQueue {


    private Recording mQueuedRecording;
    private List<MediaSessionCompat.QueueItem> mQueue;
    private int mCurrentIndex;
    private QueueUpdateListener mQueueUpdateListener;


    public PlayQueue(QueueUpdateListener listener) {
        mQueue = Collections.synchronizedList(new ArrayList<MediaSessionCompat.QueueItem>());
        mCurrentIndex = 0;
        mQueueUpdateListener = listener;
    }

    public void createQueue(Recording recording, String playMediaId) {
        int numTracks = recording.getNumberOfTracks();
        int queuePosition = 0;
        List<MediaSessionCompat.QueueItem> queue = new ArrayList<>();

        for (Track track : recording.getTracks()) {
            MediaMetadataCompat trackMetadata = track.getMediaMetadata(numTracks);
            MediaSessionCompat.QueueItem item = new MediaSessionCompat.QueueItem(trackMetadata.getDescription(), queuePosition);
            queue.add(item);
            queuePosition++;
        }

        setQueue(recording, queue, playMediaId);
    }

    public MediaSessionCompat.QueueItem getCurrentItem() {
        return mQueue.get(mCurrentIndex);
    }

    public MediaSessionCompat.QueueItem getNextItem() {
        int nextIndex = mCurrentIndex++;
        if (mQueue.size() > nextIndex) {
            return mQueue.get(nextIndex);
        }
        return null;
    }

    public Recording getCurrentRecording() {
        return mQueuedRecording;
    }

    public boolean isMediaQueued(String mediaId) {
        //Does the mediaId match the uri of the current recording?
        if (mQueuedRecording!= null && RecordingsContract.Recordings.buildRecordingUri(mQueuedRecording.getIdentifier()).toString() == mediaId) {
            return true;
        } else {
            //See if the mediaId matches an item in our queue.
            for (MediaSessionCompat.QueueItem item : mQueue) {
                if (item.getDescription().getMediaId() == mediaId) {
                    return true;
                }
            }
        }

        return false;
    }

    public int getCurrentIndex() {
        return mCurrentIndex;
    }


    private void setQueue(Recording recording, List<MediaSessionCompat.QueueItem> queue, String playMediaId) {
        mQueuedRecording = recording;

        mQueue.clear();
        mQueue.addAll(queue);

        mCurrentIndex =  getQueueItemIndex(playMediaId);
        mQueueUpdateListener.onQueueUpdated(recording.getTitle(), queue);
    }





    private int getQueueItemIndex(String mediaId) {
        int index = 0;
        for (MediaSessionCompat.QueueItem item : mQueue) {
            if (item.getDescription().getMediaId() == mediaId) {
                return index;
            }
            index++;
        }

        return 0;
    }


    public interface QueueUpdateListener {
        void onMetadataChanged(MediaMetadataCompat metadata);
        void onMetadataRetrieveError();
        void onCurrentQueueIndexUpdated(int index);
        void onQueueUpdated(String title, List<MediaSessionCompat.QueueItem> queue);
    }

}
