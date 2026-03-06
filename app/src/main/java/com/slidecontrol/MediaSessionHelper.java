package com.slidecontrol;

import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.support.v4.media.session.MediaSessionCompat;

public class MediaSessionHelper {

    public interface CommandListener {
        void onCommand(String command);
    }

    private final MediaSessionCompat mediaSession;
    private final CommandListener listener;

    public MediaSessionHelper(Context context, CommandListener listener) {
        this.listener = listener;

        mediaSession = new MediaSessionCompat(context, "SlideControlSession");

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
                }
                return false;
            }
        });

        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );
    }

    public void activate() {
        mediaSession.setActive(true);
    }

    public void deactivate() {
        mediaSession.setActive(false);
        mediaSession.release();
    }
}
