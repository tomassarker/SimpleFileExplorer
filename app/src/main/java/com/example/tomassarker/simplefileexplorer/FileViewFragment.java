package com.example.tomassarker.simplefileexplorer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.drm.DrmStore;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import java.util.ArrayList;


/**
 * Fragment, ktory zobrazi obsah vybranej zlozky.<br/>
 * V metode {@link FileViewFragment#newInstance} je potrebne vlozit zlozku typu File.<br/>
 *
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FileViewFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FileViewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FileViewFragment extends Fragment {

    //konstanty vyuzite pri savedInstanceState
    private static final String STRING_ARRAY = "array";
    private static final String CHECKED_ARRAY = "isChecked array";
    private static final String CAB_SHOWED = "isCabShowed";
    //zlozka na zobrazenie
    private File[] files;


    private View view;
    private AbsListView viewContainer;
    private OnFragmentInteractionListener mListener;
    //CAB
    private AbsListView.MultiChoiceModeListener multiChoiceModeListener = new AbsListView.MultiChoiceModeListener() {
        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            // Here you can do something when items are selected/de-selected,
            // such as update the title in the CAB

            //vynutime zobrazenie CABu
            viewContainer.getAdapter().getView(position, null, null);
            viewContainer.invalidateViews();

            //zmena titulku v CAB
            mode.setTitle( String.valueOf(viewContainer.getCheckedItemCount()) );
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate the menu for the CAB
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.context_delete_files, menu);
            mListener.setToolbarVisible(false);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // Here you can perform updates to the CAB due to
            // an invalidate() request
            mode.setTitle( String.valueOf(viewContainer.getCheckedItemCount()) );
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // Respond to clicks on the actions in the CAB
            switch (item.getItemId()) {
                case R.id.menu_delete:
                    deleteSelectedItems();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                case R.id.menu_selectAll:
                    for (int i = 0; i < files.length; i++) {
                        viewContainer.setItemChecked(i, true);
                    }
                    return true;
                default:
                    return false;
            }

        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Here you can make any necessary updates to the activity when
            // the CAB is removed. By default, selected items are deselected/unchecked.
            mListener.setToolbarVisible(true);
        }
    };


    public FileViewFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param files pole suborov, ktore sa maju zobrazit v zozname.
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

        //obnovenie dat zo savedInstanceState
        if (savedInstanceState != null && files == null) {
            String nazvySuborov[] = savedInstanceState.getStringArray(STRING_ARRAY);
            files = new File[nazvySuborov.length];
            for (int i = 0; i < nazvySuborov.length; i++) {
                files[i] = new File(nazvySuborov[i]);
            }
        }

        //praca s argumentmi - v nasom pripade nerobime nic
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment


        view = inflater.inflate(R.layout.fragment_file_view, container, false);

        viewContainer = view.findViewById(R.id.fileViewContainer); //Premenna moze obsahovat bud ListView alebo GridView - s obomi sa pracuje rovnako
        viewContainer.setAdapter(new FileListAdapter(getContext(), files));
        viewContainer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onItemClicked(files[position]);
            }
        });

        //nastavime CAB
        viewContainer.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        viewContainer.setMultiChoiceModeListener(multiChoiceModeListener);



        if (savedInstanceState != null) {
            //nastavime ListView/GridView
            //TODO:zobrazenie CABu
            //zaskrtame vybrate polozky
            if (savedInstanceState.getBoolean(CAB_SHOWED)) {
                viewContainer.startActionMode(multiChoiceModeListener);
                boolean checked[] = savedInstanceState.getBooleanArray(CHECKED_ARRAY);
                for (int i = 0; i < files.length; i++) {
                    viewContainer.setItemChecked(i, checked[i]);
                }
                viewContainer.invalidate();
                viewContainer.invalidateViews();
            }
        }

        return view;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //pre jednoduchost ulozime len pole suborov skonvertovane na String[]
        String nazvySuborov[] = new String[files.length];
        boolean showCAB = (viewContainer.getCheckedItemCount() > 0)?true:false;
        boolean checked[] = new boolean[files.length];
        for (int i = 0; i < files.length; i++) {
            nazvySuborov[i] = files[i].toString();
            checked[i] = viewContainer.isItemChecked(i);
//            viewContainer.setItemChecked(i, false);
        }

        outState.putBoolean(CAB_SHOWED,showCAB);
        outState.putStringArray(STRING_ARRAY, nazvySuborov);
        outState.putBooleanArray(CHECKED_ARRAY, checked);
    }

    /**
     * Vola sa, ak uzivatel klikne na subor/zlozku
     * @param file
     */
    public void onItemClicked(File file) {
        if (mListener != null) {
            //implementovane v nadriadenej triede
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


    private void deleteSelectedItems() {
        Log.d("deleteSelectedItems", "begin");
        //pozhrname pozadovane data
        final boolean[] delete = new boolean[files.length];
        for (int i = 0; i < files.length; i++) { delete[i]=(viewContainer.isItemChecked(i))?true:false; }
        final File[] newFiles = new File[files.length - viewContainer.getCheckedItemCount()];

        //poziadame uzivatela o potvrdenie
        AlertDialog.Builder builder = new AlertDialog.Builder((getActivity()));
        builder
                .setTitle(R.string.deleteDialog_title)
                .setMessage(R.string.deleteDialog_msg)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //vyjmazeme adapter, aby sa nepokusal robit s neexistujucimi datami
                        viewContainer.setAdapter(null);

                        //vymazeme pozadovane data a nevymnazane subory ulozimie do noveho pola
                        int zachovaneSubory = 0;
                        for (int i = 0; i < files.length; i++) {
                            if (delete[i]) {
                                files[i].delete();
                            } else {
                                newFiles[zachovaneSubory++] = files[i];
                            }
                        }

                        //opatovne nastavenie Grid/ListView
                        files = newFiles;
                        viewContainer.setAdapter( new FileListAdapter(getContext(), files) );
                        viewContainer.setMultiChoiceModeListener(multiChoiceModeListener);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);
        builder.create().show();

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
        void setToolbarVisible(boolean visible);
    }


    /**
     * Adapter na pracu s GridView/ListView
     */
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
            ImageView imageView = convertView.findViewById(R.id.fileView_imageView);
            if (file.isFile()) {
                if ( viewContainer.isItemChecked(position) ) {
                    imageView.setImageResource(R.drawable.file_checked);
                } else {
                    imageView.setImageResource(R.drawable.file);
                }
            }
            if (file.isDirectory()) {
                if ( viewContainer.isItemChecked(position) ) {
                    imageView.setImageResource(R.drawable.folder_checked);
                } else {
                    imageView.setImageResource(R.drawable.folder);
                }
            }

            // Return the completed view to render on screen
            return convertView;
        }

    }


}
