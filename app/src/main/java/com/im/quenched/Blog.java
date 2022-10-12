package com.im.quenched;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

public class Blog extends Fragment implements BlogAdapter.openBreweryFromBlog{

    // TODO: Rename parameter arguments, choose names that match

    private static final int REQUEST_CODE = 654;
    private static final int PICK_IMAGE = 777;
    private static final int CAMERA_REQUEST = 888;
    TextView newest;
    TextView top;
    TextView local;
    TextView myPosts;
    ArrayList<JSONObject> blogs = new ArrayList<>();
    ArrayList<String> hideTheseBlogs;
    ArrayList<BlogItem> posts = new ArrayList<>();
    RecyclerView blogrv;
    BlogAdapter blogAdapter;
    Context context;
    BlogAdapter.openBreweryFromBlog listener;
    ProgressBar pb;
    String email;
    HashMap<String, Boolean> postsLiked = new HashMap<>();
    ImageButton makePost;
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;



    public Blog() {
        // Required empty public constructor
    }

    public static Blog newInstance(String param1, String param2) {
        Blog fragment = new Blog();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
        //queryBlogData("");
        listener = this;
        context = getActivity();
        final SharedPreferences sp = context.getSharedPreferences("RegisteredUser", MODE_PRIVATE);
        email = sp.getString("Email", "");
        loadBlogsToHide();
        loadUserLikes();
    }

    private void loadUserLikes() {
        Thread t = new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient().newBuilder()
                        .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                        .build();
                Request request = new Request.Builder()
                        .url(MainActivity.dbEndpoint + "likes/" + MainActivity.getUser(context))
                        .get()
                        //.addHeader("x-apikey", "7f85916f94b41f0739704a6aa997e749e67fb")
                        .addHeader("cache-control", "no-cache")
                        .build();
                try {
                    Response response = client.newCall(request).execute();
                    String s;
                    if (response.body() == null) throw new AssertionError();
                    s = response.body().string();
                    JSONArray jsonArray = new JSONArray(s);
                    if(jsonArray.length() == 0) {
                        return;
                    }
                    for(int i = 0; i < jsonArray.length(); i++){
                        JSONObject jso = (JSONObject) jsonArray.get(i);
                        postsLiked.putIfAbsent(jso.optString("PostID",""), jso.optBoolean("liked"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            Toast.makeText(context, "Failed to load likes in time", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v =  inflater.inflate(R.layout.fragment_blog, container, false);
        // Inflate the layout for this fragment
        TextView title = v.findViewById(R.id.textView_blog_title);
        String[] array = context.getResources().getStringArray(R.array.blogTitles);
        String randomStr = array[new Random().nextInt(array.length)];
        title.setText(randomStr);
        makePost = v.findViewById(R.id.blog_makePost_imagebutton);
        makePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                makeNewPost();
            }
        });
        blogrv = v.findViewById(R.id.rv_blogs);
        blogrv.setLayoutManager(new LinearLayoutManager(v.getContext()));
        blogAdapter = new BlogAdapter(posts, listener);
        blogrv.setAdapter(blogAdapter);
        newest = v.findViewById(R.id.textView_blog_newest);
        newest.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                sortNavPressed(newest);
            }
        });
        top = v.findViewById(R.id.textView_blog_top);
        top.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                sortNavPressed(top);
            }
        });
        local = v.findViewById(R.id.textView_blog_local);
        local.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                sortNavPressed(local);
            }
        });
        myPosts = v.findViewById(R.id.textView_blog_myPosts);
        myPosts.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                sortNavPressed(myPosts);
            }
        });
        pb = v.findViewById(R.id.progressBar_blogRv);
        newest.performClick();
        //setTextViewDrawableColor(newest, R.color.blue);
        return v;
    }
    private void setTextViewDrawableColor(TextView textView, int color) {
        for (Drawable drawable : textView.getCompoundDrawables()) {
            if (drawable != null) {
                drawable.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(textView.getContext(), color), PorterDuff.Mode.SRC_IN));
            }
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void sortNavPressed(TextView pressed) {
        pb.setVisibility(View.VISIBLE);
        blogrv.scrollToPosition(0);
        if(pressed == newest){
            queryBlogData(1);
            newest.setTextColor(getResources().getColor(R.color.blue));
            setTextViewDrawableColor(newest, R.color.blue);
            top.setTextColor(getResources().getColor(R.color.black));
            setTextViewDrawableColor(top, R.color.black);
            local.setTextColor(getResources().getColor(R.color.black));
            setTextViewDrawableColor(local, R.color.black);
            myPosts.setTextColor(getResources().getColor(R.color.black));
            setTextViewDrawableColor(myPosts, R.color.black);
        }
        if(pressed == top){
            queryBlogData( 2);
            blogAdapter.notifyDataSetChanged();
            newest.setTextColor(getResources().getColor(R.color.black));
            setTextViewDrawableColor(newest, R.color.black);
            top.setTextColor(getResources().getColor(R.color.blue));
            setTextViewDrawableColor(top, R.color.blue);
            local.setTextColor(getResources().getColor(R.color.black));
            setTextViewDrawableColor(local, R.color.black);
            myPosts.setTextColor(getResources().getColor(R.color.black));
            setTextViewDrawableColor(myPosts, R.color.black);
        }
        if(pressed == local){
            queryBlogData(3);
            blogAdapter.notifyDataSetChanged();
            newest.setTextColor(getResources().getColor(R.color.black));
            setTextViewDrawableColor(newest, R.color.black);
            top.setTextColor(getResources().getColor(R.color.black));
            setTextViewDrawableColor(top, R.color.black);
            local.setTextColor(getResources().getColor(R.color.blue));
            setTextViewDrawableColor(local, R.color.blue);
            myPosts.setTextColor(getResources().getColor(R.color.black));
            setTextViewDrawableColor(myPosts, R.color.black);
        }
        if(pressed == myPosts){
            querySelfBlogData("posts/" + email);
            newest.setTextColor(getResources().getColor(R.color.black));
            setTextViewDrawableColor(newest, R.color.black);
            top.setTextColor(getResources().getColor(R.color.black));
            setTextViewDrawableColor(top, R.color.black);
            local.setTextColor(getResources().getColor(R.color.black));
            setTextViewDrawableColor(local, R.color.black);
            myPosts.setTextColor(getResources().getColor(R.color.blue));
            setTextViewDrawableColor(myPosts, R.color.blue);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void sortByUpvotes() {
        posts.sort(new Comparator<BlogItem>() {
            @Override
            public int compare(BlogItem blogItem, BlogItem t1) {
                return t1.upvotes - blogItem.upvotes;
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void sortByDistance() {
        posts.sort(new Comparator<BlogItem>() {
            @Override
            public int compare(BlogItem blogItem, BlogItem t1) {
                return Double.compare(MainActivity.distanceFromYou(t1.latitude, t1.longitude), MainActivity.distanceFromYou(blogItem.latitude, blogItem.longitude));
            }
        });
        ArrayList<BlogItem> newlist = new ArrayList<>();
        for (BlogItem item :
                posts) {
            if(MainActivity.distanceFromYou(item.latitude, item.longitude) <= 50){
                newlist.add(item);
            }
        }
        posts.clear();
        posts.addAll(newlist);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void sortByCreationTime() {
        posts.sort(new Comparator<BlogItem>() {
            @Override
            public int compare(BlogItem blogItem, BlogItem t1) {
                return (int) (t1.created - blogItem.created);
            }
        });
    }

    boolean hasQueried = false;
    private void querySelfBlogData(final String query) {
        hasQueried = false;
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient().newBuilder()
                        .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                        .build();
                Request request = new Request.Builder()
                        .url(MainActivity.dbEndpoint + query)
                        .get()
                        //.addHeader("x-apikey", "7f85916f94b41f0739704a6aa997e749e67fb")
                        .addHeader("cache-control", "no-cache")
                        .build();
                try {
                    Response response = client.newCall(request).execute();
                    String s;
                    if (response.body() == null) throw new AssertionError();
                    s = response.body().string();
                    JSONArray jsonArray = new JSONArray(s);
                    blogs.clear();
                    if(jsonArray.length() == 0) {
                        handApiResult(-1);
                        return;
                    }
                    for(int i = 0; i < jsonArray.length(); i++){
                        JSONObject jso = (JSONObject) jsonArray.get(i);
                        blogs.add(jso);
                    }
                    handApiResult(-1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void queryBlogData(int whichNav) {
        if(hasQueried) {
            if(blogs.size() > posts.size()){
                handApiResult(whichNav);
            }
            pb.setVisibility(View.INVISIBLE);
            switch(whichNav){
                case 1:
                    sortByCreationTime();
                    blogAdapter.notifyDataSetChanged();
                    break;
                case 2:
                    sortByUpvotes();
                    blogAdapter.notifyDataSetChanged();
                    break;
                case 3:
                    sortByDistance();
                    blogAdapter.notifyDataSetChanged();
                    break;
                default:
                    break;
            }
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient().newBuilder()
                        .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                        .build();
                Request request = new Request.Builder()
                        .url(MainActivity.dbEndpoint + "posts")
                        .get()
                        //.addHeader("x-apikey", "7f85916f94b41f0739704a6aa997e749e67fb")
                        .addHeader("cache-control", "no-cache")
                        .build();
                try {
                    Response response = client.newCall(request).execute();
                    String s;
                    if (response.body() == null) throw new AssertionError();
                    s = response.body().string();
                    JSONArray jsonArray = new JSONArray(s);
                    blogs.clear();
                    if(jsonArray.length() == 0) {
                        handApiResult(whichNav);
                        return;
                    }
                    for(int i = 0; i < jsonArray.length(); i++){
                        JSONObject jso = (JSONObject) jsonArray.get(i);
                        blogs.add(jso);
                    }
                    handApiResult(whichNav);
                    hasQueried = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void handApiResult(int navSwitch){
        posts.clear();
        for(int x = 0; x < blogs.size(); x++) {
            if(isBlogHidden(blogs.get(x).optString("id", "err"))) continue;
            String owner = blogs.get(x).optString("userEmail","error@error.com");
            boolean owns = email.equals(owner);
            posts.add(new BlogItem(blogs.get(x), blogs.get(x).optString("Brewery"), null, blogs.get(x).optInt("likes", 0), blogs.get(x).optInt("UniqueID", -1), x, blogs.get(x).optString("imgurUrl", ""), owns));
        }
        switch(navSwitch){
            case 1:
                sortByCreationTime();
                break;
            case 2:
                sortByUpvotes();
                break;
            case 3:
                sortByDistance();
                break;
            default:
                break;
        }
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pb.setVisibility(View.INVISIBLE);
                blogAdapter.notifyDataSetChanged();
            }
        });
        //loadImagesIn();
    }

    @Override
    public void openBrewery(int position) {
        int uniq = posts.get(position).breweryID;
        pb.setVisibility(View.VISIBLE);
        Intent intent = new Intent(getActivity(), ViewBrewery.class);
        MainActivity.filteredDir dir = MainActivity.findOrLoadBrewery(uniq);
        if(dir == null) {
            Toast.makeText(context, "Could not find that brewery, Sorry!", Toast.LENGTH_SHORT).show();
            pb.setVisibility(View.INVISIBLE);
            return;
        }
        intent.putExtra("jso", dir.jso.toString());
        intent.putExtra("Index", dir.uniqueBreweryID);
        //intent.putExtra("Image", dir.getThisBitmap());
        intent.setAction("Preview");
        pb.setVisibility(View.INVISIBLE);
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    public void likePost(int position, int incBy) {
        boolean updown = false;
        if(incBy > 0 ) updown = true;
        posts.get(position).upvotes += incBy;
        if(updown) {
            final OkHttpClient client = new OkHttpClient().newBuilder()
                    .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                    .build();

            String id = posts.get(position).jso.optString("id", "Error");

            String json = "{'UserID' : " + MainActivity.getUser(context) + ", 'PostID' : " + id + "}";
            JSONObject jso = null;
            try {
                jso = new JSONObject(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            final RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"), jso.toString());
            final Request request = new Request.Builder()
                    .url(MainActivity.dbEndpoint + "like")
                    .post(body)
                    //.addHeader("x-apikey", API_KEY)
                    .addHeader("cache-control", "no-cache")
                    .build();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Response response = client.newCall(request).execute();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }).start();
        } else {
            final OkHttpClient client = new OkHttpClient().newBuilder()
                    .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                    .build();

            String id = posts.get(position).jso.optString("id", "Error");

            String json = "{'UserID' : " + MainActivity.getUser(context) + ", 'PostID' : " + id + "}";
            JSONObject jso = null;
            try {
                jso = new JSONObject(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            final RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"), jso.toString());
            final Request request = new Request.Builder()
                    .url(MainActivity.dbEndpoint + "dislike")
                    .post(body)
                    //.addHeader("x-apikey", API_KEY)
                    .addHeader("cache-control", "no-cache")
                    .build();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Response response = client.newCall(request).execute();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }).start();
        }
    }

    @Override
    public void unvotePost(int position, int incBy) {
        posts.get(position).upvotes += incBy;
        final OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                .build();
        String id = posts.get(position).jso.optString("id", "Error");
        String json = "{'UserID' : " + MainActivity.getUser(context) + ", 'PostID' : " + id + "}";
        JSONObject jso = null;
        try {
            jso = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), jso.toString());
        final Request request = new Request.Builder()
                .url(MainActivity.dbEndpoint+"unvote")
                .post(body)
                //.addHeader("x-apikey", API_KEY)
                .addHeader("cache-control", "no-cache")
                .build();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Response response = client.newCall(request).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    @Override
    public void optionPost(int position, int menuChoice) {
        if(menuChoice == -1){
            attemptToDeleteBlog(position);
        }
        if(menuChoice == 0){
            reportPost(position);
        }
        if(menuChoice == 1){
            Bitmap mBitmap = posts.get(position).getImage();

            String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), mBitmap, "Image Description", null);
            Uri uri = Uri.parse(path);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/jpeg");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(intent, "Share Image"));
        }
        if(menuChoice == 2){
            Bitmap mBitmap = posts.get(position).getImage();
            saveImageBitmap(mBitmap, posts.get(position).name);
        }
    }

    private void attemptToDeleteBlog(final int position) {
        BlogItem thisblog = posts.get(position);
        final OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                .build();
        String id = thisblog.jso.optString("id", "Error");
        String json = "{'userEmail' : " + MainActivity.getUser(context) + ", 'id' : " + id + "}";
        JSONObject jso = null;
        try {
            jso = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), jso.toString());
        final Request request = new Request.Builder()
                .url(MainActivity.dbEndpoint + "post_delete")
                .post(body)
                //.addHeader("x-apikey", API_KEY)
                .addHeader("cache-control", "no-cache")
                .build();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Response response = client.newCall(request).execute();
                    if(response.code() == 200) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Successfully Deleted!", Toast.LENGTH_SHORT).show();
                                posts.remove(position);
                                blogAdapter.notifyItemRemoved(position);
                            }
                        });
                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Delete Unsuccessful...", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public boolean isStoragePermissionGranted() {
        String TAG = "Storage Permission";
        if (Build.VERSION.SDK_INT >= 23) {
            if (getActivity().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {
                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }
    }

    public void saveImageBitmap(Bitmap image_bitmap, String image_name) {
        String root = Environment.getStorageDirectory().toString();
        if (isStoragePermissionGranted()) { // check or ask permission
            File myDir = new File(root, "/saved_images");
            if (!myDir.exists()) {
                myDir.mkdirs();
            }
            String fname = "Image-" + image_name + ".jpg";
            File file = new File(myDir, fname);
            if (file.exists()) {
                file.delete();
            }
            try {
                file.createNewFile(); // if file already exists will do nothing
                FileOutputStream out = new FileOutputStream(file);
                image_bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.flush();
                out.close();
                Toast.makeText(context, fname + " saved to " + myDir.getPath(), Toast.LENGTH_SHORT);
            } catch (Exception e) {
                e.printStackTrace();
            }

            MediaScannerConnection.scanFile(context, new String[]{file.toString()}, new String[]{file.getName()}, null);
        }
    }
    private void reportPost(final int position) {
        final BlogItem thisblog = posts.get(position);
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.report_blog);
        ((TextView) dialog.findViewById(R.id.report_brewery_name)).setText(thisblog.name);
        ((ImageView) dialog.findViewById(R.id.report_blog_image)).setImageBitmap(thisblog.image);
        final Spinner catagory = dialog.findViewById(R.id.report_bug_catagory);
        final EditText descrip = dialog.findViewById(R.id.report_brewery_description);
        Button close = dialog.findViewById(R.id.button_report_cancel);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        Button rep = dialog.findViewById(R.id.button_report_confirm);
        rep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //REPORT
                final OkHttpClient client = new OkHttpClient().newBuilder()
                        .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                        .build();
                String json = "{\"Brewery\":\"" + thisblog.name + "\"," +
                        "\"blogPostID\":\"" + thisblog.jso.optString("id", "Error") + "\"," +
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
                        .url(MainActivity.dbEndpoint + "reportPost")
                        .post(body)
                        //.addHeader("x-apikey", API_KEY)
                        .addHeader("cache-control", "no-cache")
                        .build();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Response response = client.newCall(request).execute();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                Toast.makeText(context, "Reported! Thank you for your help!", Toast.LENGTH_SHORT).show();
                addBlogToHide(posts.get(position));
                dialog.dismiss();
            }
        });
        dialog.show();
    }
    public void addBlogToHide(BlogItem thisblog) {
        String id = thisblog.jso.optString("id","error");
        if (null == hideTheseBlogs) {
            hideTheseBlogs = new ArrayList<String>();
        }
        hideTheseBlogs.add(id);

        // save the task list to preference
        SharedPreferences prefs = context.getSharedPreferences("hiddenBlogs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        try {
            editor.putString("Blogs", ObjectSerializer.serialize(hideTheseBlogs));
        } catch (IOException e) {
            e.printStackTrace();
        }
        editor.commit();
    }
    public void loadBlogsToHide(){
        if (null == hideTheseBlogs) {
            hideTheseBlogs = new ArrayList<String>();
        }
        // load tasks from preference
        SharedPreferences prefs = context.getSharedPreferences("hiddenBlogs", Context.MODE_PRIVATE);
        try {
            hideTheseBlogs = (ArrayList<String>) ObjectSerializer.deserialize(prefs.getString("Blogs", ObjectSerializer.serialize(new ArrayList<String>())));
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    public boolean isBlogHidden(String id){
        for (String thisId :
                hideTheseBlogs) {
            if(id.equals(thisId)) return true;
        }
        return false;
    }


    public class BlogItem{
        JSONObject jso;
        private Bitmap image;
        public String name;
        public int upvotes;
        boolean liked = false;
        int breweryID;
        int index;
        String imgurUrl;
        boolean thisUsersPost = false;
        long created;
        float latitude;
        float longitude;
        public BlogItem(JSONObject jso, String name, Bitmap image, int upvotes, int breweryID, int index, String imgurUrl, boolean thisUsersPost){
            this.jso = jso;
            this.image = image;
            this.name = name;
            this.upvotes = upvotes;
            this.index = index;
            this.breweryID = breweryID;
            this.imgurUrl = imgurUrl;
            this.thisUsersPost = thisUsersPost;
            created = jso.optLong("Created");
            longitude = Float.parseFloat(jso.optString("Latitude", "0.0"));
            latitude = Float.parseFloat(jso.optString("Longitude", "0.0"));
            if(postsLiked.containsKey(jso.optString("id", "fail"))){
                liked = postsLiked.get(jso.optString("id", "fail"));
            } else {
                liked = false;
            }
        }
        public Bitmap getImage(){
            if(image != null) return image;
            else {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if(imgurUrl.equals("")) return;
                            image = Picasso.get().load(imgurUrl).get();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if(getActivity()==null) return;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                blogAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }).start();
                return MainActivity.defaultMissingImageBitmap;
            }
        }
    }

    private void loadImagesIn() {
        for(BlogItem bi: posts){
            if(bi.image == null) loadBlogImage(bi);
        }
    }

    private void loadBlogImage(final BlogItem bi) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(bi.imgurUrl.equals("")) return;
                    bi.image = Picasso.get().load(bi.imgurUrl).get();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(getActivity()==null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        blogAdapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();

    }

    Dialog newPostDialog;
    ImageView photoToUpload;
    private Uri photoURI;
    Bitmap toupload;
    private void makeNewPost() {
        final Boolean[] isSelectedText = {false};
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
        final Button submit = newPostDialog.findViewById(R.id.button_userSubmitted_upload);
        TextView brewName = newPostDialog.findViewById(R.id.textView_userSubmitted_brewname);
        brewName.setVisibility(View.GONE);
        final AutoCompleteTextView userBrewNameInout = newPostDialog.findViewById(R.id.editText_userInputBreweryName);
        userBrewNameInout.setVisibility(View.VISIBLE);
        userBrewNameInout.setThreshold(1);
        //String[] names = MainActivity.brewNames.toArray(new String[MainActivity.brewNames.size()]);
        String[] suggs = MainActivity.brewNames.keySet().toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.select_dialog_singlechoice, suggs);
        submit.setEnabled(false);
        userBrewNameInout.setAdapter(adapter);
        userBrewNameInout.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(userBrewNameInout.getRootView().getWindowToken(), 0);
                isSelectedText[0] = true;
                userBrewNameInout.setError(null);
                submit.setEnabled(true);
            }

        });
        userBrewNameInout.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                isSelectedText[0] = false;
                submit.setEnabled(false);
                userBrewNameInout.setError("Must choose brewery from the list");
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
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
                if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
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
                    uploadPicToImgur(toupload, catagoryBar, comment.getText().toString(), MainActivity.brewNames.get(userBrewNameInout.getText().toString()));
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
    private void uploadPicToImgur(final Bitmap picToUpload, final MapFragment.FilterBar fb, final String comment, final Integer breweryNameKey) {
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
                    final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).writeTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build();
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
                            Activity activity = getActivity();
                            if(activity != null){
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, "Post upload unsuccessful, please try again :(", Toast.LENGTH_LONG).show();
                                    }});
                            }
                        } else {
                            Activity activity = getActivity();
                            if(activity != null){
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, "Post upload was successful!", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                            submitPostToDB(url, fb, comment, MainActivity.brewJsons.get(breweryNameKey));
                        }
                    } else {
                        Activity activity = getActivity();
                        if(activity != null){
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "Post upload unsuccessful, please try again :(", Toast.LENGTH_LONG).show();
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
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return image;
    }
    public void submitPostToDB(final String url, MapFragment.FilterBar fb, final String comment, final JSONObject jso){
        final SharedPreferences sp = context.getSharedPreferences("RegisteredUser", MODE_PRIVATE);
        final String email = MainActivity.getUser(context);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final OkHttpClient client = new OkHttpClient().newBuilder()
                        .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                        .build();
                String json = "{\"Brewery\":\"" + jso.optString( "Brewery", "FailedBreweryName") + "\"," +
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
        updateRecommendations(fb, jso);
    }
    private void updateRecommendations(final MapFragment.FilterBar fb, JSONObject jso) {
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                final OkHttpClient client = new OkHttpClient().newBuilder()
                        .connectTimeout(15000, TimeUnit.MILLISECONDS).readTimeout(15000, TimeUnit.MILLISECONDS)
                        .build();
                String json = createCatagoriesJson(fb, jso);
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
                        requireActivity().runOnUiThread(new Runnable() {
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

    public String createCatagoriesJson(MapFragment.FilterBar fb, JSONObject jso){
        final SharedPreferences sp = context.getSharedPreferences("RegisteredUser", MODE_PRIVATE);
        final String email = sp.getString("Email", "");
        ArrayList<String> cats = new ArrayList<>();
        cats.add("'userEmail' : " + email);
        cats.add("'UniqueID' : " + jso.optInt("UniqueID", -1));
        if(fb.familyCheck) cats.add("'Family Friendly' : true");
        if(fb.dogCheck) cats.add("'Dog Friendly' : true");
        if(fb.outdoorCheck) cats.add("'Outdoor Seating' : true");
        if(fb.kitchenCheck) cats.add("'In House Kitchen' : true");
        if(fb.crawlCheck) cats.add("'Crawlable' : true");
        String json = "{" + TextUtils.join(", ", cats) + "}";
        JSONObject jsoo = null;
        try {
            jsoo = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        assert jsoo != null;
        return jsoo.toString();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
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
            toupload = null;
            try {
                toupload = MediaStore.Images.Media.getBitmap(context.getContentResolver(), photoURI);
                photoToUpload.setImageBitmap(toupload);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}