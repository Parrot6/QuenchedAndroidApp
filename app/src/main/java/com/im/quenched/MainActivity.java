package com.im.quenched;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
public class MainActivity extends AppCompatActivity  implements LocationListener, MapFragment.mapListener {
    private static final int RC_SIGN_IN = 321;
    BottomNavigationView bottomNavigationView;
    CardView homeSel;
    CardView searchSel;
    CardView mapSel;
    CardView socialSel;

    public static ArrayList<JSONObject> breweriesStillToLoad = new ArrayList<>();
    public static ConcurrentHashMap<Integer, filteredDir> filterableBreweriesMap = new ConcurrentHashMap<>();
    public static ArrayList<filteredDir> favoritedBreweries = new ArrayList<>();
    public static NotifyImageUpdate notifyImageUpdate;
    public static Drawable defaultMissingImageDrawable;
    public static Bitmap defaultMissingImageBitmap;

    public static double startingLat = 32.8466915;
    public static double startingLon = -79.8768743;
    static String currentState = null;
    public static String currentLocationZipcode = "29401";
    public static double map_startingLat = 32.8466915;
    public static double map_startingLon = -79.8768743;
    static String map_currentState = "South Carolina";
    public static String map_currentLocationZipcode = "29401";

    //Fragment homeFrag = new Home();
    Fragment mapFrag = new MapFragment(MainActivity.this);
    private Context mContext;

    // flag for GPS status
    boolean isGPSEnabled = false;

    // flag for network status
    boolean isNetworkEnabled = false;

    // flag for GPS status
    boolean canGetLocation = false;

    Location location; // location
    double latitude; // latitude
    double longitude; // longitude
    public static double distanceFilter = 0.0;
    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute
    static Map<String, String> states = new HashMap<>();
    public static boolean standardQual = true;
    GoogleSignInClient mGoogleSignInClient;
    // Declaring a Location Manager
    protected LocationManager locationManager;
    GoogleSignInAccount account;
    public static final String dbEndpoint = "https://mb1zattts4.execute-api.us-east-1.amazonaws.com/dev/";


    public static class filteredDir implements Serializable, Target {
        boolean crawlable = false;
        boolean kitched = false;
        boolean family = false;
        boolean dog = false;
        boolean outdoorSeating = false;
        int uniqueBreweryID;
        String zipcode;
        double lat;
        double lon;
        transient JSONObject jso;
        String Brewery;
        ArrayList<String> imgUrl;
        transient private Bitmap thisBitmap = null;
        boolean bronze;
        boolean silver;
        boolean gold;
        Target target;
        filteredDir(JSONObject jso, String brewery, int uniqueBreweryID, Bitmap thisBitmap, boolean crawlable,
                    boolean kitched,
                    boolean family,
                    boolean dog,
                    boolean outdoorSeating, double lat, double longitutde, String zipcode, boolean bronze, boolean silver, boolean gold){
            this.jso = jso;
            this.uniqueBreweryID = uniqueBreweryID;
            this.Brewery = brewery;
            this.thisBitmap = thisBitmap;
            this.crawlable = crawlable;
            this.kitched = kitched;
            this.family = family;
            this.dog = dog;
            this.outdoorSeating = outdoorSeating;
            this.lat = lat;
            this.lon = longitutde;
            this.zipcode = zipcode;
            this.bronze = bronze;
            this.silver = silver;
            this.gold = gold;
            target = this;
            JSONArray jsa = jso.optJSONArray("Image");
            ArrayList<String> pics = new ArrayList<>();
            if(jsa != null){
                for(int i = 0; i < jsa.length(); i++){
                    pics.add(jsa.optString(i));
                }
            } else {
                pics.add("");
                pics.add("");
                pics.add("");
                pics.add("");
            }
            this.imgUrl = pics;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    if(pics.get(0).equals("")) return;
                    Picasso.get().load(pics.get(0)).into(target);
                } // This is your code
            };
            mainHandler.post(myRunnable);
        }

        public Bitmap getThisBitmap(){
            if(thisBitmap == null) return MainActivity.defaultMissingImageBitmap;
            return thisBitmap;
        }
        public void setThisBitmap(Bitmap bmp){
            thisBitmap = bmp;
        }
        public double getDistance(boolean fromMap){
            if(fromMap) return distance(lat, map_startingLat, lon, map_startingLon, 0 ,0);
            return distance(lat, startingLat, lon, startingLon, 0 ,0);
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            //Log.e("imageloaded", this.imgUrl.get(0));
            this.setThisBitmap(bitmap);
            notifyImageUpdate.notifyUpdate();
        }

        @Override
        public void onBitmapFailed(Exception e, Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        account = GoogleSignIn.getLastSignedInAccount(MainActivity.this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.drawable.quenched_logo4);
        mContext  = MainActivity.this;
        populateStatesHashmap();
        loadBreweryNames();
        loadStateUpdateTimes();
        loadUserPreferences();
        try {
            InputStream ims = getAssets().open("quenchedwithbg.png");
            defaultMissingImageDrawable = Drawable.createFromStream(ims, null);
            ims.close();
            InputStream ims2 = getAssets().open("quenchedwithbg.png");
            defaultMissingImageBitmap = BitmapFactory.decodeStream(ims2);
            ims2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //homeSel = findViewById(R.id.cardView_home);
        searchSel = findViewById(R.id.cardView_search);
        mapSel = findViewById(R.id.cardView_map);
        socialSel = findViewById(R.id.cardView_social);
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        bottomNavigationView.setElevation(0.0f);
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        Fragment fragment = null;
                        switch (item.getItemId()) {
/*                            case R.id.nav_Home:
                                fragment = new Home();
                                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).commit();
                                selectedTabHighlighter(1);
                                break;*/
                            case R.id.nav_Map:
                                //getLocation();
                                fragment = mapFrag;
                                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).commit();
                                selectedTabHighlighter(2);
                                break;
                            case R.id.nav_Search:
                                fragment = new Search();
                                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).commit();
                                selectedTabHighlighter(3);
                                break;
                            case R.id.nav_Blog:
                                fragment = new Blog();
                                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).commit();
                                selectedTabHighlighter(4);
                                break;
                        }
                        return true;
                    }
                });
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, mapFrag).commit(); //starting page
        selectedTabHighlighter(2);
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        updateUI(account, true);
        getFavorites(MainActivity.this);
    }

    private void selectedTabHighlighter(final int i) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //homeSel.setVisibility(View.INVISIBLE);
                searchSel.setVisibility(View.INVISIBLE);
                mapSel.setVisibility(View.INVISIBLE);
                socialSel.setVisibility(View.INVISIBLE);
                switch (i){
/*                    case 1:
                        //homeSel.setVisibility(View.VISIBLE);
                        break;*/
                    case 2:
                        searchSel.setVisibility(View.VISIBLE);
                        break;
                    case 3:
                        mapSel.setVisibility(View.VISIBLE);
                        break;
                    case 4:
                        socialSel.setVisibility(View.VISIBLE);
                        break;
                    default:
                        break;
                }
            }
        });

    }

    private void loadUserPreferences() {
        SharedPreferences prefs = getSharedPreferences("Preferences", MODE_PRIVATE);
        standardQual = prefs.getBoolean("StandardQuality", false);
        final SharedPreferences getUser = getSharedPreferences(getString(R.string.SharedPrefUser), MODE_PRIVATE);
        currentLocationZipcode = getUser.getString("Zipcode", "29401");
        currentState = getUser.getString("State", null);
        startingLat = getUser.getFloat("Latitude", (float) startingLat);
        startingLon = getUser.getFloat("Longitude", (float) startingLat);
        if(currentState != null) updateStateFromState(mContext, currentLocationZipcode, currentState, startingLat, startingLon);
        resetMapLocation();
        //Log.e("curentState", " " + currentState);
    }

    public static String getUser(Context context){
        return context.getSharedPreferences(context.getString(R.string.SharedPrefUser), MODE_PRIVATE).getString("Email", "");
    }
    public static Boolean isLoggedIn(Context context){
        if(getUser(context).equals("")) return false;
        return true;
    }

    public static void resetMapLocation() {
        map_currentLocationZipcode = currentLocationZipcode;
        map_currentState = currentState;
        map_startingLat = startingLat;
        map_startingLon = startingLon;
    }

    private void updateUI(GoogleSignInAccount account, boolean getLoc) {
        if(account != null){
            SharedPreferences.Editor sp = getSharedPreferences(getString(R.string.SharedPrefUser), MODE_PRIVATE).edit();
            sp.putString("Email", account.getEmail());
            sp.putLong("Submitted", System.currentTimeMillis());
            sp.apply();
            if(getLoc) getLocation();
        } else {
            checkIfRegistered();
        }
    }

    @SuppressLint("RestrictedApi")
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        if(menu instanceof MenuBuilder){
            MenuBuilder m = (MenuBuilder) menu;
            m.setOptionalIconsVisible(true);
        }
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                showAboutPopup();
                return true;
            case R.id.help:
                showHelpPopup();
                return true;
            case R.id.account:
                checkIfRegistered();
                return true;
            case R.id.report_bug:
                showReportBugPopup();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showReportBugPopup() {
        final Dialog dialog = new Dialog(this);
        int width = (int)(getResources().getDisplayMetrics().widthPixels*0.90);
        int height = (int)(getResources().getDisplayMetrics().heightPixels*0.90);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.submit_bug_report);
        Spinner cat = dialog.findViewById(R.id.report_bug_catagory);
        EditText desc = dialog.findViewById(R.id.report_bug_description);
        Button close = dialog.findViewById(R.id.button_report_cancel);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        Button submit = dialog.findViewById(R.id.button_report_confirm);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final OkHttpClient client = new OkHttpClient().newBuilder()
                        .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                        .build();
                String json = "{'email':'" + getUser(dialog.getContext()) + new Date().getTime() + "','catagory':'" + cat.getSelectedItem().toString() + "', " + "'description':'" + desc.getText().toString() +"'}";
                JSONObject jso = null;
                try {
                    jso = new JSONObject(json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                final RequestBody body = RequestBody.create(
                        MediaType.parse("application/json"), jso.toString());
                Response response;
                final Request getID = new Request.Builder()
                        .url(dbEndpoint + "userReportBug")
                        .post(body)
                        //.addHeader("x-apikey", API_KEY)
                        .addHeader("cache-control", "no-cache")
                        .build();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Response response = client.newCall(getID).execute();
                            response.body().string();
                            if(response.code() == 200){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(dialog.getContext(), "Sucessfully reported bug! Thank you so much!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                dialog.dismiss();
            }
        });
        dialog.show();
        dialog.getWindow().setLayout(width, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private void showHelpPopup() {
        final Dialog dialog = new Dialog(this);
        int width = (int)(getResources().getDisplayMetrics().widthPixels*0.90);
        int height = (int)(getResources().getDisplayMetrics().heightPixels*0.90);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.help_menu);
        Button close = dialog.findViewById(R.id.button_help_close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        ImageView closex = dialog.findViewById(R.id.imageView_help_closex);
        closex.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
        dialog.getWindow().setLayout(width, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private void showAboutPopup() {
        final Dialog dialog = new Dialog(this);
        int width = (int)(getResources().getDisplayMetrics().widthPixels*0.90);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.what_is_quenched);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        Button close = dialog.findViewById(R.id.button_whatisquenched_close);
        Button subscribe = dialog.findViewById(R.id.button_whatsisquenched_subscribe);
        EditText emailentry = dialog.findViewById(R.id.editText_whatisquenched_email);
        subscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                emailentry.setEnabled(false);
                final OkHttpClient client = new OkHttpClient().newBuilder()
                        .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                        .build();
                final Request getID = new Request.Builder()
                        .url(dbEndpoint + "subscribe/" + emailentry.getText())
                        .get()
                        //.addHeader("x-apikey", API_KEY)
                        .addHeader("cache-control", "no-cache")
                        .build();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Response response = client.newCall(getID).execute();
                            JSONObject jso = new JSONObject(response.body().string());
                            if(response.code() == 200){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, jso.optString("message","Successfully subscribed!"), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                Toast.makeText(MainActivity.this, jso.optString("message","???!"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException | JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        ImageView fb = dialog.findViewById(R.id.imageView_facebookicon);
        fb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse("https://www.facebook.com/im.quenched"));
                startActivity(intent);
            }
        });
        ImageView insta = dialog.findViewById(R.id.imageView_instagramlink);
        insta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse("https://www.instagram.com/im.quenched/"));
                startActivity(intent);
            }
        });
        ImageView closex = dialog.findViewById(R.id.imageView_whatisquenched_closedx);
        closex.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
        dialog.getWindow().setLayout(width, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    public static filteredDir isBreweryLoaded(int UniqueID){
         return filterableBreweriesMap.get(UniqueID);
    }
    public static filteredDir findOrLoadBrewery(int UniqueID){
        filteredDir dir = isBreweryLoaded(UniqueID);
        if(dir != null){
            return dir;
        } else {
            return fetchBreweryByUniqueID(UniqueID);
        }
    }
    private void getFavorites(final Context context) {
        final SharedPreferences sp = context.getSharedPreferences("RegisteredUser", MODE_PRIVATE);
        String email = sp.getString("Email", "");
        if(email.equals("")) return;
            final OkHttpClient client = new OkHttpClient().newBuilder()
                    .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                    .build();
                final Request getID = new Request.Builder()
                        .url(dbEndpoint + "favorites/" + email)
                        .get()
                        //.addHeader("x-apikey", API_KEY)
                        .addHeader("cache-control", "no-cache")
                        .build();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Response response = client.newCall(getID).execute();

                                JSONArray returned = new JSONArray(response.body().string());
                                if(returned.length() == 0) return;

                                for(int i = 0; i < returned.length(); i++){
                                    JSONObject temp = (JSONObject) returned.get(i);
                                    int uniq = temp.optInt("UniqueID", -1);
                                    filteredDir existCheck = isBreweryLoaded(uniq);
                                    if(existCheck != null){
                                        favoritedBreweries.add(existCheck);
                                        continue;
                                    } else {
                                        filteredDir newFav = makeFilteredDir(temp);
                                        favoritedBreweries.add(newFav);
                                    }
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        getFragmentRefreshListener().notifyPrelimaryDataReady();
                                    }
                                });
                            } catch (IOException | JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();

    }

    public NotifyImageUpdate getFragmentRefreshListener() {
        return notifyImageUpdate;
    }
    void populateStatesHashmap(){
        states.put("Alabama","AL");
        states.put("Alaska","AK");
        states.put("Alberta","AB");
        states.put("American Samoa","AS");
        states.put("Arizona","AZ");
        states.put("Arkansas","AR");
        states.put("Armed Forces (AE)","AE");
        states.put("Armed Forces Americas","AA");
        states.put("Armed Forces Pacific","AP");
        states.put("British Columbia","BC");
        states.put("California","CA");
        states.put("Colorado","CO");
        states.put("Connecticut","CT");
        states.put("Delaware","DE");
        states.put("District Of Columbia","DC");
        states.put("Florida","FL");
        states.put("Georgia","GA");
        states.put("Guam","GU");
        states.put("Hawaii","HI");
        states.put("Idaho","ID");
        states.put("Illinois","IL");
        states.put("Indiana","IN");
        states.put("Iowa","IA");
        states.put("Kansas","KS");
        states.put("Kentucky","KY");
        states.put("Louisiana","LA");
        states.put("Maine","ME");
        states.put("Manitoba","MB");
        states.put("Maryland","MD");
        states.put("Massachusetts","MA");
        states.put("Michigan","MI");
        states.put("Minnesota","MN");
        states.put("Mississippi","MS");
        states.put("Missouri","MO");
        states.put("Montana","MT");
        states.put("Nebraska","NE");
        states.put("Nevada","NV");
        states.put("New Brunswick","NB");
        states.put("New Hampshire","NH");
        states.put("New Jersey","NJ");
        states.put("New Mexico","NM");
        states.put("New York","NY");
        states.put("Newfoundland","NF");
        states.put("North Carolina","NC");
        states.put("North Dakota","ND");
        states.put("Northwest Territories","NT");
        states.put("Nova Scotia","NS");
        states.put("Nunavut","NU");
        states.put("Ohio","OH");
        states.put("Oklahoma","OK");
        states.put("Ontario","ON");
        states.put("Oregon","OR");
        states.put("Pennsylvania","PA");
        states.put("Prince Edward Island","PE");
        states.put("Puerto Rico","PR");
        states.put("Quebec","PQ");
        states.put("Rhode Island","RI");
        states.put("Saskatchewan","SK");
        states.put("South Carolina","SC");
        states.put("South Dakota","SD");
        states.put("Tennessee","TN");
        states.put("Texas","TX");
        states.put("Utah","UT");
        states.put("Vermont","VT");
        states.put("Virgin Islands","VI");
        states.put("Virginia","VA");
        states.put("Washington","WA");
        states.put("West Virginia","WV");
        states.put("Wisconsin","WI");
        states.put("Wyoming","WY");
        states.put("Yukon Territory","YT");
    }
    public static void setFragmentRefreshListener(NotifyImageUpdate fragmentRefreshListener) {
        notifyImageUpdate = fragmentRefreshListener;
    }
    private void updateMapFromLatLng(final Context context, String area, String state, final double lat, final double lon) {
        //Log.e("updateMapFromLatLng", lat+","+longitude);
        new Thread(new Runnable() {
            @Override
            public void run() {
                map_startingLat = lat;
                map_startingLon = lon;
                map_currentLocationZipcode = area;
                if(!map_currentState.equals(state)){
                    map_currentState = state;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Loading breweries for "+map_currentState, Toast.LENGTH_LONG).show();
                        }
                    });
                    queryStateData(states.get(map_currentState));
                } else {
                    map_currentState = state;
                }
            }
        }).start();
    }
    public void updateStateFromState(final Context context, final String area, final String state, final double lat, final double longitude) {
        //Log.e("updateStateFromState", lat+","+longitude+", currstate:"+currentState+", newstate:"+state);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Object o = stateLoadTimeJsons.get(states.get(state));
                if(currentState == null || !currentState.equals(state) || o == null){
                    //Log.e("querying"," data");
                    currentState = state;
                    currentLocationZipcode = area;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Loading breweries for "+currentState, Toast.LENGTH_LONG).show();
                        }
                    });
                    queryStateData(states.get(currentState));
                } else {
                    currentLocationZipcode = area;
                    currentState = state;
                }
                final SharedPreferences.Editor getUser = getSharedPreferences(getString(R.string.SharedPrefUser), MODE_PRIVATE).edit();
                getUser.putString("State", currentState);
                getUser.putFloat("Latitude", (float) lat);
                getUser.putFloat("Longitude", (float) longitude);
                getUser.apply();
                map_startingLat = lat;
                map_startingLon = longitude;
                map_currentLocationZipcode = area;
                map_currentState = state;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(getFragmentRefreshListener()!=null){
                            getFragmentRefreshListener().notifyPrelimaryDataReady();
                        }
                    }
                });
            }
        }).start();
    }
    public void updateStateFromLatLng(final Context context, final double lat, final double longitude) {
        //Log.e("updateStateFromLatLng", lat+","+longitude);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Geocoder geocoder;
                List<Address> addresses;
                geocoder = new Geocoder(context, Locale.getDefault());
                try {
                    addresses = geocoder.getFromLocation(lat, longitude, 1);
                    if(addresses.get(0).getPostalCode() == null){
                        currentLocationZipcode = String.valueOf(addresses.get(0).getLocality());
                    } else {
                        currentLocationZipcode = String.valueOf(addresses.get(0).getPostalCode());
                    }
                    if(!currentState.equals(addresses.get(0).getAdminArea())){
                        currentState = addresses.get(0).getAdminArea();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Loading breweries for "+currentState, Toast.LENGTH_LONG).show();
                            }
                        });
                        queryStateData(states.get(currentState));
                    } else {
                        currentState = addresses.get(0).getAdminArea();
                    }
                        final SharedPreferences.Editor getUser = getSharedPreferences(getString(R.string.SharedPrefUser), MODE_PRIVATE).edit();
                        getUser.putString("State", currentState);
                        getUser.putFloat("Latitude", (float) lat);
                        getUser.putFloat("Longitude", (float) longitude);
                        getUser.apply();
                } catch (Exception e) {
                    //Log.e("newlocation:excep", currentState);
                    e.printStackTrace();
                    //currentState = "South Carolina";
                }
            }
        }).start();
    }
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    private void checkIfRegistered() {

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.introduction_dialog_google_fb_signin);
        final Button signOutButton = dialog.findViewById(R.id.sign_out_button);
        final Button close = dialog.findViewById(R.id.button_introdialog_continue);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        final SharedPreferences.Editor editUser = getSharedPreferences(getString(R.string.SharedPrefUser), MODE_PRIVATE).edit();
        final SharedPreferences.Editor editPrefs = getSharedPreferences(getString(R.string.Preferences), MODE_PRIVATE).edit();
        final SharedPreferences getUser = getSharedPreferences(getString(R.string.SharedPrefUser), MODE_PRIVATE);
        final SharedPreferences getPrefs = getSharedPreferences(getString(R.string.Preferences), MODE_PRIVATE);
        final TextView email = dialog.findViewById(R.id.textView_googleLoggedInAs);

        // Set the dimensions of the sign-in button.
        final SignInButton signInButton = dialog.findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_WIDE);

        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
                email.setText("Not Currently Logged In...");
                //email.setVisibility(View.GONE);
                signInButton.setVisibility(View.VISIBLE);
                signOutButton.setVisibility(View.GONE);
            }
        });
        if(getUser.getString("Email", "").equals("")){
            email.setText("Not Currently Logged In...");
            //email.setVisibility(View.GONE);
            signInButton.setVisibility(View.VISIBLE);
            signOutButton.setVisibility(View.GONE);
        } else {
            email.setText(String.format("Currently Logged in as %s", getUser.getString("Email", "")));
            signInButton.setVisibility(View.GONE);
            signOutButton.setVisibility(View.VISIBLE);
        }
        ImageView iv = dialog.findViewById(R.id.imageView_introQuenched);
        try {
            InputStream ims = getAssets().open("Quenched_FINAL.png");
            // load image as Drawable
            Drawable d = Drawable.createFromStream(ims, null);
            iv.setImageDrawable(d);
            ims.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        final EditText zip = dialog.findViewById(R.id.editText_intro_zipcode);
        zip.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                close.setEnabled(true);
                zip.setError(null);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        zip.setText(getUser.getString("Zipcode",""));
        final EditText username = dialog.findViewById(R.id.login_username);
        username.setText(getUser.getString("Username", ""));
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(zip.getText().length() < 5){
                    zip.setError("Please enter a 5 digit Zipcode");
                    close.setEnabled(false);
                    return;
                }
                editUser.putString("Zipcode", zip.getText().toString());
                editUser.putString("Username", username.getText().toString());
                editUser.apply();
                updateLocationByZipcode(zip.getText().toString());
                dialog.dismiss();
                getLocation();
            }
        });
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.e("account", account.getEmail());
                if(account == null){
                    signIn();
                    editUser.putString("Zipcode", zip.getText().toString());
                    editUser.putString("Username", username.getText().toString());
                    editUser.apply();
                    dialog.dismiss();
                    //close.performClick();
                    //email.setText(String.format("Currently Logged in as %s", getUser.getString("Email", "")));
                    //signInButton.setVisibility(View.GONE);
                    //signOutButton.setVisibility(View.VISIBLE);
                }
            }
        });
        dialog.show();
        int width = (int)(getResources().getDisplayMetrics().widthPixels*0.90);
        dialog.getWindow().setLayout(width, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private void updateLocationByZipcode(String toString) {
        searchZipcodeApiCall(toString);
    }

    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        account = null;
                        SharedPreferences.Editor user = getSharedPreferences(String.valueOf(R.string.SharedPrefUser), MODE_PRIVATE).edit();
                        user.clear();
                        user.remove("Email");
                        user.commit();
                        Toast.makeText(MainActivity.this, "Logged out..", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public static boolean addFavoriteBreweryByUniqueID(int uniqueID, final Context context, boolean update){
        for (Integer key :
                filterableBreweriesMap.keySet()) {
            if(key == uniqueID) {
               if(addFavoriteBrewery(filterableBreweriesMap.get(key))) {
                   if(update) addFavorite(context, uniqueID);
               }
                return true;
            }
        }
        return false;
    }

    private static filteredDir fetchBreweryByUniqueID(final int uniqueID) {
        if(brewJsons.get(uniqueID) != null){
            long now = new Date().getTime();
            long millisIn2Days = 2L * 24 * 60 * 60 * 1000;
            long saveDate = 0;
            saveDate = brewJsons.get(uniqueID).optLong("LastUpdated", 0L);
            if(saveDate < (now - millisIn2Days)){
                //too old
            } else {
                filterableBreweriesMap.putIfAbsent(uniqueID, makeFilteredDir(brewJsons.get(uniqueID)));
                return filterableBreweriesMap.get(uniqueID);
            }
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient().newBuilder()
                        .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                        .build();
                Request getID = new Request.Builder()
                        .url(dbEndpoint + "brewery/" + uniqueID)
                        .get()
                        //.addHeader("x-apikey", API_KEY)
                        .addHeader("cache-control", "no-cache")
                        .build();
                try {
                    Response response = client.newCall(getID).execute();
                    String s = response.body().string();
                    JSONObject jso = new JSONObject(s);
                    makeFilteredDir(jso);
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
        try {
            t.join();
            return isBreweryLoaded(uniqueID);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return isBreweryLoaded(uniqueID);
        }
    }
    public static void addFavorite(final Context context, final int UniqueID) {
        new Thread(new Runnable() {
            @Override
            public void run() {
        final SharedPreferences sp = context.getSharedPreferences("RegisteredUser", MODE_PRIVATE);

        String email = sp.getString("Email", "");
        String checkid = sp.getString("_id", "");
        if (!email.equals("")) {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                    .build();
            String json = "{\"email\":\"" + email + "\",\"UniqueID\":" + UniqueID + "}";
            JSONObject jso = null;
            try {
                jso = new JSONObject(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            final RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"), jso.toString());
                Response response;
            Request request = new Request.Builder()
                    .url(dbEndpoint + "add_favorite")
                    .post(body)
                    //.addHeader("x-apikey", API_KEY)
                    .addHeader("cache-control", "no-cache")
                    .build();
            try {
                response = client.newCall(request).execute();
                response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
            }
        }).start();
    }
    public static void removeFavorite(final Context context, final int UniqueID) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final SharedPreferences sp = context.getSharedPreferences("RegisteredUser", MODE_PRIVATE);

                String email = sp.getString("Email", "");
                String checkid = sp.getString("_id", "");
                if (!email.equals("")) {
                    OkHttpClient client = new OkHttpClient().newBuilder()
                            .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                            .build();
                    String json = "{\"email\":\"" + email + "\",\"UniqueID\":" + UniqueID + "}";
                    JSONObject jso = null;
                    try {
                        jso = new JSONObject(json);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    final RequestBody body = RequestBody.create(
                            MediaType.parse("application/json"), jso.toString());
                    Response response;
                    Request request = new Request.Builder()
                            .url(dbEndpoint + "remove_favorite")
                            .post(body)
                            //.addHeader("x-apikey", API_KEY)
                            .addHeader("cache-control", "no-cache")
                            .build();
                    try {
                        response = client.newCall(request).execute();
                        response.body().string();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //putOrPostTo(context, "https://quenched-8a19.restdb.io/rest/favorites", email, checkid);
                }
            }
        }).start();
    }

    public static boolean removeFavoriteBreweryByUniqueID(int uniqueID, Context context){
        for (Integer key :
                filterableBreweriesMap.keySet()) {
            if(key == uniqueID) {
                if(removeFavoriteBrewery(filterableBreweriesMap.get(key))){
                    removeFavorite(context, uniqueID);
                };
            }
        }
        return false;
    }
    public static boolean removeFavoriteBrewery(filteredDir newdir){
        for (int i = 0; i < favoritedBreweries.size(); i++) {
            if(favoritedBreweries.get(i).uniqueBreweryID == newdir.uniqueBreweryID) {
                favoritedBreweries.remove(i);
                return true;
            }
        }
        return false;
    }

    public static boolean checkFavoriteBreweryByUniqueID(int uniqueID){
        for (filteredDir dir :
                favoritedBreweries) {
            if(dir.uniqueBreweryID == uniqueID) {
                return true;
            }
        }
        return false;
    }

    private static int failed = 0;
    private void queryStateData(final String stateAbbrev) {
        //Log.e("queryStateData", " " + stateAbbrev);
        //Log.e("updateTimes", stateUpdateTimeJsons.toString());
        long now = new Date().getTime();
        long millisIn7Days = 7L * 24 * 60 * 60 * 1000;
        long saveDate = 0;
        Object o =  stateUpdateTimeJsons.get(stateAbbrev);
        if(o != null){
                saveDate = (long) o;
        }
            if(saveDate < (now - millisIn7Days)){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        OkHttpClient client = new OkHttpClient().newBuilder()
                                .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                                .build();
                        String api = dbEndpoint + "breweryByState/"+stateAbbrev;
                        Request request = new Request.Builder()
                                .url(api)
                                .get()
                                //.addHeader("x-apikey", API_KEY)
                                .addHeader("cache-control", "no-cache")
                                .build();
                        try {
                            Response response = client.newCall(request).execute();
                            String s;
                            if (response.body() == null || response.code() != 200) throw new AssertionError();
                            s = response.body().string();
                            JSONArray jsonArray = new JSONArray(s);
                            if(jsonArray.length() == 0) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        float timer = (float) Math.pow(++failed, 2);
                                        Toast.makeText(MainActivity.this, "Failed to load state data... Retrying in " + timer*3 + " seconds.", Toast.LENGTH_SHORT).show();
                                        new Timer().schedule(new TimerTask() {
                                            @Override
                                            public void run() {
                                                queryStateData(stateAbbrev);
                                                // this code will be executed after 2 seconds
                                            }
                                        }, (long)(timer*3000));
                                    }
                                });
                                return;
                            }
                            for(int i = 0; i < jsonArray.length(); i++){
                                JSONObject jso = (JSONObject) jsonArray.get(i);
                                jso.put("LastUpdated", new Date().getTime());
                                makeFilteredDir(jso);
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(getFragmentRefreshListener()!=null){
                                        getFragmentRefreshListener().notifyPrelimaryDataReady();
                                    }
                                }
                            });
                            stateLoadTimeJsons.put(stateAbbrev, new Date().getTime());
                            stateUpdateTimeJsons.put(stateAbbrev, new Date().getTime());
                            failed = 0;
                        } catch (Exception e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    float timer = (float) Math.pow(++failed, 2);
                                    Toast.makeText(MainActivity.this, "Failed to load state data... Retrying in " + timer*3 + " seconds.", Toast.LENGTH_SHORT).show();
                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            queryStateData(stateAbbrev);
                                            // this code will be executed after 2 seconds
                                        }
                                    }, (long)(timer*3000));
                                }
                            });
                            e.printStackTrace();
                        }
                    }
                }).start();
            } else {
                for (JSONObject jsonObject: brewJsons.values()
                     ) {
                    try {
                        if(jsonObject.getString("StateProvince").equals(stateAbbrev)) {
                            makeFilteredDir(jsonObject);
                        }
                    } catch (JSONException e) {
                        //do nothing
                    }
                }
                stateLoadTimeJsons.put(stateAbbrev, new Date().getTime());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(getFragmentRefreshListener()!=null){
                            getFragmentRefreshListener().notifyPrelimaryDataReady();
                        }
                    }
                });
        }
    }
    public static boolean addFavoriteBrewery(filteredDir newdir){
        for (filteredDir dir :
                favoritedBreweries) {
            if(dir.uniqueBreweryID == newdir.uniqueBreweryID) {
                return false;
            }
        }
        favoritedBreweries.add(newdir);
        return true;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        startingLon = location.getLongitude();
        startingLat = location.getLatitude();

        searchLatLongApiCall(startingLat, startingLon);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {

    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {

    }

    @Override
    public void loadNewLocation(String area, String state, double lat, double lon, boolean updateUserDefault) {
        if(updateUserDefault) updateStateFromState(MainActivity.this, area, state, lat, lon);
        else updateMapFromLatLng(MainActivity.this, area, state, lat, lon);
    }

    private void searchZipcodeApiCall(String query) {
        final OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                .build();
        String s = null;
        try {
            s = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //String api = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=" + s + "&key=" + "AIzaSyAdfG8uV9nCFS-tHLJDW_KlAro6jXBAb5c";
        String api = "https://nominatim.openstreetmap.org/search?&countrycodes=us&q=" + query + "&limit=1&format=json&addressdetails=1";
        final Request getID = new Request.Builder()
                .url(api)
                .get()
                //.addHeader("x-apikey", "7f85916f94b41f0739704a6aa997e749e67fb")
                .addHeader("cache-control", "no-cache")
                .build();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Response response = client.newCall(getID).execute();
                    String s = response.body().string();
                    JSONArray res = new JSONArray(s);
                    JSONObject orig = res.getJSONObject(0);
                    if(res.length() == 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Cannot find zipcode: "+query, Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                    final double lat = Double.parseDouble(orig.getString("lat"));
                    final double lon = Double.parseDouble(orig.getString("lon"));
                    startingLat = lat;
                    startingLon = lon;
                    JSONObject jso = orig.getJSONObject("address");
                    final String state = jso.getString("state");
                    String area = jso.optString("postcode", "");
                    if(area.equals("")) area = jso.optString("county", "");
                    if(area.equals("")) area = jso.optString("town", "Unknown");
                    final String finalArea = area;
                    loadNewLocation(finalArea, state, lat, lon, true);
                } catch (IOException | JSONException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "No Wifi or Cellular access found", Toast.LENGTH_SHORT).show();
                        }
                    });
                    e.printStackTrace();
                }
            }
        }).start();
    }
    private void searchLatLongApiCall(double lat, double lon) {
        final OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                .build();
        //String api = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=" + s + "&key=" + "AIzaSyAdfG8uV9nCFS-tHLJDW_KlAro6jXBAb5c";
        String api = "https://nominatim.openstreetmap.org/reverse?lat=" + lat + "&lon=" + lon + "&zoom=16&format=json&addressdetails=1";
        final Request getID = new Request.Builder()
                .url(api)
                .get()
                //.addHeader("x-apikey", "7f85916f94b41f0739704a6aa997e749e67fb")
                .addHeader("cache-control", "no-cache")
                .build();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Response response = client.newCall(getID).execute();
                    String s = response.body().string();
                    JSONObject res = new JSONObject(s);
                    JSONObject jso = res.getJSONObject("address");
                    if(response.code() != 200) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Cannot find lat/lon", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                    startingLon = lon;
                    //JSONObject jso = orig.getJSONObject("address");
                    final String state = jso.getString("state");
                    String area = jso.optString("postcode", "");
                    if(area.equals("")) area = jso.optString("county", "");
                    if(area.equals("")) area = jso.optString("town", "Unknown");
                    currentLocationZipcode = area;
                    currentState = state;
                    final String finalArea = area;
                    loadNewLocation(finalArea, state, lat, lon, true);
                } catch (IOException | JSONException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Cannot update zipcode without wifi or cellular access", Toast.LENGTH_SHORT).show();
                        }
                    });
                    e.printStackTrace();
                }
            }
        }).start();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
            favoritedBreweries.clear();
            getFavorites(MainActivity.this);
            checkIfRegistered();
        }
    }
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            submitUserEmail(account);
            // Signed in successfully, show authenticated UI.
            updateUI(account, false);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "signInResult:failed code=" + e.getStatusCode(), Toast.LENGTH_LONG).show();
                }
            });
            Log.w("handleSignIn", "signInResult:failed code=" + e.getStatusCode());
            updateUI(null, false);
        }
    }

    private void submitUserEmail(final GoogleSignInAccount account) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final SharedPreferences.Editor spe = getSharedPreferences("RegisteredUser", MODE_PRIVATE).edit();
                OkHttpClient client = new OkHttpClient().newBuilder()
                        .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                        .build();
                String json = "{\"email\":\"" + account.getEmail() + "\",\"name\":\""+account.getGivenName() + "\"}";;
                JSONObject jso = null;
                try {
                    jso = new JSONObject(json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                final RequestBody body = RequestBody.create(
                        MediaType.parse("application/json"), jso.toString());
                try{
                    Response response;
                        Request request = new Request.Builder()
                                .url(dbEndpoint+"login")
                                .post(body)
                                //.addHeader("x-apikey", API_KEY)
                                .addHeader("cache-control", "no-cache")
                                .build();
                        response = client.newCall(request).execute();
                        JSONObject jsoresult = new JSONObject(response.body().string());
                        if(response.code() == 200){
                            spe.putString("_id", account.getId()).apply();
                            spe.putString("email", account.getEmail()).apply();
                            JSONObject jsa = (JSONObject) jsoresult.optJSONObject("Roles");
                            if(jsa != null) spe.putString("Roles", jsa.toString()).apply();
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Status " + response.code() + response.body(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    //Objects.requireNonNull(response.body()).string();
                } catch (IOException | JSONException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Error " + String.valueOf(e), Toast.LENGTH_LONG).show();
                        }
                    });
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static ArrayList<MainActivity.filteredDir> sortFeaturedFirst(ArrayList<MainActivity.filteredDir> dirs){
        ArrayList<MainActivity.filteredDir> newlist = new ArrayList<>();
        ArrayList<MainActivity.filteredDir> restoflist = new ArrayList<>();
        for(MainActivity.filteredDir dir: dirs){
            if(dir.gold) newlist.add(dir);
            else restoflist.add(dir);
        }
        newlist.addAll(restoflist);
        return newlist;
    }

    public static filteredDir makeFilteredDir(JSONObject jso) {
        double lat = 0;
        double lon = 0;
        String zipcode = "";
        lat = jso.optDouble("Latitude", 0.0);
        lon = jso.optDouble("Longitude", 0.0);
        String s = jso.optString("address", "No Address Found");
        zipcode = jso.optString("Zip", "0");
        int uniqueID = jso.optInt("UniqueID", -1);

        filteredDir newDir = new filteredDir(jso, jso.optString("Brewery","NO NAME FOUND"), uniqueID, null, jso.optBoolean("Crawable", false),
                jso.optBoolean("In House Kitchen", false),
                jso.optBoolean("Family Friendly", false),
                jso.optBoolean("Dog Friendly", false),
                jso.optBoolean("Outdoor Seating", false), lat, lon, zipcode,
                jso.optBoolean("Bronze", false),
                jso.optBoolean("Silver", false),
                jso.optBoolean("Gold", false) );
        filterableBreweriesMap.put(uniqueID, newDir);
        brewJsons.put(uniqueID, jso);
        // Get a handler that can be used to post to the main thread

        return newDir;
    }

    interface NotifyImageUpdate{
        void notifyUpdate();
        void notifyPrelimaryDataReady();
    }

    public static Map<Integer, JSONObject> brewJsons;
    public static Map<String, Integer> brewNames = new HashMap<>();
    public void loadStateUpdateTimes(){
        stateUpdateTimeJsons = loadStateUpdateTime();
    }
    public void loadBreweryNames(){
        SharedPreferences prefs = getSharedPreferences("Data", MODE_PRIVATE);
        brewJsons = loadBreweryDataFromLocal();
        //Log.e("Names", String.valueOf(brewNames.size()));
        if(brewJsons.size() > 10000){
            return;
        } else {
            //Log.e("FETCHING BREWNAMES", "WE BE FETCHING");
            new Thread(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void run() {
                    OkHttpClient client = new OkHttpClient().newBuilder()
                            .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                            .build();
                    try{
                        Response response;
                        Request request = new Request.Builder()
                                .url(dbEndpoint+"breweryNames")
                                .get()
                                //.addHeader("x-apikey", API_KEY)
                                .addHeader("cache-control", "no-cache")
                                .build();
                        response = client.newCall(request).execute();
                        JSONArray jsaresult = new JSONArray(response.body().string());
                        if(response.code() == 200){
                            brewJsons = new HashMap<>();
                            brewNames = new HashMap<>();
                            for(int i = 0; i < jsaresult.length(); i++){
                                Integer uniq = Integer.valueOf(((JSONObject) jsaresult.get(i)).getString("UniqueID"));
                                String name = ((JSONObject) jsaresult.get(i)).getString("Brewery");
                                brewJsons.putIfAbsent(uniq, (JSONObject) jsaresult.get(i));
                                brewNames.put(name, uniq);
                            }
                            saveBreweryDataLocal();
                        }
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
    private void saveBreweryDataLocal(){
        SharedPreferences.Editor editor = getSharedPreferences("BrewData", 0).edit();
        for( Map.Entry entry : brewJsons.entrySet() )
            editor.putString(String.valueOf(entry.getKey()), entry.getValue().toString());
        editor.apply();
    }


    private Map<Integer, JSONObject> loadBreweryDataFromLocal(){
        Map<Integer, JSONObject> outputMap = new HashMap<Integer, JSONObject>();
        SharedPreferences prefs = getSharedPreferences("BrewData", 0);
        for( Map.Entry entry : prefs.getAll().entrySet() ) {
            Integer key = Integer.parseInt((String) entry.getKey());
            try {
                JSONObject value = new JSONObject((String) entry.getValue());
                brewNames.put(value.getString("Brewery") , value.getInt("UniqueID"));
                outputMap.put(key, value);
            } catch (JSONException e) {
                e.printStackTrace();
            };
        }
        return outputMap;
    }
    public static Map<String, Long> stateUpdateTimeJsons;
    public static Map<String, Long> stateLoadTimeJsons = new HashMap<>();
    private void saveStateUpdateTime(){
        SharedPreferences pSharedPref = getApplicationContext().getSharedPreferences("MyVariables", Context.MODE_PRIVATE);
        if (pSharedPref != null && stateUpdateTimeJsons != null){
            JSONObject jsonObject = new JSONObject(stateUpdateTimeJsons);
            String jsonString = jsonObject.toString();
            SharedPreferences.Editor editor = pSharedPref.edit();
            editor.remove("stateUpdate").commit();
            editor.putString("stateUpdate", jsonString);
            editor.commit();
        }
    }
    private Map<String, Long> loadStateUpdateTime(){
        Map<String, Long> outputMap = new HashMap<>();
        SharedPreferences pSharedPref = getApplicationContext().getSharedPreferences("MyVariables", Context.MODE_PRIVATE);
        try{
            if (pSharedPref != null){
                String jsonString = pSharedPref.getString("stateUpdate", (new JSONObject()).toString());
                JSONObject jsonObject = new JSONObject(jsonString);
                Iterator<String> keysItr = jsonObject.keys();
                while(keysItr.hasNext()) {
                    String key = keysItr.next();
                    Long value = (Long) jsonObject.get(key);
                    outputMap.put(key, value);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return outputMap;
    }



    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     *
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     * @returns Distance in MILES
     */
    public static double distance(double lat1, double lat2, double lon1,
                                  double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);
        double totalMeters = Math.sqrt(distance);
        return round(totalMeters/1609.0, 1);
    }
    public static double distanceFromYou(double lat, double lon){
        return round( distance(startingLat, lat, startingLon, lon, 0.0, 0.0), 1);
    }
    private static double round (double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }
    public Location getLocation() {
        try {
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

            // getting GPS status
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            } else {
                this.canGetLocation = true;
                // First get location from Network Provider
                if (isNetworkEnabled) {
                    //check the network permission
                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions((Activity) mContext, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
                    }
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);


                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }

                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        //check the network permission
                        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions((Activity) mContext, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
                        }
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                        //Log.e("GPS Enabled", "GPS Enabled");
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);

                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return location;
    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     * */

    public void stopUsingGPS(){
        if(locationManager != null){
            locationManager.removeUpdates(MainActivity.this);
        }
    }

    /**
     * Function to get latitude
     * */

    public double getLatitude(){
        if(location != null){
            latitude = location.getLatitude();
        }

        // return latitude
        return latitude;
    }

    /**
     * Function to get longitude
     * */

    public double getLongitude(){
        if(location != null){
            longitude = location.getLongitude();
        }

        // return longitude
        return longitude;
    }

    /**
     * Function to check GPS/wifi enabled
     * @return boolean
     * */

    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    /**
     * Function to show settings alert dialog
     * On pressing Settings button will lauch Settings Options
     * */

    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveBreweryDataLocal();
        saveStateUpdateTime();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveBreweryDataLocal();
        saveStateUpdateTime();
    }
}