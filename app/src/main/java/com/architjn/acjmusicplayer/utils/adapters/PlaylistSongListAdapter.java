package com.architjn.acjmusicplayer.utils.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.architjn.acjmusicplayer.R;
import com.architjn.acjmusicplayer.service.PlayerService;
import com.architjn.acjmusicplayer.task.FetchAlbum;
import com.architjn.acjmusicplayer.utils.ListSongs;
import com.architjn.acjmusicplayer.utils.Utils;
import com.architjn.acjmusicplayer.utils.items.Song;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by architjn on 28/11/15.
 */
public class PlaylistSongListAdapter extends RecyclerView.Adapter<PlaylistSongListAdapter.SimpleItemViewHolder> {

    private ArrayList<Song> items;
    private int playlistId;
    private Context context;

    public PlaylistSongListAdapter(Context context, ArrayList<Song> items, int playlistId) {
        this.context = context;
        this.items = items;
        this.playlistId = playlistId;
    }

    @Override
    public PlaylistSongListAdapter.SimpleItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.songs_list_item, parent, false);
        return new SimpleItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final PlaylistSongListAdapter.SimpleItemViewHolder holder, final int position) {
        holder.name.setText(items.get(position).getName());
        holder.artistName.setText(items.get(position).getArtist());
        setAlbumArt(position, holder);
        setOnClicks(holder, position);
    }

    private void setAlbumArt(int position, SimpleItemViewHolder holder) {
        String path = ListSongs.getAlbumArt(context, items.get(position).getAlbumName(), items.get(position).getArtist(), FetchAlbum.Quality.LOW);
        if (path != null) {
            Picasso.with(context).load(new File(path)).resize(dpToPx(50),
                    dpToPx(50)).centerCrop().into(holder.img);
        } else {
            setDefaultView(holder);
        }
    }

    private void setDefaultView(SimpleItemViewHolder holder) {
        Utils utils = new Utils(context);
        int size = (utils.getWindowWidth() - dpToPx(1)) / 2;
        holder.img.setImageBitmap(utils.getBitmapOfVector(R.drawable.default_art,
                size, size));
    }

    private void setOnClicks(final SimpleItemViewHolder holder, final int position) {
        holder.mainView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    public void run() {
                        Intent i = new Intent();
                        i.setAction(PlayerService.ACTION_PLAY_PLAYLIST);
                        i.putExtra("id", playlistId);
                        i.putExtra("pos", position);
                        context.sendBroadcast(i);
                    }
                }).start();
            }
        });
        holder.menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu pm = new PopupMenu(context, view);
                final Intent intent = new Intent();
                pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.popup_song_play:
                                intent.setAction(PlayerService.ACTION_PLAY_SINGLE);
                                intent.putExtra("songId", items.get(position).getSongId());
                                context.sendBroadcast(intent);
                                break;
                            case R.id.popup_song_addtoplaylist:
                                new Utils(context).addToPlaylist(((Activity) context),
                                        items.get(position).getSongId());
                                break;
                        }
                        return false;
                    }
                });
                pm.inflate(R.menu.popup_song);
                pm.show();
            }
        });
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class SimpleItemViewHolder extends RecyclerView.ViewHolder {

        public TextView name, artistName;
        public View mainView, menu;
        public ImageView img;

        public SimpleItemViewHolder(View itemView) {
            super(itemView);
            mainView = itemView;
            img = (ImageView) itemView.findViewById(R.id.song_item_img);
            name = (TextView) itemView.findViewById(R.id.song_item_name);
            menu = itemView.findViewById(R.id.song_item_menu);
            artistName = (TextView) itemView.findViewById(R.id.song_item_artist);
        }
    }

}
