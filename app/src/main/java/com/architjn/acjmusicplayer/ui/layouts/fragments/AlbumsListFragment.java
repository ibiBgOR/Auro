package com.architjn.acjmusicplayer.ui.layouts.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.async.Async;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.architjn.acjmusicplayer.R;
import com.architjn.acjmusicplayer.task.AlbumItemLoad;
import com.architjn.acjmusicplayer.ui.layouts.activity.AlbumActivity;
import com.architjn.acjmusicplayer.ui.layouts.activity.MainActivity;
import com.architjn.acjmusicplayer.utils.ListSongs;
import com.architjn.acjmusicplayer.utils.PermissionChecker;
import com.architjn.acjmusicplayer.utils.Utils;
import com.architjn.acjmusicplayer.utils.adapters.AlbumListAdapter;
import com.architjn.acjmusicplayer.utils.decorations.AlbumListSpacesItemDecoration;
import com.architjn.acjmusicplayer.utils.items.Album;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by architjn on 27/11/15.
 */
public class AlbumsListFragment extends Fragment {

    private static final String TAG = "AlbumsListFragment-TAG";
    private Context context;
    private View mainView;
    private RecyclerView gv;
    private AlbumListAdapter adapter;
    private PermissionChecker permissionChecker;
    private View emptyView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_albums,
                container, false);
        mainView = view;
        System.gc();
        context = mainView.getContext();
        initViews();
        return view;
    }

    private void initViews() {
        gv = (RecyclerView) mainView.findViewById(R.id.albumsListContainer);
        emptyView = mainView.findViewById(R.id.album_empty_view);
        AlbumListAdapter.onceAnimated = false;
        checkForPermissionsNeeded(false);
    }

    private void checkForPermissionsNeeded(boolean askDirectly) {
        permissionChecker = new PermissionChecker(context, getActivity(), mainView);
        permissionChecker.check(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                getResources().getString(R.string.storage_permission),
                new PermissionChecker.OnPermissionResponse() {
                    @Override
                    public void onAccepted() {
                        setList();
                    }

                    @Override
                    public void onDecline() {
                        cantProceedDialog();
                    }
                }, askDirectly);
    }

    private void cantProceedDialog() {
        new MaterialDialog.Builder(context)
                .title(R.string.cant_proceed)
                .content(R.string.cant_proceed_msg)
                .positiveText(R.string.ok)
                .negativeText(R.string.close)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        checkForPermissionsNeeded(true);
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        getActivity().finish();
                    }
                })
                .show();
    }

    private void setList() {
        new Thread(new Runnable() {
            public void run() {
                final ArrayList<Album> albumList = ListSongs.getAlbumList(context);
                final GridLayoutManager gridLayoutManager =
                        new GridLayoutManager(mainView.getContext(), 2);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gridLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                        gridLayoutManager.scrollToPosition(0);
                        gv.setLayoutManager(gridLayoutManager);
                        gv.addItemDecoration(new AlbumListSpacesItemDecoration(new
                                Utils(context).dpToPx(1)));
//                        Album lastAddedAlbum = ListSongs.getLastAddedAlbum(context);
                        adapter = new AlbumListAdapter(mainView.getContext(), albumList, gv);
                        gv.setAdapter(adapter);
                    }
                });
//                gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
//                    @Override
//                    public int getSpanSize(int position) {
//                        return adapter.isHeader(position) ? gridLayoutManager.getSpanCount() : 1;
//                    }
//                });
                if (albumList.size() < 1) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listIsEmpty();
                        }
                    });
                }
            }
        }).start();
    }

    private void setHeaderView(final Album lastAddedAlbum, View header) {
        setArt(header, lastAddedAlbum);
        ((TextView) header.findViewById(R.id.album_list_long_name))
                .setText(lastAddedAlbum.getAlbumTitle());
        ((TextView) header.findViewById(R.id.album_list_long_artist))
                .setText(lastAddedAlbum.getAlbumArtist());
        header.findViewById(R.id.album_list_long_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(context, AlbumActivity.class);
                i.putExtra("albumName", lastAddedAlbum.getAlbumTitle());
                i.putExtra("albumId", lastAddedAlbum.getAlbumId());
                context.startActivity(i);
            }
        });
    }

    public void listIsEmpty() {
        emptyView.setVisibility(View.VISIBLE);
        gv.setVisibility(View.GONE);
    }

    public void listNoMoreEmpty() {
        gv.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    private void setArt(View header, final Album lastAddedAlbum) {
        Utils utils = new Utils(context);
        int size = (utils.getWindowWidth()
                - utils.dpToPx(1)) / 2;
        if (lastAddedAlbum.getAlbumArtPath() != null) {
            if (isFilePathExist(lastAddedAlbum.getAlbumArtPath())) {
                new AlbumItemLoad(context, lastAddedAlbum.getAlbumArtPath(), header).execute();
                setAlbumArt(lastAddedAlbum, header, size, utils);
            } else setDefault(header, size, utils);
        } else {
            setDefault(header, size, utils);
        }
    }

    private void setDefault(View header, int size, Utils utils) {
        int colorPrimary = ContextCompat
                .getColor(context, R.color.colorPrimary);
        ((ImageView) header.findViewById(R.id.album_grid_header_img)).setImageBitmap(utils
                .getBitmapOfVector(R.drawable.default_art, size, size));
        header.findViewById(R.id.album_grid_header_bg).setBackgroundColor(colorPrimary);
    }

    private boolean isFilePathExist(String albumArtPath) {
        File imgFile = new File(albumArtPath);
        return imgFile.exists();
    }

    private void setAlbumArt(Album lastAddedAlbum, View header, int size, Utils utils) {
        String art = lastAddedAlbum.getAlbumArtPath();
        if (art != null)
            Picasso.with(context).load(new File(art)).resize(size, size)
                    .centerCrop().into((ImageView)
                    header.findViewById(R.id.album_grid_header_img));
        else
            ((ImageView) header.findViewById(R.id.album_grid_header_img)).setImageBitmap(utils
                    .getBitmapOfVector(R.drawable.default_art, size, size));
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Async.cancelAll();
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        permissionChecker.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void onBackPress() {
        ((MainActivity) getActivity()).killActivity();
    }

}
