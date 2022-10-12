package com.im.quenched;

import android.content.Context;
import android.graphics.drawable.Drawable;
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

public class FavoritesRvAdapter extends RecyclerView.Adapter<FavoritesRvAdapter.DirectoryViewHolder> {
    private ArrayList<MainActivity.filteredDir> data;
    public static directoryRvAdapterCallback myListener;
    Drawable defaultDraw;
    public FavoritesRvAdapter(ArrayList<MainActivity.filteredDir> dirs, directoryRvAdapterCallback listener) {
       data = dirs;
       myListener = listener;
       defaultDraw = MainActivity.defaultMissingImageDrawable;
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
            holder.fav.setVisibility(View.VISIBLE);
            holder.loading.setVisibility(View.INVISIBLE);
            holder.breweryPic.setImageDrawable(defaultDraw);
            holder.breweryName.setText("Save Breweries for Later!");
        } else {
            holder.fav.setVisibility(View.VISIBLE);
            //else holder.goldStar.setVisibility(View.INVISIBLE);
            holder.loading.setVisibility(View.GONE);
            if(data.get(position).getThisBitmap() != null) holder.breweryPic.setImageBitmap(data.get(position).getThisBitmap());
            else holder.breweryPic.setImageDrawable(defaultDraw);
            holder.breweryName.setText(data.get(position).Brewery);
        }
        holder.container.getLayoutParams().width = getScreenWidth(holder.container.getContext())/2;
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
        ImageView fav;
        DirectoryViewHolder(@NonNull View itemView) {
            super(itemView);
            fav = itemView.findViewById(R.id.image_favorited);
            loading = itemView.findViewById(R.id.progressBar);
            breweryName = itemView.findViewById(R.id.textview_breweryname);
            breweryPic = itemView.findViewById(R.id.imageView_breweryPic);
            container = itemView.findViewById(R.id.directory_item_container);
            container.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if(view.getId() == R.id.directory_item_container){
                if(data.size() > 0) {
                    myListener.openBrewery(data.get(getAdapterPosition()));
                }
            }
        }
    }
    interface directoryRvAdapterCallback{
        void openBrewery(MainActivity.filteredDir position);
    }
}
