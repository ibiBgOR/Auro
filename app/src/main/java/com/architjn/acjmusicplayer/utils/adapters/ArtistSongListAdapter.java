package com.architjn.acjmusicplayer.utils.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.architjn.acjmusicplayer.R;
import com.architjn.acjmusicplayer.service.PlayerService;
import com.architjn.acjmusicplayer.task.FetchAlbum;
import com.architjn.acjmusicplayer.utils.ImageConverter;
import com.architjn.acjmusicplayer.utils.ListSongs;
import com.architjn.acjmusicplayer.utils.Utils;
import com.architjn.acjmusicplayer.utils.handlers.AlbumImgHandler;
import com.architjn.acjmusicplayer.utils.items.Song;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by architjn on 28/11/15.
 */
public class ArtistSongListAdapter extends RecyclerView.Adapter<ArtistSongListAdapter.SimpleItemViewHolder> {

    private static final int ITEM_VIEW_TYPE_HEADER = 0;
    private static final int ITEM_VIEW_TYPE_ITEM = 1;

    private View header;
    private ArrayList<Song> items;
    private Context context;

    public ArtistSongListAdapter(Context context, View header, ArrayList<Song> items) {
        this.context = context;
        this.header = header;
        this.items = items;
    }

    public boolean isHeader(int position) {
        return position == 0;
    }

    @Override
    public ArtistSongListAdapter.SimpleItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_VIEW_TYPE_HEADER)
            return new SimpleItemViewHolder(header);
        View itemView = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.songs_list_item, parent, false);
        return new SimpleItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ArtistSongListAdapter.SimpleItemViewHolder holder, final int position) {
        if (isHeader(position))
            return;
        holder.name.setText(items.get(position - 1).getName());
        holder.artistName.setText(items.get(position - 1).getArtist());
        setAlbumArt(position - 1, holder);
        setOnClicks(holder, position - 1);
    }

    private void setAlbumArt(int position, final SimpleItemViewHolder holder) {
        AlbumImgHandler imgHandler = new AlbumImgHandler(context) {
            @Override
            public void onDownloadComplete(final String url) {
                if (url != null)
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Picasso.with(context).load(new File(url)).into(holder.img);
                            setImageToView(url, holder);
                        }
                    });
            }
        };
        String path = imgHandler.getAlbumImgFromDB(items.get(position).getAlbumName(), items.get(position).getArtist(), FetchAlbum.Quality.MEDIUM);
        if (path != null && !path.matches("")) {
            setImageToView(path, holder);
        } else {
            String urlIfAny = imgHandler.getAlbumArtWork(items.get(position).getAlbumName(), items.get(position).getArtist(), position, FetchAlbum.Quality.MEDIUM);
            setImageToView(urlIfAny, holder);
        }
    }

    public void setImageToView(String url, final SimpleItemViewHolder holder) {
        Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                Bitmap circularBitmap = ImageConverter.getRoundedCornerBitmap(bitmap, 100);
                holder.img.setImageBitmap(circularBitmap);
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
            }
        };

        if (url != null) {
            Picasso.with(context).load(new File(url)).into(target);
        } else {
            Picasso.with(context).load(R.drawable.default_art).resize(holder.img.getMaxWidth(), holder.img.getMaxHeight()).into(target);
        }
    }

    private void setOnClicks(final SimpleItemViewHolder holder, final int position) {
        holder.mainView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    public void run() {
                        Intent i = new Intent();
                        i.setAction(PlayerService.ACTION_PLAY_ARTIST);
                        i.putExtra("name", items.get(position).getArtist());
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

    @Override
    public int getItemCount() {
        return items.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (isHeader(position))
            return ITEM_VIEW_TYPE_HEADER;
        else
            return ITEM_VIEW_TYPE_ITEM;
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
