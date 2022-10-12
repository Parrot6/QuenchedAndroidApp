package com.im.quenched;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.im.quenched.MainActivity.isBreweryLoaded;
import static com.im.quenched.MainActivity.makeFilteredDir;

public class SearchResults extends AppCompatActivity implements SearchRvWithAdAdapter.searchRvWithAdAdapterCallback {
    private static final int REQUEST_CODE = 567;
    RecyclerView results;
    SearchRvWithAdAdapter adapter;
    ProgressBar pb;
    ArrayList<MainActivity.filteredDir> resultsBreweries;
    SearchRvWithAdAdapter.searchRvWithAdAdapterCallback listener;
    Context context;
    TextView title;
    private static final String ADMOB_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110";

    // The number of native ads to load.
    public static final int NUMBER_OF_ADS = 5;

    // List of MenuItems and native ads that populate the RecyclerView.
    private List<Object> mRecyclerViewItems = new ArrayList<>();

    // List of native ads that have been successfully loaded.
    private List<UnifiedNativeAd> mNativeAds = new ArrayList<>();
    AdLoader adLoader;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("recreateing","dd");
        setContentView(R.layout.activity_search_results);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.drawable.quenched_logo4);
        context = this;
        pb = findViewById(R.id.progressBar_searchResults);
        MainActivity.setFragmentRefreshListener(new MainActivity.NotifyImageUpdate() {
            @Override
            public void notifyUpdate() {
                //Toast.makeText(SearchResults.this, "updatedSR", Toast.LENGTH_SHORT).show();
                updateImages();
            }

            @Override
            public void notifyPrelimaryDataReady() {
                adapter.notifyDataSetChanged();
            }
        });
        adLoader = new AdLoader.Builder(SearchResults.this, getString(R.string.AdmobID))
                .forUnifiedNativeAd(new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
                    @Override
                    public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                        mNativeAds.add(unifiedNativeAd);
                        //Toast.makeText(SearchResults.this, "loaded a ad", Toast.LENGTH_SHORT).show();
                        insertAdInMenuItems(unifiedNativeAd);
                        if(isFinishing()){
                            //Toast.makeText(SearchResults.this, "finishing", Toast.LENGTH_SHORT).show();
                            //insertAdsInMenuItems();
                            //showads
                        }
                        // Show the ad.
                        if (isDestroyed()) {
                            unifiedNativeAd.destroy();
                            return;
                        }
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        // Handle the failure by logging, altering the UI, and so on.
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        // Methods in the NativeAdOptions.Builder class can be
                        // used here to specify individual options settings.
                        .build())
                .build();

        listener = this;

        resultsBreweries = new ArrayList<>();

   /*     ArrayList<String> names = new ArrayList<>();
        ArrayList<Bitmap> images = new ArrayList<>();
        for(MainActivity.filteredDir res: resultsBreweries){
            names.add(res.Brewery);
            images.add(res.thisBitmap);
        }*/
        results = findViewById(R.id.rv_search_results);
        GridLayoutManager glm = new GridLayoutManager(SearchResults.this, 2);
       glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if(adapter.getItemViewType(position) == 2) return 2;
                return 1;
            }
        });
        results.setLayoutManager(glm);
        adapter = new SearchRvWithAdAdapter(SearchResults.this, mRecyclerViewItems, listener);
        //adapter.setData(mRecyclerViewItems);
        // Update the RecyclerView item's list with native ads.
        results.setAdapter(adapter);
        Intent intent = getIntent();
        Search.FilterBarSelections fb;
        title = findViewById(R.id.textView_showingresults);
        if(getIntent().getExtras() != null) {
            if(intent.getAction().equals(String.valueOf(Search.SearchType.FilterBar))){
                fb = (Search.FilterBarSelections) intent.getSerializableExtra("FilterBarSelections");
                StringBuilder sb = new StringBuilder();
                if(fb.crawlCheck) sb.append(" - Crawlable");
                if(fb.dogCheck) sb.append(" - Dog Friendly");
                if(fb.familyCheck) sb.append(" - Family Friendly");
                if(fb.kitchenCheck) sb.append(" - In House Kitchen");
                if(fb.outdoorCheck) sb.append(" - Outdoor Seating");
                if(fb.distance > 0) {
                    sb.append(" within " + fb.distance);
                    sb.append(" miles");
                } else sb.append(" statewide");
                title.setText(String.format("Showing results for%s", sb.toString()));
                queryByFilter(fb);
            }
            if(intent.getAction().equals(String.valueOf(Search.SearchType.SearchByName))){
                String s = intent.getStringExtra("Name");
                if(s.length() == 0) s = "*";
                title.setText(String.format("Showing results for \"%s\"", s));
                queryByName(s);
            }
        }

    }
    public void queryByFilter(final Search.FilterBarSelections bar){
        Thread t = new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient().newBuilder()
                        .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                        .build();
                ArrayList<String> filtersOn = new ArrayList<>();
                if(bar.crawlCheck) filtersOn.add("crawlable : true");
                if(bar.outdoorCheck) filtersOn.add("outdoorSeating : true");
                if(bar.familyCheck) filtersOn.add("familyFriendly : true");
                if(bar.dogCheck) filtersOn.add("dogFriendly : true");
                if(bar.kitchenCheck) filtersOn.add("inHouseKitchen : true");
                String json = "{" + String.join(", ", filtersOn) + "}";
                JSONObject jsoo = null;
                try {
                    jsoo = new JSONObject(json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.e("finalstr", json);
                final RequestBody body = RequestBody.create(
                        jsoo.toString(), MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url(MainActivity.dbEndpoint + "breweryByFilter")
                        .post(body)
                        //.addHeader("x-apikey", "7f85916f94b41f0739704a6aa997e749e67fb")
                        .addHeader("cache-control", "no-cache")
                        .build();

                try {
                    Response response = client.newCall(request).execute();
                    String s;
                    if (response.body() == null) throw new AssertionError();
                    s = response.body().string();
                    Log.e("Response", s);
                    JSONArray jsonArray = new JSONArray(s);
                    Log.e("NUM OF BREWERIES SEARCH", String.valueOf(jsonArray.length()));
                    if(jsonArray.length() == 0) {
                        updateRVafterResults();
                        return;
                    }
                    for(int i = 0; i < jsonArray.length(); i++){
                        JSONObject jso = (JSONObject) jsonArray.get(i);
                        int uniq = jso.getInt("UniqueID");
                        MainActivity.filteredDir exists = isBreweryLoaded(uniq);
                        if(exists == null) {
                            JSONArray url = jso.optJSONArray("Image");;
                            if(url == null) {
                                url = new JSONArray();
                                url.put("");
                            }
                            final MainActivity.filteredDir newdir = makeFilteredDir(jso);
                            if(bar.distance == -1 || MainActivity.distance( MainActivity.startingLat, newdir.lat, MainActivity.startingLon, newdir.lon, 0.0, 0.0) <= bar.distance) resultsBreweries.add(newdir);
                            if(!url.getString(0).equals("")) {
                                JSONArray finalUrl = url;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Picasso.get().load(finalUrl.getString(0)).into(new Target() {
                                                @Override
                                                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                                    newdir.setThisBitmap(bitmap);
                                                    adapter.notifyDataSetChanged();
                                                }

                                                @Override
                                                public void onBitmapFailed(Exception e, Drawable errorDrawable) {

                                                }

                                                @Override
                                                public void onPrepareLoad(Drawable placeHolderDrawable) {

                                                }
                                            });
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });

                            }
                        } else {
                            if(bar.distance == -1 || MainActivity.distance(MainActivity.startingLat, exists.lat, MainActivity.startingLon, exists.lon, 0.0, 0.0) <= bar.distance) resultsBreweries.add(exists);
                        }
                    }
                    updateRVafterResults();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    public void queryByName(final String name){
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient().newBuilder()
                        .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                        .build();
                String api = MainActivity.dbEndpoint + "brewerySearch/" + name;
                Log.e("api", api);
                Request request = new Request.Builder()
                        .url(api)
                        .get()
                        //.addHeader("x-apikey", "7f85916f94b41f0739704a6aa997e749e67fb")
                        .addHeader("cache-control", "no-cache")
                        .build();
                try {
                    Response response = client.newCall(request).execute();
                    String s;
                    if (response.body() == null) throw new AssertionError();
                    s = response.body().string();
                    JSONArray jsonArray = new JSONArray(s);
                    Log.e("NUM OF BREWERIES SEARCH", String.valueOf(jsonArray.length()));
                    if(jsonArray.length() == 0 || response.code() != 200) {
                        updateRVafterResults();
                        return;
                    }
                    for(int i = 0; i < jsonArray.length(); i++){
                        JSONObject jso = (JSONObject) jsonArray.get(i);
                        int uniq = jso.getInt("UniqueID");
                        if(isBreweryLoaded(uniq) == null) {
                            JSONArray url = jso.optJSONArray("Image");;
                            if(url == null) {
                                url = new JSONArray();
                                url.put("");
                            }
                            final MainActivity.filteredDir newdir = makeFilteredDir(jso);
                            resultsBreweries.add(newdir);
                            if(!url.getString(0).equals("")) {
                                Picasso.get().load(url.getString(0)).into(new Target() {
                                    @Override
                                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                        newdir.setThisBitmap(bitmap);
                                        adapter.notifyDataSetChanged();
                                    }

                                    @Override
                                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {

                                    }

                                    @Override
                                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                                    }
                                });
                            }
                        } else resultsBreweries.add(isBreweryLoaded(uniq));
                    }
                    updateRVafterResults();
                } catch (Exception e) {
                    updateRVafterResults();
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    private void updateRVafterResults() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e("resultBreweries", String.valueOf(resultsBreweries.size()));
                if(resultsBreweries.size() == 0) title.append("\n\n... No Results Found ...\n\n");
                Toast.makeText(context, String.valueOf((int) Math.ceil((float) resultsBreweries.size()/4f)) + " loading", Toast.LENGTH_SHORT ).show();
                mRecyclerViewItems.addAll(resultsBreweries);
                adapter = new SearchRvWithAdAdapter(SearchResults.this, mRecyclerViewItems, listener);
                results.setAdapter(adapter);
                pb.setVisibility(View.GONE);
                adLoader.loadAds(new AdRequest.Builder().build(), (int) Math.ceil((float) resultsBreweries.size()/4f));
                //adapter.notifyDataSetChanged();
            }
        });
    }

    public void updateImages(){
        for (MainActivity.filteredDir dir :
                resultsBreweries) {
            dir.getThisBitmap();
        }
        adapter.notifyDataSetChanged();
    }

    int addedsofar = 0;
    private void insertAdInMenuItems(UnifiedNativeAd ad) {
        //int offset = (mRecyclerViewItems.size() / mNativeAds.size()) + 1;
        int offset = addedsofar + addedsofar*4 + 4;
        adapter.insertLoadedAd(ad, Math.min(offset, adapter.getItemCount()));
        addedsofar++;
        //adapter.setData(mRecyclerViewItems);
        //adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        Log.e("DESTROY","called");
        mNativeAds.clear();
        mRecyclerViewItems.clear();
        finish();
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("returnedFromREsults","Ada");
    }

    @Override
    public void openBrewery(MainActivity.filteredDir dir) {
        Intent intent = new Intent(SearchResults.this, ViewBrewery.class);
        intent.putExtra("jso", dir.jso.toString());
        intent.putExtra("Index", dir.uniqueBreweryID);
        intent.putExtra("layoutPosition", -1);
        intent.setAction("Preview");
        startActivity(intent);
    }
}
