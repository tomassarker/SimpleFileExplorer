package com.example.tomassarker.simplefileexplorer;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
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

    //fragment, ktory sa zobrazi pocas nacitavania zlozky samostatnym vlaknom
    private Fragment progressBarFragment;
    //boolean - pravo zapisovat - nevyhnutne
    private boolean canAccesStorage;
    //aktualne zobrazovana zlozka
    private File showedDirectory;
    private final static String BUNDLE_KEY_FILE_STRING = "file_name";


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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState != null) {
            String fileName = savedInstanceState.getString(BUNDLE_KEY_FILE_STRING);
            if (fileName != null) showedDirectory = new File(fileName);
            return;
        }


        //Na zaciatok zobrazime fragment s progress barom
        progressBarFragment = ProgressBarFragment.newInstance();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.MainActivity_FrameLayout, progressBarFragment);
        transaction.commit();


        if (showedDirectory == null) {
            //nacitame predvoleny adresar
            //TODO: nacitanie z preferences
            //showedDirectory = Environment.getExternalStorageDirectory();
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            String path = sharedPreferences.getString( "list_preference_1", Environment.getExternalStorageDirectory().toString() );
            showedDirectory = new File(path);
        }

        //overime opravnenie citat/zapisovat ulozisko
        canAccesStorage = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (!canAccesStorage) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            try {
                if (savedInstanceState == null) showPath();
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
            //TODO: zobrazenie nastaveni
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
        //reakcia na uzivatelove povolenie/zamietnutie pozadovanych povoleni
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

    /**
     * Zobrazi zlozku ulozenu v globalnej premennej {@link this#showedDirectory}
     * @throws InterruptedException
     */
    private void showPath() throws InterruptedException {
        //TODO
        Log.d("showPath",showedDirectory.toString());

        //najprv zobrazime progress bar
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.MainActivity_FrameLayout, progressBarFragment)
                //.addToBackStack(null)
                .commit();

        //nacitame obsah zelaneho priecinku a pockame na vysledok
        PathReader pathReader = new PathReader(showedDirectory);
        pathReader.thread.join();

        //zobrazime obsah priecinka
        FileViewFragment fileViewFragment = FileViewFragment.newInstance(pathReader.pathContent);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.MainActivity_FrameLayout, fileViewFragment)
                .commit();

    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        navigateUp();
    }

    @Override
    public boolean onSupportNavigateUp() {
//        return super.onSupportNavigateUp();
        navigateUp();
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        String filePath = showedDirectory.toString();
        outState.putString(BUNDLE_KEY_FILE_STRING, filePath);
    }

    /**
     * Vola sa, ak uzivatel stlaci sipku spat - zobrazi horny priecinok alebo ukonci appku
     */
    private void navigateUp() {
        if (showedDirectory == null) {
            //ak uz horny priecinok neexistuje, ukoncime appku
            finish();
        } else {
            showedDirectory = showedDirectory.getParentFile();
        }

        try {
            if (showedDirectory != null)
                showPath();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
            //TODO: otvorenie suboru
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

    /**
     * Trieda sluzi na nacitanie obsahu zlozku pomocou samostatneho vlakna
     */
    private class PathReader implements Runnable {

        Thread thread;
        File path;
        //sluzi ako vystup - utriedeny zoznam priecinkov a suborov
        File pathContent[];

        public PathReader(File path) {
            this.path = path;
            thread = new Thread( this, "PathReader-" + path.toString() );
            thread.start();
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
            int filesCount = 0, foldersCount = 0;
            File folders[] = path.listFiles(isDirectory);
            File files[] = path.listFiles(isFile);
            if (folders != null) {
                Arrays.sort(folders);
                foldersCount = folders.length;
            }
            if (files != null) {
                Arrays.sort(files);
                filesCount = files.length;
            }
            pathContent = new File[foldersCount + filesCount];
            for (int i = 0; i < foldersCount; i++) {
                pathContent[i] = folders[i];
            }
            for (int i = 0; i < filesCount; i++) {
                pathContent[i + foldersCount] = files[i];
            }

        }

    }

}
