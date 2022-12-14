package com.im.quenched;

import android.content.Context;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

public class AdsManager
{
    private static InterstitialAd interstitialAd;
    private Context ctx;
    public AdsManager(Context ctx)
    {
        this.ctx = ctx;
        MobileAds.initialize(ctx, new OnInitializationCompleteListener()
        {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus)
            {

            }
        });
    }
    public InterstitialAd createInterstitialAds()
    {
        AdRequest adRequest = new AdRequest.Builder().build();
        interstitialAd =new InterstitialAd(ctx);
        interstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");
        interstitialAd.loadAd(adRequest);
        return  interstitialAd;
    }
    public InterstitialAd getInterstitialAd()
    {
        return  interstitialAd;
    }

    public void createAds(AdView adview)
    {
        AdRequest adRequest = new AdRequest.Builder().build();
        adview.setAdListener(new AdListener()
        {
            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError)
            {
                super.onAdFailedToLoad(loadAdError);
                Toast.makeText(ctx, ""+loadAdError.getCode(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdLoaded()
            {
                super.onAdLoaded();
                Toast.makeText(ctx, "ads is loaded", Toast.LENGTH_SHORT).show();
            }
        });
        adview.loadAd(adRequest);
    }
    public void createUnifiedAds(int unitid, AdUnifiedListening listening)
    {
        AdLoader.Builder builder = new AdLoader.Builder(ctx,ctx.getString(unitid));
        builder.forUnifiedNativeAd(listening);
        builder.withAdListener(listening);
        AdLoader adload= builder.build();
        adload.loadAd(new AdRequest.Builder().build());
    }

    public void createUnifiedAds(int numads, int unitid, AdUnifiedListening listening)
    {
        AdLoader.Builder builder = new AdLoader.Builder(ctx,ctx.getString(unitid));
        builder.forUnifiedNativeAd(listening);
        builder.withAdListener(listening);
        AdLoader adload= builder.build();
        adload.loadAds(new AdRequest.Builder().build(),numads);
        listening.setAdLoader(adload);
    }
}

