package org.louiswilliams.queueupplayer.fragment;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.activity.MainActivity;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.api.SpotifyClient;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyPlaylist;

import java.util.Collections;
import java.util.List;

public class SpotifyPlaylistListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private MainActivity mActivity;
    private SwipeRefreshLayout mView;
    private GridView playlistGrid;
    private List<SpotifyPlaylist> mPlaylists;
    private PlaylistGridAdapter mAdapter;
    private SpotifyClient spotifyClient;
    private String playlistId;


    @Override
    public void onAttach(Context activity) {
        mActivity = (MainActivity) activity;
        super.onAttach(activity);
    }

    @Override
    public void onAttach(Activity activity) {
        mActivity = (MainActivity) activity;
        super.onAttach(activity);
    }

    private void populate(final boolean refresh) {
        /* Get the user's following playlists and update the adapter on success */
        mActivity.spotifyLogin(new QueueUp.CallReceiver<String>() {

            @Override
            public void onResult(String accessToken) {
                spotifyClient = SpotifyClient.with(accessToken);

                spotifyClient.getMyPlaylists(new QueueUp.CallReceiver<List<SpotifyPlaylist>>() {
                    @Override
                    public void onResult(List<SpotifyPlaylist> result) {
                        populateDone(result, null, refresh);
                    }

                    @Override
                    public void onException(Exception e) {
                        populateDone(null, "Error: " + e.getMessage(), refresh);
                    }
                });
            }

            @Override
            public void onException(Exception e) {
                populateDone(null, "Spotify: " + e.getMessage(), refresh);
            }
        });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = (SwipeRefreshLayout) inflater.inflate(R.layout.fragment_playlist_list, container, false);
        mView.setOnRefreshListener(this);

        playlistId = getArguments().getString("playlist_id");

        playlistGrid = (GridView) mView.findViewById(R.id.playlist_grid);
        playlistGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SpotifyPlaylist playlist = mPlaylists.get(position);
                Log.d(QueueUp.LOG_TAG, "Using playlist ID: " + playlist.id);

                mActivity.showSpotifyPlaylistFragment(playlistId, playlist.owner.id, playlist.id);
            }
        });

        playlistGrid.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisible, int visibleItems, int totalItems) {
                int topPosition = (playlistGrid.getChildCount() == 0) ? 0 : playlistGrid.getChildAt(0).getTop();
                mView.setEnabled(firstVisible == 0 && topPosition >= 0);
            }
        });
        mActivity.setTitle("My Playlists");

        populate(false);

        View addButton = mView.findViewById(R.id.add_playlist_button);
        addButton.setVisibility(View.GONE);

        View playerBar = mView.findViewById(R.id.player_bar);
        playerBar.setVisibility(View.GONE);

        return mView;
    }

    @Override
    public void onRefresh() {
        populate(true);
    }

    public void  populateDone(final List<SpotifyPlaylist> playlists, final String message, final boolean refresh) {
        final ProgressBar progress = (ProgressBar) mView.findViewById(R.id.loading_progress_bar);
        final TextView notification = (TextView) mView.findViewById(R.id.playlist_notification);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
            if (message != null) {
                String help = "%s. Swipe down to try again";
                String m = String.format(help, message);
                notification.setText(m);
                notification.setVisibility(View.VISIBLE);
            } else {
                notification.setVisibility(View.GONE);
            }
            if (playlists != null) {
                mPlaylists = playlists;
            } else {
                mPlaylists = Collections.emptyList();
            }
            mAdapter = new PlaylistGridAdapter(mActivity, mPlaylists, R.layout.playlist_item);
            playlistGrid.setAdapter(mAdapter);
            progress.setVisibility(View.GONE);
            if (refresh) {
                mView.setRefreshing(false);
            }
            }
        });
    }

    class PlaylistGridAdapter extends BaseAdapter {

        private Context mContext;
        private List<SpotifyPlaylist> mPlaylists;
        private int mResource;

        public PlaylistGridAdapter(Context c, List<SpotifyPlaylist> playlists, int resource) {
            mContext = c;
            mPlaylists = playlists;
            mResource = resource;
        }

        @Override
        public int getCount() {
            return mPlaylists.size();
        }

        @Override
        public Object getItem(int position) {
            return mPlaylists.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View playlistItem;
            SpotifyPlaylist playlist = mPlaylists.get(position);

            if (convertView == null) {
                playlistItem = LayoutInflater.from(mContext).inflate(mResource, parent, false);
            } else {
                playlistItem = convertView;
            }

            TextView title = (TextView) playlistItem.findViewById(R.id.playlist_list_item_title);
            TextView adminName = (TextView) playlistItem.findViewById(R.id.playlist_list_item_admin);
            ImageView adminIcon = (ImageView) playlistItem.findViewById(R.id.playlist_list_item_admin_icon);

            title.setText(playlist.name);

            adminName.setText(playlist.totalTracks + " tracks");
            adminIcon.setImageResource(R.mipmap.ic_spotify);
            adminIcon.setBackgroundResource(R.drawable.background_transparent);

            if (playlist.imageUrls != null) {
                List<String> imageUrls = playlist.imageUrls;
                if (imageUrls.size() > 0) {
                    ImageView image = (ImageView) playlistItem.findViewById(R.id.playlist_list_item_image);
                    Picasso.with(mContext).load(imageUrls.get(0)).into(image);
                }
            } else {
                ImageView image = (ImageView) playlistItem.findViewById(R.id.playlist_list_item_image);
                image.setImageResource(R.drawable.background_opaque_gray);
            }

            return playlistItem;
        }

        public void updateList(List<SpotifyPlaylist> list) {
            mPlaylists = list;

            /* Calls are going to be from different asynchronous threads, so to be safe, run on main thread */
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }
}
