package com.slidecontrol;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.view.KeyEvent;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

public class MediaSessionHelper {

    public interface CommandListener {
        void onCommand(String command);
    }

    private final MediaSessionCompat mediaSession;
    private final CommandListener listener;
    private final AudioManager audioManager;
    private final AudioManager.OnAudioFocusChangeListener audioFocusListener;

    public MediaSessionHelper(Context context, CommandListener listener) {
        this.listener = listener;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // Audio focus listener - required to intercept volume keys
        audioFocusListener = focusChange -> {};

        mediaSession = new MediaSessionCompat(context, "SlideControlSession");

        // CRITICAL: Set a valid PlaybackState - without this volume keys are ignored
        PlaybackStateCompat state = new PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
            .build();
        mediaSession.setPlaybackState(state);

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                KeyEvent event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (event == null) return false;
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;

                int keyCode = event.getKeyCode();
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    listener.onCommand("next");
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    listener.onCommand("prev");
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
                    listener.onCommand("next");
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                    listener.onCommand("prev");
                    return true;
                }
                return false;
            }

            // Also handle skip actions as fallback
            @Override
            public void onSkipToNext() {
                listener.onCommand("next");
            }

            @Override
            public void onSkipToPrevious() {
                listener.onCommand("prev");
            }
        });

        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );
    }

    public void activate() {
        // Request audio focus - this is what makes volume keys route to our session
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.media.AudioFocusRequest focusRequest =
                new android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(audioFocusListener)
                    .setWillPauseWhenDucked(false)
                    .setAcceptsDelayedFocusGain(false)
                    .build();
            audioManager.requestAudioFocus(focusRequest);
        } else {
            audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            );
        }
        mediaSession.setActive(true);
    }

    public void deactivate() {
        mediaSession.setActive(false);
        mediaSession.release();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocus(audioFocusListener);
        }
    }
}
