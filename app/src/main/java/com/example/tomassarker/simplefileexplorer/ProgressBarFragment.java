package com.example.tomassarker.simplefileexplorer;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * Zobrazi progress bar s informaciou, ze obsah sa stale nacitava..
 *
 * A simple {@link Fragment} subclass.
 * Use the {@link ProgressBarFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProgressBarFragment extends Fragment {

    View view;

    public ProgressBarFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment ProgressBarFragment.
     */
    public static ProgressBarFragment newInstance() {
        ProgressBarFragment fragment = new ProgressBarFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view =  inflater.inflate(R.layout.fragment_progress_bar, container, false);

        return view;
    }

}
