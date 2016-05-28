package net.bradball.android.sandbox.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import net.bradball.android.sandbox.sync.ArchiveOrgSyncAdapter;

public class ArchiveOrgSyncService extends Service {

    private static final Object sSyncAdapterLock = new Object();
    private static ArchiveOrgSyncAdapter sArchiveOrgSyncAdapter = null;


    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (sSyncAdapterLock) {
            if (sArchiveOrgSyncAdapter == null) {
                sArchiveOrgSyncAdapter = new ArchiveOrgSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sArchiveOrgSyncAdapter.getSyncAdapterBinder();
    }
}