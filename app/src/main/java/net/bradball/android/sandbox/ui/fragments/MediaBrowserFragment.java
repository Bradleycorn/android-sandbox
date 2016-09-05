package net.bradball.android.sandbox.ui.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.bradball.android.sandbox.R;
import net.bradball.android.sandbox.sync.SyncHelper;
import net.bradball.android.sandbox.ui.IMediaBrowser;
import net.bradball.android.sandbox.util.LogHelper;
import net.bradball.android.sandbox.util.MediaHelper;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MediaBrowserFragmentListener} interface
 * to handle interaction events.
 * Use the {@link MediaBrowserFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MediaBrowserFragment extends Fragment {
    private static final String TAG = LogHelper.makeLogTag(MediaBrowserFragment.class);

    private static final String ARG_MEDIA_ID = "mediaId";

    private String mMediaId;

    private MediaBrowserFragmentListener mListener;

    private RecyclerView mMediaList;
    private MediaAdapter mMediaListAdapter;
    private List<MediaBrowserCompat.MediaItem> mListItems;


    private final MediaBrowserCompat.SubscriptionCallback mMediaBrowserCallback = new MediaBrowserCompat.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaBrowserCompat.MediaItem> children) {
            LogHelper.d(TAG, "==== Media Loaded! ====" + " Size: " + children.size());

            mListItems = children;
            mMediaListAdapter.setItems(mListItems);
            mMediaListAdapter.notifyDataSetChanged();
        }
    };

    public MediaBrowserFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param mediaId The mediaID of the parent item that contains the currently visible list.
     * @return A new instance of fragment MediaBrowserFragment.
     */
    public static MediaBrowserFragment newInstance(String mediaId) {
        MediaBrowserFragment fragment = new MediaBrowserFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MEDIA_ID, mediaId);
        fragment.setArguments(args);
        return fragment;
    }

    public String getMediaId() {
        return mMediaId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mMediaId = getArguments().getString(ARG_MEDIA_ID);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mListener.getMediaBrowser().isConnected()) {
            onBrowserServiceConnected();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_media_browser, container, false);

        mMediaListAdapter = new MediaAdapter(mListItems);

        mMediaList = (RecyclerView) v.findViewById(R.id.media_list);
        mMediaList.setAdapter(mMediaListAdapter);

        if (mMediaId == null) {
            mMediaList.setLayoutManager(new GridLayoutManager(getActivity(), 4));
        } else {
            mMediaList.setLayoutManager(new LinearLayoutManager(getActivity()));
        }
        return v;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MediaBrowserFragmentListener) {
            mListener = (MediaBrowserFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement MediaBrowserFragmentListener");
        }

        //Make sure our sync account is setup, and trigger an initial sync if necessary.
        SyncHelper.createSyncAccount(getActivity());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void updateTitle() {
        if (MediaHelper.ROOT_ID.equals(mMediaId)) {
            mListener.onItemChanged(null);
            return;
        }

        MediaBrowserCompat mediaBrowser = mListener.getMediaBrowser();
        mediaBrowser.getItem(mMediaId, new MediaBrowserCompat.ItemCallback() {
            @Override
            public void onItemLoaded(MediaBrowserCompat.MediaItem item) {
                mListener.onItemChanged(item);
            }
        });
    }

    // Called when the MediaBrowser is connected. This method is either called by the
    // fragment.onStart() or explicitly by the activity in the case where the connection
    // completes after the onStart()
    public void onBrowserServiceConnected() {
        if (isDetached()) {
            return;
        }
        if (mMediaId == null) {
            mMediaId = mListener.getMediaBrowser().getRoot();
        }

        LogHelper.d(TAG, "Media Browser Connected. MediaID: " + mMediaId);

        updateTitle();

        // Unsubscribing before subscribing is required if this mediaId already has a subscriber
        // on this MediaBrowser instance. Subscribing to an already subscribed mediaId will replace
        // the callback, but won't trigger the initial callback.onChildrenLoaded.
        //
        // This is temporary: A bug is being fixed that will make subscribe
        // consistently call onChildrenLoaded initially, no matter if it is replacing an existing
        // subscriber or not. Currently this only happens if the mediaID has no previous
        // subscriber or if the media content changes on the service side, so we need to
        // unsubscribe first.
        mListener.getMediaBrowser().unsubscribe(mMediaId);
        mListener.getMediaBrowser().subscribe(mMediaId, mMediaBrowserCallback);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface MediaBrowserFragmentListener extends IMediaBrowser {
        void onItemChanged(MediaBrowserCompat.MediaItem item);
        void onItemSelected(MediaBrowserCompat.MediaItem mediaItem);
    }




    private class MediaItemViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener{

        public TextView textView;
        MediaBrowserCompat.MediaItem mediaItem;

        public MediaItemViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView;

            textView.setOnClickListener(this);
        }

        public void bindView(MediaBrowserCompat.MediaItem item) {
            mediaItem = item;
            textView.setText(mediaItem.getDescription().getTitle());
        }

        @Override
        public void onClick(View v) {
            mListener.onItemSelected(mediaItem);
        }
    }


    private class MediaAdapter extends RecyclerView.Adapter<MediaItemViewHolder> {
        private List<MediaBrowserCompat.MediaItem> mItems;

        public MediaAdapter(List<MediaBrowserCompat.MediaItem> items) {
            setItems(items);
        }

        public void setItems(List<MediaBrowserCompat.MediaItem> items) {
            mItems = items;
        }


        @Override
        public MediaItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View v = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            return new MediaItemViewHolder(v);
        }

        @Override
        public void onBindViewHolder(MediaItemViewHolder holder, int position) {
            holder.bindView(mItems.get(position));
        }

        @Override
        public int getItemCount() {
            if (mItems == null) { return 0; }
            return mItems.size();
        }
    }
}
