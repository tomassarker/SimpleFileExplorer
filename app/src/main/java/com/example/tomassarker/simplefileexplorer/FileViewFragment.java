package com.example.tomassarker.simplefileexplorer;

import android.content.Context;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FileViewFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FileViewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FileViewFragment extends Fragment {

    private static final String STRING_ARRAY = "array";
    private static final String ARG_FILES = "files";
    private File[] files;

    private View view;
    private OnFragmentInteractionListener mListener;

    public FileViewFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param files Parameter 1.
     * @return A new instance of fragment FileViewFragment.
     */
    public static FileViewFragment newInstance(File files[]) {
        FileViewFragment fragment = new FileViewFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        fragment.files = files;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && files == null) {
            String nazvySuborov[] = savedInstanceState.getStringArray(STRING_ARRAY);
            files = new File[nazvySuborov.length];
            for (int i = 0; i < nazvySuborov.length; i++) {
                files[i] = new File(nazvySuborov[i]);
            }
        }

        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_file_view, container, false);

        AbsListView viewContainer = view.findViewById(R.id.fileViewContainer);
        viewContainer.setAdapter(new FileListAdapter(getContext(), files));
        viewContainer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onItemClicked(files[position]);
            }
        });

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        String nazvySuborov[] = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            nazvySuborov[i] = files[i].toString();
        }
        outState.putStringArray(STRING_ARRAY, nazvySuborov);
    }

    public void onItemClicked(File file) {
        if (mListener != null) {
            mListener.onFragmentInteractionFileSelected(file);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteractionFileSelected(File file);
    }


    private class FileListAdapter extends ArrayAdapter<File> {

        public FileListAdapter(@NonNull Context context, File files[]) {
            super(context, 0, files);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            // Get the data item for this position
            File file = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.single_file_layout, parent, false);
            }
            // Lookup view for data population
            TextView tvName = (TextView) convertView.findViewById(R.id.textView_fileName);
            // Populate the data into the template view using the data object
            tvName.setText(file.getName());
            //TODO: rozlisenie medzi zlozkami a subormi
            ImageView imageView = convertView.findViewById(R.id.fileView_imageView);
            if (file.isFile()) {
                imageView.setImageResource(R.drawable.file);
//                imageView.setImageDrawable(getResources().getDrawable(R.drawable.file, null));
            }
            if (file.isDirectory()) {
//                imageView.setImageDrawable(getResources().getDrawable(R.drawable.folder, null));
                imageView.setImageResource(R.drawable.folder);
            }

            // Return the completed view to render on screen
            return convertView;
        }


    }


}
