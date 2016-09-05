/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bradball.android.sandbox.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContentResolverCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.util.LruCache;

import net.bradball.android.sandbox.R;
import net.bradball.android.sandbox.data.DatabaseSchema;
import net.bradball.android.sandbox.data.TrackParser;
import net.bradball.android.sandbox.data.jsonModel.RecordingDetailsJson;
import net.bradball.android.sandbox.model.Show;
import net.bradball.android.sandbox.model.Track;
import net.bradball.android.sandbox.network.ArchiveAPI;
import net.bradball.android.sandbox.provider.RecordingUriMatcher;
import net.bradball.android.sandbox.provider.RecordingUrisEnum;
import net.bradball.android.sandbox.provider.RecordingsContract;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import net.bradball.android.sandbox.model.Recording;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple data provider for music tracks. The actual metadata source is delegated to a
 * MusicProviderSource defined by a constructor argument of this class.
 */
public class MusicLoader {

    private static final String TAG = LogHelper.makeLogTag(MusicLoader.class);

    private static final int CACHE_SIZE_MB = 5; //Cache Size in MB
    private static final LruCache<String, Recording> mRecordingCache;

    private final Context mContext;




    static {
        mRecordingCache = new LruCache<>(CACHE_SIZE_MB * 1024 * 1024);
    }

    public MusicLoader(Context context) {
        mContext = context.getApplicationContext();
    }


    public LinkedHashMap<String, Integer> getYears(Uri mediaUri) {
        Cursor cursor = null;
        final String[] projection = new String[] {
                RecordingsContract.Shows.YEAR,
                RecordingsContract.Shows._COUNT
        };
        final String orderBy = RecordingsContract.Shows.YEAR + " desc";
        LinkedHashMap<String, Integer> years = new LinkedHashMap<>();

        cursor = mContext.getContentResolver().query(mediaUri, projection, null, null, orderBy);
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    while (cursor.moveToNext()) {
                        years.put(cursor.getString(0), cursor.getInt(1));
                    }
                }
            } finally {
                    cursor.close();
            }
        }

        return years;
    }

    public Iterable<Show> getShows(Uri mediaUri) {
        List<Show> shows = new ArrayList<>();
        Cursor cursor = null;
        String selection = RecordingsContract.Shows.YEAR + " = ?";
        String[] selectionArgs = {RecordingsContract.Shows.getShowDate(mediaUri)};

        cursor = mContext.getContentResolver().query(mediaUri, RecordingsContract.Shows.PROJECTION, selection, selectionArgs, DatabaseSchema.ShowsTable.NAME + "." + RecordingsContract.Shows.DATE + " desc");
        if (cursor != null) {
            try {
                cursor.moveToFirst();
                do {
                    shows.add(Show.getFromCursor(cursor));
                } while (cursor.moveToNext());
            } finally {
                cursor.close();
            }
        }

        return shows;
    }

    public Iterable<Recording> getRecordings(Uri mediaUri) {
        List<Recording> recordings = new ArrayList<>();
        Uri recordingsUri = RecordingsContract.Shows.buildShowRecordingsUri(mediaUri);
        Cursor cursor = null;
        Recording recording = null;

        cursor =  mContext.getContentResolver().query(recordingsUri, RecordingsContract.Recordings.PROJECTION, null, null, RecordingsContract.Recordings.DATE + " desc");
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    recording = Recording.getFromCursor(cursor);
                    recordings.add(recording);
                    addToCache(recording);
                }
            } finally {
                cursor.close();
            }
        }

        return recordings;
    }

    public Recording getRecording(Uri mediaUri, boolean skipCache) {
        Cursor cursor = null;
        Recording recording = null;

        if (!skipCache) {
            String recordingIdentifier = RecordingsContract.Recordings.getRecordingArchiveID(mediaUri);

            //Get the recording from the cache.
            recording = mRecordingCache.get(recordingIdentifier);

            if (recording != null) {
                return recording;
            }
        }

        cursor = mContext.getContentResolver().query(mediaUri, RecordingsContract.Recordings.PROJECTION, null, null, null);
        if (cursor != null) {
            try {
                cursor.moveToFirst();
                recording = Recording.getFromCursor(cursor);
                addToCache(recording);
            } finally {
                cursor.close();
            }
        }

        return recording;
    }

    public Recording getRecordingWithTracks(Uri mediaUri) {
        Recording recording = getRecording(mediaUri, false);

        if (recording.getNumberOfTracks() < 1) {
            ArrayList<Track>  tracks = getTracksFromAPI(recording.getIdentifier());
            recording.setTracks(tracks);
            addToCache(recording);
        }

        return recording;
    }

    /**
     * Try to get a list of tracks for a Play Queue from a track mediaId.
     * This method will only look in the cache for the track's recording.
     * If the recording is found in the cache, it'll return the recording's tracks.
     * If not, it'll return an empty list.
     *
     * @param trackMediaID - The mediaId of a track to be queued. We'll create the queue from the track's recording
     * @param extras - For future use: So we can create a queue from search, random, etc.
     * @return A List<Track>.
     */
    public List<Track> getTracksForQueue(String trackMediaID, Bundle extras) {
        Uri mediaUri = Uri.parse(trackMediaID);
        String identifier = MediaHelper.extractRecordingIdentifier(mediaUri);


        if (identifier != null) {
            Recording recording = mRecordingCache.get(identifier);
            if (recording != null) {
                return recording.getTracks();
            } else {
                LogHelper.e(TAG, "Could not find a recording in the cache for " + trackMediaID);
            }
        }

        return new ArrayList<>();
    }


    public List<MediaBrowserCompat.MediaItem> getChildren(String mediaId, Resources resources) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        if (!MediaHelper.isBrowsable(mediaId)) {
            return mediaItems;
        }

        Uri mediaUri = Uri.parse(mediaId);
        RecordingUriMatcher uriMatcher = new RecordingUriMatcher();
        RecordingUrisEnum mediaUriType = uriMatcher.matchUri(mediaUri);


        switch (mediaUriType) {
            case SHOW_YEARS:
                LinkedHashMap<String, Integer> years = getYears(mediaUri);
                for (String year : years.keySet()) {
                    String subtitle = resources.getQuantityString(R.plurals.show_count, years.get(year), years.get(year));
                    mediaItems.add(MediaHelper.createMediaItem(year, subtitle));
                }
                break;
            case SHOWS_BY_YEAR:
                for (Show show : getShows(mediaUri)) {
                    mediaItems.add(MediaHelper.createMediaItem(show));
                }
                break;
            case SHOWS_BY_ID:
                for (Recording recording : getRecordings(mediaUri)) {
                    addToCache(recording);
                    mediaItems.add(MediaHelper.createMediaItem(recording));
                }
                break;
            case RECORDING_BY_ARCHIVE:

                Recording recording = getRecordingWithTracks(mediaUri);

                for (Track track : recording.getTracks()) {
                    mediaItems.add(MediaHelper.createMediaItem(track, recording));
                }
                break;
        }

        return mediaItems;
    }

    public ArrayList<Track> getTracksFromAPI(String identifier) {
        ArrayList<Track> list = new ArrayList<Track>();

        if (identifier == null) {
            return list;
        }

        ArchiveAPI api = new ArchiveAPI();
        String json = api.fetchRecordingDetails(identifier);

        Gson gson = new Gson();
        RecordingDetailsJson details = gson.fromJson(json, RecordingDetailsJson.class);

        //We'll have to manually parse the files/tracks in the json, because
        //it's an object with a bunch of child objects, and each child's key is the filepath to the track
        //and each child object contains the track info (name, play time, etc).
        //To make matters worse, not every child is an mp3 filepath. There are also xml, txt, shn, md5 files etc
        //so we have to account for all of that.
        TrackParser trackHandler;
        for (Map.Entry<String, JsonElement> entry : details.files.entrySet()) {

            String filePath = entry.getKey();

            if (getExtension(filePath).equals(".mp3")) {
                trackHandler = new TrackParser(filePath, 0, identifier);
                trackHandler.processJson(entry.getValue());
                Track track = trackHandler.getTrack();
                list.add(track);
            }
        }

        return list;
    }

    private String getExtension(String file) {
        int i = file.lastIndexOf(".");

        if (i > 0 && i < file.length() - 1) {
            return file.substring(i).toLowerCase();
        }
        return "";
    }

    /**
     * Utility method to add a recording to the memory cache of recordings
     * that the user has accessed. If the recording already exists, we'll
     * replace it, in case there is any new metadata.
     *
     * @param recording - the recording to be added to the cache
     */
    private void addToCache(Recording recording) {
        Recording existingRecording = mRecordingCache.remove(recording.getIdentifier());
        if (existingRecording != null && existingRecording.getNumberOfTracks() > 0) {
            recording.setTracks(existingRecording.getTracks());
        }

        mRecordingCache.put(recording.getIdentifier(), recording);
    }

    public void clearCache() {
        mRecordingCache.evictAll();
    }
}
