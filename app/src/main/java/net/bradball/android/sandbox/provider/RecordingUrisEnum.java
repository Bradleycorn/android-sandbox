package net.bradball.android.sandbox.provider;

import net.bradball.android.sandbox.data.DatabaseSchema;

public enum RecordingUrisEnum {
    /*  When Adding URI's, if the value for the table field is set,
        Then the URI will support insert operations.

        if the table value is null, then the URI does NOT support inserts.

        Fields:  CODE, PATH, Content Type ID, is Item, table

     */
    SHOWS(          100, RecordingsContract.PATH_SHOWS,                                                         RecordingsContract.Shows.CONTENT_TYPE_ID,       false,  DatabaseSchema.ShowsTable.NAME),
    SHOWS_BY_ID(    101, RecordingsContract.PATH_SHOWS + "/#",                                                  RecordingsContract.Shows.CONTENT_TYPE_ID,       true,   DatabaseSchema.ShowsTable.NAME),
    SHOW_RECORDINGS(102, RecordingsContract.PATH_SHOWS + "/#/" + RecordingsContract.PATH_RECORDINGS,            RecordingsContract.Recordings.CONTENT_TYPE_ID,  false,  null),
    SHOW_YEARS(     103, RecordingsContract.PATH_SHOWS + "/" + RecordingsContract.PATH_SHOWS_BY_DATE,           RecordingsContract.Shows.CONTENT_TYPE_ID,       false,  null),
    SHOWS_BY_YEAR(  104, RecordingsContract.PATH_SHOWS + "/" + RecordingsContract.PATH_SHOWS_BY_DATE + "/#",    RecordingsContract.Shows.CONTENT_TYPE_ID,       false,  null),
    SHOWS_BY_DATE(  104, RecordingsContract.PATH_SHOWS + "/" + RecordingsContract.PATH_SHOWS_BY_DATE + "/*",    RecordingsContract.Shows.CONTENT_TYPE_ID,       false,  null),

    RECORDINGS(             200, RecordingsContract.PATH_RECORDINGS,                                                    RecordingsContract.Recordings.CONTENT_TYPE_ID, false,   DatabaseSchema.RecordingsTable.NAME),
    RECORDING_BY_ARCHIVE(   201, RecordingsContract.PATH_RECORDINGS + "/" + RecordingsContract.PATH_ARCHIVE_ID + "/*",  RecordingsContract.Recordings.CONTENT_TYPE_ID, true,    null),
    RECORDING_BY_ID(        202, RecordingsContract.PATH_RECORDINGS + "/#",                                             RecordingsContract.Recordings.CONTENT_TYPE_ID, true,    DatabaseSchema.RecordingsTable.NAME),
    RECORDING_RANDOM(       203, RecordingsContract.PATH_RECORDINGS + "/random",                                        RecordingsContract.Recordings.CONTENT_TYPE_ID, true,    null),

    TRACK(                  301, RecordingsContract.PATH_TRACK_DOWNLOAD + "/*/*.mp3",                                                                   RecordingsContract.Recordings.CONTENT_TYPE_ID, true,    null);

    public int code;
    public String path;
    public String contentType;
    public String table;


    RecordingUrisEnum(int code, String path, String contentTypeId, boolean isItem, String table) {
        this.code = code;
        this.path = path;
        this.contentType = RecordingsContract.makeContentType(contentTypeId, isItem);
        this.table = table;
    }
}
