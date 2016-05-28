package net.bradball.android.sandbox.data;

public class DatabaseSchema {

    public static final class RecordingsTable {
        public static final String NAME = "recordings";

        public static final class COLUMNS {
            public static final String SHOW_ID = "show_id";
            public static final String DATE = "showDate";
            public static final String NUM_REVIEWS = "numReviews";
            public static final String LOCATION = "location";
            public static final String SETLIST = "setlist";
            public static final String DOWNLOADS = "downloads";
            public static final String IDENTIFIER = "identifier";
            public static final String TITLE = "title";
            public static final String RATING = "rating";
            public static final String PUBLISHER = "publisher";
            public static final String SOUNDBOARD = "soundboard";
            public static final String AVAILABLE_OFFLINE = "available_offline";
            public static final String SOURCE = "source";
        }
    }

    public static final class ShowsTable {
        public static final String NAME = "shows";

        public static final class COLUMNS {
            public static final String YEAR = "year";
            public static final String DATE = "showDate";
            public static final String LOCATION = "location";
            public static final String SETLIST = "setlist";
            public static final String DOWNLOADS = "downloads";
            public static final String TITLE = "title";
            public static final String SOUNDBOARD = "soundboard";
        }
    }

}