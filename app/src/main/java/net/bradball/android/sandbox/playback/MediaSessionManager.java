package net.bradball.android.sandbox.playback;

import android.support.v4.media.session.MediaSessionCompat;

/**
 *               |  Created   |   Playing   |    Paused   |   Stopped   | Destroyed |
 * --------------|------------|-------------|-------------|-------------|-----------|
 * MediaPlayer   |    new     |   prepare   |    pause    |    stop     |  release  |
 *               |            |    play     |             |             |           |
 *               |            |             |             |             |
 * --------------|------------|-------------|-------------|-------------|-----------|
 * Audio Focus   |            |request focus|             | clear focus |           |
 * --------------|------------|-------------|-------------|-------------|-----------|
 * Noisy         |            |  register   | unregister  |             |           |
 * --------------|------------|-------------|-------------|-------------|-----------|
 * Media Session |    new     |  setActive  |             |set InActive |  release  |
 *               | set flags  |set metadata |             |             |           |
 *               |set callback|  set state  | set state   |             |           |
 * --------------|------------|-------------|-------------|-------------|-----------|
 * Notification  |            | start FG    |stopFG(false)|stopFG(true) |           |
 *---------------|------------|-------------|-------------|-------------|-----------|
 * Service       |            | load Tracks |             |             |           |
 *               |            | delay stop  |             | delay stop  |           |
 *               |            |   clear     |             |  start      |           |
 *---------------|------------|-------------|-------------|-------------|-----------|
 */

public class MediaSessionManager {
    private static final String TAG = "MediaSessionManager";

    private MediaSessionCompat mMediaSession;

    public MediaSessionManager(MediaSessionCompat mediaSession) {
        mMediaSession = mediaSession;
    }

    public void onPlayMusic() {}


}
