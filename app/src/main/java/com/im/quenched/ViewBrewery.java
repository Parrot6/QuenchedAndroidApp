package com.im.quenched;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.text.util.Linkify;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.formats.MediaView;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ViewBrewery extends AppCompatActivity {
    ViewPager2 pics;
    TextView breweryName;
    ImageView favoriteBrewery;
    ImageView report;
    TextView breweryType;
    TextView phone;
    TextView address;
    TextView facebook;
    TextView insta;
    TextView brewerySummary;
    TextView breweryWebsite;
    ImageButton hours;
    Button makePost;
    Button openUber;
    Button openLyft;
    ArrayList<Bitmap> images = new ArrayList<>();
    JSONObject jso;
    Context context;
    boolean favorited = false;
    private int PICK_IMAGE = 777;
    private static final int CAMERA_REQUEST = 888;
    private static final int MY_CAMERA_PERMISSION_CODE = 123;
    ImageView photoToUpload = null;
    Bitmap toupload;
    private Uri photoURI;
    ConstraintLayout noBrewCatagories;
    RecyclerView RVuserPics;
    ViewBreweryRvAdapter adapter;
    ArrayList<Bitmap> userPostPhotos = new ArrayList<>();
    // Hold a reference to the current animator,
    // so that it can be canceled mid-way.
    private Animator currentAnimator;
    ImageView viewpagerleft;
    ImageView viewpagerright;
    // The system "short" animation time duration, in milliseconds. This
    // duration is ideal for subtle animations or animations that occur
    // very frequently.
    private int shortAnimationDuration;
    AdLoader adLoader;
    private UnifiedNativeAd nativeAd;
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_brewery_v2);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.drawable.quenched_logo4);
        context = this;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        pics = findViewById(R.id.viewPager_breweryPicSlider);
        pics.setAdapter(new ViewBrewerySliderAdapter(ViewBrewery.this, images, new ViewBrewerySliderAdapter.sliderPicAdapterCallback() {
            @Override
            public void openPic(ViewBrewerySliderAdapter.ImageSlider_ViewHolder holder, int position) {
                zoomImageFromThumb(holder.itemView, images.get(position));
            }
        }));
        breweryName = findViewById(R.id.textview_breweryname);
        breweryType = findViewById(R.id.textView_breweryType);
        phone = findViewById(R.id.textView_brewery_phone);
        address = findViewById(R.id.textView_brewery_address);
        facebook = findViewById(R.id.textView_brewery_socialFacebook);
        insta = findViewById(R.id.textView_brewery_socialInstagram);
        brewerySummary = findViewById(R.id.textView_brewerySummary);
        breweryWebsite = findViewById(R.id.textView_brewery_website);
        favoriteBrewery = findViewById(R.id.imageView_favorite);
        report = findViewById(R.id.imageView_reportBrewery);
        report.setOnClickListener(showReportDialog());
        hours = findViewById(R.id.imageButton_viewBrew_hours);
        hours.setOnClickListener(showHoursDialog());
        final String formattedAddress;
        final Intent intent = getIntent();
        try {
            jso = new JSONObject(Objects.requireNonNull(intent.getStringExtra("jso")));
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        final MainActivity.filteredDir thisBrewery = MainActivity.findOrLoadBrewery(jso.optInt("UniqueID", -1));
            SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(notEmptyOptString(jso, "Brewery", "BreweryName"));

            breweryName.setText(builder);
            breweryType.setText(notEmptyOptString(jso, "BreweryType", "Brewery")); //hide if none event
            phone.setText(notEmptyOptString(jso, "phone_Number", "No Number Found"));
            phone.setAutoLinkMask(Linkify.PHONE_NUMBERS);
            facebook.setText(notEmptyOptString(jso, "facebook", "No Facebook Link"));
            facebook.setMovementMethod(LinkMovementMethod.getInstance());
            insta.setText(notEmptyOptString(jso, "instagram", "No Instagram Link"));
            address.setText(String.format("%s, %s, %s %s", notEmptyOptString(jso, "address", "Street Address Unknown"), notEmptyOptString(jso, "City", "City Unknown"), notEmptyOptString(jso, "StateProvince", "State Unknown"), notEmptyOptString(jso, "Zip", "Zip Unknown")));
            formattedAddress = URLEncoder.encode(address.getText().toString());
            address.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                            Uri.parse("google.navigation:q="+formattedAddress));
                    startActivity(intent);
                }
            });
            address.setPaintFlags(address.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            /*insta.setLinksClickable(false);
            insta.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //String s = notEmptyOptString(jso, "instagram", "No Instagram Link");
                    //String[] ss = s.split("/", 5);
                    //getApplicationContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("instagram://user?username="+ss[2].toString())));
                    Uri uri = Uri.parse(notEmptyOptString(jso, "instagram", "No Instagram Link"));
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setPackage("com.instagram.android");
                    startActivity(intent);

                }
            });*/
            int count = 0;
            final ConstraintLayout brewTypes = findViewById(R.id.constraintLayout_types);
            View crawlable = brewTypes.findViewById(R.id.textView_viewBrewcrawlable);
            View inhousekitchen = brewTypes.findViewById(R.id.textView_viewBrewkitchen);
            View familyFriendly = brewTypes.findViewById(R.id.textView_viewBrewfamily);
            View dogFriendly = brewTypes.findViewById(R.id.textView_viewBrewdog);
            View outdoorSeating = brewTypes.findViewById(R.id.textView_viewBrewoutdoor);
            noBrewCatagories = findViewById(R.id.ConstraimtLayout_noBreweryTypes);
            if(jso.optBoolean("Crawlable", false)){
                crawlable.setVisibility(View.VISIBLE);
                count++;
            } else {
                crawlable.setVisibility(View.GONE);
            }
            if(jso.optBoolean("In House Kitchen", false)){
                inhousekitchen.setVisibility(View.VISIBLE);
                count++;
            } else {
                inhousekitchen.setVisibility(View.GONE);
            }
            if(jso.optBoolean("Family Friendly", false)){
                familyFriendly.setVisibility(View.VISIBLE);
                count++;
            } else {
                familyFriendly.setVisibility(View.GONE);
            }
            if(jso.optBoolean("Dog Friendly", false)){
                if(count >= 3){
                    findViewById(R.id.textView_viewBrewdog2).setVisibility(View.VISIBLE);
                    dogFriendly.setVisibility(View.GONE);
                } else {
                    dogFriendly.setVisibility(View.VISIBLE);
                }
                count++;
            } else {
                dogFriendly.setVisibility(View.GONE);
            }
            if(jso.optBoolean("Outdoor Seating", false)){
                if(count >= 3){
                    findViewById(R.id.textView_viewBrewoutdoor2).setVisibility(View.VISIBLE);
                    outdoorSeating.setVisibility(View.GONE);
                } else {
                    outdoorSeating.setVisibility(View.VISIBLE);
                }
                count++;
            } else {
                outdoorSeating.setVisibility(View.GONE);
            }
            final View overflowTypes = findViewById(R.id.constraintlayout_overflow_types);
            if(count >= 4) {
                overflowTypes.setVisibility(View.VISIBLE);
                noBrewCatagories.setVisibility(View.GONE);
            }
            else if(count > 0) {
                noBrewCatagories.setVisibility(View.GONE);
                overflowTypes.setVisibility(View.GONE);
            }
            if(jso.optBoolean("Verified", false)) {
                Drawable fuDrawable = ContextCompat.getDrawable(context, R.drawable.ic_iconmonstr_verified);
                fuDrawable.setBounds(0, 0, breweryName.getLineHeight(), breweryName.getLineHeight());
                ImageSpan imageSpan = new ImageSpan(fuDrawable);
                builder.append(" ", imageSpan, 0);
            } else {
                //brewTypes.setVisibility(View.GONE);
                //overflowTypes.setVisibility(View.GONE);
                noBrewCatagories.setVisibility(View.VISIBLE);
                final Button showCats = noBrewCatagories.findViewById(R.id.button_showCatagoriesToRecommend);
                final Button submitCats = findViewById(R.id.button_submitRecommendedCatagories);
                final MapFragment.FilterBar fb = new MapFragment.FilterBar(findViewById(R.id.include_filterbar_recommendations), false) {
                    @Override
                    void doSomething(View view) {

                    }
                };
                noBrewCatagories.findViewById(R.id.include19).setVisibility(View.GONE);
                noBrewCatagories.findViewById(R.id.constraintLayout13).setVisibility(View.GONE);
                showCats.setVisibility(View.VISIBLE);
                showCats.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showCats.setVisibility(View.GONE);
                        findViewById(R.id.include_filterbar_recommendations).setVisibility(View.VISIBLE);
                        submitCats.setVisibility(View.VISIBLE);
                    }
                });
                submitCats.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        updateRecommendations(fb);
                        submitCats.setVisibility(View.GONE);
                        findViewById(R.id.include_filterbar_recommendations).setVisibility(View.GONE);
                        ((TextView) findViewById(R.id.textView_prompt_catagories)).setText("Thank you for helping to improve Quenched for everyone!");
                    }
                });
            }
            ((ScrollView) findViewById(R.id.scrollView4)).getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                @Override
                public void onScrollChanged() {

                }
            });
            brewerySummary.setText(notEmptyOptString(jso, "description", "No Description Found"));
            if(brewerySummary.getText().toString().equals("No Description Found")){
                findViewById(R.id.cardView_descriptonBox).setVisibility(View.GONE);
            } else {
                findViewById(R.id.cardView_descriptonBox).setVisibility(View.VISIBLE);
            }
            breweryWebsite.setText(notEmptyOptString(jso, "WebSite", "No Website Found"));
            images.add(thisBrewery.getThisBitmap());
                    if(!thisBrewery.imgUrl.get(0).equals("")) {
                        Picasso.get().load(thisBrewery.imgUrl.get(0)).into(new Target() {
                            @Override
                            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
                                images.set(0, bitmap);
                                pics.getAdapter().notifyDataSetChanged();
                            }

                            @Override
                            public void onBitmapFailed(Exception e, Drawable drawable) {

                            }

                            @Override
                            public void onPrepareLoad(Drawable drawable) {

                            }
                        });
                    }
                    if(thisBrewery.imgUrl.size() > 1 && !thisBrewery.imgUrl.get(1).equals("")) {
                        images.add(MainActivity.defaultMissingImageBitmap);
                        Picasso.get().load(thisBrewery.imgUrl.get(1)).into(new Target() {
                            @Override
                            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
                                images.set(1, bitmap);
                                pics.getAdapter().notifyDataSetChanged();
                            }

                            @Override
                            public void onBitmapFailed(Exception e, Drawable drawable) {

                            }

                            @Override
                            public void onPrepareLoad(Drawable drawable) {

                            }
                        });
                    }
                    if(thisBrewery.imgUrl.size() > 2 && !thisBrewery.imgUrl.get(2).equals("")) {
                        images.add(MainActivity.defaultMissingImageBitmap);
                        Picasso.get().load(thisBrewery.imgUrl.get(2)).into(new Target() {
                            @Override
                            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
                                images.set(2, bitmap);
                                pics.getAdapter().notifyDataSetChanged();
                            }

                            @Override
                            public void onBitmapFailed(Exception e, Drawable drawable) {

                            }

                            @Override
                            public void onPrepareLoad(Drawable drawable) {

                            }
                        });
                    }
                    if(thisBrewery.imgUrl.size() > 3 && !thisBrewery.imgUrl.get(3).equals("")) {
                        images.add(MainActivity.defaultMissingImageBitmap);
                        Picasso.get().load(thisBrewery.imgUrl.get(3)).into(new Target() {
                            @Override
                            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
                                images.set(3, bitmap);
                                pics.getAdapter().notifyDataSetChanged();
                            }

                            @Override
                            public void onBitmapFailed(Exception e, Drawable drawable) {

                            }

                            @Override
                            public void onPrepareLoad(Drawable drawable) {

                            }
                        });
                    }
            favorited = MainActivity.checkFavoriteBreweryByUniqueID(thisBrewery.uniqueBreweryID);
            if(favorited){
                favoriteBrewery.setImageResource(R.drawable.ic_iconmonstr_favorite_1minus);
            } else {
                favoriteBrewery.setImageResource(R.drawable.ic_iconmonstr_favorite_plus);
            }
            favoriteBrewery.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!favorited) {
                        favoriteBrewery.setImageResource(R.drawable.ic_iconmonstr_favorite_1minus);
                        MainActivity.addFavoriteBreweryByUniqueID(jso.optInt("UniqueID", -1), ViewBrewery.this, true);
                    } else {
                        favoriteBrewery.setImageResource(R.drawable.ic_iconmonstr_favorite_plus);
                        MainActivity.removeFavoriteBreweryByUniqueID(jso.optInt("UniqueID", -1), ViewBrewery.this);
                    }
                    favorited = !favorited;
                }
            });
            openUber = findViewById(R.id.button_openUber);
            openUber.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                String buildURl = "uber://?client_id=rC3LeSNyncKuWq8O89OUCBdoUQWOo_SO&action=setPickup&pickup=my_location&pickup[nickname]=You&dropoff[latitude]="+jso.optDouble("Latitude", -1)
                                                        +"&dropoff[longitude]="+jso.optDouble("Longitude", -1)+"&dropoff[nickname]="+ breweryName.getText().toString() +"&product_id=a1111c8c-c720-46c3-8534-2fcdd730040d&link_text=View%20team%20roster&partner_deeplink=partner%3A%2F%2Fteam%2F9383";
                                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(buildURl)));
                                            }
                                        }
            );
            openLyft = findViewById(R.id.button_openLyft);
            openLyft.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                String buildURl = "lyft://ridetype?id=lyft&pickup[latitude]=null&pickup[longitude]=null&destination[latitude]="+jso.optDouble("Latitude", -1)
                                                        +"&destination[longitude]="+jso.optDouble("Longitude", -1);
                                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(buildURl)));
                                            }
                                        }
            );
        makePost = findViewById(R.id.button_upload);
        makePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                makeNewPost();
            }
        });
        viewpagerleft = findViewById(R.id.imageView_viewpager_left);
        viewpagerleft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pics.setCurrentItem(pics.getCurrentItem() - 1, true);
//                if(currentpic < images.size()) viewpagerright.setVisibility(View.VISIBLE);
            }
        });
        viewpagerleft.setVisibility(View.GONE);
        viewpagerright = findViewById(R.id.imageView_viewpager_right);
        viewpagerright.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pics.setCurrentItem(pics.getCurrentItem()+1, true);
//                if(currentpic > 0) viewpagerleft.setVisibility(View.VISIBLE);
//                if(currentpic == images.size()) viewpagerright.setVisibility(View.GONE);
            }
        });
        RVuserPics = findViewById(R.id.rv_viewBrewery_userPhotos);
        RVuserPics.setLayoutManager(new GridLayoutManager(context, 3));
        adapter = new ViewBreweryRvAdapter(userPostPhotos, new ViewBreweryRvAdapter.directoryRvAdapterCallback() {
            @Override
            public void openPic(ViewBreweryRvAdapter.DirectoryViewHolder holder, int position) {
                zoomImageFromThumb(holder.itemView, userPostPhotos.get(position));
            }
        });
        pics.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if(position == 0)
                    viewpagerleft.setVisibility(View.INVISIBLE);
                else
                    viewpagerleft.setVisibility(View.VISIBLE);
                if(position == images.size() - 1)
                    viewpagerright.setVisibility(View.INVISIBLE);
                else
                    viewpagerright.setVisibility(View.VISIBLE);
            }
        });
        RVuserPics.setAdapter(adapter);
        getBreweryUserPics(userPostPhotos);
        refreshAd();
    }
    public int currentpic = 0;
    private void zoomImageFromThumb(final View thumbView, Bitmap imageResId) {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (currentAnimator != null) {
            currentAnimator.cancel();
        }

        // Load the high-resolution "zoomed-in" image.
        final ImageView expandedImageView = (ImageView) findViewById(
                R.id.imageView_expandedImage);
        expandedImageView.setImageBitmap(imageResId);

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBounds);
        findViewById(R.id.container)
                .getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        thumbView.setAlpha(0f);
        expandedImageView.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expandedImageView.setPivotX(0f);
        expandedImageView.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(expandedImageView, View.X,
                        startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y,
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X,
                        startScale, 1f))
                .with(ObjectAnimator.ofFloat(expandedImageView,
                        View.SCALE_Y, startScale, 1f));
        set.setDuration(shortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                currentAnimator = null;
            }
        });
        set.start();
        currentAnimator = set;

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        final float startScaleFinal = startScale;
        expandedImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentAnimator != null) {
                    currentAnimator.cancel();
                }

                // Animate the four positioning/sizing properties in parallel,
                // back to their original values.
                AnimatorSet set = new AnimatorSet();
                set.play(ObjectAnimator
                        .ofFloat(expandedImageView, View.X, startBounds.left))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.Y,startBounds.top))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_X, startScaleFinal))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_Y, startScaleFinal));
                set.setDuration(shortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        currentAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        currentAnimator = null;
                    }
                });
                set.start();
                currentAnimator = set;
            }
        });
    }
    @NotNull
    private View.OnClickListener showReportDialog() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Dialog dialog = new Dialog(ViewBrewery.this);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setCancelable(true);
                dialog.setContentView(R.layout.report_brewery);
                ((TextView) dialog.findViewById(R.id.report_brewery_name)).setText(notEmptyOptString(jso, "Brewery", "BreweryName"));
                final Spinner catagory = dialog.findViewById(R.id.report_brewery_catagory);
                final EditText descrip = dialog.findViewById(R.id.report_brewery_description);
                Button close = dialog.findViewById(R.id.button_report_cancel);
                close.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
                Button rep = dialog.findViewById(R.id.button_report_confirm);
                rep.setOnClickListener(sendReportToDb(dialog, catagory, descrip));
                dialog.show();
            }
        };
    }

    @NotNull
    private View.OnClickListener showHoursDialog() {
        return new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                final Dialog dialog = new Dialog(ViewBrewery.this);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setCancelable(true);
                dialog.setContentView(R.layout.hours_display);
                ((TextView) dialog.findViewById(R.id.textView_mondayhours)).setText(convertTime(notEmptyOptString(jso, "monday_Hours", "Unknown")));
                ((TextView) dialog.findViewById(R.id.textView_tuesdayhours)).setText(convertTime(notEmptyOptString(jso, "tuesday_Hours", "Unknown")));
                ((TextView) dialog.findViewById(R.id.textView_wednesdayhours)).setText(convertTime(notEmptyOptString(jso, "wednesday_Hours", "Unknown")));
                ((TextView) dialog.findViewById(R.id.textView_thursdayhours)).setText(convertTime(notEmptyOptString(jso, "thursday_Hours", "Unknown")));
                ((TextView) dialog.findViewById(R.id.textView_fridayhours)).setText(convertTime(notEmptyOptString(jso, "friday_Hours", "Unknown")));
                ((TextView) dialog.findViewById(R.id.textView_saturdayhours)).setText(convertTime(notEmptyOptString(jso, "saturday_Hours", "Unknown")));
                ((TextView) dialog.findViewById(R.id.textView_sundayhours)).setText(convertTime(notEmptyOptString(jso, "sunday_Hours", "Unknown")));
                ((TextView) dialog.findViewById(R.id.hours_display_lastupdated)).setText("Last Updated " + notEmptyOptString(jso, "lastUpdated", "Unknown"));
                Button close = dialog.findViewById(R.id.button_hoursdisplay_close);
                close.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        };
    }
    private String convertTime(String s){
        if(s.equals("Unknown")) return "Unknown";
        StringBuilder sb = new StringBuilder();
        try {
            String[] times = s.split("-");
            String[] hour = times[0].split(":");
            int houropen = Integer.parseInt(hour[0]);
            if (houropen < 12) {
                sb.append(times[0] + "AM - ");
            } else {
                if (houropen >= 13) {
                    houropen = houropen - 12;
                    sb.append(houropen).append(":").append(hour[1]).append("PM - ");
                } else {
                    sb.append(times[0]).append("PM - ");
                }
            }
            String[] hour2 = times[1].split(":");
            int hourclose = Integer.parseInt(hour2[0]);
            if (hourclose < 12) {
                sb.append(times[0] + "AM");
            } else {
                if (hourclose >= 13) {
                    hourclose = hourclose - 12;
                    sb.append(hourclose).append(":").append(hour2[1]).append("PM");
                } else {
                    sb.append(times[0]).append("PM");
                }
            }
            return sb.toString();
        } catch (Exception e){
            return sb.toString();
        }
    }

    @NotNull
    private View.OnClickListener sendReportToDb(final Dialog dialog, final Spinner catagory, final EditText descrip) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //REPORT
                final SharedPreferences sp = ViewBrewery.this.getSharedPreferences("RegisteredUser", MODE_PRIVATE);
                String email = sp.getString("Email", "");
                final OkHttpClient client = new OkHttpClient().newBuilder()
                        .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                        .build();
                String json = "{\"Brewery\":\"" + notEmptyOptString(jso, "Brewery", "BreweryName") + "\"," +
                        "\"UniqueID\":\"" + jso.optInt("UniqueID", -1) + "\"," +
                        "\"Catagory\":\"" + catagory.getSelectedItem() + "\"," +
                        "\"Description\":\"" + descrip.getText().toString() + "\"," +
                        "\"Email\":\"" + email + "\"}";
                JSONObject jso = null;
                try {
                    jso = new JSONObject(json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                final RequestBody body = RequestBody.create(
                        MediaType.parse("application/json"), jso.toString());
                final Request request = new Request.Builder()
                        .url(MainActivity.dbEndpoint + "reportBrewery")
                        .post(body)
                        //.addHeader("x-apikey", "7f85916f94b41f0739704a6aa997e749e67fb")
                        .addHeader("cache-control", "no-cache")
                        .build();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Response response = client.newCall(request).execute();
                                response.body().string();
                                if(response.code() == 200){
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(ViewBrewery.this, "Reported! Thank you for your help!", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(context, "Failed to report brewery..", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                dialog.dismiss();
            }
        };
    }

    public void getBreweryUserPics(final ArrayList<Bitmap> userPics){
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                final OkHttpClient client = new OkHttpClient().newBuilder()
                        .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                        .build();
                final Request request = new Request.Builder()
                        .url(MainActivity.dbEndpoint + "postsByBrewery/" + jso.optInt("UniqueID", -1))
                        .get()
                        //.addHeader("x-apikey", "7f85916f94b41f0739704a6aa997e749e67fb")
                        .addHeader("cache-control", "no-cache")
                        .build();
                try {
                    Response response = client.newCall(request).execute();

                    String res = response.body().string();
                    if(response.code() != 200){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "User pics failed to load", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        final JSONArray jsa = new JSONArray(res);
                        final List<JSONObject> jsonValues = new ArrayList<JSONObject>();
                        for (int i = 0; i < jsa.length(); i++) {
                            jsonValues.add(jsa.getJSONObject(i));
                        }
                        jsonValues.sort(new Comparator<JSONObject>() {
                            @Override
                            public int compare(JSONObject jsonObject, JSONObject t1) {
                                return t1.optInt("likes") - jsonObject.optInt("likes");
                            }
                        });
                        for(int i = 0; i < jsonValues.size(); i++){
                            userPics.add(null);
                            final int position = i;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Picasso.get().load(jsonValues.get(position).getString("imgurUrl")).into(new Target() {
                                            @Override
                                            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
                                                userPics.set(position, bitmap);
                                                RVuserPics.getAdapter().notifyDataSetChanged();
                                            }

                                            @Override
                                            public void onBitmapFailed(Exception e, Drawable drawable) {

                                            }

                                            @Override
                                            public void onPrepareLoad(Drawable drawable) {

                                            }
                                        });
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                        }
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    private void updateRecommendations(final MapFragment.FilterBar fb) {
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                final OkHttpClient client = new OkHttpClient().newBuilder()
                        .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                        .build();
                String json = createCatagoriesJson(fb);
                RequestBody body = RequestBody.create(
                        MediaType.parse("application/json"), json);
                final Request request = new Request.Builder()
                        .url(MainActivity.dbEndpoint + "recs_create")
                        .post(body)
                        //.addHeader("x-apikey", "7f85916f94b41f0739704a6aa997e749e67fb")
                        .addHeader("cache-control", "no-cache")
                        .build();
                try {
                    Response response = client.newCall(request).execute();
                    if(response.code() != 200){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Recommendations failed to submit", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    Dialog newPostDialog;
    private void makeNewPost() {
        newPostDialog = new Dialog(context);
        newPostDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        newPostDialog.setCancelable(false);
        newPostDialog.setContentView(R.layout.user_submitted_post);
        int width = (int)(getResources().getDisplayMetrics().widthPixels*0.98);
        int height = (int)(getResources().getDisplayMetrics().heightPixels*0.90);
        //newPostDialog.getWindow().setLayout(width, height);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(newPostDialog.getWindow().getAttributes());
        lp.width = width;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        Button submit = newPostDialog.findViewById(R.id.button_userSubmitted_upload);
        TextView brewName = newPostDialog.findViewById(R.id.textView_userSubmitted_brewname);
        brewName.setText(breweryName.getText().toString());
        final EditText comment = newPostDialog.findViewById(R.id.editText_userSubmitted_comment);
        ConstraintLayout fb = newPostDialog.findViewById(R.id.user_submitted_post_filterbar);
        final MapFragment.FilterBar catagoryBar = new MapFragment.FilterBar(fb, false) {
            @Override
            void doSomething(View view) {

            }
        };
        fb.findViewById(R.id.include19).setVisibility(View.GONE);
        fb.findViewById(R.id.constraintLayout13).setVisibility(View.GONE);
        photoToUpload = newPostDialog.findViewById(R.id.imageView_userSubmitted_image);
        Button chooseImage = newPostDialog.findViewById(R.id.button_chooseFromGallery);
        chooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getIntent.setType("image/*");

                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("image/*");

                Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

                startActivityForResult(chooserIntent, PICK_IMAGE);
            }
        });
        Button takePhoto = newPostDialog.findViewById(R.id.button_takePhoto);

        takePhoto.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                // Ensure that there's a camera activity to handle the intent
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {

                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        photoURI = FileProvider.getUriForFile(context,
                                "com.im.quenched.provider",
                                photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(takePictureIntent, CAMERA_REQUEST);

                    }
                }
            }
        });
        submit.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                if(MainActivity.isLoggedIn(context)) {
                    uploadPicToImgur(toupload, catagoryBar, comment.getText().toString());
                    newPostDialog.dismiss();
                    Toast.makeText(context, "Upload Started... This could take some time! Continue to use the app freely", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "You must be logged in to make a blog post, Go to account settings to log in!", Toast.LENGTH_LONG).show();
                }
            }
        });
        Button close = newPostDialog.findViewById(R.id.button_userSubmitted_cancel);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newPostDialog.dismiss();
            }
        });
        newPostDialog.show();
        newPostDialog.getWindow().setAttributes(lp);
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public String createCatagoriesJson(MapFragment.FilterBar fb){
        final SharedPreferences sp = ViewBrewery.this.getSharedPreferences("RegisteredUser", MODE_PRIVATE);
        final String email = sp.getString("Email", "");
        ArrayList<String> cats = new ArrayList<>();
        cats.add("'userEmail' : " + email);
        cats.add("'UniqueID' : " + jso.optInt("UniqueID", -1));
        if(fb.familyCheck) cats.add("'Family Friendly' : true");
        if(fb.dogCheck) cats.add("'Dog Friendly' : true");
        if(fb.outdoorCheck) cats.add("'Outdoor Seating' : true");
        if(fb.kitchenCheck) cats.add("'In House Kitchen' : true");
        if(fb.crawlCheck) cats.add("'Crawlable' : true");
        String json = "{" + String.join(", ", cats) + "}";
        JSONObject jsoo = null;
        try {
            jsoo = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        assert jsoo != null;
        return jsoo.toString();
    }
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                //Display an error
                return;
            }
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(data.getData());
                toupload = BitmapFactory.decodeStream(inputStream);
                photoToUpload.setImageBitmap(toupload);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            //Now you can do whatever you want with your inpustream, save it as file, upload to a server, decode a bitmap...
        }
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show();

            toupload = null;
            try {
                toupload = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoURI);
                photoToUpload.setImageBitmap(toupload);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void uploadPicToImgur(final Bitmap picToUpload, final MapFragment.FilterBar fb, final String comment) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    Bitmap bitmap = picToUpload;
                    Bitmap scaled = scaleDown(bitmap, 1000f, true);
                    scaled.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] imageBytes = baos.toByteArray();

                    String imageString = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                    final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(70, TimeUnit.SECONDS).writeTimeout(0, TimeUnit.SECONDS).readTimeout(0, TimeUnit.SECONDS).build();
                    MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");

                    RequestBody body = RequestBody.create(mediaType, imageString);
                    final Request request = new Request.Builder()
                            .url("https://imgur-apiv3.p.rapidapi.com/3/image")
                            .post(body)
                            .addHeader("Content-Type", "application/x-www-form-urlencoded")
                            .addHeader("Authorization", "Client-ID d977a7cc9464eca")
                            .addHeader("x-rapidapi-key", "7a33fb023dmsh26f191c59816e64p1168dfjsn5e1e67646390")
                            .addHeader("x-rapidapi-host", "imgur-apiv3.p.rapidapi.com")
                            .build();
                    Response response = client.newCall(request).execute();
                    JSONObject res = new JSONObject(response.body().string());
                    JSONObject data = res.getJSONObject("data");
                    String url = data.getString("link");
                    if(response.code() == 200){
                        if(url.equals("")){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "Post upload unsuccessful, please try again :(", Toast.LENGTH_LONG).show();
                                }});
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "Post upload was successful!", Toast.LENGTH_LONG).show();
                                }
                            });
                            submitPostToDB(url, fb, comment);
                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Post upload unsuccessful, please try again :(", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    public static Bitmap scaleDown(Bitmap realImage, float maxImageSize,
                                   boolean filter) {
        float ratio = Math.min(
                (float) maxImageSize / realImage.getWidth(),
                (float) maxImageSize / realImage.getHeight());
        if(ratio <= 1) return realImage;
        int width = Math.round((float) ratio * realImage.getWidth());
        int height = Math.round((float) ratio * realImage.getHeight());

        Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
                height, filter);
        return newBitmap;
    }
    public void submitPostToDB(final String url, MapFragment.FilterBar fb, final String comment){
        final SharedPreferences sp = ViewBrewery.this.getSharedPreferences("RegisteredUser", MODE_PRIVATE);
        final String email = sp.getString("Email", "");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final OkHttpClient client = new OkHttpClient().newBuilder()
                        .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                        .build();
                String json = "{\"Brewery\":\"" + notEmptyOptString(jso, "Brewery", "BreweryName") + "\"," +
                        "\"UniqueID\":\"" + jso.optInt("UniqueID", -1) + "\"," +
                        "\"privateComment\":\"" + comment + "\"," +
                        "\"imgurUrl\":\"" + url + "\"," +
                        "\"username\":\"" + sp.getString("Username", "Anonymous") + "\"," +
                        "\"Roles\":\"" + sp.getString("Roles", "{}") + "\"," +
                        "\"location\":\"" + jso.optString("City") + ", " + jso.optString("StateProvince") + "\"," +
                        "\"Latitude\":\"" + jso.optString("Latitude") + "\"," +
                        "\"Longitude\":\"" + jso.optString("Longitude") + "\"," +
                        "\"userEmail\":\"" + email + "\"}";
                JSONObject jsoo = null;
                try {
                    jsoo = new JSONObject(json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                assert jsoo != null;
                final RequestBody body = RequestBody.create(
                        jsoo.toString(), MediaType.parse("application/json; charset=utf-8"));
                final Request request = new Request.Builder()
                        .url(MainActivity.dbEndpoint + "post_create")
                        .post(body)
                        .addHeader("x-apikey", "7f85916f94b41f0739704a6aa997e749e67fb")
                        .addHeader("cache-control", "no-cache")
                        .build();
                try {
                    Response response = client.newCall(request).execute();
                    response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        updateRecommendations(fb);
    }
    public String notEmptyOptString(JSONObject json, String field, String fallback){
        String s = json.optString(field, fallback);
        if(s.trim().equals("")){
            return fallback;
        }
        return s;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    private void populateUnifiedNativeAdView(UnifiedNativeAd nativeAd, UnifiedNativeAdView adView) {
        // Set the media view.
        adView.setMediaView((MediaView) adView.findViewById(R.id.ad_media));

        // Set other ad assets.
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        // The headline and mediaContent are guaranteed to be in every UnifiedNativeAd.
        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        adView.getMediaView().setMediaContent(nativeAd.getMediaContent());

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.getBody() == null) {
            adView.getBodyView().setVisibility(View.INVISIBLE);
        } else {
            adView.getBodyView().setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            adView.getCallToActionView().setVisibility(View.INVISIBLE);
        } else {
            adView.getCallToActionView().setVisibility(View.VISIBLE);
            ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }

        if (nativeAd.getIcon() == null) {
            adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(
                    nativeAd.getIcon().getDrawable());
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

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd);

    }
    private void refreshAd() {
        AdLoader.Builder builder = new AdLoader.Builder(this, getString(R.string.AdmobID));

        builder.forUnifiedNativeAd(
                new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
                    // OnUnifiedNativeAdLoadedListener implementation.
                    @Override
                    public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                        // If this callback occurs after the activity is destroyed, you must call
                        // destroy and return or you may get a memory leak.
                        boolean isDestroyed = false;
                        isDestroyed = isDestroyed();
                        if (isDestroyed || isFinishing() || isChangingConfigurations()) {
                            unifiedNativeAd.destroy();
                            return;
                        }
                        // You must call destroy on old ads when you are done with them,
                        // otherwise you will have a memory leak.
                        if (nativeAd != null) {
                            nativeAd.destroy();
                        }
                        nativeAd = unifiedNativeAd;
                        FrameLayout frameLayout = findViewById(R.id.viewBreweryAdHolder);
                        UnifiedNativeAdView adView =
                                (UnifiedNativeAdView) getLayoutInflater()
                                        .inflate(R.layout.ad_unified, null);
                        populateUnifiedNativeAdView(unifiedNativeAd, adView);
                        frameLayout.removeAllViews();
                        frameLayout.addView(adView);
                    }
                });

        AdLoader adLoader =
                builder.withAdListener(
                                new AdListener() {
                                    @Override
                                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                                        String error =
                                                String.format(
                                                        "domain: %s, code: %d, message: %s",
                                                        loadAdError.getDomain(),
                                                        loadAdError.getCode(),
                                                        loadAdError.getMessage());
                                        Toast.makeText(
                                                ViewBrewery.this,
                                                "Failed to load native ad with error " + error,
                                                Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                })
                        .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    @Override
    protected void onDestroy() {
        if (nativeAd != null) {
            nativeAd.destroy();
        }
        super.onDestroy();
    }
}