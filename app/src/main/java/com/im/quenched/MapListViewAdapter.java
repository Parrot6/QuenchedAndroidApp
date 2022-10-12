package com.im.quenched;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MapListViewAdapter extends RecyclerView.Adapter<MapListViewAdapter.DirectoryViewHolder> {
    private static ArrayList<MainActivity.filteredDir> filtered = new ArrayList<>();
    private ArrayList<MainActivity.filteredDir> original = new ArrayList<>();
    public static directoryRvAdapterCallback myListener;
    private boolean nothingInRange = false;
    Drawable defaultImage;

    public MapListViewAdapter(ArrayList<MainActivity.filteredDir> list, directoryRvAdapterCallback listener) {
        filtered.addAll(list);
        original = list;
        myListener = listener;
        // load image as Drawable
        defaultImage = MainActivity.defaultMissingImageDrawable;
    }

    @NonNull
    @Override
    public DirectoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.directory_item_divided, parent, false);
        DirectoryViewHolder holder = new DirectoryViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull DirectoryViewHolder holder, int position) {
        if(filtered.size() == 0) {
            if(nothingInRange){
                holder.loading.setVisibility(View.GONE);
                holder.breweryPic.setImageDrawable(defaultImage);
                holder.breweryName.setText("No Breweries In Range");
            } else {
                holder.loading.setVisibility(View.VISIBLE);
                holder.breweryPic.setImageDrawable(defaultImage);
                holder.breweryName.setText("Loading...");
            }
        } else {
            holder.milesaway.setText(String.valueOf(MainActivity.distance(filtered.get(position).lat, MainActivity.map_startingLat, filtered.get(position).lon, MainActivity.map_startingLon, 0.0, 0.0) + " miles"));
            holder.loading.setVisibility(View.GONE);
            Bitmap bmp = filtered.get(position).getThisBitmap();
            if(bmp != null) holder.breweryPic.setImageBitmap(bmp);
            else holder.breweryPic.setImageDrawable(defaultImage);
            holder.breweryName.setText(filtered.get(position).Brewery);
        }
    }

    public void updateFilter(MapFragment.FilterBar filterBar){
        if(!filterBar.crawlCheck&&!filterBar.kitchenCheck&&!filterBar.dogCheck&&!filterBar.familyCheck&&!filterBar.outdoorCheck){
            filtered.clear();
            for (MainActivity.filteredDir dir :
                    original) {
                if (MainActivity.distance(MainActivity.map_startingLat, dir.lat, MainActivity.map_startingLon, dir.lon, 0.0, 0.0) < filterBar.dist || filterBar.dist < 0)
                    filtered.add(dir);
            }
            //filtered.addAll(original);
        } else {
            filtered.clear();
            for (MainActivity.filteredDir dir :
                    original) {
                boolean add = false;
                boolean dontAdd = false;
                if(MainActivity.distance(MainActivity.map_startingLat, dir.lat, MainActivity.map_startingLon, dir.lon, 0.0, 0.0) > filterBar.dist && filterBar.dist > 0) dontAdd = true;
                if (filterBar.crawlCheck) {
                    if (dir.crawlable) add = true;
                    else dontAdd = true;
                }
                if (filterBar.kitchenCheck) {
                    if (dir.kitched) add = true;
                    else dontAdd = true;
                }
                if (filterBar.familyCheck) {
                    if (dir.family) add = true;
                    else dontAdd = true;
                }
                if (filterBar.dogCheck) {
                    if (dir.dog) add = true;
                    else dontAdd = true;
                }
                if (filterBar.outdoorCheck) {
                    if (dir.outdoorSeating) add = true;
                    else dontAdd = true;
                }
                if (add && !dontAdd) filtered.add(dir);
            }
        }
        nothingInRange = filtered.size() == 0;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return Math.max(1, filtered.size());
    }

    public static class DirectoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        ImageView breweryPic;
        TextView breweryName;
        ProgressBar loading;
        CardView container;
        TextView milesaway;
        DirectoryViewHolder(@NonNull View itemView) {
            super(itemView);
            loading = itemView.findViewById(R.id.progressBar);
            breweryName = itemView.findViewById(R.id.textview_breweryname);
            breweryPic = itemView.findViewById(R.id.imageView_breweryPic);
            container = itemView.findViewById(R.id.directory_item_container);
            milesaway = itemView.findViewById(R.id.textView_directoryItem_milesAway);
            milesaway.setVisibility(View.VISIBLE);
            container.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if(view.getId() == R.id.directory_item_container){
                if(filtered.size() == 0) return;
                myListener.openBrewery(filtered.get(getAdapterPosition()));
            }
        }
    }
    interface directoryRvAdapterCallback{
        void openBrewery(MainActivity.filteredDir dir);
    }
}
