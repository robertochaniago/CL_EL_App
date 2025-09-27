package com.el.cmr.nusaindah.ui.app_home.progress.style;

import android.animation.ValueAnimator;
import android.os.Build;

import com.el.cmr.nusaindah.ui.app_home.progress.lessview.SpriteAnimatorBuilder;
import com.el.cmr.nusaindah.ui.app_home.progress.fanta.CircleSprite;
import com.el.cmr.nusaindah.ui.app_home.progress.fanta.Sprite;
import com.el.cmr.nusaindah.ui.app_home.progress.fanta.SpriteContainer;

/**
 * Created by ybq.
 */
public class DoubleBounce extends SpriteContainer {

    @Override
    public Sprite[] onCreateChild() {
        return new Sprite[]{
                new Bounce(), new Bounce()
        };
    }

    @Override
    public void onChildCreated(Sprite... sprites) {
        super.onChildCreated(sprites);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sprites[1].setAnimationDelay(1000);
        } else {
            sprites[1].setAnimationDelay(-1000);
        }
    }

    private class Bounce extends CircleSprite {

        Bounce() {
            setAlpha(153);
            setScale(0f);
        }

        @Override
        public ValueAnimator onCreateAnimation() {
            float fractions[] = new float[]{0f, 0.5f, 1f};
            return new SpriteAnimatorBuilder(this).scale(fractions, 0f, 1f, 0f).
                    duration(2000).
                    easeInOut(fractions)
                    .build();
        }
    }
}
