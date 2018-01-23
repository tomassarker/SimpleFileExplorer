package com.example.tomassarker.simplefileexplorer;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.os.StrictMode;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
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
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * MainActivity - zabezpecuje zobrazovanie {@link FileViewFragment}
 */
public class MainActivity extends AppCompatActivity implements FileViewFragment.OnFragmentInteractionListener {


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


        if (savedInstanceState != null) {
            String fileName = savedInstanceState.getString(BUNDLE_KEY_FILE_STRING);
            if (fileName != null) showedDirectory = new File(fileName);
            //ak existuje matersky priecinok, zobrazime sipku spat
            if (showedDirectory.getParentFile() != null) { getSupportActionBar().setDisplayHomeAsUpEnabled(true); }
            return;
        }


        //Na zaciatok zobrazime fragment s progress barom
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.MainActivity_FrameLayout, ProgressBarFragment.newInstance());
        transaction.commit();


        if (showedDirectory == null) {
            //nacitame predvoleny adresar
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            String path = sharedPreferences.getString( "edit_text_preference_1", null );
            if (path == null) {
                //ak nebol v SharedPref ulozeny predvoleny priecinok, otvorime "root" a nastavime ho ako predvoleny
                path = Environment.getExternalStorageDirectory().toString();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("edit_text_preference_1", path);
                editor.commit();
            }
            //cestu k zlozke prekovertujeme na File
            showedDirectory = new File(path);
        }

        //ak existuje matersky priecinok, zobrazime sipku spat
        if (showedDirectory.getParentFile() != null) { getSupportActionBar().setDisplayHomeAsUpEnabled(true); }


        //overime opravnenie zapisovat ulozisko
        canAccesStorage = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (!canAccesStorage) {
            //ak nemame opravnenie, vypytame ho
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            //ak opravnenie mame, zobrazime defaultny priecinok
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
            //zobrazime nastavenia
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra(SettingsActivity.CURRENT_PATH, showedDirectory.toString());
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_refresh) {
            //znovu zobrazime aktualnu zlozku
            try {
                showPath();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
        }
        if (id == R.id.action_home) {
            //zobrazime default folder
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            String path = sharedPreferences.getString( "edit_text_preference_1", null );
            showedDirectory = new File(path);
            try {
                showPath();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Zobrazi alebo skryje ActionBar
     * @param visible
     */
    @Override
    public void setToolbarVisible(boolean visible) {
        if (!visible) getSupportActionBar().hide();
        if (visible) getSupportActionBar().show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //reakcia na uzivatelove povolenie/zamietnutie pozadovanych povoleni
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED) {
            //ak boli pridelene opravnenia, netreba robit (zatial) nic
        } else {
            //ak neboli opravnenia udelene, informujeme uzivatela o nefuncnosti appky a poziadame o ne znovu...
            //TODO: informovanie cez dialog - tato metoda by sa zavolala len ak by uzivatel klikol ok
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dialog_permisionNotGranted_title)
                    .setMessage(R.string.dialog_permisionNotGranted_msg)
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissionsAgain();
                        }
                    })
                    .create()
                    .show();
        }
        try {
            showPath();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void requestPermissionsAgain() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }

    /**
     * Zobrazi zlozku ulozenu v globalnej premennej {@link this#showedDirectory}
     * @throws InterruptedException
     */
    private void showPath() throws InterruptedException {
        Log.d("showPath",showedDirectory.toString());

        //najprv zobrazime progress bar
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit)
                .replace(R.id.MainActivity_FrameLayout, ProgressBarFragment.newInstance())
                //.addToBackStack(null)
                .commitAllowingStateLoss();

        //

        //nacitame obsah zelaneho priecinku a pockame na vysledok
        PathReader pathReader = new PathReader(showedDirectory);
        pathReader.thread.join();

        //ak existuje matersky priecinok, zobrazime sipku spat
        if (showedDirectory.getParentFile() != null) { getSupportActionBar().setDisplayHomeAsUpEnabled(true); }

        //zobrazime obsah priecinka
        FileViewFragment fileViewFragment = FileViewFragment.newInstance(pathReader.pathContent);
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit)
                .replace(R.id.MainActivity_FrameLayout, fileViewFragment)
                .commitAllowingStateLoss();

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
        if (showedDirectory.getParentFile() == null) {
            //ak uz horny priecinok neexistuje, ukoncime appku
            finish();
        } else {
            //posunieme sa o uroven vyssie
            showedDirectory = showedDirectory.getParentFile();
            //ak uz neexistuje dalsi matersky priecinok, schovame sipku spat z toolbaru
            if ( showedDirectory != null && (showedDirectory.getParentFile()) == null ) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            } else {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            }
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
            Log.d("show file", file.toString());

            //Potrebne pre OS Android >=7.0
            if(Build.VERSION.SDK_INT>=24){
                try{
                    Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                    m.invoke(null);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }

            //zistime typ suboru podla mime
            MimeTypeMap map = MimeTypeMap.getSingleton();
            String ext = MimeTypeMap.getFileExtensionFromUrl(file.getName());
            String type = map.getMimeTypeFromExtension(ext);

            //ak sme nedokazali rozoznat typ suboru, urcime ho vseobecne
            if (type == null)
                type = "*/*";

            //zobrazenie suboru s moznostnou vyberu apouzitej appky
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri data = Uri.fromFile(file);
            intent.setDataAndType(data, type);
            startActivity(intent);
        }
    }

    /**
     * Trieda sluzi na nacitanie obsahu zlozky pomocou samostatneho vlakna
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
