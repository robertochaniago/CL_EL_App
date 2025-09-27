package com.el.cmr.nusaindah.ui.app_home.progress.style;

import com.el.cmr.nusaindah.ui.app_home.progress.fanta.Sprite;
import com.el.cmr.nusaindah.ui.app_home.progress.fanta.SpriteContainer;

/**
 * Created by ybq.
 */
public class MultiplePulseRing extends SpriteContainer {

    @Override
    public Sprite[] onCreateChild() {
        return new Sprite[]{
                new PulseRing(),
                new PulseRing(),
                new PulseRing(),
        };
    }

    @Override
    public void onChildCreated(Sprite... sprites) {
        for (int i = 0; i < sprites.length; i++) {
            sprites[i].setAnimationDelay(200 * (i + 1));
        }
    }
}
