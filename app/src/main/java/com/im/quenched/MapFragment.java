package com.im.quenched;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.MarkerManager;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MapFragment extends Fragment implements OnMapReadyCallback, MapListViewAdapter.directoryRvAdapterCallback {
    private static final int FINE_AND_COARSE_REQUEST = 667;
    ArrayList<filteredPin> pins = new ArrayList<>();
    ArrayList<filteredPin> sortedPins = new ArrayList<>();
    private ClusterManager<filteredPin> clusterManager;
    FilterBar filterBar;
    GoogleMap googleMap;
    Button swapViews;

    private static final int REQUEST_CODE = 100;
    private RecyclerView RV;
    ArrayList<MainActivity.filteredDir> originalDirs = new ArrayList<>();
    MapListViewAdapter adapter;
    MapListViewAdapter.directoryRvAdapterCallback listener;
    Context context;

    SearchView searchLoc;
    ImageButton resetLocation;
    ImageView clearText;
    int baseZoomLevel = 10;
    ImageButton toggleFilterBar;

    ConstraintLayout infoPopup;
    static int lastOpened = -1;
    static Marker lastMarker = null;
    static BitmapDescriptor originalLastMarkerPin;
    TextView phone;
    TextView address;
    TextView brewName;
    ImageView breweryImage;
    ImageButton closeInfoPopup;
    Button openBrewery;
    mapListener mainListener;
    TextView filterbarlabel;
    public static boolean shouldCluster = true;

    @Override
    public void openBrewery(MainActivity.filteredDir dir) {
        Intent intent = new Intent(getActivity(), ViewBrewery.class);
        intent.putExtra("jso", dir.jso.toString());
        intent.putExtra("Index", dir.uniqueBreweryID);
        intent.setAction("Preview");
        startActivityForResult(intent, REQUEST_CODE);
    }


    public static class filteredPin implements ClusterItem {
        MainActivity.filteredDir dir;
        MarkerOptions thisPin;
        //Marker marker = null;
        boolean isImageLoaded;
        int tag;

        filteredPin(MarkerOptions thisPin, boolean isImageLoaded, /*Marker marker,*/ MainActivity.filteredDir thisdir) {
            /*this.marker = marker;*/
            this.isImageLoaded = isImageLoaded;
            this.thisPin = thisPin;
            dir = thisdir;
            ClusterItem s = this;
        }

        public filteredPin setTag(int tag) {
            this.tag = tag;
            return this;
        }

        public int getTag() {
            return tag;
        }

        @Override
        public LatLng getPosition() {
            return thisPin.getPosition();
        }

        @Override
        public String getTitle() {
            return thisPin.getTitle();
        }

        @Override
        public String getSnippet() {
            return thisPin.getSnippet();
        }
    }

    public class CustomClusterRenderer extends DefaultClusterRenderer<filteredPin> {
        private final Context mContext;

        public CustomClusterRenderer(Context context, GoogleMap map,
                                     ClusterManager<filteredPin> clusterManager) {
            super(context, map, clusterManager);
            mContext = context;
        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster<filteredPin> cluster) {
            if (!shouldCluster) return false;
            return super.shouldRenderAsCluster(cluster);
        }

        @Override
        protected void onBeforeClusterItemRendered(filteredPin item, MarkerOptions markerOptions) {
            markerOptions.icon(item.thisPin.getIcon());
            super.onBeforeClusterItemRendered(item, markerOptions);
        }
    }

    public MapFragment(mapListener listener) {
        // Required empty public constructor
        this.mainListener = listener;
    }

    private void sortDirectory() {
        adapter.updateFilter(filterBar);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateImages();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the SupportMapFragment and request notification
        // when the map is ready to be used.
        listener = this;
        context = getActivity();
    }

    public void updateImages() {
        for (int i = 0; i < originalDirs.size(); i++) {
            originalDirs.get(i).getThisBitmap();
        }
        adapter.notifyDataSetChanged();
        sortDirectory();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_map_v2, container, false);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapFragment.this);
        v.findViewById(R.id.map).setVisibility(View.INVISIBLE); //DIRECTORY SHOW FIRST
        filterBar = new FilterBar(v, true) {
            @Override
            void doSomething(View view) {
                closeInfoPopup();
                sortDirectory();
                sortPins();
            }
        };
        MainActivity.setFragmentRefreshListener(new MainActivity.NotifyImageUpdate() {
            @Override
            public void notifyUpdate() {
                if (getActivity() == null) return;
                updateImages();
                //updateImagePins();
            }

            @Override
            public void notifyPrelimaryDataReady() {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        populateDataAndSortFrom(originalDirs);
                        filterBar.refreshLocation();
                        if (googleMap != null) {
                            createPins(googleMap);
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(MainActivity.map_startingLat, MainActivity.map_startingLon), baseZoomLevel+.1f));
                            checkAndEnableLocation();
                        }
                        sortPins();
                        sortDirectory();
                    }
                });
            }
        });
        resetLocation = v.findViewById(R.id.imageView_resetLocation);
        resetLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetLocation.setVisibility(View.GONE);
                clearPreviousSearch(true);
            }
        });
        if (!MainActivity.currentLocationZipcode.equals(MainActivity.map_currentLocationZipcode))
            resetLocation.setVisibility(View.VISIBLE);
        else resetLocation.setVisibility(View.GONE);
        swapViews = v.findViewById(R.id.button_swap_views);
        swapViews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (swapViews.getText().toString().toLowerCase().equals("map view"))
                    swapViews.setText("List View");
                else if (swapViews.getText().toString().toLowerCase().equals("list view"))
                    swapViews.setText("Map View");
                toggleViews(v);
            }
        });
        infoPopup = v.findViewById(R.id.constraintLayout_mapPopup);
        infoPopup.setVisibility(View.GONE);
        brewName = v.findViewById(R.id.textView_mapBreweryName);
        phone = v.findViewById(R.id.textView_mapPhoneNumber);
        phone.setAutoLinkMask(Linkify.PHONE_NUMBERS);
        address = v.findViewById(R.id.textView_mapAddress);
        breweryImage = v.findViewById(R.id.imageView_mapBreweryPic);
        closeInfoPopup = v.findViewById(R.id.imageButton_mapCloseWindow);
        closeInfoPopup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeInfoPopup();
            }
        });
        openBrewery = v.findViewById(R.id.button_mapOpenProfile);
        openBrewery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openBrewery(originalDirs.get(lastOpened));
            }
        });
        searchLoc = v.findViewById(R.id.editText_searchField);
        searchLoc.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                searchLocation(v);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
        filterbarlabel = v.findViewById(R.id.filterbarminimizeLabel);
        filterbarlabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View fb = v.findViewById(R.id.include2);
                if (fb.getVisibility() == View.VISIBLE) {
                    fb.setVisibility(View.GONE);
                    toggleFilterBar.setImageResource(R.drawable.ic_iconmonstr_arrow_63);
                } else {
                    fb.setVisibility(View.VISIBLE);
                    toggleFilterBar.setImageResource(R.drawable.ic_iconmonstr_arrow_65);
                }
            }
        });
        toggleFilterBar = v.findViewById(R.id.imageView_filterbarHideToggle);
        toggleFilterBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View fb = v.findViewById(R.id.include2);
                if (fb.getVisibility() == View.VISIBLE) {
                    fb.setVisibility(View.GONE);
                    toggleFilterBar.setImageResource(R.drawable.ic_iconmonstr_arrow_63);
                } else {
                    fb.setVisibility(View.VISIBLE);
                    toggleFilterBar.setImageResource(R.drawable.ic_iconmonstr_arrow_65);
                }
            }
        });

        adapter = new MapListViewAdapter(originalDirs, listener);
        RV = v.findViewById(R.id.rv_directory_listview);
        RV.setLayoutManager(new GridLayoutManager(v.getContext(), 2));
        RV.setAdapter(adapter);
        populateDataAndSortFrom(originalDirs);
        toggleFilterBar.callOnClick();
        swapViews.callOnClick();
        // Inflate the layout for this fragment
        return v;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void searchLocation(View v) {
        if (!searchLoc.getQuery().toString().trim().equals("")) {
            if (RV.getVisibility() == View.VISIBLE) resetLocation.setVisibility(View.VISIBLE);
            searchPlaceApiCall(searchLoc.getQuery().toString().trim());
            InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void populateDataAndSortFrom(ArrayList<MainActivity.filteredDir> originalDirs) {
        originalDirs.clear();
        originalDirs.addAll(MainActivity.filterableBreweriesMap.values());
        originalDirs.sort(new Comparator<MainActivity.filteredDir>() {
            @Override
            public int compare(MainActivity.filteredDir filteredDir, MainActivity.filteredDir t1) {
                if (filteredDir.getDistance(true) > t1.getDistance(true)) return 1;
                else return -1;
            }
        });
        adapter.notifyDataSetChanged();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void searchPlaceApiCall(String query) {
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
        String api = "https://nominatim.openstreetmap.org/search?countrycodes=us&q=" + query + "&limit=1&format=json&addressdetails=1";
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
                    if (res.length() == 0) return;

                    final double lat = Double.parseDouble(orig.getString("lat"));
                    final double lon = Double.parseDouble(orig.getString("lon"));
                    JSONObject jso = orig.getJSONObject("address");
                    final String state = jso.getString("state");
                    String area = jso.optString("town", "");
                    if(area.equals("")) area = jso.optString("county", "");
                    if(area.equals("")) area = jso.optString("postcode", "Unknown");
                    final String finalArea = area;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), baseZoomLevel));
                            mainListener.loadNewLocation(finalArea, state, lat, lon, false);
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), baseZoomLevel+.1f));
                        }
                    });
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void toggleViews(View v) {
        if (RV.getVisibility() == View.VISIBLE) {
            RV.setVisibility(View.GONE);
            v.findViewById(R.id.map).setVisibility(View.VISIBLE);
            resetLocation.setVisibility(View.GONE);
        } else {
            v.findViewById(R.id.map).setVisibility(View.GONE);
            if (!MainActivity.currentLocationZipcode.equals(MainActivity.map_currentLocationZipcode))
                resetLocation.setVisibility(View.VISIBLE);
            RV.setVisibility(View.VISIBLE);
        }
    }

    private void closeInfoPopup() {
        infoPopup.setVisibility(View.GONE);
        if (lastMarker != null && lastMarker.getTag() != null && originalLastMarkerPin != null) {
            lastMarker.setIcon(originalLastMarkerPin);
            lastMarker.setZIndex(0.0f);
        }
        lastOpened = -1;
    }

    private void populateInfoPopup(Marker mark, int pos) {
        final int position = pos;
        if (lastMarker != null && originalLastMarkerPin != null) {
            lastMarker.setIcon(originalLastMarkerPin);
            lastMarker.setZIndex(0.0f);
        }
        if (lastOpened != position) {
            originalLastMarkerPin = pins.get(position).thisPin.getIcon();
            if (pins.get(position).dir.gold) {
                mark.setIcon(BitmapDescriptorFactory.fromBitmap(makeSelectedStarPin()));
            } else {
                mark.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            }
            mark.setZIndex(2.0f);
            MainActivity.filteredDir thisPin = originalDirs.get(position);
            breweryImage.setImageBitmap(thisPin.getThisBitmap());
            brewName.setText(thisPin.Brewery);
            phone.setText(thisPin.jso.optString("phone_Number", "Phone# Unknown"));
            address.setText(String.format("%s, %s, %s %s", thisPin.jso.optString("address", "Street Address Unknown"), thisPin.jso.optString("City", "City Unknown"), thisPin.jso.optString("StateProvince", "State Unknown"), thisPin.jso.optString("Zip", "Zip Unknown")));
            infoPopup.setVisibility(View.VISIBLE);
            int filterCount = 0;
            TextView crawlableFil = infoPopup.findViewById(R.id.info_popupBreweryTypeCrawlable);
            if (thisPin.crawlable) {
                crawlableFil.setVisibility(View.VISIBLE);
                filterCount++;
            } else {
                crawlableFil.setVisibility(View.GONE);
            }
            TextView kitchenFil = infoPopup.findViewById(R.id.info_popupBreweryTypeKitchen);
            if (thisPin.kitched) {
                filterCount++;
                kitchenFil.setVisibility(View.VISIBLE);
            } else {
                kitchenFil.setVisibility(View.GONE);
            }
            TextView outdoorFil = infoPopup.findViewById(R.id.info_popupBreweryTypeOutdoorSeating);
            if (thisPin.outdoorSeating) {
                filterCount++;
                outdoorFil.setVisibility(View.VISIBLE);
            } else {
                outdoorFil.setVisibility(View.GONE);
            }
            TextView familyFil = infoPopup.findViewById(R.id.info_popupBreweryTypeFamilyFriendly);
            if (thisPin.family) {
                filterCount++;
                familyFil.setVisibility(View.VISIBLE);
            } else {
                familyFil.setVisibility(View.GONE);
            }
            TextView dogFil = infoPopup.findViewById(R.id.info_popupBreweryTypeDogFriendly);
            if (thisPin.dog) {
                filterCount++;
                dogFil.setVisibility(View.VISIBLE);
            } else {
                dogFil.setVisibility(View.GONE);
            }
            infoPopup.findViewById(R.id.constraintLayout_filterholder).setVisibility(View.VISIBLE);
            if (filterCount >= 4) {
                float size = 12f;
                if (filterCount >= 5) size = 10f;
                crawlableFil.setTextSize(size);
                kitchenFil.setTextSize(size);
                outdoorFil.setTextSize(size);
                familyFil.setTextSize(size);
                dogFil.setTextSize(size);
            } else if (filterCount == 0) {
                infoPopup.findViewById(R.id.constraintLayout_filterholder).setVisibility(View.GONE);
            }
            lastMarker = mark;
            lastMarker.setTag("open");
            lastOpened = position;
        } else {
            infoPopup.setVisibility(View.GONE);
            lastOpened = -1;
        }

    }

    abstract static public class FilterBar {
        public boolean crawlCheck = false;
        public boolean kitchenCheck = false;
        public boolean familyCheck = false;
        public boolean dogCheck = false;
        public boolean outdoorCheck = false;
        private TextView crawlable;
        private TextView kitchen;
        private TextView family;
        private TextView dog;
        private TextView outdoor;
        public Spinner milesAway;
        TextView milesAwayWords;
        Context context;
        public double dist = 0;
        Boolean isMap = false;

        FilterBar(View v, Boolean isMap) {
            context = v.getContext();
            this.isMap = isMap;
            milesAway = v.findViewById(R.id.spinner_location);
            milesAwayWords = v.findViewById(R.id.textView_filterbar_location);
            refreshLocation();
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
                    R.array.milesAway, R.layout.spinner_item_milesaway);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            milesAway.setAdapter(adapter);
            milesAway.setSelection(2);
            milesAway.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    switch (i) {
                        case 0:
                            dist = 10.0;
                            MainActivity.distanceFilter = 10;
                            break;
                        case 1:
                            dist = 25.0;
                            MainActivity.distanceFilter = 25;
                            break;
                        case 2:
                            dist = 50.0;
                            MainActivity.distanceFilter = 50;
                            break;
                        case 3:
                            dist = 100.0;
                            MainActivity.distanceFilter = 100;
                            break;
                        case 4:
                            dist = 200.0;
                            MainActivity.distanceFilter = 200;
                            break;
                        case 5:
                            dist = -1.0;
                            MainActivity.distanceFilter = -1;
                            break;
                        default:
                            dist = -1.0;
                            break;
                    }
                    doSomething(view);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
            crawlable = v.findViewById(R.id.textView_viewBrewcrawlable);
            crawlable.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    crawlCheck = !crawlCheck;
                    doSomething(view);
                    updateSelectedImages();
                }
            });
            kitchen = v.findViewById(R.id.textView_inhousekitchen);
            kitchen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    kitchenCheck = !kitchenCheck;
                    doSomething(view);
                    updateSelectedImages();
                }
            });
            family = v.findViewById(R.id.textView_familyfriendly);
            family.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    familyCheck = !familyCheck;
                    doSomething(view);
                    updateSelectedImages();
                }
            });
            dog = v.findViewById(R.id.textView_dogfriendly);
            dog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dogCheck = !dogCheck;
                    doSomething(view);
                    updateSelectedImages();
                }
            });
            outdoor = v.findViewById(R.id.textView_outdoorseating);
            outdoor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    outdoorCheck = !outdoorCheck;
                    doSomething(view);
                    updateSelectedImages();
                }
            });
            updateSelectedImages();

        }

        public void refreshLocation() {
            if (isMap)
                milesAwayWords.setText(String.format("%s, %s", MainActivity.map_currentLocationZipcode, MainActivity.map_currentState));
            else
                milesAwayWords.setText(String.format("%s, %s", MainActivity.currentLocationZipcode, MainActivity.currentState));
        }

        abstract void doSomething(View view);

        private void updateSelectedImages() {
            int selectedCol = context.getResources().getColor(android.R.color.white);
            int unselectedCol = context.getResources().getColor(R.color.buttonText);
            Drawable selected = ResourcesCompat.getDrawable(context.getResources(), R.drawable.rounded_button_selected, null);
            Drawable deselected = ResourcesCompat.getDrawable(context.getResources(), R.drawable.rounded_button_deselected, null);
            if (dogCheck) {
                ((CardView) dog.getParent()).setCardBackgroundColor(context.getResources().getColor(R.color.iconblue));
                //dog.setBackground(selected);
                dog.setTextColor(selectedCol);
            } else {
                ((CardView) dog.getParent()).setCardBackgroundColor(context.getResources().getColor(R.color.lightgrey));
                //dog.setBackground(deselected);
                dog.setTextColor(unselectedCol);
            }
            if (outdoorCheck) {
                ((CardView) outdoor.getParent()).setCardBackgroundColor(context.getResources().getColor(R.color.iconblue));
                //outdoor.setBackground(selected);
                outdoor.setTextColor(selectedCol);
            } else {
                ((CardView) outdoor.getParent()).setCardBackgroundColor(context.getResources().getColor(R.color.lightgrey));
                //outdoor.setBackground(deselected);
                outdoor.setTextColor(unselectedCol);
            }
            if (familyCheck) {
                ((CardView) family.getParent()).setCardBackgroundColor(context.getResources().getColor(R.color.iconblue));
                //family.setBackground(selected);
                family.setTextColor(selectedCol);
            } else {
                ((CardView) family.getParent()).setCardBackgroundColor(context.getResources().getColor(R.color.lightgrey));
                //family.setBackground(deselected);
                family.setTextColor(unselectedCol);
            }
            if (kitchenCheck) {
                ((CardView) kitchen.getParent()).setCardBackgroundColor(context.getResources().getColor(R.color.iconblue));
                //kitchen.setBackground(selected);
                kitchen.setTextColor(selectedCol);
            } else {
                ((CardView) kitchen.getParent()).setCardBackgroundColor(context.getResources().getColor(R.color.lightgrey));
                //kitchen.setBackground(deselected);
                kitchen.setTextColor(unselectedCol);
            }
            if (crawlCheck) {
                ((CardView) crawlable.getParent()).setCardBackgroundColor(context.getResources().getColor(R.color.iconblue));
                //crawlable.setBackground(selected);
                crawlable.setTextColor(selectedCol);
            } else {
                ((CardView) crawlable.getParent()).setCardBackgroundColor(context.getResources().getColor(R.color.lightgrey));
                //crawlable.setBackground(deselected);
                crawlable.setTextColor(unselectedCol);
            }
        }
    }

    private void sortPins() {
        if(clusterManager == null) return;
        clusterManager.clearItems();
        if (!filterBar.crawlCheck && !filterBar.kitchenCheck && !filterBar.dogCheck && !filterBar.familyCheck && !filterBar.outdoorCheck) {
            sortedPins = new ArrayList<>();
            for (filteredPin pin :
                    pins) {
                if (MainActivity.distance(MainActivity.map_startingLat, pin.dir.lat, MainActivity.map_startingLon, pin.dir.lon, 0.0, 0.0) < filterBar.dist || filterBar.dist < 0)
                    sortedPins.add(pin);
            }
        } else {
            sortedPins = new ArrayList<>();
            for (filteredPin pin :
                    pins) {
                boolean add = false;
                boolean dontAdd = false;
                if (MainActivity.distance(MainActivity.map_startingLat, pin.dir.lat, MainActivity.map_startingLon, pin.dir.lon, 0.0, 0.0) > filterBar.dist && filterBar.dist > 0)
                    dontAdd = true;
                if (filterBar.crawlCheck) {
                    if (pin.dir.crawlable) add = true;
                    else dontAdd = true;
                }
                if (filterBar.kitchenCheck) {
                    if (pin.dir.kitched) add = true;
                    else dontAdd = true;
                }
                if (filterBar.familyCheck) {
                    if (pin.dir.family) add = true;
                    else dontAdd = true;
                }
                if (filterBar.dogCheck) {
                    if (pin.dir.dog) add = true;
                    else dontAdd = true;
                }
                if (filterBar.outdoorCheck) {
                    if (pin.dir.outdoorSeating) add = true;
                    else dontAdd = true;
                }
                if (add && !dontAdd) sortedPins.add(pin);
            }
        }
        clusterManager.addItems(sortedPins);
        clusterManager.cluster();
    }

    CustomClusterRenderer renderer;
    float mapzoomlevel = 0;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            googleMap.setMyLocationEnabled(true);
    }
    public void checkAndEnableLocation(){
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            ActivityCompat.requestPermissions(requireActivity(), perms, FINE_AND_COARSE_REQUEST);
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        } else {
            googleMap.setMyLocationEnabled(true);
        }
    }
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        this.googleMap = googleMap;
        checkAndEnableLocation();
        //LatLng first = new LatLng(MainActivity.startingLat, MainActivity.startingLon);
        LatLng last = new LatLng(MainActivity.map_startingLat, MainActivity.map_startingLon);
        MarkerManager markerManager = new MarkerManager(googleMap);
        clusterManager = new ClusterManager<filteredPin>(context, googleMap, markerManager);
        MarkerManager.Collection regularMarkers = markerManager.newCollection("normal");
        googleMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                if(googleMap.getCameraPosition().zoom < mapzoomlevel){
                    closeInfoPopup();
                }
                mapzoomlevel = googleMap.getCameraPosition().zoom;
                if(googleMap.getCameraPosition().zoom <= 10) shouldCluster = true;
                else shouldCluster = false;
            }
        });

        googleMap.setOnCameraIdleListener(clusterManager);
        googleMap.setOnMarkerClickListener(clusterManager);
        renderer = new CustomClusterRenderer(context, googleMap, clusterManager);
        clusterManager.setRenderer(renderer);
        renderer.setMinClusterSize(5);
        //https://medium.com/@tonyshkurenko/work-with-clustermanager-bdf3d70fb0fd
        clusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<filteredPin>() {
            @Override
            public boolean onClusterItemClick(filteredPin thispin) {
                    Marker mark = renderer.getMarker(thispin);
                    populateInfoPopup(mark, thispin.getTag());
                    return true;
            }
        });
        createPins(googleMap);
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(last)
                .build();
        googleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public boolean onMyLocationButtonClick() {
                clearPreviousSearch(false);
                return false;
            }
        });
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(baseZoomLevel), 1000, null);
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void clearPreviousSearch(boolean moveMap) {
        MainActivity.resetMapLocation();
        filterBar.refreshLocation();
        populateDataAndSortFrom(originalDirs);
/*        adapter = new MapListViewAdapter(originalDirs, listener);
        RV.setAdapter(adapter);*/
        adapter.updateFilter(filterBar);
        sortPins();
        searchLoc.setQuery("", false);
        if(moveMap){
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(MainActivity.map_startingLat, MainActivity.map_startingLon), baseZoomLevel));
        }
    }

    private void createPins(GoogleMap googleMap) {
        pins.clear();
        for(int i = 0; i < originalDirs.size(); i++) {
            double x = originalDirs.get(i).lat;
            double y = originalDirs.get(i).lon;
            String title = originalDirs.get(i).Brewery;
            LatLng thisLoc = new LatLng(x, y);
            if(originalDirs.get(i).bronze){
                createAndAddPin(googleMap, title, thisLoc, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE), i, true);
                continue;
             }
            if(originalDirs.get(i).silver){
                createAndAddPin(googleMap, title, thisLoc, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN), i, true);
                continue;
            }
            if(originalDirs.get(i).gold){
                //makeGoldImageMarker(googleMap, title, thisLoc, i);
                makeGoldStarPin(googleMap, title, thisLoc, i);
                continue;
            }
            createAndAddPin(googleMap, title, thisLoc, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), i, true);
        }
    }
    private void createAndAddStartPin(GoogleMap googleMap, String title, LatLng thisLoc, BitmapDescriptor bd, int index, float zIndex) {
        MarkerOptions thisOpt = new MarkerOptions()
                .position(thisLoc)
                .title(title)
                .icon(bd)
                .zIndex(zIndex);
        Marker newMarker = googleMap.addMarker(thisOpt);
        newMarker.setDraggable(true);
        newMarker.setTag(index);
        //pins.add(getFilteredPin(null, thisOpt, picLoaded, newMarker));
    }
    private void createAndAddPin(GoogleMap googleMap, String title, LatLng thisLoc, BitmapDescriptor bd, int index, boolean picLoaded, float zIndex) {
        MarkerOptions thisOpt = new MarkerOptions()
                .position(thisLoc)
                .title(title)
                .icon(bd)
                .zIndex(zIndex);
        //Marker newMarker = googleMap.addMarker(thisOpt);
        //clusterManager.addItem(getFilteredPin(originalDirs.get(index), thisOpt, picLoaded, newMarker));
        //newMarker.setTag(index);
        filteredPin newPin = getFilteredPin(originalDirs.get(index), thisOpt, picLoaded/*, newMarker*/).setTag(index);
        pins.add(newPin);
        clusterManager.addItem(newPin);
    }
    private void createAndAddPin(GoogleMap googleMap, String title, LatLng thisLoc, BitmapDescriptor bd, int index, boolean picLoaded) {
        createAndAddPin( googleMap,  title,  thisLoc,  bd, index, picLoaded, 0.0f);
    }
    private void makeGoldStarPin(GoogleMap googleMap, String title, LatLng thisLoc, int i) {
        Bitmap bmp = makeStarPin();
        createAndAddPin(googleMap, title, thisLoc, BitmapDescriptorFactory.fromBitmap(bmp), i, true, 2.0f);
    }
    private void makeGoldImageMarker(GoogleMap googleMap, String title, LatLng thisLoc, int i) {
        if(originalDirs.get(i).getThisBitmap() != null) {
            Bitmap bmp = makeGoldPin(originalDirs.get(i).getThisBitmap());
            createAndAddPin(googleMap, title, thisLoc, BitmapDescriptorFactory.fromBitmap(bmp), i, true);
        } else {
            createAndAddPin(googleMap, title, thisLoc, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW), i, false);
        }
    }

    @NotNull
    private Bitmap makeStarPin() {
        Drawable drawable = AppCompatResources.getDrawable(context, R.drawable.pin_starred_brewery);
        Bitmap bitmap;

        bitmap = Bitmap.createBitmap(130, 130, Bitmap.Config.ARGB_8888);

        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        //Bitmap bmp = Bitmap.createBitmap(bitmap.getIntrinsicWidth(), bitmap.getIntrinsicHeight(), conf);
        Canvas canvas1 = new Canvas(bitmap);
        drawable.setBounds(0,0, canvas1.getWidth(), canvas1.getHeight());
        drawable.draw(canvas1);

        return bitmap;
    }
    @NotNull
    private Bitmap makeSelectedStarPin() {
        Drawable drawable = AppCompatResources.getDrawable(context, R.drawable.pin_starred_brewery_selected);
        Bitmap bitmap;

        bitmap = Bitmap.createBitmap(130, 130, Bitmap.Config.ARGB_8888);

        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        //Bitmap bmp = Bitmap.createBitmap(bitmap.getIntrinsicWidth(), bitmap.getIntrinsicHeight(), conf);
        Canvas canvas1 = new Canvas(bitmap);
        drawable.setBounds(0,0, canvas1.getWidth(), canvas1.getHeight());
        drawable.draw(canvas1);

        return bitmap;
    }
    @NotNull
    private Bitmap makeSearchPin() {
        Drawable drawable = AppCompatResources.getDrawable(context, R.drawable.ic_you_searched_here_pin);
        Bitmap bitmap;

        bitmap = Bitmap.createBitmap(130, 130, Bitmap.Config.ARGB_8888);

        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        //Bitmap bmp = Bitmap.createBitmap(bitmap.getIntrinsicWidth(), bitmap.getIntrinsicHeight(), conf);
        Canvas canvas1 = new Canvas(bitmap);
        drawable.setBounds(0,0, canvas1.getWidth(), canvas1.getHeight());
        drawable.draw(canvas1);

        return bitmap;
    }
    @NotNull
    private Bitmap makeGoldPin(Bitmap bm) {
        Drawable drawable = AppCompatResources.getDrawable(context, R.drawable.ic_pin2);
        Bitmap bitmap;
        bm = resize(bm, 234, 200);
        float aspectRatio = bm.getWidth() /
                (float) bm.getHeight();
        Drawable drawablestar = AppCompatResources.getDrawable(context, R.drawable.ic_iconmonstr_star_3);

        bitmap = Bitmap.createBitmap(Math.min(bm.getWidth() + 16,250), Math.min(bm.getHeight()+50,250), Bitmap.Config.ARGB_8888);

        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        //Bitmap bmp = Bitmap.createBitmap(bitmap.getIntrinsicWidth(), bitmap.getIntrinsicHeight(), conf);
        Canvas canvas1 = new Canvas(bitmap);
        drawablestar.setBounds(canvas1.getWidth()-45, 10, canvas1.getWidth()-15,40);
        drawable.setBounds(0,0, canvas1.getWidth(), canvas1.getHeight());
        drawable.draw(canvas1);


        int marginleft = (canvas1.getWidth() - bm.getWidth())/2;
        int margintop = 10;
        canvas1.drawBitmap(bm, marginleft, margintop, null);
        drawablestar.draw(canvas1);
        //canvas1.drawText("User Name!", 0, 0, color);
        return bitmap;
    }
    private static Bitmap resize(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > ratioBitmap) {
                finalWidth = (int) ((float)maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float)maxWidth / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            return image;
        } else {
            return image;
        }
    }
    private void updateImagePins(){
        for(filteredPin pin: sortedPins){
            if(!pin.isImageLoaded){
                if(pin.dir.getThisBitmap() != null) {
                    //pin.marker.setIcon(BitmapDescriptorFactory.fromBitmap(makeGoldPin(pin.dir.getThisBitmap())));
                    pin.isImageLoaded = true;
                }
            }
        }
    }

    @NotNull
    private filteredPin getFilteredPin(MainActivity.filteredDir dir, MarkerOptions thisOpt, boolean isImageLoaded/*, Marker marker*/) {
        return new filteredPin(thisOpt, isImageLoaded, /*marker,*/ dir);
    }
    interface mapListener{
        void loadNewLocation(String state, String area, double lat, double lon, boolean updateUserDefault);
    }
}