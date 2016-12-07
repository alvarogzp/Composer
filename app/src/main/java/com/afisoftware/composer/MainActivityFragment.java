package com.afisoftware.composer;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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

import static android.app.Activity.RESULT_OK;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    private static final int REFRESH_PLAYBACK_INTERVAL_MILLIS_WHILE_PLAYING = 10;

    private static final int SELECT_FILE_CODE = 1;

    @BindView(R.id.audio_name) TextView audioName;
    @BindView(R.id.play_button) FloatingActionButton playButton;

    @BindView(R.id.playback_state) TextView playbackState;
    @BindView(R.id.playback_bar) SeekBar playbackBar;
    @BindView(R.id.playback_time) TextView playbackTime;

    private MediaPlayer player;
    private Handler playbackRefreshHandler;
    private String totalPlaybackTime;
    private boolean draggingPlaybackBar = false;
    private Uri audioUri;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, view);
        playbackBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                draggingPlaybackBar = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                draggingPlaybackBar = false;
                if (player != null) {
                    player.seekTo(seekBar.getProgress());
                    if (!isPlaying()) {
                        showPausedPlayback();
                    }
                } else {
                    showStoppedPlayback();
                }
            }
        });
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

    @OnClick(R.id.select_file_button)
    public void selectFileButtonClicked() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Choose a music file"), SELECT_FILE_CODE);
        } catch (ActivityNotFoundException e) {
            Snackbar.make(getView(), "Please install a File Manager.", Snackbar.LENGTH_LONG)
                    .setAction("Action", null)
                    .show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SELECT_FILE_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    audioName.setText(uri.toString());
                    audioUri = uri;
                    if (player != null) {
                        player.stop();
                        player = null;
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @OnClick(R.id.play_button)
    public void playClicked() {
        if (isPlaying()) {
            player.pause();
        } else if (player != null) {
            player.start();
            resumedPlayback();
        } else {
            if (audioUri == null) {
                player = MediaPlayer.create(getActivity(), R.raw.music);
            } else {
                player = MediaPlayer.create(getActivity(), audioUri);
            }
            player.start();
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    player = null;
                    showStoppedPlayback();
                }
            });
            Log.d("Composer", "Playing!");
            startedPlayback();
        }
    }

    private void startedPlayback() {
        int totalPlaybackMillis = player.getDuration();
        playbackBar.setMax(totalPlaybackMillis);
        totalPlaybackTime = formatMillis(totalPlaybackMillis);

        resumedPlayback();
    }

    private void resumedPlayback() {
        playbackState.setText("Playing");
        playButton.setImageResource(android.R.drawable.ic_media_pause);
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
            } else if (player != null) {
                showPausedPlayback();
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
        if (!draggingPlaybackBar) {
            playbackBar.setProgress(currentPlaybackMillis);
        }
        playbackTime.setText(formatMillis(currentPlaybackMillis) + " / " + totalPlaybackTime);
    }

    private void showPausedPlayback() {
        playbackState.setText("Paused");
        refreshPlayingPlayback();
        playButton.setImageResource(android.R.drawable.ic_media_play);
    }

    private void showStoppedPlayback() {
        playbackState.setText("Stopped");
        playbackBar.setProgress(0);
        playbackTime.setText(formatMillis(0) + " / " +  formatMillis(0));
        playButton.setImageResource(android.R.drawable.ic_media_play);
    }

    private String formatMillis(int millis) {
        int mins = millis / 60000;
        int secs = (millis / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs);
    }
}
