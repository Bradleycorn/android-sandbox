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

import java.io.IOException;

/**
 * Created by bradb on 5/23/16.
 */
public class LocalPlayback implements
        AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnSeekCompleteListener {
    private final String TAG = "LocalPlayback";


    //Volume & Audio Focus Settings
    private static final float VOLUME_NORMAL = 1.0f;
    private static final float VOLUME_DUCK = 0.2f;
    private static final int AUDIO_FOCUS_NONE = 0;
    private static final int AUDIO_FOCUS_DUCK = 1;
    private static final int AUDIO_FOCUSED = 2;

    //Android Objects that we'll need for playing media
    private Context mContext;
    private MediaPlayer mMediaPlayer;
    private AudioManager mAudioManager;
    private WifiManager.WifiLock mWifiLock;

    //Some properties that we'll use to keep track of the player's current state.
    private int mPlayerState;                       //Keep track of the player's current state (playing, paused, etc)
    private volatile int mCurrentPosition;          //Current scrub position of the playing track (note this is not continually updated in real time)
    private volatile String mCurrentMediaId;        //Media id of the currently playing tack
    private int mAudioFocus = AUDIO_FOCUS_NONE;     //Do we currently have audio focus? (See AUDIO_FOCUS constants above)
    private boolean mAudioNoisyReceiverRegistered;  //Has the broadcast receiver for the "noisy" event been registered?

    //When we lose audio focus (say an incoming phone call or something), we will want to
    //note the player state and set a flag to indicate whether or not we should restart
    //playback when we regain audio focus (when the call ends). See onAudioFocusChange()
    private boolean mPlayOnFocusGain;


    //When someone (i.e. our music service), creates an instance of this class,
    //They might need to get notified when some playback related things happen.
    //So, we setup a member variable to hold an object that implements our PlaybackListener
    //interface (defined below). When related playback events happen, we'll call the
    //appropriate interface method on this object.
    private PlaybackListener mPlaybackListener;




    public LocalPlayback(Context context) {
        mContext = context;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mPlayerState = PlaybackStateCompat.STATE_NONE;

        //Get a wifi lock. This just gets the lock, it still needs to be set,
        //but we don't need/want to do that until we actually play some music.
        mWifiLock = ((WifiManager) mContext.getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, R.string.app_name+"_wifi_lock");
    }



    public int getPlaybackState() {
        return mPlayerState;
    }

    public boolean isPlaying() {
        return (mPlayOnFocusGain || (mMediaPlayer != null && mMediaPlayer.isPlaying()));
    }

    //The local player is always connected. This will end up being an override for a method needed for Chromecast playback.
    public boolean isConnected() {
        return true;
    }

    public long getPlaybackPosition() {
        return mCurrentPosition;
    }


    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            mAudioFocus = AUDIO_FOCUS_NONE;
            if (mPlayerState == PlaybackStateCompat.STATE_PLAYING) {
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
        if (mPlayerState == PlaybackStateCompat.STATE_PLAYING) {
            setPlayerVolume();
        } else if (mPlayOnFocusGain && (mAudioFocus == AUDIO_FOCUSED || mAudioFocus == AUDIO_FOCUS_DUCK)) {
            startPlayback();
            mPlayOnFocusGain = false;
        }
    }


    public void play(MediaSessionCompat.QueueItem queueItem) {
        String mediaId = queueItem.getDescription().getMediaId();
        boolean mediaChanged = !TextUtils.equals(mediaId, mCurrentMediaId);

        if (mediaChanged) {
            mCurrentPosition = 0;
            mCurrentMediaId = mediaId;
        }

        getAudioFocus();

        if (!mediaChanged && mPlayerState == PlaybackStateCompat.STATE_PAUSED && mMediaPlayer != null) {
            startPlayback();
        } else {
            mPlayerState = PlaybackStateCompat.STATE_STOPPED;
            relaxResources(false);

            try {
                //Create or reset the media player
                resetMediaPlayer();

                //Tell it what to play
                mMediaPlayer.setDataSource(mediaId);


                //Prepare the media player. When it's prepared, it'll call onPrepared() below,
                //which will in turn start playing the song.
                mMediaPlayer.prepareAsync();

                //Aquire the wifilock that we created earlier
                mWifiLock.acquire();
                setPlaybackState(PlaybackStateCompat.STATE_BUFFERING);
            } catch (IOException ex) {
                if (mPlaybackListener != null) {
                    mPlaybackListener.onError(ex.getMessage());
                }
            }
        }
    }

    public void pause() {
        if (mPlayerState == PlaybackStateCompat.STATE_PLAYING) {
            if (mMediaPlayer!= null && mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentPosition = mMediaPlayer.getCurrentPosition();
                setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            }
            relaxResources(false);
            unregisterAudioNoisyReceiver();
        }
    }

    public void seekTo(int position) {
        if (mMediaPlayer == null) {
            mCurrentPosition = position;
        } else {
            if (mMediaPlayer.isPlaying()) {
                setPlaybackState(PlaybackStateCompat.STATE_BUFFERING);
            }
            mMediaPlayer.seekTo(position);
            //onSeekComplete() below handles starting playback again and updating
            //our state variable when seeking is complete.
        }
    }


    private void startPlayback() {

        if (mAudioFocus == AUDIO_FOCUS_NONE) {
            //TODO: It's possible that the playbackstate at this point is "buffering" (if we attempted to play but didn't get audio focus). We should do something.
            //We don't have audio focus. Can't start playback.
            mPlaybackListener.onError("Tried to start Playback without audio focus");
            return;
        }

        setPlayerVolume();
        registerAudioNoisyReceiver();
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            mMediaPlayer.start();
        }
    }

    private void setPlayerVolume() {
        if (mMediaPlayer != null) {
            if (mAudioFocus == AUDIO_FOCUS_DUCK) {
                mMediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK);
            } else {
                mMediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL);
            }
        }
    }


    private void setPlaybackState(int newState) {
        if (newState != mPlayerState) {
            mPlayerState = newState;
            if (mPlaybackListener != null) {
                mPlaybackListener.onPlaybackStatusChanged(newState);
            }
        }
    }

    /**
     * Makes sure the media player exists and has been reset. This will create
     * the media player if needed, or reset the existing media player if one
     * already exists.
     */
    private void resetMediaPlayer() {
        Log.d(TAG, "reset MediaPlayer - Need to create it: " + (mMediaPlayer==null));
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();

            // Make sure the media player will acquire a wake-lock while
            // playing. If we don't do that, the CPU might go to sleep while the
            // song is playing, causing playback to stop.
            mMediaPlayer.setWakeMode(mContext.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing,
            // and when it's done playing:
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
        } else {
            mMediaPlayer.reset();
        }
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

    }

    /**
     * Releases resources used by the service for playback. This includes the
     * "foreground service" status, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also
     *            be released or not
     */
    private void relaxResources(boolean releaseMediaPlayer) {
        Log.d(TAG, "relaxResources. releaseMediaPlayer=" + releaseMediaPlayer);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }


    /**
     * Try to get the system audio focus.
     */
    private void getAudioFocus() {
        Log.d(TAG, "tryToGetAudioFocus");
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
        Log.d(TAG, "giveUpAudioFocus");
        if (mAudioFocus == AUDIO_FOCUSED || mAudioFocus == AUDIO_FOCUS_DUCK) {
            if (mAudioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_FOCUS_NONE;
            }
        }
    }




    private final IntentFilter mAudioNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private final BroadcastReceiver mAudioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                Log.d(TAG, "Headphones disconnected.");

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
                if (mPlayerState == PlaybackStateCompat.STATE_PLAYING || mPlayerState == PlaybackStateCompat.STATE_BUFFERING) {
                    pause();
                    mPlayOnFocusGain = false; //Since headphones have been pulled, don't auto play again if audio focus is returned.
                }

            }
        }
    };

    private void registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            mContext.registerReceiver(mAudioNoisyReceiver, mAudioNoisyIntentFilter);
            mAudioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            mContext.unregisterReceiver(mAudioNoisyReceiver);
            mAudioNoisyReceiverRegistered = false;
        }
    }


    /* MEDIAPLAYER INTERFACE EVENT HANDLERS */

    @Override
    public void onPrepared(MediaPlayer mp) {
        //The MediaPlayer is prepared. We can start playing now.
        startPlayback();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        mCurrentPosition = mp.getCurrentPosition();
        if (mPlayerState == PlaybackStateCompat.STATE_BUFFERING) {
            startPlayback();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

        //Current song is finished playing.

        if (mPlaybackListener != null) {
            mPlaybackListener.onPlaybackComplete();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer Error: what="+what+", extra="+extra);

        if (mPlaybackListener != null) {
            mPlaybackListener.onError("Media Playback Error " + what + "(" + extra + ")");
        }

        return true;
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

        void onPlaybackComplete();

        void onPlaybackStatusChanged(int playbackState);

        void onError(String error);

        void currentMediaIdChanged(String mediaId);
    }




}
