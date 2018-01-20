package com.example.tomassarker.simplefileexplorer;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements FileViewFragment.OnFragmentInteractionListener {

    private Fragment progressBarFragment;
    private boolean canAccesStorage;
    private File showedDirectory;


    //request kody pre povolenia
    static public final HashMap<String, Integer> permissions = new HashMap<String, Integer>(){};
    static {
        permissions.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, 1);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Na zaciatok zobrazime fragment s progress barom
        progressBarFragment = ProgressBarFragment.newInstance();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.MainActivity_FrameLayout, progressBarFragment);
        transaction.commit();

        //nacitame predvoleny adresar
        //TODO: nacitanie z preferences
        //showedDirectory = Environment.getExternalStorageDirectory();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String path = sharedPreferences.getString( "list_preference_1", Environment.getExternalStorageDirectory().toString() );
        showedDirectory = new File(path);

        //overime opravnenie citat/zapisovat ulozisko
        canAccesStorage = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (!canAccesStorage) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            try {
                showPath();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.action_refresh) {
            try {
                showPath();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED) {
            try {
                showPath();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            Snackbar.make(findViewById(android.R.id.content), "appka nebude fungovat..", 1500).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    private void showPath() throws InterruptedException {
        //TODO
        Log.d("showPath",showedDirectory.toString());

        //najprv zobrazime progress bar
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.MainActivity_FrameLayout, progressBarFragment);
        transaction.commit();

        //nacitame obsah zelaneho priecinku a pockame na vysledok
        PathReader pathReader = new PathReader(showedDirectory);
        pathReader.thread.join();

        //zobrazime obsah priecinka
        FileViewFragment fileViewFragment = FileViewFragment.newInstance(pathReader.pathContent);
        transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.MainActivity_FrameLayout, fileViewFragment);
//        transaction.addToBackStack(null);
        transaction.commit();

        //docasne riesenie - vypis
//        for (int i = 0; i<pathReader.pathContent.length; i++) {
//            File f = pathReader.pathContent[i];
//            Toast.makeText(this, f.getName(), Toast.LENGTH_SHORT).show();
//        }
    }

    //akcia sa vyvola pri stlaceni polozky vo fragmente so subormi
    @Override
    public void onFragmentInteractionFileSelected(File file) {
        if (file.isDirectory()) {
            showedDirectory = file;
            try {
                showPath();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        else if (file.isFile()) {
            //TODO:
            Uri uri = Uri.fromFile(file);
            String mime = getContentResolver().getType(uri);

            Intent openFileIntent = new Intent();
//            openFileIntent.setAction(Intent.ACTION_VIEW);
//            openFileIntent.setDataAndType(uri, mime);
//            openFileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//            startActivity(openFileIntent);

            openFileIntent.setData(uri);
            //openFileIntent.setAction(android.content.Intent.ACTION_VIEW);


//            String extension[] = file.toString().split(".");
//            String ex = extension[extension.length-1];
//

//            String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ex);

            openFileIntent.setType("file/?");

            startActivity(openFileIntent);
        }
    }

    private class PathReader implements Runnable {

        Thread thread;
        File path;
        File pathContent[];

        public PathReader(File path) {
            this.path = path;
            thread = new Thread( this, "PathReader-" + path.toString() );
            thread.start();
//            thread.run();
        }

        /**
         * Metoda nacita obsah priecinku a zoradi ho... pouzite rozdelenie dvoch poli - jednoduchsie ako implementacia komparatorom
         */
        @Override
        public void run() {
            Log.d(thread.getName(), "run");

            //Filter, ktory vyfiltruje len priecinky
            FileFilter isDirectory = new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory();
                }
            };
            //Filter, ktory vyfiltruje len subory
            FileFilter isFile = new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isFile();
                }
            };

            //ziskame zoznam priecinkov a suborov zvlast a nasledne ich utriedime a spojime
            File folders[] = path.listFiles(isDirectory);
            File files[] = path.listFiles(isFile);
            Arrays.sort(folders);
            Arrays.sort(files);
            pathContent = new File[folders.length + files.length];
            for (int i = 0; i < folders.length; i++) {
                pathContent[i] = folders[i];
            }
            for (int i = 0; i < files.length; i++) {
                pathContent[i + folders.length] = files[i];
            }
        }

    }

}
