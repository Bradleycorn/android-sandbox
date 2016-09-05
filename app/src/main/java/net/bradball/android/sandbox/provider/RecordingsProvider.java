package net.bradball.android.sandbox.provider;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import net.bradball.android.sandbox.data.DatabaseHelper;
import net.bradball.android.sandbox.data.DatabaseSchema;
import net.bradball.android.sandbox.util.LogHelper;

import java.util.ArrayList;

public class RecordingsProvider extends ContentProvider {
    private static final String TAG = LogHelper.makeLogTag(RecordingsProvider.class);
    private DatabaseHelper mDatabaseHelper;
    private RecordingUriMatcher mRecordingUriMatcher;
    private Context mContext;

    @Override
    public boolean onCreate() {
        mContext = getContext();
        mDatabaseHelper = new DatabaseHelper(getContext());
        mRecordingUriMatcher = new RecordingUriMatcher();
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] columns, String where, String[] whereArgs, String sortOrder) {
        final SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();

        final SelectionBuilder builder = createBaseQuery(uri);

        if (where != null) {
            builder.where(where, whereArgs);
        }

        Cursor cursor = builder.query(db, false, columns, sortOrder, null);
        cursor.setNotificationUri(mContext.getContentResolver(), uri);

        return cursor;
    }

    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {

        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        //db.beginTransaction();
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                ContentProviderOperation operation = operations.get(i);
                results[i] = operation.apply(this, results, i);
            }
            //db.setTransactionSuccessful();
            return results;
        } finally {
            //db.endTransaction();
        }
    }


    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        LogHelper.v(TAG, "insert(uri=", uri, ", values=", values.toString(), ")");
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        long rowId;

        RecordingUrisEnum uriEnum = mRecordingUriMatcher.matchUri(uri);
        if (uriEnum.table == null) {
            throw new IllegalArgumentException("Given uri does not support insert: " + uri);
        }

        rowId = db.insertOrThrow(uriEnum.table, null, values);

        if (rowId > 0) {
            notifyChange(uri);
        }

        switch (uriEnum) {
            case SHOWS:
            case SHOWS_BY_ID:
                return RecordingsContract.Shows.buildShowUri(rowId);
            case RECORDINGS:
                return RecordingsContract.Recordings.buildRecordingUri(rowId);
            default: {
                throw new IllegalArgumentException("Unknown insert uri: " + uri);
            }
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        SelectionBuilder builder = createBaseQuery(uri);
        RecordingUrisEnum uriEnum = mRecordingUriMatcher.matchUri(uri);
        String id;

        builder.where(selection, selectionArgs);

        switch(uriEnum) {
            case SHOWS_BY_ID:
                id = RecordingsContract.Shows.getShowId(uri);
                builder.where(RecordingsContract.Shows._ID + " = ?", id);
                break;

            case RECORDING_BY_ID:
                id = RecordingsContract.Recordings.getRecordingID(uri);
                builder.where(RecordingsContract.Recordings._ID + " = ?", id);
                break;

            case RECORDING_BY_ARCHIVE:
                id = RecordingsContract.Recordings.getRecordingArchiveID(uri);
                builder.where(RecordingsContract.Recordings.IDENTIFIER + " = ?", id);
                break;
        }

        int rowsChanged = builder.update(db, values);
        if (rowsChanged > 0)
            notifyChange(uri);
        return rowsChanged;

    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        SelectionBuilder builder = createBaseQuery(uri);

        int rowsDeleted = builder.where(selection, selectionArgs).delete(db);
        if (rowsDeleted > 0)
            notifyChange(uri);
        return rowsDeleted;
    }


    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        RecordingUrisEnum matchingUriEnum = mRecordingUriMatcher.matchUri(uri);
        return matchingUriEnum.contentType;
    }

    private void notifyChange(Uri uri) {
        mContext.getContentResolver().notifyChange(uri, null, false);

        // Widgets can't register content observers so we refresh widgets separately.
        //context.sendBroadcast(ScheduleWidgetProvider.getRefreshBroadcastIntent(context, false));
    }




    private SelectionBuilder createBaseQuery(Uri uri) {
        RecordingUrisEnum uriEnum = mRecordingUriMatcher.matchUri(uri);
        SelectionBuilder builder = new SelectionBuilder();

        LogHelper.d(TAG, "creating Query for URI: ", uriEnum);

        switch(uriEnum) {
            case SHOWS:
                builder.table(DatabaseSchema.ShowsTable.NAME);
                break;

            case SHOWS_BY_ID:
                builder.table(DatabaseSchema.ShowsTable.NAME)
                        .where(RecordingsContract.Shows._ID + "=?", RecordingsContract.Shows.getShowIdentifier(uri));
                break;

            case SHOW_RECORDINGS:
                builder.table(DatabaseSchema.RecordingsTable.NAME)
                        .where(RecordingsContract.Recordings.SHOW_ID + "=?", RecordingsContract.Shows.getShowIdentifier(uri));
                break;

            case SHOW_YEARS:
                builder.table(DatabaseSchema.ShowsTable.NAME)
                        .map(RecordingsContract.Shows._COUNT, "count(" + RecordingsContract.Shows._ID + ")")
                        .groupBy(RecordingsContract.Shows.YEAR);
                break;

            case SHOWS_BY_YEAR:
            case SHOWS_BY_DATE:

                builder.table(RecordingsContract.JOIN_SHOWS_RECORDINGS)
                        .mapToTable(RecordingsContract.Shows.DATE, DatabaseSchema.ShowsTable.NAME)
                        .mapToTable(RecordingsContract.Shows.DOWNLOADS, DatabaseSchema.ShowsTable.NAME)
                        .mapToTable(RecordingsContract.Shows.LOCATION, DatabaseSchema.ShowsTable.NAME)
                        .mapToTable(RecordingsContract.Shows.SETLIST, DatabaseSchema.ShowsTable.NAME)
                        .mapToTable(RecordingsContract.Shows.SOUNDBOARD, DatabaseSchema.ShowsTable.NAME)
                        .mapToTable(RecordingsContract.Shows.TITLE, DatabaseSchema.ShowsTable.NAME)
                        .mapToTable(RecordingsContract.Shows._ID, DatabaseSchema.ShowsTable.NAME)
                        .groupBy(DatabaseSchema.ShowsTable.NAME + "." + RecordingsContract.Shows._ID);
                break;


            case RECORDINGS:
            case RECORDING_RANDOM:
                builder.table(DatabaseSchema.RecordingsTable.NAME);
                break;

            case RECORDING_BY_ARCHIVE:
                builder.table(DatabaseSchema.RecordingsTable.NAME)
                        .where(RecordingsContract.Recordings.IDENTIFIER + "=?", RecordingsContract.Recordings.getRecordingArchiveID(uri));
                break;

            case RECORDING_BY_ID:
                builder.table(DatabaseSchema.RecordingsTable.NAME)
                        .where(RecordingsContract.Recordings._ID + "=?", RecordingsContract.Recordings.getRecordingID(uri));
                break;

        }

        return builder;
    }
}
