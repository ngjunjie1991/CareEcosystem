package com.ucsf.core_phone.ui.widgets;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ucsf.core.data.Entry;
import com.ucsf.core_phone.R;
import com.ucsf.core_phone.ui.Theme;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;


/**
 * Base activity for almost all the screens of the application.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public abstract class AppScreen extends Activity implements AppComponent {
    private ViewHolder mHolder;
    private boolean mOrientationLocked = true;

    @Override
    public Theme getViewTheme() {
        return mHolder.theme;
    }

    @Override
    public Context getViewContext() {
        return this;
    }

    /**
     * Returns the view header.
     */
    protected HeaderView getHeader() {
        return mHolder.header;
    }

    /**
     * Returns the centralLayout.
     */
    protected ViewGroup getCentralLayout() {
        return mHolder.centralLayout;
    }

    /**
     * Set the header title.
     */
    public void setTitle(int textId) {
        mHolder.setTitle(getResources().getString(textId));
    }

    /**
     * Set the header title.
     */
    protected void setTitle(String text) {
        mHolder.setTitle(text);
    }

    /**
     * Add a menu button with the given text and OnClickListener.
     */
    protected Button addMenuButton(int textId, View.OnClickListener listener) {
        return mHolder.addMenuButton(getResources().getString(textId), -1, listener);
    }

    /**
     * Add a menu button with the given text and OnClickListener.
     */
    public Button addMenuButton(int textId, int index, View.OnClickListener listener) {
        return mHolder.addMenuButton(getResources().getString(textId), index, listener);
    }

    /**
     * Add a small frame in the central layout containing the given text.
     */
    protected TextView addInstruction(int textId) {
        return mHolder.addInstruction(getResources().getString(textId), -1);
    }

    /**
     * Add a small frame in the central layout containing the given text.
     */
    protected TextView addInstruction(String text) {
        return mHolder.addInstruction(text, -1);
    }

    /**
     * Add a frame in the central layout containing the given text.
     */
    protected TextView addMessage(int textId) {
        return mHolder.addMessage(getResources().getString(textId), -1);
    }

    /**
     * Add a frame in the central layout containing the given text.
     */
    protected TextView addMessage(String text) {
        return mHolder.addMessage(text, -1);
    }

    /**
     * Add a frame in the central layout containing the given text.
     */
    public TextView addInstruction(int textId, int index) {
        return mHolder.addInstruction(getResources().getString(textId), index);
    }

    /**
     * Add an instruction message and an edit text into the central layout.
     */
    protected EditText addInputMessagePrompt(int messageId, int inputType) {
        return mHolder.addInputMessagePrompt(getResources().getString(messageId), "", inputType, -1);
    }

    /**
     * Add an instruction message and an edit text into the central layout.
     */
    protected EditText addInputMessagePrompt(int messageId, int unitId, int inputType) {
        return mHolder.addInputMessagePrompt(getResources().getString(messageId),
                getResources().getString(unitId), inputType, -1);
    }

    /**
     * Add an instruction message and an edit text into the central layout.
     * If the unit should not be displayed, pass R.id.null_id
     */
    public EditText addInputMessagePrompt(int messageId, int unitId, int inputType, int index) {
        if (unitId == R.id.null_id)
            return mHolder.addInputMessagePrompt(getResources().getString(messageId), "",
                    inputType, index);
        else
            return mHolder.addInputMessagePrompt(getResources().getString(messageId),
                    getResources().getString(unitId), inputType, index);
    }

    /**
     * Add a list defined by the given adapter into the central layout.
     */
    protected ListView addList(ListAdapter adapter) {
        return mHolder.addList(adapter, -1);
    }

    /**
     * Add a list defined by the given adapter into the central layout.
     */
    public ListView addList(ListAdapter adapter, int index) {
        return mHolder.addList(adapter, index);
    }

    /**
     * Add an expandable list defined by the given adapter into the central layout.
     */
    protected ExpandableListView addExpandableList(ExpandableListAdapter adapter) {
        return mHolder.addExpandableList(adapter, -1);
    }

    /**
     * Add an expandable list defined by the given adapter into the central layout.
     */
    public ExpandableListView addExpandableList(ExpandableListAdapter adapter, int index) {
        return mHolder.addExpandableList(adapter, index);
    }

    /**
     * Add the given view to the central layout.
     */
    protected View addView(View view) {
        return mHolder.addView(view, -1);
    }

    /**
     * Add the given view to the central layout.
     */
    public View addView(View view, int index) {
        return mHolder.addView(view, index);
    }

    /**
     * Set the text and OnClickListener of the back button.
     */
    protected Button addFooterButton(int textId, View.OnClickListener listener) {
        return mHolder.addFooterButton(getResources().getString(textId), listener);
    }

    /**
     * Method responsible of the header icon status. To override.
     */
    protected boolean isStatusValid() {
        return true;
    }

    protected Intent onCreate(Bundle savedInstanceState, Disposition disposition, Theme theme) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.screen_layout);
        mHolder = new ViewHolder(disposition, theme);
        lockOrientation(true);

        // Restore saved intents
        if (savedInstanceState != null) {
            Intent intent = savedInstanceState.getParcelable("SAVE_INIT_INTENT");
            if (intent != null)
                getIntent().fillIn(intent, Intent.FILL_IN_DATA);
        }

        return getIntent();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("SAVE_INIT_INTENT", getIntent());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mHolder.header.updateStatusIcon(isStatusValid());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHolder.header.updateStatusIcon(isStatusValid());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mHolder.header.setOrientation(newConfig.orientation);
    }

    protected void lockOrientation(boolean lock) {
        if ((mOrientationLocked = lock))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        onConfigurationChanged(getResources().getConfiguration());
    }

    /**
     * Opens the given activity as a child activity.
     */
    protected void openChildActivity(Class<? extends Activity> activity, Entry... entries) {
        Intent intent = new Intent(this, activity);
        writeEntriesToIntent(intent, entries);
        startActivity(intent);
    }

    /**
     * Goes back to the given parent activity. Finishes the current one.
     */
    protected void goBackToParentActivity(Class<? extends Activity> parentActivity, Entry... entries) {
        Intent intent = new Intent(this, parentActivity);
        writeEntriesToIntent(intent, entries);
        startActivity(intent);
        finish();
    }

    private void writeEntriesToIntent(Intent intent, Entry[] entries) {
        for (Entry entry : entries) {
            if (entry.value instanceof Bundle)
                intent.putExtra(entry.tag, (Bundle) entry.value);
            else if (entry.value instanceof Serializable)
                intent.putExtra(entry.tag, (Serializable) entry.value);
        }
    }

    protected enum Disposition {
        Centered,     // The elements are displayed from top to bottom, around the center of the screen
        LastExpanded, // The elements are displayed from top to bottom, the last element occupies all the remaining space
    }

    public class ViewHolder {
        public final Disposition disposition;
        public final Theme theme;
        public final HeaderView header;
        public final RelativeLayout mainLayout;
        public final ViewGroup centralLayout;
        public final List<View> components = new LinkedList<>();
        public final Button[] footerButtons = new Button[3];
        public int menuButtonCount = 0;
        public int footerButtonCount = 0;

        public ViewHolder(Disposition disposition, Theme theme) {
            this.disposition = disposition;
            this.theme = theme;
            this.header = (HeaderView) findViewById(R.id.header);
            this.mainLayout = (RelativeLayout) findViewById(R.id.main_layout);

            ViewGroup layout = null;
            switch (disposition) {
                case Centered:
                    layout = new LinearLayout(AppScreen.this);
                    ((LinearLayout) layout).setOrientation(LinearLayout.VERTICAL);
                    ((LinearLayout) layout).setGravity(Gravity.CENTER);
                    break;
                case LastExpanded:
                    layout = new RelativeLayout(AppScreen.this);
                    break;
            }
            this.centralLayout = layout;
            this.mainLayout.addView(this.centralLayout);

            int margin = (int) getResources().getDimension(R.dimen.components_margin);
            RelativeLayout.LayoutParams params =
                    (RelativeLayout.LayoutParams) this.centralLayout.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.addRule(RelativeLayout.BELOW, this.header.getId());
            params.setMargins(margin, 0, margin, 0);
        }

        public void setTitle(String title) {
            header.setTitle(title);
        }

        public Button addMenuButton(String text, int index, View.OnClickListener listener) {
            boolean odd = ((++menuButtonCount) % 2) == 0;
            MenuButton button = new MenuButton(AppScreen.this, odd, !odd);
            if (odd)
                button.setTextColor(getResources().getColor(R.color.text_color));
            button.setText(text);
            button.setOnClickListener(listener);

            addView(button, index);

            int margin = (int) getResources().getDimension(R.dimen.components_margin);
            int sideMargin = (int) getResources().getDimension(R.dimen.menu_list_margin);
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) button.getLayoutParams();
            params.setMargins(sideMargin, margin, sideMargin, margin);

            return button;
        }

        public TextView addInstruction(String text, int index) {
            int padding = (int) getResources().getDimension(R.dimen.message_padding);

            Drawable background = getResources().getDrawable(R.drawable.plain_button);
            assert background != null;
            background.setColorFilter(theme.getBackgroundColor(AppScreen.this),
                    PorterDuff.Mode.SRC_ATOP);

            TextView label = new TextView(AppScreen.this);
            label.setText(text);
            label.setTextColor(getResources().getColor(R.color.text_color));
            label.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimension(R.dimen.small_text_size));
            label.setGravity(Gravity.CENTER);
            label.setPadding(padding, 0, padding, 0);
            label.setBackground(background);

            addView(label, index);

            int margin = (int) getResources().getDimension(R.dimen.small_components_margin);
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) label.getLayoutParams();
            params.setMargins(margin, margin, margin, margin);

            return label;
        }

        public TextView addMessage(String text, int index) {
            int padding = (int) (2 * getResources().getDimension(R.dimen.components_margin));

            Drawable background = getResources().getDrawable(R.drawable.plain_button);
            assert background != null;
            background.setColorFilter(theme.getBackgroundColor(AppScreen.this),
                    PorterDuff.Mode.SRC_ATOP);

            TextView label = new TextView(AppScreen.this);
            label.setText(text);
            label.setTextColor(getResources().getColor(R.color.text_color));
            label.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimension(R.dimen.text_size));
            label.setGravity(Gravity.CENTER);
            label.setPadding(padding, 2 * padding, padding, 2 * padding);
            label.setBackground(background);

            addView(label, index);

            int margin = (int) getResources().getDimension(R.dimen.small_message_margin);
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) label.getLayoutParams();
            params.setMargins(margin, margin, margin, margin);

            return label;
        }

        public EditText addInputMessagePrompt(String message, String unit, int inputType, int index) {
            LayoutInflater inflater = (LayoutInflater)
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = inflater.inflate(R.layout.input_message_prompt,
                    centralLayout, false);
            TextView messageLabel = (TextView) view.findViewById(R.id.instructions);
            ViewGroup textLayout = (ViewGroup) view.findViewById(R.id.text_field_layout);
            EditText textField = (EditText) view.findViewById(R.id.text_field);
            TextView unitLabel = (TextView) view.findViewById(R.id.unit_label);
            ViewGroup layout = (ViewGroup) view.findViewById(R.id.layout);

            messageLabel.setText(message);
            textField.setInputType(inputType);

            if (unit == null || unit.isEmpty())
                unitLabel.setVisibility(View.GONE);
            else
                unitLabel.setText(unit);

            textLayout.getBackground().setColorFilter(theme.getBackgroundColor(AppScreen.this),
                    PorterDuff.Mode.SCREEN);
            layout.getBackground().setColorFilter(theme.getBackgroundColor(AppScreen.this),
                    PorterDuff.Mode.SRC_ATOP);

            addView(view, index);

            return textField;
        }

        public ListView addList(ListAdapter adapter, int index) {
            ListView list = new ListView(AppScreen.this);
            list.setDivider(null);
            list.setAdapter(adapter);

            addView(list, index);

            return list;
        }

        public ExpandableListView addExpandableList(ExpandableListAdapter adapter, int index) {
            ExpandableListView list = new ExpandableListView(AppScreen.this);
            list.setDivider(null);
            list.setChildDivider(null);
            list.setAdapter(adapter);

            addView(list, index);

            return list;
        }

        public View addView(View view, int index) {
            centralLayout.addView(view, index);
            if (index < 0 || index > components.size())
                index = components.size();
            components.add(index, view);
            view.setId(UUID.randomUUID().hashCode());

            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;

            updateComponents(view, index);

            return view;
        }

        public Button addFooterButton(String text, View.OnClickListener listener) {
            Button button = footerButtons[footerButtonCount++] =
                    new MenuButton(AppScreen.this, Theme.Default);
            button.setText(text);
            button.setOnClickListener(listener);
            button.setMinWidth((int) getResources().getDimension(R.dimen.nav_button_min_width));
            mainLayout.addView(button);
            updateFooterButtons();
            return button;
        }

        void updateComponents(View view, int index) {
            switch (disposition) {
                case Centered:
                    break;
                case LastExpanded:
                    View prev = index == 0 ? null : components.get(index - 1);
                    View next = index == components.size() - 1 ? null : components.get(index + 1);
                    RelativeLayout.LayoutParams params =
                            (RelativeLayout.LayoutParams) view.getLayoutParams();

                    if (prev == null)
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                    else {
                        params.addRule(RelativeLayout.BELOW, prev.getId());
                        ((RelativeLayout.LayoutParams) prev.getLayoutParams())
                                .removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    }

                    if (next == null)
                        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    else {
                        RelativeLayout.LayoutParams nextParams =
                                (RelativeLayout.LayoutParams) next.getLayoutParams();
                        nextParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
                        nextParams.addRule(RelativeLayout.BELOW, view.getId());
                    }
                    break;
            }
        }

        void updateFooterButtons() {
            int margin = (int) getResources().getDimension(R.dimen.nav_button_margin);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT),
                    p1, p2, p3;
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.setMargins(margin, margin, margin, margin);

            switch (footerButtonCount) {
                case 1:
                    params.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    footerButtons[0].setLayoutParams(params);
                    footerButtons[0].setId(android.R.id.hint);
                    break;
                case 2:
                    p1 = new RelativeLayout.LayoutParams(params);
                    p1.addRule(RelativeLayout.ALIGN_PARENT_START);
                    footerButtons[0].setLayoutParams(p1);
                    p2 = new RelativeLayout.LayoutParams(params);
                    p2.addRule(RelativeLayout.ALIGN_PARENT_END);
                    footerButtons[1].setLayoutParams(p2);
                    break;
                case 3:
                    p1 = new RelativeLayout.LayoutParams(params);
                    p1.addRule(RelativeLayout.ALIGN_PARENT_START);
                    footerButtons[0].setLayoutParams(p1);
                    p2 = new RelativeLayout.LayoutParams(params);
                    p2.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    footerButtons[1].setLayoutParams(p2);
                    p3 = new RelativeLayout.LayoutParams(params);
                    p3.addRule(RelativeLayout.ALIGN_PARENT_END);
                    footerButtons[2].setLayoutParams(p3);
                    break;
            }

            // Make sure that the central layout is above the footer buttons
            updateCentralLayoutPosition();
        }

        void updateCentralLayoutPosition() {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    (RelativeLayout.LayoutParams) centralLayout.getLayoutParams());
            params.addRule(RelativeLayout.ABOVE, footerButtons[0].getId());
            centralLayout.setLayoutParams(params);
        }

    }
}
