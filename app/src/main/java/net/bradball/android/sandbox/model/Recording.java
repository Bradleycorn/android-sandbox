package net.bradball.android.sandbox.model;

import android.database.Cursor;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collections;

import net.bradball.android.sandbox.provider.RecordingsContract;
/**
 * Created by bradb on 12/29/15.
 */
public class Recording {
    private static final String TAG = "Recording";
    private static final String DATE_DISPLAY_FORMAT = "MM-dd-yyyy";

    private long mID;

    private long mShowID;

    private String mIdentifier;

    private LocalDate mDate;

    private String mLocation;

    private String mTitle;

    private String mSetlist;

    private float mRating;

    private int mNumReviews;

    private int mDownloads;

    private String mPublisher;

    private boolean mSoundboard;

    private String mSource;

    private boolean mAvailableOffline;

    private ArrayList<Review> mReviews;
    private ArrayList<Track> mTracks;

    //region GETTERS-SETTERS
    public long getID() {
        return mID;
    }

    public void setID(long id) {
        mID = id;
    }

    public long getShowID() {
        return mShowID;
    }

    public void setShowID(long id) {
        mShowID = id;
    }


    public String getIdentifier() {
        return mIdentifier;
    }

    public LocalDate getDate() {
        return mDate;
    }

    public void setDate(LocalDate date) {
        mDate = date;
    }

    public int getDownloads() {
        return mDownloads;
    }

    public void setDownloads(int downloads) {
        mDownloads = downloads;
    }

    public boolean isSoundboard() {
        return mSoundboard;
    }

    public void setSoundboard(boolean isSoundboard) {
        mSoundboard = isSoundboard;
    }

    public String getLocation() {
        return mLocation;
    }

    public void setLocation(String location) {
        mLocation = location;
    }

    public String getPublisher() {
        return mPublisher;
    }

    public void setPublisher(String publisher) {
        mPublisher = publisher;
    }

    public float getRating() {
        return mRating;
    }

    public void setRating(float rating) {
        mRating = rating;
    }

    public int getNumReviews() {
        return mNumReviews;
    }

    public void setNumReviews(int numReviews) {
        mNumReviews = numReviews;
    }

    public String getSetlist() {
        return mSetlist;
    }

    public void setSetlist(String setlist) {
        mSetlist = setlist;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getSource() {
        return mSource;
    }

    public boolean isAvailableOffline() {
        return mAvailableOffline;
    }

    public void setAvailableOffline(boolean availableOffline) {
        mAvailableOffline = availableOffline;
    }

    public void setSource(String source) {
        mSource = source;
    }

    public ArrayList<Review> getReviews() {

        if (mReviews.size() > 1) {
            Collections.sort(mReviews);
        }
        return mReviews;
    }

    public ArrayList<Track> getTracks() {

        if (mTracks.size() > 1) {
            Collections.sort(mTracks);
        }

        return mTracks;
    }

    public void setTracks(ArrayList<Track> tracks) {
        mTracks = tracks;
    }

    //endregion

    public static Recording getFromCursor(Cursor c) {
        Recording recording = new Recording(c.getString(c.getColumnIndex(RecordingsContract.Recordings.IDENTIFIER)));

        recording.setID(c.getLong(c.getColumnIndex(RecordingsContract.Recordings._ID)));
        recording.setShowID(c.getLong(c.getColumnIndex(RecordingsContract.Recordings.SHOW_ID)));
        recording.setDate(RecordingsContract.parseRecordingDate(c.getString(c.getColumnIndex(RecordingsContract.Recordings.DATE))));
        recording.setLocation(c.getString(c.getColumnIndex(RecordingsContract.Recordings.LOCATION)));
        recording.setTitle(c.getString(c.getColumnIndex(RecordingsContract.Recordings.TITLE)));
        recording.setSetlist(c.getString(c.getColumnIndex(RecordingsContract.Recordings.SETLIST)));
        recording.setRating(c.getFloat(c.getColumnIndex(RecordingsContract.Recordings.RATING)));
        recording.setNumReviews(c.getInt(c.getColumnIndex(RecordingsContract.Recordings.NUM_REVIEWS)));
        recording.setDownloads(c.getInt(c.getColumnIndex(RecordingsContract.Recordings.DOWNLOADS)));
        recording.setPublisher(c.getString(c.getColumnIndex(RecordingsContract.Recordings.PUBLISHER)));
        recording.setSoundboard(c.getInt(c.getColumnIndex(RecordingsContract.Recordings.SOUNDBOARD)) == 1);
        recording.setSource(c.getString(c.getColumnIndex(RecordingsContract.Recordings.SOURCE)));
        recording.setAvailableOffline(c.getInt(c.getColumnIndex(RecordingsContract.Recordings.AVAILABLE_OFFLINE)) == 1);

        return recording;
    }

    public Recording() {
        this(null);
    }

    public Recording(String identifier) {

        mIdentifier = identifier;
        mReviews = new ArrayList<Review>();
        mTracks = new ArrayList<Track>();
    }

    public void addReview(Review review) {
        mReviews.add(review);
    }

    public void addTrack(Track track) {
        mTracks.add(track);
    }

    public Track findTrack(String filename) {
        for (Track track : mTracks) {
            if (filename.equals(track.getFilename())) {
                return track;
            }
        }

        return null;
    }

    public Track findTrackByMediaId(String mediaId) {
        for (Track track: mTracks) {
            if (mediaId.equals(track.getMediaMetadata().getDescription().getMediaId())) {
                return track;
            }
        }
        return null;
    }

    public String getDisplayDate() {
        DateTimeFormatter fmt = DateTimeFormat.forPattern(DATE_DISPLAY_FORMAT);
        return fmt.print(getDate());
    }

    public int getNumberOfTracks() {
        return mTracks.size();
    }

}