package net.bradball.android.sandbox;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.bradball.android.sandbox.data.DatabaseSchema;
import net.bradball.android.sandbox.model.Show;
import net.bradball.android.sandbox.provider.RecordingsContract;
import net.bradball.android.sandbox.sync.SyncHelper;


import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

public class SandboxFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    private static final String TAG = "SanboxFragment";
    private static final int LOADER_ID = 0;
    private static final String[] SHOWS_PROJECTION = {
            RecordingsContract.Shows._ID,
            RecordingsContract.Shows.DATE,
            RecordingsContract.Shows.DOWNLOADS,
            RecordingsContract.Shows.LOCATION,
            RecordingsContract.Shows.SETLIST,
            RecordingsContract.Shows.SOUNDBOARD,
            RecordingsContract.Shows.TITLE,
            "count(" + DatabaseSchema.RecordingsTable.NAME + "." + RecordingsContract.Recordings.SHOW_ID + ") AS " + BaseColumns._COUNT
    };

    private Callbacks mCallbacks;
    private RecyclerView mShowList;
    private ShowAdapter mShowsAdapter;
    private TextView mEmptyText;

    public static SandboxFragment newInstance() {
        return new SandboxFragment();
    }

    public interface Callbacks {
        String getSelectedYear();
    }


    public void setYear(String year) {
        Bundle args = new Bundle();
        args.putString("year", year);
        getLoaderManager().restartLoader(LOADER_ID, args, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri loaderUri = RecordingsContract.Shows.buildShowsByDateUri(args.getString("year"));

        return new CursorLoader(getActivity(), loaderUri, RecordingsContract.Shows.PROJECTION, null, null, RecordingsContract.Shows.DATE + " desc");
    }



    @Override
    public void onLoadFinished(Loader loader, Cursor c) {
        Log.d(TAG, "====== LOADER - LOAD FINISHED ======");
        //c.setNotificationUri(getActivity().getContentResolver(), RecordingsContract.Shows.BY_DATE_URI);
        if (mShowsAdapter == null) {
            mShowsAdapter = new ShowAdapter();
            mShowList.setAdapter(mShowsAdapter);
        }

        mShowsAdapter.swapCursor(c);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        Log.d(TAG, "=== LOADER RESET ===");
        mShowsAdapter.swapCursor(null);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle args = new Bundle();
        args.putString("year", mCallbacks.getSelectedYear());

        getLoaderManager().initLoader(LOADER_ID, args, this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        //Make sure our sync account is setup, and trigger an initial sync if necessary.
        SyncHelper.createSyncAccount(context);

        mCallbacks = (Callbacks) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_sandbox, container, false);

        mEmptyText = (TextView) v.findViewById(R.id.show_list_empty_text);
        mShowList = (RecyclerView) v.findViewById(R.id.show_list);
        mShowList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL,false));

        //mShowsAdapter = new ShowAdapter();
        //mShowList.setAdapter(mShowsAdapter);

        return v;

    }

    private class ItemHolder extends RecyclerView.ViewHolder {
        private TextView mItemTitle;
        private TextView mRecordings;
        private TextView mCity;
        private TextView mSoundboard;


        public ItemHolder(View itemView) {
            super(itemView);

            mItemTitle = (TextView) itemView.findViewById(R.id.item_title);
            mCity = (TextView) itemView.findViewById(R.id.item_subtitle);
            mRecordings = (TextView) itemView.findViewById(R.id.recordings);
            mSoundboard = (TextView) itemView.findViewById(R.id.soundboard);
        }

        public void bindShow(Show show) {


            mItemTitle.setText(show.getDisplayDate() + " " + show.getTitle());
            mCity.setText(show.getLocation());
            mRecordings.setText(Integer.toString(show.getRecordingsCount()));
            if (show.isSoundboard()) {
                mSoundboard.setVisibility(View.VISIBLE);
            } else {
                mSoundboard.setVisibility(View.GONE);
            }
        }
    }

    private class ShowAdapter extends RecyclerViewCursorAdapter<ItemHolder> {

        public ShowAdapter() {
            super(null);
        }

        @Override
        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(getActivity()).inflate(R.layout.list_item_show, parent, false);
            return new ItemHolder(v);
        }

        @Override
        public void onBindViewHolder(ItemHolder holder, Cursor cursor) {
            Show show = Show.getFromCursor(cursor);
            holder.bindShow(show);
        }

        @Override
        public void swapCursor(Cursor newCursor) {
            super.swapCursor(newCursor);

            if (getItemCount() > 0) {
                mEmptyText.setVisibility(View.GONE);
            } else {
                mEmptyText.setVisibility(View.VISIBLE);
            }
        }
    }





}
