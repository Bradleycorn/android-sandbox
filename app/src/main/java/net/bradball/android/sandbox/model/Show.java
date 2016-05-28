package net.bradball.android.sandbox.model;

import android.database.Cursor;
import android.util.Log;

import net.bradball.android.sandbox.provider.RecordingsContract;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;

public class Show {

    private static final String TAG = "Show";
    private static final String DATE_DISPLAY_FORMAT = "MM-dd-yyyy";

    private long mID;
    private LocalDate mDate;
    private String mLocation;
    private String mTitle;
    private String mSetlist;
    private boolean mSoundboard;
    private int mDownloads;
    private int mRecordingsCount;
    private ArrayList<Recording> mRecordings;

    public static Show getFromCursor(Cursor c) {
        Show show = new Show();
        show.setID(c.getLong(c.getColumnIndex(RecordingsContract.Shows._ID)));
        show.setLocation(c.getString(c.getColumnIndex(RecordingsContract.Shows.LOCATION)));
        show.setDate(RecordingsContract.parseRecordingDate(c.getString(c.getColumnIndex(RecordingsContract.Shows.DATE))));
        show.setTitle(c.getString(c.getColumnIndex(RecordingsContract.Shows.TITLE)));
        show.setSetlist(c.getString(c.getColumnIndex(RecordingsContract.Shows.SETLIST)));
        show.setSoundboard(c.getInt(c.getColumnIndex(RecordingsContract.Shows.SOUNDBOARD)) == 1);
        show.setDownloads(c.getInt(c.getColumnIndex(RecordingsContract.Shows.DOWNLOADS)));

        if (c.getColumnIndex(RecordingsContract.Shows._COUNT) >= 0) {
            show.setRecordingsCount(c.getInt(c.getColumnIndex(RecordingsContract.Shows._COUNT)));
        }

        return show;
    }

    //region GETTERS-SETTERS
    public long getID() {
        return mID;
    }

    public void setID(long id) {
        mID = id;
    }

    public LocalDate getDate() {
        return mDate;
    }

    public void setDate(LocalDate date) {
        mDate = date;
    }

    public String getLocation() {
        return mLocation;
    }

    public void setLocation(String location) {
        mLocation = location;
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


    public int getDownloads() {
        if (mRecordings.size() > 0) {
            return getTotalDownloads();
        } else {
            return mDownloads;
        }

    }
    public void setDownloads(int downloads) {
        mDownloads = downloads;
    }

    public boolean isSoundboard() {
        if (mRecordings.size() > 0) {
            return hasSoundboard();
        } else {
            return mSoundboard;
        }
    }

    public void setSoundboard(boolean soundboard) {
        mSoundboard = soundboard;
    }

    public int getRecordingsCount() {
        if (mRecordings.size() > 0 || mRecordingsCount == 0) {
            return mRecordings.size();
        } else {
            return mRecordingsCount;
        }
    }

    public void setRecordingsCount(int value) {
        if (mRecordings.size() == 0) {
            mRecordingsCount = value;
        }
    }

    //endregion

    public Show() {
        initMembers();
    }

    public Show(LocalDate date) {
        mDate = date;
        initMembers();
    }

    private void initMembers() {
        mRecordings = new ArrayList<Recording>();
        mSoundboard = false;
    }

    public void addRecording(Recording recording) {
        mRecordings.add(recording);
    }

    public ArrayList<Recording> getRecordings() {
        return mRecordings;
    }

    private int getTotalDownloads() {
        int downloads = 0;
        for (Recording recording: mRecordings) {
            downloads += recording.getDownloads();
        }
        return downloads;
    }

    private boolean hasSoundboard() {
        for (Recording recording: mRecordings) {
            if (recording.isSoundboard()) {
                return true;
            }
        }
        return false;
    }

    public String getDisplayDate() {
        DateTimeFormatter fmt = DateTimeFormat.forPattern(DATE_DISPLAY_FORMAT);
        return fmt.print(getDate());
    }

    public String getYear() {
        return Integer.toString(mDate.getYear());
    }
}