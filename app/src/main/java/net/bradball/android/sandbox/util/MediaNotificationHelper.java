package net.bradball.android.sandbox.util;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.view.KeyEvent;

import net.bradball.android.sandbox.R;

/**
 * Helper APIs for constructing MediaStyle notifications
 */
public class MediaNotificationHelper  {

    public static Notification createNotification(Context context, MediaSessionCompat mediaSession) {

        MediaMetadataCompat metadata = mediaSession.getController().getMetadata();
        MediaDescriptionCompat description = metadata.getDescription();
        PendingIntent sessionActivity = mediaSession.getController().getSessionActivity();
        PlaybackStateCompat playbackState = mediaSession.getController().getPlaybackState();
        NotificationCompat.MediaStyle mediaStyle = new NotificationCompat.MediaStyle();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        builder.addAction(R.drawable.ic_skip_previous_24dp, context.getString(R.string.label_previous), getActionIntent(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS));

        if (playbackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            builder.addAction(R.drawable.ic_pause_24dp, context.getString(R.string.label_pause), getActionIntent(context, KeyEvent.KEYCODE_MEDIA_PAUSE));
        } else {
            builder.addAction(R.drawable.ic_play_24dp, context.getString(R.string.label_play), getActionIntent(context, KeyEvent.KEYCODE_MEDIA_PLAY));
        }

        builder.addAction(R.drawable.ic_skip_next_24dp, context.getString(R.string.label_next), getActionIntent(context, KeyEvent.KEYCODE_MEDIA_NEXT));


        mediaStyle.setMediaSession(mediaSession.getSessionToken());
        mediaStyle.setCancelButtonIntent(getActionIntent(context, KeyEvent.KEYCODE_MEDIA_STOP));
        mediaStyle.setShowCancelButton(true);
        mediaStyle.setShowActionsInCompactView(1, 2);


        builder
                .setStyle(mediaStyle)
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setSubText(description.getDescription())
                .setLargeIcon(description.getIconBitmap())
                .setContentIntent(sessionActivity)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing((playbackState.getState() == PlaybackStateCompat.STATE_PLAYING));


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setColor(context.getResources().getColor(android.R.color.white, null));
        } else {
            builder.setColor(context.getResources().getColor(android.R.color.white));
        }

        return builder.build();
    }

    /**
     * Create a {@link PendingIntent} appropriate for a MediaStyle notification's action. Assumes
     * you are using a media button receiver.
     * @param context Context used to contruct the pending intent.
     * @param mediaKeyEvent KeyEvent code to send to your media button receiver.
     * @return An appropriate pending intent for sending a media button to your media button
     *      receiver.
     */
   public static PendingIntent getActionIntent(Context context, int mediaKeyEvent) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.setPackage(context.getPackageName());
        intent.putExtra(Intent.EXTRA_KEY_EVENT,
                new KeyEvent(KeyEvent.ACTION_DOWN, mediaKeyEvent));
        return PendingIntent.getBroadcast(context, mediaKeyEvent, intent, 0);
    }

}