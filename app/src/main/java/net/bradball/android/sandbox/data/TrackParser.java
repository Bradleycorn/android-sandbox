package net.bradball.android.sandbox.data;

import android.content.ContentProviderOperation;

import net.bradball.android.sandbox.model.Track;
import net.bradball.android.sandbox.network.ArchiveAPI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Created by bradb on 2/4/16.
 */
public class TrackParser extends JSONParser implements JsonDeserializer<Track> {
    private Track mTrack;
    private final String mFilename;
    private final long mRecordingID;
    private final String mRecordingIdentifier;

    public Track getTrack() {
        return mTrack;
    }

    public TrackParser(String filename, long recordingID, String recordingIdentifier) {
        mFilename = filename;
        mRecordingID = recordingID;
        mRecordingIdentifier = recordingIdentifier;
    }

    public Track deserialize(JsonElement json, Type typeOfSrc, JsonDeserializationContext context) throws JsonParseException {
        final Track track;

        setDeserializationContext(context, json.getAsJsonObject());

        String album = getValue(ArchiveAPI.RECORDING_DETAIL_FIELDS.FILE_FILEDS.ALBUM, "");
        String bitrate = getValue(ArchiveAPI.RECORDING_DETAIL_FIELDS.FILE_FILEDS.BITRATE, "unknown");
        String format = getValue(ArchiveAPI.RECORDING_DETAIL_FIELDS.FILE_FILEDS.FORMAT, "mp3");
        String length = getValue(ArchiveAPI.RECORDING_DETAIL_FIELDS.FILE_FILEDS.LENGTH, "00:00");
        String md5 = getValue(ArchiveAPI.RECORDING_DETAIL_FIELDS.FILE_FILEDS.MD5, "");
        int number = getStringInt(ArchiveAPI.RECORDING_DETAIL_FIELDS.FILE_FILEDS.NUMBER, 0);
        long size = getStringLong(ArchiveAPI.RECORDING_DETAIL_FIELDS.FILE_FILEDS.SIZE, 0L);
        String title = getValue(ArchiveAPI.RECORDING_DETAIL_FIELDS.FILE_FILEDS.TITLE, "");

        track = new Track();

        track.setAlbum(album);
        track.setBitRate(bitrate);
        track.setFormat(format);
        track.setLength(length);
        track.setMd5(md5);
        track.setNumber(number);
        track.setSize(size);
        track.setTitle(title);

        return track;
    }

    @Override
    public void processJson(JsonElement element) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Track.class, this);
        Gson gson = gsonBuilder.create();

        mTrack = gson.fromJson(element, Track.class);
        mTrack.setFilename(mFilename);
        mTrack.setRecordingID(mRecordingID);
        mTrack.setRecordingIdentifier(mRecordingIdentifier);
    }

    @Override
    public void getContentProviderInserts(ArrayList<ContentProviderOperation> list) {
        //Nothing to do here ... yet.
        //This will likely be useful when it comes time to add "Make available offline" features.
    }
}
