package net.bradball.android.sandbox.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import net.bradball.android.sandbox.provider.RecordingsContract;
import net.bradball.android.sandbox.service.StubAuthenticatorService;
import net.bradball.android.sandbox.util.LogHelper;

import java.util.Date;

/**
 * Created by bradb on 1/12/16.
 */
public class SyncHelper {
    private static final String TAG = LogHelper.makeLogTag(SyncHelper.class);

    /**
     * Set the frequency at which the Sync Adapter should run a sync.
     *
     * The frequency is set in terms of hours
     */
    private static final long SYNC_FREQUENCY_HOURS = 12;


    /**
     * Our Sync adapter should use the same content authority as our Recordings provider
     * Android uses the sync adapter's authority to figure out which content provider
     * to bind to the sync adapter.
     */
    private static final String CONTENT_AUTHORITY = RecordingsContract.CONTENT_AUTHORITY;


    /**
     * The account type is use by both our Sync Adapter AND our stub account.
     * It's important that the value here match exactly the accountType attribute
     * that is set in both the syncadapter.xml and authenticator.xml files.
     */
    public static final String ACCOUNT_TYPE = "net.bradball.android.sandbox.stubaccount";

    /**
     * Define a key for the shared preference that tracks if initial data has been loaded.
     * If a user were to delete app data, their account may still exist, but all app data
     * including shared preferences and the app database could be deleted. We can use
     * the existence of this preference to determine if we need to reload initial app data.
     */
    private static final String PREF_DATA_LOADED = "recordings_last_update";

    public static Date getLastUpdate(Context context) {
        long lastUpdate = PreferenceManager.getDefaultSharedPreferences(context).getLong(PREF_DATA_LOADED, 0);
        return new Date(lastUpdate);
    }

    public static void setLastUpdate(Context context, Date lastUpdate) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(PREF_DATA_LOADED, lastUpdate.getTime()).apply();
    }

    public static boolean isSyncActive() {
        Account account = StubAuthenticatorService.GetAccount(ACCOUNT_TYPE);
        return ContentResolver.isSyncActive(account, CONTENT_AUTHORITY);
    }


    public static void createSyncAccount(Context context) {
        Account account = StubAuthenticatorService.GetAccount(ACCOUNT_TYPE);
        AccountManager acctManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        Date lastUpdate = getLastUpdate(context);


        //addAccountExplicitly() will itself trigger a Sync on our Sync Adapter
        //if a new account is created. In that case we can just finish
        //there.
        // If a new account is not created, we may have to manually trigger a sync. See below for that.
        if (acctManager.addAccountExplicitly(account, null, null)) {
            //In the following calls to the ContentResolver, the CONTENT_AUTHORITY will
            //be used by the system to figure out which Sync Adapter to use, and thus
            //will use our ArchiveOrgSyncAdapter

            //Tell android that this account is syncable.
            ContentResolver.setIsSyncable(account, CONTENT_AUTHORITY, 1);

            //Tell android that it can sync the account automatically when a network connection is available.
            ContentResolver.setSyncAutomatically(account, CONTENT_AUTHORITY, true);

            //Tell android how often it should sync automatically.
            long frequency = SYNC_FREQUENCY_HOURS * 60 * 60;
            ContentResolver.addPeriodicSync(account, CONTENT_AUTHORITY, new Bundle(), frequency);

            return;
        }

        //If we get here then a new account has not been created.
        //However, if the user has deleted app data, it's possible that the
        //the database/sharedPrefs have been cleared, so we'll check that
        //and run a sync if necessary.
        if (!lastUpdate.after(new Date(0)) ) {
            TriggerRefresh();
        }
    }


    /**
     * Helper method to trigger an immediate sync ("refresh").
     *
     * <p>This should only be used when we need to preempt the normal sync schedule. Typically, this
     * means the user has pressed the "refresh" button.
     *
     * Note that SYNC_EXTRAS_MANUAL will cause an immediate sync, without any optimization to
     * preserve battery life. If you know new data is available (perhaps via a GCM notification),
     * but the user is not actively waiting for that data, you should omit this flag; this will give
     * the OS additional freedom in scheduling your sync request.
     */
    public static void TriggerRefresh() {
        Bundle extras = new Bundle();

        // Disable sync backoff and ignore sync preferences. In other words...perform sync NOW!
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        ContentResolver.requestSync(
                StubAuthenticatorService.GetAccount(ACCOUNT_TYPE),  // Sync account
                CONTENT_AUTHORITY,                                  // Content authority
                extras                                              // Extras
        );
    }
}

