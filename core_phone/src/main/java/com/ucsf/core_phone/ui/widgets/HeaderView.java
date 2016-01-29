package com.ucsf.core_phone.ui.widgets;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ucsf.core_phone.R;


/**
 * Custom view to display the screen header. Mainly used to display if the services are enabled.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class HeaderView extends RelativeLayout {
    private TextView mTitleLabel;
    private ImageView mStatusIcon;
    private ImageView mLogo;
    private Drawable mActiveIcon;
    private Drawable mInactiveIcon;

    public HeaderView(Context context) {
        super(context, null);
        init(context, (String) null);
    }

    public HeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public HeaderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.HeaderView);
        String title = attributes.getString(R.styleable.HeaderView_header_title);
        attributes.recycle();
        init(context, title);
    }

    private void init(Context context, String title) {
        inflate(context, R.layout.header_view, this);

        mTitleLabel = (TextView) findViewById(R.id.label_screen_title);
        mStatusIcon = (ImageView) findViewById(R.id.image_view_status);
        mLogo = (ImageView) findViewById(R.id.image_view_screen_header);
        mActiveIcon = getResources().getDrawable(R.drawable.active);
        mInactiveIcon = getResources().getDrawable(R.drawable.inactive);

        setBackground(new ColorDrawable(getResources().getColor(R.color.background)));

        mTitleLabel.setText(title);
    }

    /**
     * Set the header title.
     */
    public void setTitle(String title) {
        mTitleLabel.setText(title);
    }

    /**
     * Update the status icon accordingly to the services status (running or not).
     */
    public void updateStatusIcon(boolean active) {
        if (active)
            mStatusIcon.setImageDrawable(mActiveIcon);
        else
            mStatusIcon.setImageDrawable(mInactiveIcon);
    }

    /**
     * Set the header orientation. On landscape mode, hide the logo.
     */
    public void setOrientation(int orientation) {
        switch (orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                mLogo.setVisibility(GONE);
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                mLogo.setVisibility(VISIBLE);
                break;
        }
    }

}
