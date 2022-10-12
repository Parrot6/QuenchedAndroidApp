package com.im.quenched;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Home#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Home extends Fragment implements DirectoryRvAdapter.directoryRvAdapterCallback {

    private static final int REQUEST_CODE = 876;
    public static final double hiddenHomeRadius = 50.0;
    Context context;
    TextView resultsInfo;
    RecyclerView rv_inhousekitchen;
    RecyclerView rv_crawlable;
    RecyclerView rv_dogfriendly;
    RecyclerView rv_familyfriendly;
    RecyclerView rv_outdoorseating;
    ArrayList<MainActivity.filteredDir> crawlableFD = new ArrayList<>();
    ArrayList<MainActivity.filteredDir> inHouseKitchenFD = new ArrayList<>();
    ArrayList<MainActivity.filteredDir> familyFriendlyFD = new ArrayList<>();
    ArrayList<MainActivity.filteredDir> dogFriendlyFD = new ArrayList<>();
    ArrayList<MainActivity.filteredDir> outdoorSeatingFD = new ArrayList<>();
    DirectoryRvAdapter crawlableAdapter;
    DirectoryRvAdapter inHouseKitchenAdapter;
    DirectoryRvAdapter dogFriendlyAdapter;
    DirectoryRvAdapter familyFriendlyAdapter;
    DirectoryRvAdapter outdoorSeatingAdapter;
    DirectoryRvAdapter.directoryRvAdapterCallback listener;


    public Home() {
        // Required empty public constructor

    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Home.
     */
    // TODO: Rename and change types and number of parameters
    public static Home newInstance(String param1, String param2) {
        Home fragment = new Home();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
        listener = this;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getContext();
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        //Button getlocation = v.findViewById(R.id.button_home_getLocation);
        resultsInfo = v.findViewById(R.id.textView_homeResultsInfo);
        updateResultInfo();
        rv_inhousekitchen = v.findViewById(R.id.rv_home_inhousekitchen);
        rv_inhousekitchen.setHasFixedSize(true);
        rv_inhousekitchen.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        inHouseKitchenAdapter = new DirectoryRvAdapter(inHouseKitchenFD, listener);
        rv_inhousekitchen.setAdapter(inHouseKitchenAdapter);

        rv_crawlable = v.findViewById(R.id.rv_home_crawlable);
        rv_crawlable.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        crawlableAdapter = new DirectoryRvAdapter(crawlableFD, listener);
        rv_crawlable.setAdapter(crawlableAdapter);

        rv_outdoorseating = v.findViewById(R.id.rv_home_outdoorSeating);
        rv_outdoorseating.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        outdoorSeatingAdapter = new DirectoryRvAdapter(outdoorSeatingFD, listener);
        rv_outdoorseating.setAdapter(outdoorSeatingAdapter);

        rv_familyfriendly = v.findViewById(R.id.rv_home_familyFriendly);
        rv_familyfriendly.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        familyFriendlyAdapter = new DirectoryRvAdapter(familyFriendlyFD, listener);
        rv_familyfriendly.setAdapter(familyFriendlyAdapter);

        rv_dogfriendly = v.findViewById(R.id.rv_home_dogFriendly);
        rv_dogfriendly.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        dogFriendlyAdapter = new DirectoryRvAdapter(dogFriendlyFD, listener);
        rv_dogfriendly.setAdapter(dogFriendlyAdapter);
        //createAllDataSets();

        MainActivity.setFragmentRefreshListener(new MainActivity.NotifyImageUpdate() {
            @Override
            public void notifyUpdate() {
                if(getActivity() == null) return;
               // Toast.makeText(context, "updated", Toast.LENGTH_SHORT).show();
                refreshImages();
            }

            @Override
            public void notifyPrelimaryDataReady() {
                createAllDataSets();
            }
        });

/*        getlocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) getActivity()).getLocation();
            }
        });*/
        // Inflate the layout for this fragment
        return v;
    }

    private void updateResultInfo() {
        resultsInfo.setText("Based on breweries nearby " + MainActivity.currentLocationZipcode + ", " + MainActivity.currentState);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshImages();
        createAllDataSets();
    }

    public void refreshImages(){
        /*for (MainActivity.filteredDir entry :
                MainActivity.filterableBreweriesMap.values()) {
            entry.getThisBitmap();
        }
        inHouseKitchenAdapter.notifyDataSetChanged();
        crawlableAdapter.notifyDataSetChanged();
        outdoorSeatingAdapter.notifyDataSetChanged();
        familyFriendlyAdapter.notifyDataSetChanged();
        dogFriendlyAdapter.notifyDataSetChanged();*/
    }
    public void createAllDataSets(){
        crawlableFD.clear();
        outdoorSeatingFD.clear();
        inHouseKitchenFD.clear();
        familyFriendlyFD.clear();
        dogFriendlyFD.clear();
        for (MainActivity.filteredDir entry :
                MainActivity.filterableBreweriesMap.values()) {

            if(entry.getDistance(false) <= hiddenHomeRadius) {
                if (entry.crawlable) {
                    crawlableFD.add(entry);
                }
                if (entry.outdoorSeating) {
                    outdoorSeatingFD.add(entry);
                }
                if (entry.dog) {
                    dogFriendlyFD.add(entry);
                }
                if (entry.family) {
                    familyFriendlyFD.add(entry);
                }
                if (entry.kitched) {
                    inHouseKitchenFD.add(entry);
                }
            } else {

            }
        }
        Collections.shuffle(crawlableFD);
        Collections.shuffle(outdoorSeatingFD);
        Collections.shuffle(dogFriendlyFD);
        Collections.shuffle(familyFriendlyFD);
        Collections.shuffle(inHouseKitchenFD);
        crawlableFD = sortGoldFirst(crawlableFD);
        outdoorSeatingFD = sortGoldFirst(outdoorSeatingFD);
        dogFriendlyFD = sortGoldFirst(dogFriendlyFD);
        familyFriendlyFD = sortGoldFirst(familyFriendlyFD);
        inHouseKitchenFD = sortGoldFirst(inHouseKitchenFD);
        updateResultInfo();
        inHouseKitchenAdapter = new DirectoryRvAdapter(inHouseKitchenFD, listener);
        rv_inhousekitchen.setAdapter(inHouseKitchenAdapter);
        crawlableAdapter = new DirectoryRvAdapter(crawlableFD, listener);
        rv_crawlable.setAdapter(crawlableAdapter);
        outdoorSeatingAdapter = new DirectoryRvAdapter(outdoorSeatingFD, listener);
        rv_outdoorseating.setAdapter(outdoorSeatingAdapter);
        familyFriendlyAdapter = new DirectoryRvAdapter(familyFriendlyFD, listener);
        rv_familyfriendly.setAdapter(familyFriendlyAdapter);
        dogFriendlyAdapter = new DirectoryRvAdapter(dogFriendlyFD, listener);
        rv_dogfriendly.setAdapter(dogFriendlyAdapter);
    }
    public ArrayList<MainActivity.filteredDir> sortGoldFirst(ArrayList<MainActivity.filteredDir> dirs){
        ArrayList<MainActivity.filteredDir> newList = new ArrayList<>();
        ArrayList<MainActivity.filteredDir> newListNotGold = new ArrayList<>();
        for (MainActivity.filteredDir thisdir :
                dirs) {
            if(thisdir.gold) newList.add(thisdir);
            else newListNotGold.add(thisdir);
        }
        newList.addAll(newListNotGold);
        return newList;
    }


    @Override
    public void openBrewery(MainActivity.filteredDir dir) {
        Intent intent = new Intent(getActivity(), ViewBrewery.class);
        intent.putExtra("jso", dir.jso.toString());
        intent.putExtra("Index", dir.uniqueBreweryID);
        intent.putExtra("layoutPosition", -1);
        intent.setAction("Preview");
        startActivityForResult(intent, REQUEST_CODE);
    }
}