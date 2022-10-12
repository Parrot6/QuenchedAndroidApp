package com.im.quenched;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ViewBreweryRvAdapter extends RecyclerView.Adapter<ViewBreweryRvAdapter.DirectoryViewHolder> {
    private ArrayList<Bitmap> data;
    public static directoryRvAdapterCallback myListener;
    Drawable defaultDraw;
    public ViewBreweryRvAdapter(ArrayList<Bitmap> pics, directoryRvAdapterCallback listener) {
        data = pics;
        myListener = listener;
        defaultDraw = MainActivity.defaultMissingImageDrawable;
    }

    @NonNull
    @Override
    public DirectoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewbrewery_rv_item, parent, false);
        DirectoryViewHolder holder = new DirectoryViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull DirectoryViewHolder holder, int position) {
        if(data.size() == 0) {
            holder.image.setVisibility(View.GONE);
            holder.itemView.findViewById(R.id.no_userpics_toload).setVisibility(View.VISIBLE);
            holder.topBox.getLayoutParams().width = getScreenSingleWidth(holder.topBox.getContext());
            holder.topBox.getLayoutParams().height = getScreenWidth(holder.topBox.getContext())/3;
        } else {
            holder.image.setVisibility(View.VISIBLE);
            holder.itemView.findViewById(R.id.no_userpics_toload).setVisibility(View.GONE);
            //else holder.goldStar.setVisibility(View.INVISIBLE);
            if(data.get(position) != null){
                holder.image.setImageBitmap(data.get(position));
                holder.itemView.setVisibility(View.VISIBLE);
            }
            else holder.itemView.setVisibility(View.GONE); //holder.image.setImageDrawable(defaultDraw);
            holder.topBox.getLayoutParams().width = getScreenWidth(holder.topBox.getContext())/3;
            holder.topBox.getLayoutParams().height = getScreenWidth(holder.topBox.getContext())/3;
        }

    }

    public static int getScreenWidth(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int pxWidth = displayMetrics.widthPixels;
        float dpWidth = pxWidth / displayMetrics.density;
        int pxHeight = displayMetrics.heightPixels;
        float dpHeight = pxHeight / displayMetrics.density;
        return (int) (pxWidth - (displayMetrics.density*28));
    }
    public static int getScreenSingleWidth(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int pxWidth = displayMetrics.widthPixels;
        float dpWidth = pxWidth / displayMetrics.density;
        int pxHeight = displayMetrics.heightPixels;
        float dpHeight = pxHeight / displayMetrics.density;
        return (int) (pxWidth - (displayMetrics.density*16));
    }
    @Override
    public int getItemCount() {
        return Math.max(1, data.size());
    }

    public class DirectoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        ImageView image;
        CardView topBox;
        DirectoryViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.viewBrewery_rv_pic);
            topBox = itemView.findViewById(R.id.CardView_viewbrewery_rv_item);
            topBox.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if(view.getId() == R.id.CardView_viewbrewery_rv_item){
                if(data.size() > 0) {
                    myListener.openPic(this, getAdapterPosition());
                }
            }
        }
    }
    interface directoryRvAdapterCallback{
        void openPic(DirectoryViewHolder holder, int position);
    }
}
