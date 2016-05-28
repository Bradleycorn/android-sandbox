package net.bradball.android.sandbox.util;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;

import net.bradball.android.sandbox.R;
import net.bradball.android.sandbox.model.Recording;
import net.bradball.android.sandbox.model.Show;
import net.bradball.android.sandbox.model.Track;
import net.bradball.android.sandbox.provider.RecordingUriMatcher;
import net.bradball.android.sandbox.provider.RecordingUrisEnum;
import net.bradball.android.sandbox.provider.RecordingsContract;

import java.util.List;

/**
 * A static set of methods to help deal with the various Media ID's, MediaMetadata,
 * MediaItems, and MediaDescriptions of the various shows, recordings, and tracks
 * that the Music Service uses to play songs.
 *
 * The different objects can be confusing and overlapping. So let's try to explain:
 *
 * MediaID
 * -------
 * A MediaID is a unique identifier for a particular media item (year, show, recording, track).
 * In our case, it will always be a URI (though not always a Uri object, it could be a string).
 * If it's a Year, Show, or Recording, it will be a URI that can be handled by our
 * RecordingProvider's content resolver. If it's a Track, it'll be the URL to play the track.
 *
 * MediaMetadata
 * -------------
 * The MediaMetadata object contains detailed information about a song/track to be played.
 * It contains basic information (title, album, artist, etc) as well as more detail about
 * the track (disc number, track number, duration, rating, album art, genre, etc).
 * A MediaMetadata object is useful for Tracks, but not so much for recordings or shows.
 *
 * MediaDescription
 * ----------------
 * The MediaDescription object contains basic information (title, subtitle, description)
 * about any media item (show, recording, track, etc). This is used when displaying
 * the item (usually in a list). The MediaDescription is a more basic version of a
 * MediaMetadata object, and in fact the MediaMetadata object has a "getDescription" method
 * that will return a MediaDescription.
 *
 * MediaItem
 * ---------
 * The MediaItem object is basically just a wrapper for a MediaDescription, and is used by the
 * MediaBrowser[Service]. The MediaBrowser returns the MediaDescription to clients so they
 * can display information about the app's media. But since it uses a parent/child heirarchy,
 * the MediaBrowser needs one more piece of information that is not in a MediaDescription,
 * that is, is the media represented by the MediaDescription browsable (a show, or recording)
 * or playable (a track). So, a MediaItem only has 2 properties: a MediaDescription and
 * a flag to indicate browsable (has children) or playable (does not have children).
 */
public class MediaHelper {
    public static final String EXTRA_RECORDING_COUNT = "recordings";
    public static final String EXTRA_SOUNDBOARD = "is_soundboard";
    public static final String EXTRA_RATING = "rating";
    public static final String EXTRA_DOWNLOADS = "downloads";
    public static final String EXTRA_SOURCE = "source";
    public static final String EXTRA_IDENTIFIER = "identifier";
    public static final String EXTRA_REVIEWS = "reviews";
    public static final String EXTRA_TRACK_METADATA = "track_metadata";


    public static final String ROOT_ID = RecordingsContract.Shows.CONTENT_URI.toString();


    /**
     *
     * @param mediaId - The mediaID should be a proper URI that can be used to get the media item
     *                  from permanent storage. That is, either a Uri for our RecordingsProvider,
     *                  or (in the case of a track to be played), the URL for the track.
     * @param title - The title of the item represented by the mediaID uri.
     * @param subtitle - A subtitle (album, show date, venue, etc)
     * @param description - A longer description of the item (setlist, etc)
     * @param extras - A bundle with any other info that needs to be stored with the item
     * @return MediaDescription
     *
     * This method returns a MediaDescription object for a given media item (show, recording, track, etc).
     * A MediaDescription is simply a set of fields containing metadata about the media item (it's uri, title,
     * etc). A MediaDescription isn't all that useful on it's own, you really need a MediaItem.
     *
     * The MediaDescription object that is returned can be passed to a MediaItem object, which is
     * What the MediaBrowser service passes around to clients.
     */
    private static MediaDescriptionCompat createMediaDescription(@Nullable Uri mediaId, @Nullable String title, @Nullable String subtitle, @Nullable String description, @Nullable Bundle extras) {
        MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder();

        if (mediaId != null)
            builder.setMediaId(mediaId.toString());
        builder.setTitle(title);
        builder.setSubtitle(subtitle);
        builder.setDescription(description);
        builder.setExtras(extras);

        return builder.build();
    }

    /**
     *
     * @param recording - A recording object
     * @return Bundle - A bundle with information about the recording.
     *
     * A MediaDescription object only contains basic information about a media item (title, subtitle, etc)
     * We need to store some additional information about recordings (is it a soundboard, how many
     * times has it been downloaded, etc). This method creates a Bundle with the extra information
     * we store about recordings. The returned bundle can be added to a MediaDescription object
     * and stored in it's "extras" property.
     */
    @NonNull
    private static Bundle getRecordingExtras(Recording recording) {
        Bundle extras = new Bundle();
        extras.putBoolean(EXTRA_SOUNDBOARD, recording.isSoundboard());
        extras.putInt(EXTRA_DOWNLOADS, recording.getDownloads());
        extras.putString(EXTRA_IDENTIFIER, recording.getIdentifier());
        extras.putFloat(EXTRA_RATING, recording.getRating());
        extras.putInt(EXTRA_REVIEWS, recording.getNumReviews());
        extras.putString(EXTRA_SOURCE, recording.getSource());
        return extras;
    }

    public static boolean isBrowsable(@NonNull String mediaID) {
        return mediaID.startsWith(RecordingsContract.BASE_CONTENT_URI.toString());
    }

    public static MediaBrowserCompat.MediaItem createMediaItem(String year, String subtitle) {
        Uri mediaUri = RecordingsContract.Shows.buildShowsByDateUri(year);

        MediaDescriptionCompat description = createMediaDescription(mediaUri, year, subtitle, null, null);
       return new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    public static MediaBrowserCompat.MediaItem createMediaItem(Show show) {

        Uri mediaUri = RecordingsContract.Shows.buildShowUri(show.getID());

        Bundle extras = new Bundle();
        extras.putInt(EXTRA_RECORDING_COUNT, show.getRecordingsCount());
        extras.putBoolean(EXTRA_SOUNDBOARD, show.isSoundboard());

        String title = show.getDisplayDate() + " " + show.getTitle();
        MediaDescriptionCompat description = createMediaDescription(mediaUri, title, show.getLocation(), show.getSetlist(), extras);

        return new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    public static MediaBrowserCompat.MediaItem createMediaItem(Recording recording) {

        Uri mediaUri = RecordingsContract.Recordings.buildRecordingUri(recording.getIdentifier());

        Bundle extras = getRecordingExtras(recording);

        String title = recording.getDisplayDate() + " " + recording.getTitle();
        MediaDescriptionCompat description = createMediaDescription(mediaUri, title, recording.getLocation(), recording.getSetlist(), extras);

        return new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }


    public static MediaBrowserCompat.MediaItem createMediaItem(Track track, Recording recording) {
        MediaDescriptionCompat trackDescription = track
                .getMediaMetadata(recording.getNumberOfTracks())
                .getDescription();

        return new MediaBrowserCompat.MediaItem(trackDescription, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
    }

    public static String extractRecordingIdentifier(Uri trackUri) {
        List<String> segments = trackUri.getPathSegments();
        if (segments.size() == 3) {
            return segments.get(1);
        }

        return null;
    }

    public static RecordingUrisEnum getMediaIdType(String mediaId) {
        Uri mediaUri = Uri.parse(mediaId);

        RecordingUriMatcher matcher = new RecordingUriMatcher();
        return matcher.matchUri(mediaUri);
    }

}
