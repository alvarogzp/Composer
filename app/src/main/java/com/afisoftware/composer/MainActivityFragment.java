package com.afisoftware.composer;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    private static final int REFRESH_PLAYBACK_INTERVAL_MILLIS_WHILE_PLAYING = 10;

    @BindView(R.id.playback_state) TextView playbackState;
    @BindView(R.id.playback_bar) SeekBar playbackBar;
    @BindView(R.id.playback_time) TextView playbackTime;

    private MediaPlayer player;
    private Handler playbackRefreshHandler;
    private String totalPlaybackTime;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        playbackRefreshHandler = new Handler();
        refreshPlaybackScheduler();
    }

    @Override
    public void onPause() {
        super.onPause();
        playbackRefreshHandler = null;
    }

    @OnClick(R.id.button)
    public void playClicked() {
        if (player != null) {
            player.stop();
        }
        player = MediaPlayer.create(getActivity(), R.raw.music);
        player.start();
        Log.d("Composer", "Playing!");
        startedPlayback();
    }

    private void startedPlayback() {
        int totalPlaybackMillis = player.getDuration();
        playbackBar.setMax(totalPlaybackMillis);
        totalPlaybackTime = formatMillis(totalPlaybackMillis);

        playbackState.setText("Playing");

        refreshPlaybackScheduler();
    }

    private void refreshPlaybackScheduler() {
        if (playbackRefreshHandler != null) {
            boolean isPlaying = isPlaying();

            if (isPlaying) {
                refreshPlayingPlayback();

                playbackRefreshHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshPlaybackScheduler();
                    }
                }, REFRESH_PLAYBACK_INTERVAL_MILLIS_WHILE_PLAYING);
            } else {
                showStoppedPlayback();
            }
        }
    }

    private boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    private void refreshPlayingPlayback() {
        int currentPlaybackMillis = player.getCurrentPosition();
        playbackBar.setProgress(currentPlaybackMillis);
        playbackTime.setText(formatMillis(currentPlaybackMillis) + " / " + totalPlaybackTime);
    }

    private void showStoppedPlayback() {
        playbackState.setText("Stopped");
        playbackBar.setProgress(0);
        playbackTime.setText(formatMillis(0) + " / " +  formatMillis(0));
    }

    private String formatMillis(int millis) {
        int mins = millis / 60000;
        int secs = (millis / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs);
    }
}
