package net.bradball.android.sandbox.sync;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import net.bradball.android.sandbox.data.RecordingParser;
import net.bradball.android.sandbox.data.jsonModel.RecordingsListJson;
import net.bradball.android.sandbox.network.ArchiveAPI;
import net.bradball.android.sandbox.provider.RecordingsContract;
import net.bradball.android.sandbox.util.LogHelper;

import java.util.ArrayList;
import java.util.Date;

/**
 * A sync adapter for syncing data from Archive.org
 *
 * <p>This class is instantiated in {@link net.bradball.android.sandbox.service.ArchiveOrgSyncService}, which also binds SyncAdapter to the system.
 * SyncAdapter should only be initialized in SyncService, never anywhere else.
 *
 * <p>The system calls onPerformSync() via an RPC call through the IBinder object supplied by
 * SyncService.
 */
public class ArchiveOrgSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = LogHelper.makeLogTag(ArchiveOrgSyncAdapter.class);

    private final ContentResolver mContentResolver;
    private final Context mContext;

    public ArchiveOrgSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        mContext = context;
        mContentResolver = context.getContentResolver();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public ArchiveOrgSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mContext = context;
        mContentResolver = context.getContentResolver();
    }

    /**
     * onPerformSync handles the actual sync with the server. It mainly consists of a loop that
     * makes requests the Archive.org API via {@code ArchiveAPI}, getting results one page at a time
     * until it has processed all pages.
     *
     * The database is updated several times (once with each page of results) on purpose, so that in
     * turn we can tell the content provider to notify observers that the data has changed as soon
     * as the first page is processed. That way, when there are many pages of results that need
     * to be processed (like on the initial population of the db), the UI can go ahead and start
     * showing some shows without having to wait for 11 plus pages of results to be queried and
     * processed.
     *
     *  There are several tasks to be performed
     *
     */
    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        LogHelper.i(TAG, "Syncing with Archive.org");
        //Pull and store the date of the last update.
        Date lastUpdate = SyncHelper.getLastUpdate(mContext);

        //Create some variables to handle multiple pages of results, and looping through them.
        //As well as for the json we'll be processing.
        String archiveCursor = null;
        String json;
        RecordingsListJson recordingsList;
        int itemsLeft = 0;


        //The ArchiveAPI class has methods for actually getting data from the web,
        //so get a handle to that, along with a Gson object and a RecordingParser
        //to process the returned string of JSON.
        ArchiveAPI archiveAPI = new ArchiveAPI();
        Gson gson = new Gson();

        //Finally, we're ready for action.
        //Start looping ....
        do {
            LogHelper.d(TAG, "Fetching Data from Network");

            //Fetch a page of results from the api
            json = archiveAPI.fetchShows(lastUpdate, archiveCursor);

            //process the returned json
            recordingsList = gson.fromJson(json, RecordingsListJson.class);
            archiveCursor = recordingsList.cursor;

            //The "total" returned from the API is how many total items
            //there are from this request forward, INCLUDING the items in this request
            //So the number of items we have left to fetch is the total minus
            //the number of items in this request (the "count").
            itemsLeft = recordingsList.total - recordingsList.count;

            new RecordingsHandler().execute(recordingsList);
        } while (!TextUtils.isEmpty(archiveCursor) && itemsLeft > 0); //keep looping until we've processed all pages


        //Now that we're done, update the shared preference that stores the date of the last update
        SyncHelper.setLastUpdate(mContext, new Date());
    }

    private class RecordingsHandler extends AsyncTask<RecordingsListJson, Void, RecordingsListJson> {

        @Override
        protected RecordingsListJson doInBackground(RecordingsListJson... params) {
            RecordingsListJson json = params[0];

            RecordingParser recordingParser = new RecordingParser(mContentResolver);
            ArrayList<ContentProviderOperation> inserts = new ArrayList<>();

            recordingParser.processJson(json.items);
            recordingParser.getContentProviderInserts(inserts);

            try {
                int rows = inserts.size();
                mContentResolver.applyBatch(RecordingsContract.CONTENT_AUTHORITY, inserts);
                LogHelper.d(TAG, "Inserted a set of data: ", rows, " rows");
            } catch (RemoteException ex) {
                LogHelper.e(TAG, "RemoteException while applying content provider operations.");
                //TODO: A notification that there was an error?
            } catch (OperationApplicationException ex) {
                LogHelper.e(TAG, "OperationApplicationException while applying content provider operations.");
                //TODO: A notification that there was an error?
            }

            return json;
        }

        @Override
        protected void onPostExecute(RecordingsListJson json) {
            LogHelper.d(TAG, "Sending database change notifications ... ");

            //mContentResolver.notifyChange(RecordingsContract.Shows.CONTENT_URI, null, false);
            mContentResolver.notifyChange(RecordingsContract.Shows.SHOW_YEARS_URI, null, false);
            //mContentResolver.notifyChange(RecordingsContract.Recordings.CONTENT_URI, null, false);

            if (TextUtils.isEmpty(json.cursor) || ( json.total - json.count < 1 )) {
                LogHelper.i(TAG, "Archive.org sync complete");
            }
        }
    }
}
