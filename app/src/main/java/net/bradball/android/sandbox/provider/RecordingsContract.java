package net.bradball.android.sandbox.provider;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.google.gson.annotations.SerializedName;

import net.bradball.android.sandbox.data.DatabaseSchema;
import net.bradball.android.sandbox.data.DatabaseSchema.RecordingsTable;
import net.bradball.android.sandbox.data.DatabaseSchema.ShowsTable;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by bradb on 12/29/15.
 */
public class RecordingsContract {

    public static final String CONTENT_AUTHORITY = "com.example.android.uamp";
    public static final String TRACK_AUTHORITY = "archive.org";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_SHOWS = "shows";
    public static final String PATH_RECORDINGS = "recordings";
    public static final String PATH_ARCHIVE_ID = "fromArchiveId";
    public static final String PATH_SHOWS_BY_DATE = "by_date";
    public static final String PATH_TRACK_DOWNLOAD = "download";

    public static final String APP_CONTENT_TYPE_NAME = "/vnd.com.example.android.uamp.";
    public static final String CONTENT_TYPE_ITEM_BASE = ContentResolver.CURSOR_ITEM_BASE_TYPE + APP_CONTENT_TYPE_NAME;
    public static final String CONTENT_TYPE_DIR_BASE = ContentResolver.CURSOR_DIR_BASE_TYPE + APP_CONTENT_TYPE_NAME;

    public enum DateFormat {
        FULL_DATE("yyyy-MM-dd"),
        MONTH("yyyy-MM"),
        YEAR("yyyy");

        public String formatString;

        DateFormat(String value) {
            formatString = value;
        }
    }

    public static final String JOIN_SHOWS_RECORDINGS = ShowsTable.NAME + " LEFT OUTER JOIN " + RecordingsTable.NAME + " ON " + ShowsTable.NAME + "." + Shows._ID + " = " + RecordingsTable.NAME + "." + Recordings.SHOW_ID;

    interface ShowColumns {

        /** Year of the show. */
        String YEAR = DatabaseSchema.ShowsTable.COLUMNS.YEAR;

        /** Date of the show. */
        String DATE = ShowsTable.COLUMNS.DATE;

        /** Location (City, State) where the show took place. */
        String LOCATION = ShowsTable.COLUMNS.LOCATION;

        /** The Setlist that was played at the show, or the show's description. */
        String SETLIST = ShowsTable.COLUMNS.SETLIST;

        /** The Recording's title (usually includes date and location). */
        String TITLE = ShowsTable.COLUMNS.TITLE;

        /** Whether or not a soundboard recording exists for this show, or if this recording is a soundboard. */
        String SOUNDBOARD = ShowsTable.COLUMNS.SOUNDBOARD;

        /** The total number of downloads for this show **/
        String DOWNLOADS = ShowsTable.COLUMNS.DOWNLOADS;
    }

    interface RecordingColumns {
        /** _ID of the show from the shows table. */
        String SHOW_ID = RecordingsTable.COLUMNS.SHOW_ID;

        /** Date of the show. */
        String DATE = RecordingsTable.COLUMNS.DATE;

        /** Location (City, State) where the show took place. */
        String LOCATION = RecordingsTable.COLUMNS.LOCATION;

        /** The Setlist that was played at the show, or the show's description. */
        String SETLIST = RecordingsTable.COLUMNS.SETLIST;

        /** The Recording's title (usually includes date and location). */
        String TITLE = RecordingsTable.COLUMNS.TITLE;

        /** Whether or this recording is a soundboard. */
        String SOUNDBOARD = RecordingsTable.COLUMNS.SOUNDBOARD;

        /** The Archive.org Identifier for this recording. */
        String IDENTIFIER = RecordingsTable.COLUMNS.IDENTIFIER;

        /** How many reviews of this recording have been submitted. */
        String NUM_REVIEWS = RecordingsTable.COLUMNS.NUM_REVIEWS;

        /** The average rating given to this recording. */
        String RATING = RecordingsTable.COLUMNS.RATING;

        /** How many times this recording has been downloaded. */
        String DOWNLOADS = RecordingsTable.COLUMNS.DOWNLOADS;

        /** The username of the person who submitted this recording. */
        String PUBLISHER = RecordingsTable.COLUMNS.PUBLISHER;

        /** Whether or not this recording is to be downloaded and available offline */
        String AVAILABLE_OFFLINE = RecordingsTable.COLUMNS.AVAILABLE_OFFLINE;

        /** Source information from the recording (i.e. it's lineage) **/
        String SOURCE = RecordingsTable.COLUMNS.SOURCE;
    }


    /**
     * A static class that can be used to build valid URI's for show data
     * The returned URI's can be used to make queries to the RecordingsProvider
     * for show data.
     *
     * Possible URI Paths:
     *
     * Path: /shows
     * Returns: A directory of all shows
     **
     * Path: /shows/YYYY-MM-DD
     * Returns: A directory of all shows that match the given date.
     *          Note that a partial date (Just a year, or a year and month)
     *          can be provided, and all shows that match will be returned.
     *          For example, /shows/by_date/1978 will return all shows in 1978.
     *
     * Path: /shows/ID
     * Returns: An item with data for the show who's _id field matches the given ID
     *
     * Path: /shows/ID/recordings
     * Returns: A directory of recordings that match the given show id.
     *
     */
    public static class Shows implements ShowColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_SHOWS).build();
        public static final Uri SHOW_YEARS_URI = CONTENT_URI.buildUpon().appendPath(PATH_SHOWS_BY_DATE).build();

        public static final String CONTENT_TYPE_ID = "show";

        public static final String[] PROJECTION = {
                RecordingsContract.Shows._ID,
                RecordingsContract.Shows.YEAR,
                RecordingsContract.Shows.DATE,
                RecordingsContract.Shows.DOWNLOADS,
                RecordingsContract.Shows.LOCATION,
                RecordingsContract.Shows.SETLIST,
                RecordingsContract.Shows.SOUNDBOARD,
                RecordingsContract.Shows.TITLE,
                "count(" + DatabaseSchema.RecordingsTable.NAME + "." + RecordingsContract.Recordings.SHOW_ID + ") AS " + BaseColumns._COUNT
        };

        /**
         * Build a URI to get a show based on it's ID
         *
         * Path will be: /shows/ID
         */
        public static Uri buildShowUri(long showID) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(showID)).build();
        }

        /** Build a Uri for a directory of recordings based on the show id */
        public static Uri buildShowRecordingsUri(long showID) {
            return buildShowRecordingsUri(buildShowUri((showID)));
        }

        public static Uri buildShowRecordingsUri(Uri showUri) {
            return showUri.buildUpon().appendPath(PATH_RECORDINGS).build();
        }

        public static Uri buildShowsByDateUri(String date) {
            return SHOW_YEARS_URI.buildUpon().appendPath(date).build();
        }


        /**
         * Get the show date as a string (yyyy-mm-dd) from a uri.
         *
         */
        public static String getShowIdentifier(Uri uri) {
            List<String> segments = uri.getPathSegments();
            if (segments.size() > 1)
                return segments.get(1);
            else
                return null;
        }

        public static String getShowId(Uri uri) {
            List<String> segments = uri.getPathSegments();
            try {
                //Parse the segment into a long so we can be sure it's an ID,
                //the convert it back to a string.
                long showId = Long.parseLong(segments.get(segments.size() -1));
                return Long.toString(showId);
            } catch (Exception ex){
                return null;
            }
        }

        public static String getShowDate(Uri uri) {
            List<String> segments = uri.getPathSegments();
            int index = segments.indexOf(PATH_SHOWS_BY_DATE) + 1;

            if (segments.size() >= index + 1) {
                return segments.get(index);
            }

            return null;
        }
    }



    public static class Track {
        public static final Uri CONTENT_URI = Uri.parse("http://" + TRACK_AUTHORITY);


        public static Uri buildUri(String recordingIdentifier, String filename) {
            return CONTENT_URI.buildUpon()
                    .appendPath(PATH_TRACK_DOWNLOAD)
                    .appendPath(recordingIdentifier)
                    .appendPath(filename)
                    .build();
        }
    }

    /**
     * A static class that can be used to build valid URI's for recording data.
     * The returned URI's can be used to make queries to the RecordingsProvider
     * for recording data.
     *
     * Possible URI Paths:
     *
     * Path: /recordings
     * Returns: A directory of all recordings  (THIS WILL BE BIG!)
     **
     * Path: /recordings/IDENTIFIER
     * Returns: A recording item for the given archive.org identifier
     *
     * Path: /recordings/ID
     * Returns: An item with data for the recording who's _id field matches the given ID
     *
     */
    public static class Recordings implements RecordingColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_RECORDINGS).build();
        public static final String CONTENT_TYPE_ID = "recording";

        public static final String[] PROJECTION = {
                RecordingsContract.Recordings._ID,
                RecordingsContract.Recordings.IDENTIFIER,
                RecordingsContract.Recordings.DATE,
                RecordingsContract.Recordings.DOWNLOADS,
                RecordingsContract.Recordings.LOCATION,
                RecordingsContract.Recordings.NUM_REVIEWS,
                RecordingsContract.Recordings.RATING,
                RecordingsContract.Recordings.SHOW_ID,
                RecordingsContract.Recordings.SOUNDBOARD,
                RecordingsContract.Recordings.TITLE,
                RecordingsContract.Recordings.SETLIST,
                RecordingsContract.Recordings.PUBLISHER,
                RecordingsContract.Recordings.SOURCE,
                RecordingsContract.Recordings.AVAILABLE_OFFLINE
        };

        /** Build a Uri for the requested recording, based on its Archive.org Identifier. */
        public static Uri buildRecordingUri(String archiveIdentifier) {
            return CONTENT_URI.buildUpon().appendPath(PATH_ARCHIVE_ID).appendPath(archiveIdentifier).build();
        }

        /** Build a Uri for the requested recording, based on its database _ID. */
        public static Uri buildRecordingUri(long recordingID) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(recordingID)).build();
        }

        public static Uri buildRandomRecordingUri() {
            return CONTENT_URI.buildUpon().appendPath("random").build();
        }


        public static String getRecordingArchiveID(Uri uri) {
            List<String> segments = uri.getPathSegments();
            if (segments.size() > 2)
                return segments.get(2);
            else
                return null;
        }

        public static String getRecordingID(Uri uri) {
            List<String> segments = uri.getPathSegments();
            if (segments.size() > 1)
                return segments.get(1);
            else
                return null;
        }
    }

    public static String makeContentType(String id, boolean isItem) {
        if (id == null)
            return null;

        if (isItem)
            return CONTENT_TYPE_ITEM_BASE + id;
        else
            return CONTENT_TYPE_DIR_BASE + id;
    }

    public static String formatRecordingDate(LocalDate recordingDate, DateFormat dateFormat) {
        if (recordingDate == null)
            return "";
        DateTimeFormatter fmt = DateTimeFormat.forPattern(dateFormat.formatString);
        return fmt.print(recordingDate);
    }

    public static LocalDate parseRecordingDate(String dateStr) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern(DateFormat.FULL_DATE.formatString);
        return fmt.parseLocalDate(dateStr);
    }

    private RecordingsContract() {}
}