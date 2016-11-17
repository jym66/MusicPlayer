package com.chenhao.musicplayer.mod;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import com.chenhao.musicplayer.bean.MusicInfo;
import com.chenhao.musicplayer.messagemgr.MediaPlayerObserver;
import com.chenhao.musicplayer.messagemgr.MessageID;
import com.chenhao.musicplayer.messagemgr.MessageManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by chenhao on 2016/11/4.
 */

public class MediaPlayerManager {
    public final static int MEDIA_PLAY_DEFAULT = 0;
    private final static int MEDIA_PLAY_PLAYING = 1;
    private final static int MEDIA_PLAY_PAUSE = 2;
    private final static int MEDIA_PLAY_STOP = 3;
    private final static int MEDIA_PLAY_PREPARE = 4;
    public final static int MEDIA_PLAY_RESET = 5;
    private final static int MEDIA_PLAY_RELEASE = 6;
    private List<MusicInfo> mInfos = new ArrayList<>();
    private int mPosition = -1;
    private static MediaPlayerManager mInstance = null;
    private static android.media.MediaPlayer mMediaPlayer = null;
    private boolean mBufferCompletePlay = true;
    private int mState = MEDIA_PLAY_DEFAULT;

    private static String mClassName;

    static {
        mClassName = MediaPlayerManager.class.getSimpleName();
    }

    private MediaPlayerManager() {
    }

    public static MediaPlayerManager getInstance() {
        if (null == mInstance) {
            synchronized (MediaPlayerManager.class) {
                if (null == mInstance) {
                    mInstance = new MediaPlayerManager();
                    Log.e("chenhaolog", mClassName + "[getInstance]" + mInstance.getClass().hashCode());
                }
            }
        }
        return mInstance;
    }

    public void setInfos(List<MusicInfo> infos) {
        this.mInfos = infos;
    }

    public List<MusicInfo> getInfos() {
        return mInfos;
    }

    public int getMediaPlayState(){
        return mState;
    }

    public void init() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new android.media.MediaPlayer();
            Log.i("chenhaolog", mClassName + "[new MediaPlayer]" + mMediaPlayer.getClass().hashCode());
        }
        mMediaPlayer.setOnBufferingUpdateListener(new android.media.MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(android.media.MediaPlayer mediaPlayer, final int i) {
                MessageManager.getInstance().asyncNotify(MessageID.OBSERVER_MEDIA_PLAYER, new MessageManager.Caller<MediaPlayerObserver>() {
                    @Override
                    public void call() {
                        ob.onBuffering(i);
                    }
                });
                Log.i("chenhaolog", "mMediaPlayer [onBufferingUpdate]" + mMediaPlayer.getClass().hashCode());
            }
        });
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mState = MEDIA_PLAY_PREPARE;
                if (mBufferCompletePlay) {
                    mInstance.startMediaPlayer();
                }
                MessageManager.getInstance().asyncNotify(MessageID.OBSERVER_MEDIA_PLAYER, new MessageManager.Caller<MediaPlayerObserver>() {
                    @Override
                    public void call() {
                        ob.onPrepared(mInfos, mPosition);
                    }
                });
                Log.i("chenhaolog", "mMediaPlayer [onPrepared]" + mMediaPlayer.getClass().hashCode());
            }
        });
        mMediaPlayer.setOnCompletionListener(new android.media.MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(android.media.MediaPlayer mediaPlayer) {
                Log.i("chenhaolog", "mMediaPlayer [onCompletion]" + mMediaPlayer.getClass().hashCode());
                mInstance.playerNext();
            }
        });
        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.i("chenhaolog", "mMediaPlayer [onError]" + what + ":" + extra);
                return true;
            }
        });
    }

    public void setMediaPlayerUrlAndStart(List<MusicInfo> infos, final int position) {
        setInfos(infos);
        resetMediaPlayer();
        Log.i("chenhaolog", mClassName + "[setMediaPlayerUrlAndStart]" + position);
        setMediaPlayerUrl(position, true);
    }

    public void setMediaPlayerUrl(int position, boolean bufferCompletePlay) {
        try {
            mBufferCompletePlay = bufferCompletePlay;
            mPosition = position;
            mMediaPlayer.setDataSource(mInfos.get(position).getUrl());
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void startMediaPlayer() {
        if (mMediaPlayer != null) {
            if (mPosition < 0 && mInfos.size() < 1) {
                return;
            }
            Log.i("chenhaolog", mClassName + "[startMediaPlayer]");
            mMediaPlayer.start();
            mState = MEDIA_PLAY_PLAYING;
            MessageManager.getInstance().asyncNotify(MessageID.OBSERVER_MEDIA_PLAYER, new MessageManager.Caller<MediaPlayerObserver>() {
                @Override
                public void call() {
                    ob.startMusic(mInfos, mPosition);
                }
            });
        }
    }

    public void playerNext() {
        if (mMediaPlayer != null && (mPosition + 1) > 0 && (mPosition + 1) < mInfos.size()) {
            setMediaPlayerUrlAndStart(mInfos, mPosition + 1);
        }
    }

    public void playerFront() {
        if (mMediaPlayer != null && (mPosition - 1) > -1 && mPosition < mInfos.size()) {
            setMediaPlayerUrlAndStart(mInfos, mPosition - 1);
        }
    }

    public boolean isPlaying() {
        boolean playing = false;
        if (mMediaPlayer != null) {
            playing = mMediaPlayer.isPlaying();
        }
        return playing;
    }

    private void pauseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
            mState = MEDIA_PLAY_PAUSE;
            MessageManager.getInstance().asyncNotify(MessageID.OBSERVER_MEDIA_PLAYER, new MessageManager.Caller<MediaPlayerObserver>() {
                @Override
                public void call() {
                    ob.onPause();
                }
            });

        }
    }

    public void pauseOrPlay() {
        if (isPlaying()) {
            pauseMediaPlayer();
        } else {
            startMediaPlayer();
        }
    }

    public void stopMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mState = MEDIA_PLAY_STOP;
        }
    }

    private void resetMediaPlayer() {
        if (mMediaPlayer != null) {
            Log.i("chenhaolog", mClassName + "[resetMediaPlayer]");
            mMediaPlayer.reset();
            mState = MEDIA_PLAY_RESET;
            MessageManager.getInstance().asyncNotify(MessageID.OBSERVER_MEDIA_PLAYER, new MessageManager.Caller<MediaPlayerObserver>() {
                @Override
                public void call() {
                    ob.stopMusic();
                }
            });
        }
    }

    public void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mState = MEDIA_PLAY_RELEASE;
        }
        mMediaPlayer = null;
        mInstance = null;
    }

    public void seekTo(int position) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(position);
        }
    }

    //获取歌曲长度
    public int getMusicDuration() {
        int rtn = 0;
        if (mMediaPlayer != null) {
            rtn = mMediaPlayer.getDuration();
        }
        return rtn;
    }

    //获取当前播放进度
    public int getMusicCurrentPosition() {
        int rtn = 0;
        if (mMediaPlayer != null) {
            rtn = mMediaPlayer.getCurrentPosition();

        }
        return rtn;
    }

}