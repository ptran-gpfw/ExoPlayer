package com.google.android.exoplayer.demo.player;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.MediaClock;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.TrackRenderer;

/**
 * \internal
 */

public class ClockRenderer extends TrackRenderer {
    private final SpeedupMediaClock mClock;

    public ClockRenderer() {
        mClock = new SpeedupMediaClock();
        mClock.setSpeedFactor(2.0f);
    }

    @Override
    protected void onStarted() throws ExoPlaybackException {
        super.onStarted();
        mClock.start();
    }

    @Override
    protected void onStopped() throws ExoPlaybackException {
        super.onStopped();
        mClock.stop();
    }

    @Override
    protected MediaClock getMediaClock() {
        return mClock;
    }

    @Override
    protected boolean doPrepare(long positionUs) throws ExoPlaybackException {
        return true;
    }

    @Override
    protected int getTrackCount() {
        return 1;
    }

    @Override
    protected MediaFormat getFormat(int track) {
        return null;
    }

    @Override
    protected boolean isEnded() {
        return false;
    }

    @Override
    protected boolean isReady() {
        return true;
    }

    @Override
    protected void doSomeWork(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {

    }

    @Override
    protected void maybeThrowError() throws ExoPlaybackException {

    }

    @Override
    protected long getDurationUs() {
        return 0;
    }

    @Override
    protected long getBufferedPositionUs() {
        return 0;
    }

    @Override
    protected void seekTo(long positionUs) throws ExoPlaybackException {

    }
}
