package net.bradball.android.sandbox.data;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import net.bradball.android.sandbox.model.Recording;
import net.bradball.android.sandbox.network.ArchiveAPI;
import net.bradball.android.sandbox.provider.RecordingsContract;
import net.bradball.android.sandbox.model.Show;
import net.bradball.android.sandbox.util.LogHelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import org.joda.time.LocalDate;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class RecordingParser extends JSONParser implements JsonDeserializer<Recording> {
    private static final String TAG = LogHelper.makeLogTag(RecordingParser.class);
    private LinkedHashMap<LocalDate, Show> mShows = new LinkedHashMap<LocalDate, Show>();
    private ContentResolver mContentResolver;

    public RecordingParser(ContentResolver cr) {
        super();
        mContentResolver = cr;
    }

    public Recording deserialize(JsonElement json, Type typeOfSrc, JsonDeserializationContext context) throws JsonParseException {
        final Recording recording;

        setDeserializationContext(context, json.getAsJsonObject());

        String identifier = getValue(ArchiveAPI.RECORDING_FIELDS.IDENTIFIER, "");
        ArrayList<String> publishers = getStringArray(ArchiveAPI.RECORDING_FIELDS.PUBLISHER);
        ArrayList<String> collections = getStringArray(ArchiveAPI.RECORDING_FIELDS.COLLECTION);
        LocalDate date = getDate(ArchiveAPI.RECORDING_FIELDS.DATE);
        String location = getValue(ArchiveAPI.RECORDING_FIELDS.COVERAGE, "");
        String title = getValue(ArchiveAPI.RECORDING_FIELDS.TITLE, "");
        String setlist = getValue(ArchiveAPI.RECORDING_FIELDS.DESCRIPTION, "");
        Float rating = getValue(ArchiveAPI.RECORDING_FIELDS.RATING, 0.0f);
        int reviews = getValue(ArchiveAPI.RECORDING_FIELDS.REVIEWS, 0);
        int downloads = getValue(ArchiveAPI.RECORDING_FIELDS.DOWNLOADS, 0);
        String publisher = TextUtils.join(", ", publishers);
        boolean soundboard = (collections.contains(ArchiveAPI.SOUNDBOARD_COLLECTION));
        String source = getValue(ArchiveAPI.RECORDING_FIELDS.SOURCE, "");

        title = title.replace("Grateful Dead Live at", "").replace("on " + RecordingsContract.formatRecordingDate(date, RecordingsContract.DateFormat.FULL_DATE), "").trim();

        recording = new Recording(identifier);
        recording.setDate(date);
        recording.setLocation(location);
        recording.setTitle(title);
        recording.setSetlist(setlist);
        recording.setRating(rating);
        recording.setNumReviews(reviews);
        recording.setDownloads(downloads);
        recording.setPublisher(publisher);
        recording.setSoundboard(soundboard);
        recording.setSource(source);

        return recording;
    }

    @Override
    public void processJson(JsonElement element) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Recording.class, this);
        Gson gson = gsonBuilder.create();
        Show show;

        element = element.getAsJsonArray();

        Recording[] recordings = gson.fromJson(element, Recording[].class);

        for(Recording recording : recordings) {

            if (recording.getDate() != null) {

                if (mShows.containsKey(recording.getDate())) {
                    mShows.get(recording.getDate()).addRecording(recording);
                } else {
                    show = new Show(recording.getDate());
                    show.setLocation(recording.getLocation());
                    show.setSetlist(recording.getSetlist());
                    show.setTitle(recording.getTitle());

                    show.addRecording(recording);

                    mShows.put(show.getDate(), show);
                }

            }
        }
    }

    @Override
    public void getContentProviderInserts(ArrayList<ContentProviderOperation> list) {
        long showID;
        int insertIndex;
        HashMap<LocalDate, Long> existingShows = loadShowDates();
        HashMap<String, Long> existingRecordings = loadRecordingDates();
        boolean incrementalUpdate = (existingShows != null && existingRecordings.size() > 0);

        for(Show show : mShows.values()) {

            // add or update
            boolean isNew = !incrementalUpdate || !existingShows.containsKey(show.getDate());
            ContentProviderOperation.Builder builder;
            if (isNew) {
                builder = ContentProviderOperation.newInsert(RecordingsContract.Shows.CONTENT_URI);
                showID = 0;
                insertIndex = list.size();
            } else {
                showID = existingShows.get(show.getDate());
                builder = ContentProviderOperation.newUpdate(RecordingsContract.Shows.buildShowUri(showID));
                insertIndex = -1; //Just to be safe
            }

            builder.withValues(getShowContentValues(show, isNew));
            list.add(builder.build());

            for (Recording recording : show.getRecordings()) {

                isNew = (!incrementalUpdate || !existingRecordings.containsKey(recording.getIdentifier()));

                if (isNew) {
                    recording.setAvailableOffline(false);
                    builder = ContentProviderOperation.newInsert(RecordingsContract.Recordings.CONTENT_URI);
                    if (insertIndex >= 0) {
                        builder.withValueBackReference(RecordingsContract.Recordings.SHOW_ID, insertIndex);
                        showID = 0; //Just to be safe
                    }
                } else {
                    builder = ContentProviderOperation.newUpdate(RecordingsContract.Recordings.buildRecordingUri(existingRecordings.get(recording.getIdentifier())));
                }

                builder.withValues(getRecordingContentValues(recording, showID));
                list.add(builder.build());
            }
        }
    }

    private ContentValues getShowContentValues(Show show, boolean isNew) {
        ContentValues values = new ContentValues();

        values.put(RecordingsContract.Shows.YEAR, show.getYear());
        values.put(RecordingsContract.Shows.DATE, RecordingsContract.formatRecordingDate(show.getDate(), RecordingsContract.DateFormat.FULL_DATE));
        values.put(RecordingsContract.Shows.DOWNLOADS, show.getDownloads());
        values.put(RecordingsContract.Shows.LOCATION, show.getLocation());
        values.put(RecordingsContract.Shows.SETLIST, show.getSetlist());
        values.put(RecordingsContract.Shows.TITLE, show.getTitle());

        /*
            If it's a new show, we can always set the soundboard value.
            When updating a show, we can't be sure if it's a soundboard or
            not, based on the recordings returned from the api, as it won't
            be a complete list of recordings, only those with updates.
            So, if one of the updates is a soundboard, then we can mark the
            show as a soundboard. Otherwise, we just won't update the field.
        */
        if (isNew || show.isSoundboard()) {
            values.put(RecordingsContract.Shows.SOUNDBOARD, show.isSoundboard());
        }

        return values;
    }

    private ContentValues getRecordingContentValues(Recording recording, long showID) {
        ContentValues values = new ContentValues();

        if (showID > 0) {
            values.put(RecordingsContract.Recordings.SHOW_ID, showID);
        }
        values.put(RecordingsContract.Recordings.IDENTIFIER, recording.getIdentifier());
        values.put(RecordingsContract.Recordings.DATE, RecordingsContract.formatRecordingDate(recording.getDate(), RecordingsContract.DateFormat.FULL_DATE));
        values.put(RecordingsContract.Recordings.LOCATION, recording.getLocation());
        values.put(RecordingsContract.Recordings.TITLE, recording.getTitle());
        values.put(RecordingsContract.Recordings.SOUNDBOARD, recording.isSoundboard());
        values.put(RecordingsContract.Recordings.PUBLISHER, recording.getPublisher());
        values.put(RecordingsContract.Recordings.SETLIST, recording.getSetlist());
        values.put(RecordingsContract.Recordings.RATING, recording.getRating());
        values.put(RecordingsContract.Recordings.NUM_REVIEWS, recording.getNumReviews());
        values.put(RecordingsContract.Recordings.DOWNLOADS, recording.getDownloads());

        return values;
    }


    private HashMap<LocalDate, Long> loadShowDates() {

        Cursor cursor = mContentResolver.query(RecordingsContract.Shows.CONTENT_URI, ShowDatesQuery.PROJECTION,null,null,null);
        try {
            if (cursor == null || cursor.getCount() < 1) {
                return null;
            }

            HashMap<LocalDate, Long> showDates = new HashMap<>();
            if (cursor.moveToFirst()) {
                do {
                    Long id = cursor.getLong(ShowDatesQuery._ID);
                    LocalDate date = RecordingsContract.parseRecordingDate(cursor.getString(ShowDatesQuery.DATE));
                    showDates.put(date,id);
                } while (cursor.moveToNext());
            }
            return showDates;
        } finally {
            if (cursor !=null) {
                cursor.close();
            }
        }
    }

    private HashMap<String, Long> loadRecordingDates() {

        Cursor cursor = mContentResolver.query(RecordingsContract.Recordings.CONTENT_URI, RecordingDatesQuery.PROJECTION,null,null,null);
        try {
            HashMap<String, Long> recordingDates = new HashMap<>();
            if (cursor == null || cursor.getCount() < 1) {
                return recordingDates;
            }

            if (cursor.moveToFirst()) {
                do {
                    long id = Long.parseLong(cursor.getString(RecordingDatesQuery._ID));
                    String identifier = cursor.getString(RecordingDatesQuery.IDENTIFIER);
                    recordingDates.put(identifier, id);
                } while (cursor.moveToNext());
            }
            return recordingDates;
        } finally {
            if (cursor !=null) {
                cursor.close();
            }
        }
    }

    private interface ShowDatesQuery {
        String[] PROJECTION = {
                RecordingsContract.Shows._ID,
                RecordingsContract.Shows.DATE
        };

        int _ID = 0;
        int DATE = 1;
    }

    private interface RecordingDatesQuery {
        String[] PROJECTION = {
                RecordingsContract.Recordings._ID,
                RecordingsContract.Recordings.IDENTIFIER
        };

        int _ID = 0;
        int IDENTIFIER = 1;
    }
}