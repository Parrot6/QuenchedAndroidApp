package com.im.quenched;


import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ViewBrewerySliderAdapter extends RecyclerView.Adapter<ViewBrewerySliderAdapter.ImageSlider_ViewHolder> {

    // Context object
    Context context;

    // Array of images
    ArrayList<Bitmap> images;

    // Layout Inflater
    LayoutInflater mLayoutInflater;
    public static sliderPicAdapterCallback myListener;
    // Viewpager Constructor
    public ViewBrewerySliderAdapter(Context context, ArrayList<Bitmap> images, ViewBrewerySliderAdapter.sliderPicAdapterCallback listener) {
        this.context = context;
        this.images = images;
        myListener = listener;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @NonNull
    @Override
    public ImageSlider_ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        return new ImageSlider_ViewHolder(LayoutInflater.from(context).inflate(R.layout.brewery_pic_slider_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ImageSlider_ViewHolder holder, int position) {
        holder.imageView.setImageBitmap(images.get(position));

    }


    @Override
    public int getItemCount() {
        return images.size();
    }
    public class ImageSlider_ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        private ImageView imageView;

        ImageSlider_ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewMain);
            imageView.setOnClickListener(this);
        }
        @Override
        public void onClick(View view) {
            if(view.getId() == R.id.imageViewMain){
                if(getItemCount() > 0) {
                    myListener.openPic(this, getAdapterPosition());
                }
            }
        }
    }
    interface sliderPicAdapterCallback{
        void openPic(ImageSlider_ViewHolder holder, int position);
    }
}
