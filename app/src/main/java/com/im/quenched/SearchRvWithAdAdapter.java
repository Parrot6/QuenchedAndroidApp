package com.im.quenched;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.formats.MediaView;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;

import java.util.ArrayList;
import java.util.List;

public class SearchRvWithAdAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int AD_TYPE = 2;
    private static final int CONTENT_TYPE = 1;
    Context context;
    private ArrayList<Object> data = new ArrayList<>();
    searchRvWithAdAdapterCallback myListener;

    public SearchRvWithAdAdapter(Context context, List<Object> dirs, searchRvWithAdAdapterCallback listener){
        data.addAll(dirs);
        myListener = listener;
        this.context = context;
    }
    public void setData(List<Object> dirs){
        data.clear();
        data.addAll(dirs);
        Log.e("ListUPDATED", String.valueOf(data.size()));
        notifyDataSetChanged();
    }
    public void insertLoadedAd(Object o, int position){
        data.add(position, o);
        //notifyItemInserted(position);
        notifyDataSetChanged();
    }
    public void setAds(List<UnifiedNativeAd> ads){
        data.addAll(ads);
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == AD_TYPE){
            //Log.e("createAdView", "here");
            View view = LayoutInflater.from(this.context).inflate(R.layout.ad_unified, parent, false);
            return new UnifiedNativeAdViewHolder(view);
        }
        View view = LayoutInflater.from(this.context).inflate(R.layout.directory_item_divided, parent, false);

        return new DirectoryViewHolder(view);
    }

    @Override
    public int getItemViewType(int position)
    {
        return data.get(position) instanceof UnifiedNativeAd? AD_TYPE: CONTENT_TYPE;
    }

    public static int getScreenWidth(Context context) {
        WindowManager wm= (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewtype = getItemViewType(position);
        if(viewtype == AD_TYPE){
            /*
                AdViewHolder vh = (AdViewHolder) holder;
                vh.setUnifiedNativeAd((UnifiedNativeAd) data.get(position));
                vh.ll.getLayoutParams().width = getScreenWidth(vh.ll.getContext());
           */
         populateNativeAdView((UnifiedNativeAd) data.get(position), (UnifiedNativeAdView) holder.itemView);
            //GridLayoutManager glm = (GridLayoutManager) holder.itemView.
            //UnifiedNativeAd nativeAd = (UnifiedNativeAd) data.get(position);
            //populateNativeAdView(nativeAd, ((UnifiedNativeAdViewHolder) holder).getAdView());
            return;
        }
        MainActivity.filteredDir item = (MainActivity.filteredDir) data.get(position);
        DirectoryViewHolder vh = (DirectoryViewHolder) holder;
        vh.loading.setVisibility(View.GONE);
        vh.breweryPic.setImageBitmap(item.getThisBitmap());
        vh.breweryName.setText(item.Brewery);
    }

    private void populateNativeAdView(UnifiedNativeAd nativeAd,
                                      UnifiedNativeAdView adView) {
        // Some assets are guaranteed to be in every UnifiedNativeAd.
        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        NativeAd.Image icon = nativeAd.getIcon();

        if (icon == null) {
            adView.getIconView().setVisibility(View.INVISIBLE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(icon.getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getPrice() == null) {
            adView.getPriceView().setVisibility(View.INVISIBLE);
        } else {
            adView.getPriceView().setVisibility(View.VISIBLE);
            ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
        }

        if (nativeAd.getStore() == null) {
            adView.getStoreView().setVisibility(View.INVISIBLE);
        } else {
            adView.getStoreView().setVisibility(View.VISIBLE);
            ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
        }

        if (nativeAd.getStarRating() == null) {
            adView.getStarRatingView().setVisibility(View.INVISIBLE);
        } else {
            ((RatingBar) adView.getStarRatingView())
                    .setRating(nativeAd.getStarRating().floatValue());
            adView.getStarRatingView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getAdvertiser() == null) {
            adView.getAdvertiserView().setVisibility(View.INVISIBLE);
        } else {
            ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
            adView.getAdvertiserView().setVisibility(View.VISIBLE);
        }

        // Assign native ad object to the native view.
        adView.setNativeAd(nativeAd);
    }
    public class DirectoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        ImageView breweryPic;
        TextView breweryName;
        ProgressBar loading;
        CardView container;
        DirectoryViewHolder(@NonNull View itemView) {
            super(itemView);
            loading = itemView.findViewById(R.id.progressBar);
            breweryName = itemView.findViewById(R.id.textview_breweryname);
            breweryPic = itemView.findViewById(R.id.imageView_breweryPic);
            container = itemView.findViewById(R.id.directory_item_container);
            container.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if(view.getId() == R.id.directory_item_container){
                myListener.openBrewery((MainActivity.filteredDir) data.get(getAdapterPosition()));
            }
        }
    }
    interface searchRvWithAdAdapterCallback{
        void openBrewery(MainActivity.filteredDir dir);
    }
    private void displayUnifiedNativeAd(ViewGroup parent, UnifiedNativeAd ad) {

        // Inflate a layout and add it to the parent ViewGroup.
        LayoutInflater inflater = (LayoutInflater) parent.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        UnifiedNativeAdView adView = (UnifiedNativeAdView) inflater
                .inflate(R.layout.ad_unified, parent);
        // Locate the view that will hold the headline, set its text, and call the
        // UnifiedNativeAdView's setHeadlineView method to register it.
        TextView headlineView = adView.findViewById(R.id.ad_headline);
        headlineView.setText(ad.getHeadline());
        adView.setHeadlineView(headlineView);

        // Repeat the above process for the other assets in the UnifiedNativeAd
        // using additional view objects (Buttons, ImageViews, etc).

        // If the app is using a MediaView, it should be
        // instantiated and passed to setMediaView. This view is a little different
        // in that the asset is populated automatically, so there's one less step.
        //MediaView mediaView = (MediaView) adView.findViewById(R.id.ad_media);
        //adView.setMediaView(mediaView);

        // Call the UnifiedNativeAdView's setNativeAd method to register the
        // NativeAdObject.
        adView.setNativeAd(ad);

        // Ensure that the parent view doesn't already contain an ad view.
        parent.removeAllViews();

        // Place the AdView into the parent.
        parent.addView(adView);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class UnifiedNativeAdViewHolder extends RecyclerView.ViewHolder {

        private UnifiedNativeAdView adView;

        public UnifiedNativeAdView getAdView() {
            return adView;
        }

        UnifiedNativeAdViewHolder(View view) {
            super(view);
            adView = (UnifiedNativeAdView) view.findViewById(R.id.ad_view);

            // The MediaView will display a video asset if one is present in the ad, and the
            // first image asset otherwise.
            adView.setMediaView((MediaView) adView.findViewById(R.id.ad_media));

            // Register the view used for each individual asset.
            adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
            adView.setBodyView(adView.findViewById(R.id.ad_body));
            adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
            adView.setIconView(adView.findViewById(R.id.ad_icon));
            adView.setPriceView(adView.findViewById(R.id.ad_price));
            adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
            adView.setStoreView(adView.findViewById(R.id.ad_store));
            adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));
        }
    }
}
