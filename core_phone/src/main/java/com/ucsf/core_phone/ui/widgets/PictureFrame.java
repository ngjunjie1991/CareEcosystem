package com.ucsf.core_phone.ui.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.ucsf.core_phone.R;

/**
 * View displaying a picture into a round frame.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class PictureFrame extends ImageView {
    private static final int IDX_INF_64PX = 0;
    private static final int IDX_SUP_64PX = 1;

    private static final Paint mMaskPaint = new Paint();
    private static final Paint mImagePaint = new Paint();
    private static final Bitmap[] mMasks = new Bitmap[2];
    private static final Drawable[] mFrames = new Drawable[2];
    private static boolean mIsInit = false;
    private static int m64PxSize;
    private static Bitmap mDefaultImage;
    private final Rect mMaskRect = new Rect();
    private final Rect mImageRect = new Rect();
    private final Rect mViewRect = new Rect();
    private Bitmap mMask;
    private Bitmap mImage;
    private Drawable mFrame;
    private int mFrameColor;

    public PictureFrame(Context context) {
        super(context);
        init();
    }

    public PictureFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PictureFrame(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mImagePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));

        if (!mIsInit) {
            mIsInit = true;

            Drawable drawable = getContext().getResources().getDrawable(R.drawable.icon);
            assert drawable != null;
            mDefaultImage = ((BitmapDrawable) drawable).getBitmap();

            drawable = getContext().getResources().getDrawable(R.drawable.circle_mask_x64);
            assert drawable != null;
            mMasks[IDX_INF_64PX] = ((BitmapDrawable) drawable).getBitmap();
            drawable = getContext().getResources().getDrawable(R.drawable.circle_mask_x120);
            assert drawable != null;
            mMasks[IDX_SUP_64PX] = ((BitmapDrawable) drawable).getBitmap();
            mFrames[IDX_INF_64PX] = getContext().getResources()
                    .getDrawable(R.drawable.circle_frame_x64);
            mFrames[IDX_SUP_64PX] = getContext().getResources()
                    .getDrawable(R.drawable.circle_frame_x120);

            m64PxSize = (int) getContext().getResources().getDimension(R.dimen.size_64px);
        }

        setImageBitmap(mDefaultImage);
        onSizeChanged(64, 64, 0, 0);
    }

    public void setFrameColor(int color) {
        mFrameColor = color;
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        int idx = (w <= m64PxSize && h <= m64PxSize) ? IDX_INF_64PX : IDX_SUP_64PX;
        mMask = mMasks[idx];
        mFrame = mFrames[idx].getConstantState().newDrawable();
        setImageRect(mMask, mMaskRect);
        mViewRect.set(0, 0, w, h);
    }

    @Override
    public void setImageBitmap(Bitmap image) {
        mImage = image == null ? mDefaultImage : image;
        setImageRect(mImage, mImageRect);
        invalidate();
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        canvas.save();
        canvas.drawBitmap(mMask, mMaskRect, mViewRect, mMaskPaint);
        canvas.drawBitmap(mImage, mImageRect, mViewRect, mImagePaint);
        canvas.restore();

        mFrame.setBounds(mViewRect);
        mFrame.setColorFilter(mFrameColor, PorterDuff.Mode.SRC_ATOP);
        mFrame.draw(canvas);
    }

    private void setImageRect(Bitmap img, Rect rect) {
        rect.set(0, 0, img.getWidth(), img.getHeight());
    }
}
