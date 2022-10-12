package com.im.quenched;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.io.Serializable;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Search#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Search extends Fragment {


    private static final int REQUEST_CODE_NAME = 222;
    private static final int REQUEST_CODE_FILTER = 333;
    AutoCompleteTextView searchField;
    Button searchName;
    Button searchFilter;
    MapFragment.FilterBar filterBar;
    Context context;
    RecyclerView recyclerView;
    FavoritesRvAdapter adapter;
    boolean itemSelected = false;
    public enum SearchType{
        FilterBar, SearchByName
    }
    public Search() {
        // Required empty public constructor
    }
    public static class FilterBarSelections implements Serializable {
        public boolean crawlCheck = false;
        public boolean kitchenCheck = false;
        public boolean familyCheck = false;
        public boolean dogCheck = false;
        public boolean outdoorCheck = false;
        public double distance = 0;
        public FilterBarSelections(MapFragment.FilterBar fb){
            crawlCheck = fb.crawlCheck;
            kitchenCheck = fb.kitchenCheck;
            familyCheck = fb.familyCheck;
            dogCheck = fb.dogCheck;
            outdoorCheck = fb.outdoorCheck;
            distance = fb.dist;
        }
    }
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Search.
     */
    // TODO: Rename and change types and number of parameters
    public static Search newInstance(String param1, String param2) {
        Search fragment = new Search();
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
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View v = inflater.inflate(R.layout.fragment_search, container, false);
        context = v.getContext();
        filterBar = new MapFragment.FilterBar(v, false) {
            @Override
            void doSomething(View view) {
                //no need
            }
        };

        MainActivity.setFragmentRefreshListener(new MainActivity.NotifyImageUpdate() {
            @Override
            public void notifyUpdate() {
                adapter.notifyDataSetChanged();
            }

            @Override
            public void notifyPrelimaryDataReady() {
                if(getActivity() == null) return;
                adapter.notifyDataSetChanged();
            }
        });
        searchField = v.findViewById(R.id.editTextBreweryName);
        String[] suggs = MainActivity.brewNames.keySet().toArray(new String[0]);
        ArrayAdapter<String> names = new ArrayAdapter<String>(context, android.R.layout.select_dialog_singlechoice, suggs);
        searchField.setAdapter(names);
        searchField.setThreshold(1);
        searchField.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchField.getRootView().getWindowToken(), 0);
                itemSelected = true;
            }
        });
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                itemSelected = false;
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        searchName = v.findViewById(R.id.button_searchByName);
        searchName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(itemSelected){
                    int uniqu = MainActivity.brewNames.get(searchField.getText().toString());
                    MainActivity.filteredDir newdir = MainActivity.findOrLoadBrewery(uniqu);
                    if(newdir == null){
                        Toast.makeText(context, "Failed to fetch brewery data, please check your connection", Toast.LENGTH_LONG).show();
                        return;
                    }
                    Intent intent = new Intent(getActivity(), ViewBrewery.class);
                    intent.putExtra("jso", newdir.jso.toString());
                    intent.putExtra("Index", newdir.uniqueBreweryID);
                    intent.setAction("Preview");
                    startActivityForResult(intent, 1);
                    searchField.setText("");
                } else {
                    String searchTerm = searchField.getText().toString().toLowerCase();
                    if (searchTerm.length() == 0) return;
                    //ArrayList<MainActivity.filteredDir> toShow = MainActivity.filteredResultsByName(searchTerm);
                    Intent newIntent = new Intent(context, SearchResults.class);
                    newIntent.setAction(String.valueOf(SearchType.SearchByName));
                    newIntent.putExtra("Name", searchField.getText().toString());
                    startActivityForResult(newIntent, REQUEST_CODE_NAME);
                }
            }
        });
        searchFilter = v.findViewById(R.id.button_searchCatagories);
        searchFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent newIntent = new Intent(context, SearchResults.class);
                newIntent.setAction(String.valueOf(SearchType.FilterBar));
                newIntent.putExtra("FilterBarSelections", new FilterBarSelections(filterBar));
                startActivityForResult(newIntent, REQUEST_CODE_FILTER);
            }
        });
        recyclerView = v.findViewById(R.id.rv_favoriteBreweries);
        recyclerView.setLayoutManager(new GridLayoutManager(v.getContext(), 2));
        adapter = new FavoritesRvAdapter(MainActivity.favoritedBreweries, new FavoritesRvAdapter.directoryRvAdapterCallback() {
            @Override
            public void openBrewery(MainActivity.filteredDir dir) {
                Intent intent = new Intent(getActivity(), ViewBrewery.class);
                intent.putExtra("jso", dir.jso.toString());
                intent.putExtra("Index", dir.uniqueBreweryID);
                intent.setAction("Preview");
                startActivityForResult(intent, 1);
            }
        });
        recyclerView.setAdapter(adapter);
        searchFilter.setVisibility(View.VISIBLE); //start with search with filters vi
        v.findViewById(R.id.included_filterbar_search).setVisibility(View.VISIBLE);//start with search with filters vis
        searchField.setVisibility(View.GONE);//start with search with filters vis
        searchName.setVisibility(View.GONE);//start with search with filters vis
        TabLayout tabs = v.findViewById(R.id.tabLayout_search);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        searchFilter.setVisibility(View.VISIBLE);
                        v.findViewById(R.id.included_filterbar_search).setVisibility(View.VISIBLE);
                        searchField.setVisibility(View.GONE);
                        searchName.setVisibility(View.GONE);
                        break;
                    case 1:
                        searchFilter.setVisibility(View.GONE);
                        v.findViewById(R.id.included_filterbar_search).setVisibility(View.GONE);
                        searchField.setVisibility(View.VISIBLE);
                        searchName.setVisibility(View.VISIBLE);
                        break;
                    default:
                        break;
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        return v;
    }


}