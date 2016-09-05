package net.bradball.android.sandbox.network;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import net.bradball.android.sandbox.util.LogHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ArchiveAPI {
    private final static String TAG = LogHelper.makeLogTag(ArchiveAPI.class);

    private final static Uri BASE_URI = Uri.parse("http://archive.org/");
    private final static String BASE_TRACK_URL = BASE_URI.toString() + "download/";
    private final static Uri RECORDING_DETAIL_ENDPOINT = BASE_URI.buildUpon()
            .appendPath("details")
            .build();

    private final static Uri RECORDINGS_ENDPOINT = BASE_URI.buildUpon()
            .appendPath("services")
            .appendPath("search")
            .appendPath("beta")
            .appendPath("scrape.php")
            .build();
    private final static String SORT = "date desc";
    private final static String CREATOR = "Grateful Dead";
    private final static String ARTIST_COLLECTION = "GratefulDead";
    public final static String SOUNDBOARD_COLLECTION = "stream_only";
    public final static int FETCH_ROWS = 2000;
    public final static String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public static final class RECORDING_DETAIL_FIELDS {
        public static final class FILE_FILEDS {
            public final static String TITLE = "title";
            public final static String NUMBER = "track";
            public final static String ALBUM = "album";
            public final static String BITRATE = "bitrate";
            public final static String LENGTH = "length";
            public final static String FORMAT = "format";
            public final static String SIZE = "size";
            public final static String MD5 = "md5";
        }

        public static final class REVIEW_FIELDS {
            public final static String ID = "review_id";
            public final static String BODY = "reviewbody";
            public final static String TITLE = "reviewtitle";
            public final static String REVIEWER = "reviewer";
            public final static String DATE = "reviewdate";
            public final static String STARS = "stars";

        }
    }

    public static final class RECORDING_FIELDS {
        public final static String RATING = "avg_rating";
        public final static String COLLECTION = "collection";
        public final static String COVERAGE = "coverage";
        public final static String DATE = "date";
        public final static String DESCRIPTION = "description";
        public final static String DOWNLOADS = "downloads";
        public final static String IDENTIFIER = "identifier";
        public final static String REVIEWS = "num_reviews";
        public final static String PUBLISHER = "publisher";
        public final static String SOURCE = "source";
        public final static String TITLE = "title";
        public final static String CREATOR = "creator";
        public final static String FORMAT = "format";
        public final static String INDEX_DATE = "indexdate";
        public final static String OAI_UPDATE = "oai_update";

        public static ArrayList<String> asList() {
            ArrayList<String> list = new ArrayList<String>();
            list.add(RATING);
            list.add(COLLECTION);
            list.add(COVERAGE);
            list.add(DATE);
            list.add(DESCRIPTION);
            list.add(DOWNLOADS);
            list.add(IDENTIFIER);
            list.add(REVIEWS);
            list.add(PUBLISHER);
            list.add(SOURCE);
            list.add(TITLE);

            return list;
        }
    }

    public static String getTrackUrl(String recordingIdentifier, String filename) {
        if (!filename.startsWith("/")) {
            filename = "/" + filename;
        }
        return BASE_TRACK_URL + recordingIdentifier + filename;
    }

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        LogHelper.d(TAG, "Fetching URL: ", urlSpec);
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0)
                out.write(buffer, 0, bytesRead);

            out.close();
            return out.toByteArray();

        } finally {
            connection.disconnect();
        }

    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }


    public String fetchAllShows(String cursor) {
        return fetchShows(null, cursor);
    }

    public String fetchShows(Date lastUpdate, String cursor) {
        String url = buildShowsUrl(lastUpdate, cursor);
        String json = null;
        try {
            json = getUrlString(url);
        } catch (IOException ex) {
            LogHelper.e(TAG, "Failed to fetch shows from URL: " + url, ex);
        }

        return json;
    }

    public String fetchRecordingDetails(String recordingIdentifier) {
        String url = buildDetailUrl(recordingIdentifier);
        String json = null;
        try {
            json = getUrlString(url);
        } catch (IOException ex) {
            LogHelper.e(TAG, "Failed to fetch shows from URL: " + url, ex);
        }

        return json;
    }

    private String buildDetailUrl(String identifier) {
        return RECORDING_DETAIL_ENDPOINT.buildUpon()
                .appendPath(identifier)
                .appendQueryParameter("output", "json")
                .build().toString();
    }

    private String buildShowsUrl(String cursor) {
        return buildShowsUrl(null, cursor);
    }

    private String buildShowsUrl(Date changesSince, String cursor) {
        Uri.Builder uriBuilder = getUriBuilder(cursor);
        StringBuilder query = getQueryBuilder();

        if (changesSince != null) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT, Locale.US);
            query.append(" AND ").append(getQueryRange(RECORDING_FIELDS.INDEX_DATE, dateFormatter.format(changesSince), null));
        }
        uriBuilder.appendQueryParameter("q", query.toString());

        return uriBuilder.build().toString();
    }


    private String getQueryItem(String key, String value) {
        return key + ":(" + value + ")";
    }

    private String getQueryRange(String key, String from, String to) {

        from = (TextUtils.isEmpty(from)) ? "*" : from;
        to = (TextUtils.isEmpty(to)) ? "*" : to;

        return key + ":[" + from + " TO " + to + "]";
    }

    private StringBuilder getQueryBuilder() {
        StringBuilder query = new StringBuilder();

        query.append(getQueryItem(RECORDING_FIELDS.CREATOR, CREATOR));
        query.append(" AND ");
        query.append(getQueryItem(RECORDING_FIELDS.COLLECTION, ARTIST_COLLECTION));
        query.append(" AND ");
        query.append(getQueryItem(RECORDING_FIELDS.FORMAT, "MP3"));

        return query;
    }

    private Uri.Builder getUriBuilder(String cursor) {
        Uri.Builder uriBuilder = RECORDINGS_ENDPOINT.buildUpon();


        uriBuilder.appendQueryParameter("sorts", SORT);
        uriBuilder.appendQueryParameter("size", Integer.toString(FETCH_ROWS));
        uriBuilder.appendQueryParameter("fields", TextUtils.join(",", RECORDING_FIELDS.asList()));

        if (!TextUtils.isEmpty(cursor)) {
            uriBuilder.appendQueryParameter("cursor", cursor);
        }

        return uriBuilder;
    }


}