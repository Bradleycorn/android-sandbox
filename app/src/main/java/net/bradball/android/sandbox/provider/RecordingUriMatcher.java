package net.bradball.android.sandbox.provider;

import android.content.UriMatcher;
import android.net.Uri;
import android.util.SparseArray;


public class RecordingUriMatcher {

    /**
     * All methods on a {@link UriMatcher} are thread safe, except {@code addURI}.
     */
    private UriMatcher mUriMatcher;

    private SparseArray<RecordingUrisEnum> mEnumsMap = new SparseArray<>();

    /**
     * This constructor needs to be called from a thread-safe method as it isn't thread-safe itself.
     */
    public RecordingUriMatcher(){
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        buildUriMatcher();
    }

    private void buildUriMatcher() {
        final String authority = RecordingsContract.CONTENT_AUTHORITY;
        final String trackAuthority = RecordingsContract.TRACK_AUTHORITY;

        RecordingUrisEnum[] uris = RecordingUrisEnum.values();
        for (int i = 0; i < uris.length; i++) {
            if (uris[i].code < 300 || uris[i].code > 399) {
                mUriMatcher.addURI(authority, uris[i].path, uris[i].code);
            } else {
                mUriMatcher.addURI(trackAuthority, uris[i].path, uris[i].code);
            }

        }

        buildEnumsMap();
    }

    private void buildEnumsMap() {
        RecordingUrisEnum[] uris = RecordingUrisEnum.values();
        for (int i = 0; i < uris.length; i++) {
            mEnumsMap.put(uris[i].code, uris[i]);
        }
    }

    /**
     * Matches a {@code uri} to a {@link RecordingUrisEnum}.
     *
     * @return the {@link RecordingUrisEnum}, or throws new UnsupportedOperationException if no match.
     */
    public RecordingUrisEnum matchUri(Uri uri){
        final int code = mUriMatcher.match(uri);
        try {
            return matchCode(code);
        } catch (UnsupportedOperationException e){
            throw new UnsupportedOperationException("Unknown uri " + uri);
        }
    }

    /**
     * Matches a {@code code} to a {@link RecordingUrisEnum}.
     *
     * @return the {@link RecordingUrisEnum}, or throws new UnsupportedOperationException if no match.
     */
    public RecordingUrisEnum matchCode(int code){
        RecordingUrisEnum RecordingUrisEnum = mEnumsMap.get(code);
        if (RecordingUrisEnum != null){
            return RecordingUrisEnum;
        } else {
            throw new UnsupportedOperationException("Unknown uri with code " + code);
        }
    }
}