package services;

import android.content.Context;
import android.widget.MediaController;

public class PlayMusicController extends MediaController {

    public PlayMusicController(Context c) {
        super(c);
    }

    /**
     * We're overriding the parent's `hide` method, so we
     * can prevent the controls from hiding after 3 seconds.
     */
    /*public void hide() { }*/
}
