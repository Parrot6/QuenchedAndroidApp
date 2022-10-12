package com.im.quenched;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class DirectoryRvAdapter extends RecyclerView.Adapter<DirectoryRvAdapter.DirectoryViewHolder> {
    private ArrayList<MainActivity.filteredDir> data;
    public static directoryRvAdapterCallback myListener;

    public DirectoryRvAdapter(ArrayList<MainActivity.filteredDir> dirs, directoryRvAdapterCallback listener) {
       data = dirs;
       myListener = listener;
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
        if(data.size() == 0) {
            holder.loading.setVisibility(View.INVISIBLE);
            holder.breweryPic.setImageBitmap(MainActivity.defaultMissingImageBitmap);
            holder.breweryName.setText("No breweries to show");
        } else {
            if(data.get(position).gold) holder.goldStar.setVisibility(View.VISIBLE);
            else holder.goldStar.setVisibility(View.INVISIBLE);
            holder.loading.setVisibility(View.GONE);
            holder.breweryPic.setImageBitmap(data.get(position).getThisBitmap());
            holder.breweryName.setText(data.get(position).Brewery);
            holder.milesAway.setText(String.valueOf(MainActivity.distance(data.get(position).lat, MainActivity.startingLat, data.get(position).lon, MainActivity.startingLon, 0.0, 0.0) + " miles"));
        }
        holder.container.getLayoutParams().width = (int) (getScreenWidth(holder.container.getContext())/1.7);
    }

    public static int getScreenWidth(Context context) {
        WindowManager wm= (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    @Override
    public int getItemCount() {
        return Math.max(1, data.size());
    }

    public class DirectoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        ImageView breweryPic;
        TextView breweryName;
        ProgressBar loading;
        CardView container;
        ImageView goldStar;
        TextView milesAway;
        DirectoryViewHolder(@NonNull View itemView) {
            super(itemView);
            milesAway = itemView.findViewById(R.id.textView_directoryItem_milesAway);
            milesAway.setVisibility(View.VISIBLE);
            goldStar = itemView.findViewById(R.id.image_featuredStar);
            loading = itemView.findViewById(R.id.progressBar);
            breweryName = itemView.findViewById(R.id.textview_breweryname);
            breweryPic = itemView.findViewById(R.id.imageView_breweryPic);
            container = itemView.findViewById(R.id.directory_item_container);
            container.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if(view.getId() == R.id.directory_item_container){
                if(data.size() == 0 || getAdapterPosition() == -1) return;
                else myListener.openBrewery(data.get(getAdapterPosition()));
            }
        }
    }
    interface directoryRvAdapterCallback{
        void openBrewery(MainActivity.filteredDir position);
    }
}
