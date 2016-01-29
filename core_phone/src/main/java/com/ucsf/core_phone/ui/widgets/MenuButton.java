package com.ucsf.core_phone.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;

import com.ucsf.core_phone.R;
import com.ucsf.core_phone.ui.Theme;


/**
 * Custom button class (with frame/background color depending on the screen theme).
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class MenuButton extends Button implements AppComponent {
    private static final boolean DEFAULT_SHOW_BACKGROUND = true;
    private static final boolean DEFAULT_SHOW_BORDERS = false;
    private static final Theme DEFAULT_THEME = Theme.Default;

    private Theme mTheme;
    private int mAppearance;
    private int mBackgroundColor;
    private int mBordersColor;

    public MenuButton(Context context) {
        this(context, getContextTheme(context), DEFAULT_SHOW_BACKGROUND, DEFAULT_SHOW_BORDERS);
    }

    public MenuButton(Context context, Theme theme) {
        this(context, theme, DEFAULT_SHOW_BACKGROUND, DEFAULT_SHOW_BORDERS);
    }

    public MenuButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public MenuButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public MenuButton(AppComponent parent, boolean showBackground, boolean showBorders) {
        this(parent.getViewContext(), parent.getViewTheme(), showBackground, showBorders);
    }

    private MenuButton(Context context, Theme theme, boolean showBackground, boolean showBorders) {
        super(context);
        mTheme = theme;
        mBackgroundColor = mBordersColor = getViewTheme().getBackgroundColor(getViewContext());
        setTextColor(getViewTheme().getForegroundColor(getContext()));
        init(showBackground, showBorders);
    }

    private static Theme getContextTheme(Context context) {
        return getContextTheme(context, Theme.Default);
    }

    private static Theme getContextTheme(Context context, Theme defaultTheme) {
        if (context instanceof AppComponent)
            return ((AppComponent) context).getViewTheme();
        return defaultTheme;
    }

    private void init(AttributeSet attrs) {
        TypedArray attributes = getContext().obtainStyledAttributes(attrs, R.styleable.MenuButton);
        boolean showBackground = attributes.getBoolean(R.styleable.MenuButton_show_background,
                DEFAULT_SHOW_BACKGROUND);
        boolean showBorders = attributes.getBoolean(R.styleable.MenuButton_show_borders,
                DEFAULT_SHOW_BORDERS);

        mTheme = Theme.valueOf(attributes.getInt(R.styleable.MenuButton_button_theme,
                getContextTheme(getContext()).getId()));

        mBackgroundColor = attributes.getColor(R.styleable.MenuButton_background_color,
                getViewTheme().getBackgroundColor(getContext()));
        mBordersColor = attributes.getColor(R.styleable.MenuButton_borders_color,
                getViewTheme().getBackgroundColor(getContext()));
        setTextColor(attributes.getColor(R.styleable.MenuButton_text_color,
                getViewTheme().getForegroundColor(getContext())));

        attributes.recycle();
        init(showBackground, showBorders);
    }

    private void init(boolean showBackground, boolean showBorders) {
        setGravity(Gravity.CENTER);
        setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.button_text_size));
        setAllCaps(false);

        mAppearance = (showBackground ? 0x01 : 0x00) + (showBorders ? 0x10 : 0x00);

        update();
    }

    public void enableBackground(boolean enabled) {
        if (enabled)
            mAppearance |= 0x01;
        else
            mAppearance &= 0x01;
        update();
    }

    public void enabledBorders(boolean enabled) {
        if (enabled)
            mAppearance |= 0x10;
        else
            mAppearance &= 0x10;
        update();
    }

    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
        update();
    }

    public void setBordersColor(int color) {
        mBordersColor = color;
        update();
    }

    private void update() {
        Drawable borders, background;
        switch (mAppearance) {
            case 0x10: // Borders only
                borders = getResources().getDrawable(R.drawable.frame_button);
                assert borders != null;
                borders.setColorFilter(mBordersColor, PorterDuff.Mode.SRC_ATOP);
                setBackground(borders);
                break;
            case 0x01: // Background only
                background = getResources().getDrawable(R.drawable.plain_button);
                assert background != null;
                background.setColorFilter(mBackgroundColor, PorterDuff.Mode.SRC_ATOP);
                setBackground(background);
                break;
            case 0x11: // Background and borders
                borders = getResources().getDrawable(R.drawable.frame_button);
                assert borders != null;
                borders.setColorFilter(mBordersColor, PorterDuff.Mode.SRC_ATOP);
                background = getResources().getDrawable(R.drawable.plain_button);
                assert background != null;
                background.setColorFilter(mBackgroundColor, PorterDuff.Mode.SRC_ATOP);
                setBackground(new LayerDrawable(new Drawable[]{background, borders}));
                break;
        }
    }

    @Override
    public Theme getViewTheme() {
        return mTheme;
    }

    @Override
    public Context getViewContext() {
        return getContext();
    }
}
