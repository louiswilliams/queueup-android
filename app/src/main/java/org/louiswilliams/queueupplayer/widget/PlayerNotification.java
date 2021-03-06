package org.louiswilliams.queueupplayer.widget;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import android.widget.RemoteViews;

import com.squareup.picasso.Picasso;

import org.louiswilliams.queueupplayer.R;
import org.louiswilliams.queueupplayer.activity.MainActivity;
import org.louiswilliams.queueupplayer.queueup.PlaybackController;
import org.louiswilliams.queueupplayer.queueup.PlaylistListener;
import org.louiswilliams.queueupplayer.queueup.QueueUp;
import org.louiswilliams.queueupplayer.queueup.objects.QueueUpTrack;
import org.louiswilliams.queueupplayer.queueup.objects.SpotifyTrack;

import java.util.List;

public class PlayerNotification extends Notification implements PlaylistListener {

    public static final int NOTIFICATION_ID = 1;
    private static final String PLAY_BUTTON_INTENT = "QUEUEUP_PLAY_BUTTON";
    private static final String SKIP_BUTTON_INTENT = "QUEUEUP_SKIP_BUTTON";
    private static final String STOP_BUTTON_INTENT = "QUEUEUP_STOP_BUTTON";

    public String CREATOR = "org.louiswilliams.queueup";

    private Context mContext;
    private NotificationManager mNotificationManager;
    private Builder mBuilder;
    private RemoteViews mContentView;

    private BroadcastReceiver playReceiver;
    private BroadcastReceiver skipReceiver;
    private BroadcastReceiver stopReceiver;

    public PlayerNotification(Context context) {
        super();

        Log.d(QueueUp.LOG_TAG, "Creating notification for the first time...");

        mContext = context;
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        playReceiver = new PlayButtonHandler();
        skipReceiver = new SkipButtonHandler();
        stopReceiver = new StopButtonHandler();

        mContext.registerReceiver(playReceiver, new IntentFilter(PLAY_BUTTON_INTENT));
        mContext.registerReceiver(skipReceiver, new IntentFilter(SKIP_BUTTON_INTENT));
        mContext.registerReceiver(stopReceiver, new IntentFilter(STOP_BUTTON_INTENT));


        mBuilder = new Notification.Builder(mContext);
        mBuilder.setOngoing(true);

        mContentView = new RemoteViews(mContext.getPackageName(), R.layout.notification_player);
        mBuilder.setContent(mContentView);

        Intent playButtonIntent = new Intent(PLAY_BUTTON_INTENT);
        Intent skipButtonIntent = new Intent(SKIP_BUTTON_INTENT);
        Intent stopButtonIntent = new Intent(STOP_BUTTON_INTENT);


        mContentView.setOnClickPendingIntent(R.id.play_button, PendingIntent.getBroadcast(mContext, 0, playButtonIntent, 0));
        mContentView.setOnClickPendingIntent(R.id.skip_button, PendingIntent.getBroadcast(mContext, 0, skipButtonIntent, 0));
        mContentView.setOnClickPendingIntent(R.id.stop_playback_button, PendingIntent.getBroadcast(mContext, 0, stopButtonIntent, 0));

        PendingIntent openAppIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext, MainActivity.class), 0);
        mBuilder.setContentIntent(openAppIntent);

        mBuilder.setSmallIcon(R.mipmap.ic_queueup);

    }

    public void cancel() {
        try {
            mContext.unregisterReceiver(playReceiver);
            mContext.unregisterReceiver(skipReceiver);
            mContext.unregisterReceiver(stopReceiver);
        } catch (IllegalArgumentException e) {
            Log.w(QueueUp.LOG_TAG, e.getMessage());
        }

        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    @Override
    public void onPlayingChanged(boolean playing) {

        if (playing) {
            mContentView.setImageViewResource(R.id.play_button, R.drawable.ic_action_pause_36);
            mBuilder.setSmallIcon(R.drawable.ic_action_play_circle_fill);
        } else {
            mContentView.setImageViewResource(R.id.play_button, R.drawable.ic_action_play_arrow_36);
            mBuilder.setSmallIcon(R.drawable.ic_action_pause_circle_fill);
        }

        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    @Override
    public void onTrackChanged(final SpotifyTrack current) {
        final Notification notification = mBuilder.build();

        if (current != null) {
            mContentView.setTextViewText(R.id.playlist_current_track, current.name);
            mContentView.setTextViewText(R.id.playlist_current_artist, current.artists.get(0).name);
            if (current.album.imageUrls != null && current.album.imageUrls.size() > 0) {

                Handler handler = new Handler(mContext.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                    Picasso.with(mContext).load(current.album.imageUrls.get(0)).into(mContentView, R.id.playlist_image, NOTIFICATION_ID, notification);
                    }
                });
            }
        }


        mNotificationManager.notify(NOTIFICATION_ID, notification);

    }

    @Override
    public void onTrackProgress(int progress, int duration) {
        // NOOP
    }

    @Override
    public void onQueueChanged(List<QueueUpTrack> tracks) {   }

    @Override
    public void onPlayerReady(boolean ready) {
        if (!ready) {
            cancel();
        }
    }

    @Override
    public String getPlaylistId() {
        return null;
    }

    public class PlayButtonHandler extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(QueueUp.LOG_TAG, "Play button pressed");
            if (context instanceof PlaybackController) {
                PlaybackController controller = ((PlaybackController) context);

                boolean playing = controller.getCurrentState().playing;
                controller.updateTrackPlaying(!playing);
            } else {
                Log.e(QueueUp.LOG_TAG, "Received context isn't an instance of Main activity...");
            }
        }
    }

    public class SkipButtonHandler extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(QueueUp.LOG_TAG, "Skip button pressed");
            if (context instanceof PlaybackController) {

                PlaybackController controller = ((PlaybackController) context);

                controller.updateTrackDone();
            } else {
                Log.e(QueueUp.LOG_TAG, "Received context isn't an instance of Main activity...");
            }
        }
    }

    public class StopButtonHandler extends  BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            cancel();
            Log.d(QueueUp.LOG_TAG, "Stop pressed");
            if (context instanceof  PlaybackController) {
                ((PlaybackController) context).stopPlayback();
            }
        }
    }
}
