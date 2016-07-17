package net.bradball.android.sandbox.model;

import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;

import net.bradball.android.sandbox.R;
import net.bradball.android.sandbox.provider.RecordingsContract;

public class Track implements Comparable<Track> {
    /*
        http://archive.org/download/gd1995-07-08.sbd.miller.114363.flac16/gd95-07-08d1t01.mp3



    "/gd1995-07-09d1t01.mp3": {
        "source": "derivative",
        "creator": "Grateful Dead",
        "title": "Touch Of Grey",
        "track": "1",
        "album": "1995-07-09 - Soldier Field",
        "bitrate": "197",
        "length": "07:19",
        "format": "VBR MP3",
        "original": "gd1995-07-09d1t01.shn",
        "mtime": "1417376963",
        "size": "10816512",
        "md5": "ac231e12151c93b3429e041e50db2fd7",
        "crc32": "e875ec2c",
        "sha1": "8044ae11643d3b11a4ef9de54dc1f8d38e18c83a",
        "height": "0",
        "width": "0"
    }

     */

    private long mID;
    private long mRecordingID;
    private String mRecordingIdentifier;
    private String mFilename;
    private String mTitle;
    private int mNumber;
    private String mAlbum;
    private String mBitRate;
    private String mLength;
    private String mFormat;
    private long mSize;
    private String mMd5;


    //region GETTERS-SETTERS
    public String getAlbum() {
        return mAlbum;
    }

    public void setAlbum(String album) {
        mAlbum = album;
    }

    public String getBitRate() {
        return mBitRate;
    }

    public void setBitRate(String bitRate) {
        mBitRate = bitRate;
    }

    public String getFilename() {
        return mFilename;
    }

    public void setFilename(String filename) {
        mFilename = filename;
    }

    public String getFormat() {
        return mFormat;
    }

    public void setFormat(String format) {
        mFormat = format;
    }

    public long getID() {
        return mID;
    }

    public void setID(long ID) {
        mID = ID;
    }

    public String getLength() {
        return mLength;
    }

    public void setLength(String length) {
        mLength = length;
    }

    public String getMd5() {
        return mMd5;
    }

    public void setMd5(String md5) {
        mMd5 = md5;
    }

    public int getNumber() {
        return mNumber;
    }

    public void setNumber(int number) {
        mNumber = number;
    }

    public long getRecordingID() {
        return mRecordingID;
    }

    public void setRecordingID(long recordingID) {
        mRecordingID = recordingID;
    }

    public long getSize() {
        return mSize;
    }

    public void setSize(long size) {
        mSize = size;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getRecordingIdentifier() {
        return mRecordingIdentifier;
    }

    public void setRecordingIdentifier(String recordingIdentifier) {
        mRecordingIdentifier = recordingIdentifier;
    }

    //endregion

    public Track() {

    }

    public Uri getUri() {
        return RecordingsContract.Track.buildUri(getRecordingIdentifier(), getFilename());
    }

    public MediaMetadataCompat getMediaMetadata(long numTracks) {

        //TODO: Remove hard coded strings
        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, getUri().toString())
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, getLength())
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Grateful Dead")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration())
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "Live")
                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, getNumber())
                .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, numTracks)
                .build();


    }

    private long getDuration() {
        int minutes;
        int seconds;
        try {
            String[] parts = getLength().split(":");
            minutes = Integer.parseInt(parts[0]);
            seconds = Integer.parseInt(parts[1]);
        } catch(Exception ex) {
            return 0;
        }

        return ((minutes * 60) + seconds) * 1000L;
    }

    @Override
    public int compareTo(Track another) {

        if (getNumber() > another.getNumber())
            return 1;

        if (getNumber() < another.getNumber())
            return -1;

        return 0;
    }
}
