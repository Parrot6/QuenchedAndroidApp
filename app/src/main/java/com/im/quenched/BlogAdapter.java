package com.im.quenched;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class BlogAdapter extends RecyclerView.Adapter<BlogAdapter.BlogViewHolder> {

    static ArrayList<Blog.BlogItem> posts = new ArrayList<>();
    static BlogAdapter.openBreweryFromBlog listener;
    public BlogAdapter(ArrayList<Blog.BlogItem> post, openBreweryFromBlog listen) {
        posts = post;
        listener = listen;
    }

    @NonNull
    @Override
    public BlogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.blog_item_variant_insta, parent, false);
        BlogViewHolder holder = new BlogViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull final BlogViewHolder holder, int position) {
        if(posts.size() == 0){
            holder.brewName.setText("No Results");
            holder.blogPic.setImageDrawable(MainActivity.defaultMissingImageDrawable);
            holder.upvotes.setVisibility(View.GONE);
            //holder.down.setVisibility(View.GONE);
            holder.up.setVisibility(View.GONE);
            holder.link.setVisibility(View.GONE);
            holder.brewLoc.setText("Unknown");
            holder.postUsername.setText("Anonymous");
            holder.postFlairs.setVisibility(View.GONE);
            return;
        }
        holder.postUsername.setText(posts.get(position).jso.optString("username", "Anonymous"));
        String flairs = posts.get(position).jso.optString("Roles", "");
        StringBuilder output = new StringBuilder();
        if(!flairs.equals("") && !flairs.equals("{}")) {
            holder.postFlairs.setVisibility(View.VISIBLE);
            try {
                JSONArray flairJso = new JSONArray(flairs);
                for (int i = 0; i < flairJso.length(); i++) {
                    output.append(flairJso.get(0).toString());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else holder.postFlairs.setVisibility(View.GONE);
        //if(flairs.equals("{}") || flairs.equals(""))
        holder.postFlairs.setText(output.toString());
        holder.upvotes.setVisibility(View.VISIBLE);
        holder.brewLoc.setText(posts.get(position).jso.optString("location", "Unknown"));
        //holder.down.setVisibility(View.VISIBLE);
        holder.up.setVisibility(View.VISIBLE);
        holder.link.setVisibility(View.VISIBLE);
        holder.brewName.setText(posts.get(position).name);
        holder.blogPic.setImageBitmap(posts.get(position).getImage());
        holder.upvotes.setText(String.valueOf(posts.get(position).upvotes));
        giveVotedBackground(holder.up, posts.get(position).liked);
        //giveVotedBackground(holder.down, posts.get(position).disliked);
    }
    public void giveVotedBackground(ImageView iv, boolean voted){
        if(voted) iv.setImageResource(R.drawable.ic_mug_full);
        else iv.setImageResource(R.drawable.ic_mug_empty);
    }
    @Override
    public int getItemCount() {
        return Math.max(1, posts.size());
    }

    public static class BlogViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        ImageView blogPic;
        TextView postUsername;
        TextView postFlairs;
        TextView brewName;
        TextView brewLoc;
        TextView upvotes;
        ImageView up;
        ImageView down;
        ImageView link;
        BlogViewHolder(@NonNull View itemView) {
            super(itemView);
            postUsername = itemView.findViewById(R.id.blog_username);
            postFlairs = itemView.findViewById(R.id.blog_flairs);
            brewName = itemView.findViewById(R.id.blog_item_title);
            brewName.setOnClickListener(this);
            brewLoc = itemView.findViewById(R.id.blog_item_title_location);
            brewLoc.setOnClickListener(this);
            blogPic = itemView.findViewById(R.id.blog_item_image);
            upvotes = itemView.findViewById(R.id.textView_blog_upvotes);
            link = itemView.findViewById(R.id.blog_moreMenu);
            link.setOnClickListener(this);
            up = itemView.findViewById(R.id.blog_upvote);
            up.setOnClickListener(this);
            //down = itemView.findViewById(R.id.blog_downvote);
            //down.setOnClickListener(this);
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.blog_moreMenu:
                    //creating a popup menu
                    PopupMenu popup = new PopupMenu(view.getContext(), view);
                    //inflating menu from xml resource
                    popup.inflate(R.menu.blog_options);
                    if(!posts.get(getAdapterPosition()).thisUsersPost) popup.getMenu().findItem(R.id.delete_blog).setVisible(false);
                    //adding click listener
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.delete_blog:
                                    listener.optionPost(getAdapterPosition(), -1);
                                    //handle menu2 click
                                    return true;
                                case R.id.report_blog:
                                    listener.optionPost(getAdapterPosition(), 0);
                                    //handle menu1 click
                                    return true;
                                case R.id.save_blog:
                                    listener.optionPost(getAdapterPosition(), 1);
                                    //handle menu2 click
                                    return true;
                                case R.id.save_internal_blog:
                                    listener.optionPost(getAdapterPosition(), 2);
                                    //handle menu2 click
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });
                    popup.show();
                    break;
                case R.id.blog_item_title_location:
                case R.id.blog_item_title:
                    if(posts.size() == 0) break;
                    else listener.openBrewery(getAdapterPosition());
                    break;
                case R.id.blog_upvote:
                    if(posts.get(getAdapterPosition()).liked){
                        posts.get(getAdapterPosition()).liked = false;
                        listener.unvotePost(getAdapterPosition(), -1);
                        upvotes.setText(String.valueOf(Integer.parseInt(upvotes.getText().toString()) - 1));
                        up.setImageResource(R.drawable.ic_mug_empty);
                        break;
                    } else if (!posts.get(getAdapterPosition()).liked){
                        posts.get(getAdapterPosition()).liked = true;
                        listener.likePost(getAdapterPosition(), 1);
                        upvotes.setText(String.valueOf(Integer.parseInt(upvotes.getText().toString()) + 1));
                        up.setImageResource(R.drawable.ic_mug_full);
                    } //else if(!posts.get(getAdapterPosition()).liked && posts.get(getAdapterPosition()).disliked){
//                        posts.get(getAdapterPosition()).liked = true;
//                        posts.get(getAdapterPosition()).disliked = false;
//                        listener.likePost(getAdapterPosition(), 2);
//                        upvotes.setText(String.valueOf(Integer.parseInt(upvotes.getText().toString()) + 2));
//                        up.setImageResource(R.drawable.ic_mug_full);
//                        //down.setBackgroundResource(R.drawable.rounded_outline_vote_buttons);
//                    }
                    break;
//                case R.id.blog_downvote:
//                    if(posts.get(getAdapterPosition()).disliked){
//                        posts.get(getAdapterPosition()).disliked = false;
//                        listener.unvotePost(getAdapterPosition(), +1);
//                        upvotes.setText(String.valueOf(Integer.parseInt(upvotes.getText().toString()) + 1));
//                        down.setBackgroundResource(R.drawable.rounded_outline_vote_buttons);
//                        break;
//                    } else if (!posts.get(getAdapterPosition()).disliked && !posts.get(getAdapterPosition()).liked){
//                        posts.get(getAdapterPosition()).disliked = true;
//                        listener.likePost(getAdapterPosition(), -1);
//                        upvotes.setText(String.valueOf(Integer.parseInt(upvotes.getText().toString()) - 1));
//                        down.setBackgroundResource(R.drawable.rounded_outline_vote_buttons_hasvoted);
//                    } else if(!posts.get(getAdapterPosition()).disliked && posts.get(getAdapterPosition()).liked){
//                        posts.get(getAdapterPosition()).liked = false;
//                        posts.get(getAdapterPosition()).disliked = true;
//                        listener.likePost(getAdapterPosition(), -2);
//                        upvotes.setText(String.valueOf(Integer.parseInt(upvotes.getText().toString()) - 2));
//                        down.setBackgroundResource(R.drawable.rounded_outline_vote_buttons_hasvoted);
//                        up.setBackgroundResource(R.drawable.rounded_outline_vote_buttons);
//                    }
//                    break;
                default:
            }

        }


    }

    public interface openBreweryFromBlog{
        public void openBrewery(int position);
        public void likePost(int position, int incBy);
        public void unvotePost(int position, int incBy);
        public void optionPost(int position, int menuChoice);
    }
}
