package net.bradball.android.sandbox.ui.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.bradball.android.sandbox.R;
import net.bradball.android.sandbox.ui.IMediaBrowser;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PlaybackControlsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlaybackControlsFragment extends Fragment {


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PlaybackControlsFragment.
     */
    public static PlaybackControlsFragment newInstance() {
       return new PlaybackControlsFragment();
    }

    public PlaybackControlsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_playback_controls, container, false);
    }


}
