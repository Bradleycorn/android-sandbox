package net.bradball.android.sandbox.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import net.bradball.android.sandbox.R;
import net.bradball.android.sandbox.util.LogHelper;

import java.io.IOException;

/**
 * Created by bradb on 6/5/16.
 */
public class Playback implements
        AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnSeekCompleteListener {

    private static final String TAG = LogHelper.makeLogTag(Playback.class);

    //Volume & Audio Focus Settings
    private static final float VOLUME_NORMAL = 1.0f;
    private static final float VOLUME_DUCK = 0.2f;
    private static final int AUDIO_FOCUS_NONE = 0;
    private static final int AUDIO_FOCUS_DUCK = 1;
    private static final int AUDIO_FOCUSED = 2;

    //Objects and properties for the current media player
    private MediaPlayer mCurrentMediaPlayer = null;
    private String mCurrentMediaId = null;
    private int mCurrentPosition = 0;

    //Objects and properties to pre-buffer the next song in the queue
    private MediaPlayer mNextMediaPlayer = null;
    private String mNextMediaId = null;
    private boolean mNextMediaBuffering = false;


    //Properties used to keep track of the current playback state
    private int mPlaybackState;
    private int mAudioFocus = AUDIO_FOCUS_NONE;
    private boolean mNoisyReceiverRegistered;

    //When we lose audio focus (say an incoming phone call or something), we will want to
    //note the player state and set a flag to indicate whether or not we should restart
    //playback when we regain audio focus (when the call ends). See onAudioFocusChange()
    private boolean mPlayOnFocusGain;


    private Context mContext;
    private AudioManager mAudioManager;
    private WifiManager.WifiLock mWifiLock;
    private Playback.PlaybackListener mPlaybackListener;

    public Playback(Context context) {
        mContext = context;

        //Get a wifi lock. This just gets the lock, it still needs to be set,
        //but we don't need/want to do that until we actually play some music.
        mWifiLock = ((WifiManager) mContext.getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, R.string.app_name+"_wifi_lock");

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }


    /**
     * Play a song
     *
     * Call this method to play a song
     *
     * This method is called in several situations.
     *
     * 1.) To start playing music initially.
     * 2.) To resume playback when paused.
     * 3.) To play a new song (skip, new queue, etc)
     *
     * While it doesn't actually start playback, this method
     * is responsible for setting up everything that needs to be setup
     * before playback can start. That includes:
     *
     * - Getting Audio Focus (if we don't have it already)
     * - Setting up the MediaPlayer object (create it or reset it)
     * - Calling prepareAsync on the MediaPlayer.
     *
     * If it just needs to resume from a paused state, this method
     * will just start the player.
     *
     *
     * If it's playing a new song in the same queue, things are a little
     * complicated. It needs to see if the new song to play is already
     * buffered in the mNextMediaPlayer.
     *      If it is, then we need to handle it somehow...
     *
     *      If not, then it's a new song, and it should fall through
     *      to the "new song all together" item below.
     *
     * If it's starting a new song all together, it'll clear/reset
     * all previous players and setup everything for playback.
     */
    public void play(MediaSessionCompat.QueueItem queueItem) {
        String mediaId = queueItem.getDescription().getMediaId();
        boolean mediaChanged = !TextUtils.equals(mediaId, mCurrentMediaId);

        if (mediaChanged) {
            mCurrentPosition = 0;
            mCurrentMediaId = mediaId;
        }

        if (!mediaChanged && mPlaybackState == PlaybackStateCompat.STATE_PAUSED) {
            startPlayback();
        } else if (mediaChanged && mCurrentMediaId == mNextMediaId) {
            //TODO: swap players, play new song.
        } else {
            mPlaybackState = PlaybackStateCompat.STATE_STOPPED;
            if (mWifiLock.isHeld()) {
                mWifiLock.release();
            }
            unregisterAudioNoisyReceiver();

            try {
                resetPlayer(mCurrentMediaPlayer);
                stopNextMediaPlayer();

                setPlaybackState(PlaybackStateCompat.STATE_BUFFERING);
                getAudioFocus();
                mCurrentMediaPlayer.setDataSource(mCurrentMediaId);
                mCurrentMediaPlayer.prepareAsync();
            } catch (IOException ex) {
                if (mPlaybackListener != null) {
                    mPlaybackListener.onError(ex.getMessage());
                }
            }

        }
    }


    /**
     * Pause the song
     *
     * This method pauses the MediaPlayer, as well as
     * handling the other things that need to happen
     * when the player isn't actually playing to
     * ease system resources. That includes:
     *
     * - pausing the MediaPlayer
     * - releasing the WiFi lock
     * - Unregister the Noisy Receiver
     * - Notify the Service that playback is paused
     *      (The service needs to:)
     *      - Update the MediaSession(playback state)
     *      - Allow the service to stop if it has to (but keep the notification)
     */
    public void pause() {

        if (mPlaybackState == PlaybackStateCompat.STATE_PLAYING) {
            if (mCurrentMediaPlayer!= null && mCurrentMediaPlayer.isPlaying()) {
                mCurrentMediaPlayer.pause();
                mCurrentPosition = mCurrentMediaPlayer.getCurrentPosition();
                setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            }

        }

        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }

        unregisterAudioNoisyReceiver();
    }


    /**
     * Stop Playback (for good)
     *
     * This method should be called when you want to stop playback for good
     * such as when you lose audio focus entirely, or the service is shutting
     * down.
     *
     * It is responsible for cleaning up the media players and releasing resources.
     */
    public void stop() {
        if (mCurrentMediaPlayer != null) {
            mCurrentMediaPlayer.setNextMediaPlayer(null);

            if (mCurrentMediaPlayer.isPlaying()) {
                mCurrentMediaPlayer.stop();
            }

            mCurrentMediaPlayer.release();
            mCurrentMediaPlayer.reset();
            mCurrentMediaPlayer = null;

        }

        mCurrentMediaId = null;

        mPlaybackState = PlaybackStateCompat.STATE_STOPPED;

        stopNextMediaPlayer();

        giveUpAudioFocus();
        unregisterAudioNoisyReceiver();
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }

    /**
     * Seek to a specific position in the currently playing song.
     *
     * @param position - the position to seek to (in milliseconds?)
     *
     * If there is no current MediaPlayer, this method just
     * sets the mCurrentPosition member variable. Otherwise it
     * calls the MediaPlayer.seekTo method on the current MediaPlayer
     * to seek to the passed in position.  The MediaPlayer will
     * then call it's onSeekComplete() callback (which should be
     * set to this class), and there we will update the mCurrentPosition
     * member variable and start playback again.
     */
    public void seekTo(int position) {
        if (mCurrentMediaPlayer == null) {
            mCurrentPosition = position;
        } else {
            if (mCurrentMediaPlayer.isPlaying()) {
                setPlaybackState(PlaybackStateCompat.STATE_BUFFERING);
            }
            mCurrentMediaPlayer.seekTo(position);
        }
    }


    /**
     * Start buffering (prepareAsync) on the next song to be played.
     *
     * This method is responsible for setting up the mNextMediaPlayer
     * and calling it's prepareAsync() to start buffering the track.
     * Finally, this method will set the mNextMediaPlayerBuffering flag.
     */
    public void bufferNext(MediaSessionCompat.QueueItem nextItem) {
        resetPlayer(mNextMediaPlayer);

        mNextMediaId = nextItem.getDescription().getMediaId();
        try {
            mNextMediaPlayer.setDataSource(mNextMediaId);
            mNextMediaPlayer.prepareAsync();
            mNextMediaBuffering = true;
        } catch(IOException ex) {
            stopNextMediaPlayer();
            return;
        }

    }

    public int getPlaybackState() {
        return mPlaybackState;
    }

    public int getPlaybackPosition() {
        return mCurrentPosition;
    }


    /**
     * Test to see if the mCurrentMediaPlayer is in the middle of playback.
     *
     * @return True if the mCurrentMediaPlayer is set, has started playback, but
     * has not yet finished playback. Otherwise returns False.
     *
     * Note that a return value of True does not gaurantee that
     * playback is started. The player could be paused or buffering,
     * and this method will still return true.
     */
    public boolean isActive() {
        return (mCurrentMediaPlayer != null &&
            (
                mPlaybackState == PlaybackStateCompat.STATE_BUFFERING ||
                mPlaybackState == PlaybackStateCompat.STATE_PLAYING ||
                mPlaybackState == PlaybackStateCompat.STATE_PAUSED
            )
        );
    }

    public boolean isPlaying() {
        return mPlayOnFocusGain || (mCurrentMediaPlayer != null && mCurrentMediaPlayer.isPlaying());
    }


    private void stopNextMediaPlayer() {
        if (mNextMediaPlayer != null) {
            mNextMediaPlayer.reset();
            mNextMediaPlayer.release();
            mNextMediaPlayer = null;

        }
        mNextMediaId = null;
        mNextMediaBuffering = false;
    }

    /**
     * Try to get the system audio focus.
     */
    private void getAudioFocus() {
        if (mAudioFocus != AUDIO_FOCUSED) {
            int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_FOCUSED;
            }
        }
    }

    /**
     * Give up the audio focus.
     */
    private void giveUpAudioFocus() {
        if (mAudioFocus == AUDIO_FOCUSED || mAudioFocus == AUDIO_FOCUS_DUCK) {
            if (mAudioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_FOCUS_NONE;
            }
        }
    }


    private void resetPlayer(MediaPlayer mp) {
        if (mp == null) {
            mp = new MediaPlayer();

            // Make sure the media player will acquire a wake-lock while
            // playing. If we don't do that, the CPU might go to sleep while the
            // song is playing, causing playback to stop.
            mp.setWakeMode(mContext.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing,
            // and when it's done playing:
            mp.setOnPreparedListener(this);
            mp.setOnCompletionListener(this);
            mp.setOnErrorListener(this);
            mp.setOnSeekCompleteListener(this);
            mp.setOnBufferingUpdateListener(this);
        } else {
            mp.reset();
        }
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }


    private void setPlaybackState(int newState) {
        if (newState != mPlaybackState) {
            mPlaybackState = newState;
            if (mPlaybackListener != null) {
                mPlaybackListener.onPlaybackStateChanged(newState);
            }
        }
    }


    private void setPlayerVolume() {
        if (mCurrentMediaPlayer != null) {
            if (mAudioFocus == AUDIO_FOCUS_DUCK) {
                mCurrentMediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK);
            } else {
                mCurrentMediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL);
            }
        }
    }

    /**
     * Start Playback
     *
     * This method actually starts/resumes playback.
     *
     * This method actually starts the MediaPlayer, as well
     * as setting up everything else that has to go on in
     * support of playback including:
     *
     * - Aquire a WiFi lock (if we don't have one already)
     * - Set the player volume
     * - Register the Noisy Receiver
     * - Notify the Service that playback is starting
     *     (The service needs to:)
     *     - Update the MediaSession(Active?, metadata?, playback state)
     *     - Start the Notification
     *     - keep itself alive
     *
     */
    private void startPlayback() {

        if (mAudioFocus == AUDIO_FOCUS_NONE) {
            //TODO: It's possible that the playbackstate at this point is "buffering" (if we attempted to play but didn't get audio focus). We should do something.
            //We don't have audio focus. Can't start playback.
            mPlaybackListener.onError("Tried to start Playback without audio focus");
            return;
        }

        //Aquire the wifilock that we created earlier
        mWifiLock.acquire();

        setPlayerVolume();
        registerAudioNoisyReceiver();
        if (mCurrentMediaPlayer != null && !mCurrentMediaPlayer.isPlaying()) {
            setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            mCurrentMediaPlayer.start();
        }
    }


    /**
     * Called when the mNextMediaPlayer is going to start playing.
     *
     * This method is responsible for switching over the next media
     * player and making it the current media player. It also will
     * notify the playback manager that playback for a song is starting
     * so that the manager can update the mediasession.
     */
    private void onNextStarted() {
        swapPlayers();

        if (mPlaybackListener!=null) {
            //mPlaybackListener.onNextStarting(mCurrentMediaId);
        }

    }

    private void swapPlayers() {
        if (mCurrentMediaPlayer != null) {
            mCurrentMediaPlayer.reset();
            mCurrentMediaPlayer.release();
        }

        mCurrentMediaPlayer = mNextMediaPlayer;
        mCurrentMediaId = mNextMediaId;
        mCurrentPosition = 0;

        mNextMediaPlayer = null;
        mNextMediaId = null;

        if (mPlaybackListener != null) {
            mPlaybackListener.onNextStarting(mCurrentMediaId);
        }
    }



    /* CALLBACK IMPLEMENTATIONS
     * ========================
     */

    /**
     * Callback from a mediaplayer object to let us know that it is
     * prepared and ready to start playback.
     *
     * This method checks the mediaplayer passed to it and acts
     * accordingly depeding on if it's the current mediaplayer or the
     * next mediaplayer.
     *
     * If its' the current mediaplayer, then we can startPlayback();
     *
     * If it's not the current media player, and the current mediaplayer exists
     * and is not complete
     *      then we'll set this media player up as the next media player.
     * otherwise, if the next player buffering flag is set
     *      then OnNextStarted(), Resume()
     * clear the next player buffering flag
     *
     *
     * @param mp
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        if (mp == mCurrentMediaPlayer) {
            startPlayback();
        } else {
            if (mCurrentMediaPlayer != null && isActive()) {
                mCurrentMediaPlayer.setNextMediaPlayer(mNextMediaPlayer);
            //}  else if (mNextMediaBuffering) {
            //  swapPlayers();
            //  startPlayback();
            }
            mNextMediaBuffering = false;
        }
    }

    /**
     *
     * This should only be called on the mCurrentMediaPlayer.
     * Per https://developer.android.com/reference/android/media/MediaPlayer.html
     * This is only called while the mediaplayer is in the started state,
     * and the mNextMediaPlayer is never started (and once it is,
     * it gets assigned to mCurrentMediaPlayer).
     */
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

        if (mp == mCurrentMediaPlayer && mPlaybackListener != null) {
            mPlaybackListener.onBufferUpdate(percent);
        }
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        if (mp == mCurrentMediaPlayer) {
            mCurrentPosition = mp.getCurrentPosition();
            if (mPlaybackState == PlaybackStateCompat.STATE_BUFFERING) {
                mCurrentMediaPlayer.start();
                setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            }
        }
    }


    /**
     * MediaPlayer.onCompletion callback - This method is called when
     * the mCurrentMediaPlayer finishes playing.
     *
     * This method is responsible for notifying the playback manager
     * that playback of a specific song is finished. Since
     * android will automatically start playback on the mNextMediaPlayer,
     * we should also notify the manager that playback is starting
     * on a song if the next media player is set.
     *
     * @param mp the mediaplayer that fired the event.
     *
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        boolean nextPlayerExists = (mNextMediaPlayer != null);


        if (nextPlayerExists) {
            swapPlayers();
            if (mNextMediaBuffering) {
                setPlaybackState(PlaybackStateCompat.STATE_BUFFERING);
                mNextMediaBuffering = false; //Now, it's the current media.
            } else {
                setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            }
        }

        if (mPlaybackListener != null) {
            mPlaybackListener.onPlaybackComplete(nextPlayerExists);
        }

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        LogHelper.e(TAG, "MediaPlayer Error: what="+what+", extra="+extra);

        if (mp == mNextMediaPlayer) {
            stopNextMediaPlayer();
            return true;
        }

        if (mPlaybackListener != null) {
            mPlaybackListener.onError("Media Playback Error " + what + "(" + extra + ")");
        }

        return true;
    }


    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            mAudioFocus = AUDIO_FOCUS_NONE;
            if (mPlaybackState == PlaybackStateCompat.STATE_PLAYING) {
                mPlayOnFocusGain = true; //set the flag to restart playback when we gain focus again
                pause();
            }

            //If we've lost audio focus entirely, then it's not coming back.
            //Alert the playback listener that audio focus was lost. It'll probably
            //want to shut down the whole service.
            if (mAudioFocus == AudioManager.AUDIOFOCUS_LOSS && mPlaybackListener != null) {
                mPlaybackListener.onLostAudioFocus();
            }
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            mAudioFocus = AUDIO_FOCUSED;
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            mAudioFocus = AUDIO_FOCUS_DUCK;
        }

        //if the player is playing, adjust the volume to the correct level.
        //Otherwise, if our flag to restart playback is set, and we have focus, then start playback
        //(which will also set the volume)
        if (mPlaybackState == PlaybackStateCompat.STATE_PLAYING) {
            setPlayerVolume();
        } else if (mPlayOnFocusGain && (mAudioFocus == AUDIO_FOCUSED || mAudioFocus == AUDIO_FOCUS_DUCK)) {
            startPlayback();
            mPlayOnFocusGain = false;
        }
    }

    /*
     * NOISY AUDIO RECEIVER SETUP
     * ==========================
     */

    private final IntentFilter mAudioNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private final BroadcastReceiver mAudioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {

                //Not sure why UAMP completely restarts the service, with a "pause"
                //command in the intent??
                /*
                if (isPlaying()) {
                    Intent i = new Intent(context, MusicService.class);
                    i.setAction(MusicService.ACTION_CMD);
                    i.putExtra(MusicService.CMD_NAME, MusicService.CMD_PAUSE);
                    mContext.startService(i);
                }
                */

                //Instead of all that, we're just going to pause.
                if (mPlaybackState == PlaybackStateCompat.STATE_PLAYING || mPlaybackState == PlaybackStateCompat.STATE_BUFFERING) {
                    pause();
                    mPlayOnFocusGain = false; //Since headphones have been pulled, don't auto play again if audio focus is returned.
                }
            }
        }
    };

    private void registerAudioNoisyReceiver() {
        if (!mNoisyReceiverRegistered) {
            mContext.registerReceiver(mAudioNoisyReceiver, mAudioNoisyIntentFilter);
            mNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (mNoisyReceiverRegistered) {
            mContext.unregisterReceiver(mAudioNoisyReceiver);
            mNoisyReceiverRegistered = false;
        }
    }


    /*
     *  PLAYBACK LISTENER INTERFACE
     *
     *  We define an interface, PlaybackListener, that contains several media player event related
     *  methods. We also define a method used to set a PlaybackListener member variable on this instance.
     *  When the appropriate media player events happen, we'll call the associated PlaybackListener
     *  method. This allows a client (like our service) to get notified when media related things happen.
     */

    public void setPlaybackListener(PlaybackListener listener) {
        mPlaybackListener = listener;
    }

    interface PlaybackListener {
        void onLostAudioFocus();

        void onPlaybackComplete(boolean nextSongQueued);

        void onPlaybackStateChanged(int playbackState);

        void onError(String error);

        void onNextStarting(String mediaId);

        void onBufferUpdate(int bufferAmount);
    }

}
