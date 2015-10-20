package com.ukiuni.slite.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by tito on 2015/10/16.
 */
public class CircleImageView extends ImageView {

    private static final String TAG = "ClipImageView";

    public CircleImageView(Context context) {
        super(context);
    }

    public CircleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CircleImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private Path pathCircle = new Path();

    /**
     * Viewのサイズ確保
     */
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        pathCircle.addCircle(w / 2, h / 2, w / 2, Path.Direction.CW);
    }

    /**
     * 描画処理
     */
    protected void onDraw(Canvas canvas) {
        canvas.clipPath(pathCircle);
        super.onDraw(canvas);
    }

}
