package com.ucsf.core_phone.ui.widgets;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ucsf.core_phone.R;
import com.ucsf.core_phone.ui.Theme;

/**
 * Custom dialog popup window.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class CustomDialog extends Dialog implements AppComponent {
    private final AppComponent mParent;

    CustomDialog(AppComponent parent) {
        super(parent.getViewContext());
        mParent = parent;
    }

    @Override
    public Theme getViewTheme() {
        return mParent.getViewTheme();
    }

    @Override
    public Context getViewContext() {
        return getContext();
    }

    /**
     * Helper class for creating a custom dialog.
     */
    public static class Builder {
        private final AppComponent mParent;
        private final FooterButton[] mFooterButtons = new FooterButton[3];
        private boolean mCancelable = true;
        private String mTitle = null;
        private String mMessage = null;
        private View mView = null;
        private int mFooterButtonCount = 0;
        private OnCancelListener mOnCancelListener = null;
        private DialogInterface.OnDismissListener mListener = null;

        public Builder(AppComponent parent) {
            mParent = parent;
        }

        /**
         * Creates and display the dialog.
         */
        public CustomDialog show() {
            CustomDialog dialog = create();
            dialog.show();
            return dialog;
        }

        /**
         * Authorize or not cancellation.
         */
        public Builder setCancelable(boolean cancelable) {
            mCancelable = cancelable;
            return this;
        }

        /**
         * set the OnCancelListener. Is is called before the dialog is dismissed.
         */
        public Builder setOnCancelListener(OnCancelListener listener) {
            mOnCancelListener = listener;
            return this;
        }

        /**
         * Set the dialog title.
         */
        public Builder setTitle(String title) {
            mTitle = title;
            return this;
        }

        /**
         * Set the dialog title.
         */
        public Builder setTitle(int textId) {
            return setTitle(mParent.getViewContext().getResources().getString(textId));
        }

        /**
         * Set the dialog message.
         */
        public Builder setMessage(String text) {
            mMessage = text;
            return this;
        }

        /**
         * Set the dialog message.
         */
        public Builder setMessage(int textId) {
            return setMessage(mParent.getViewContext().getResources().getString(textId));
        }

        /**
         * Set the internal view of the dialog. This custom view is ignored if a message is set.
         */
        public Builder setView(View view) {
            mView = view;
            return this;
        }

        /**
         * Add a footer button using the given text and OnClickListener (which may be null).
         * Remark that this button will dismiss the dialog.
         */
        public Builder addFooterButton(String text, OnClickListener listener) {
            mFooterButtons[mFooterButtonCount++] = new FooterButton(text, listener);
            return this;
        }

        /**
         * Add a footer button using the given text and OnClickListener (which may be null).
         * Remark that this button will dismiss the dialog.
         */
        public Builder addFooterButton(int textId, OnClickListener listener) {
            return addFooterButton(mParent.getViewContext().getResources().getString(textId),
                    listener);
        }

        /**
         * Set the dismiss listener.
         */
        public Builder setOnDismissListener(DialogInterface.OnDismissListener listener) {
            mListener = listener;
            return this;
        }

        /**
         * Create the custom dialog
         */
        public CustomDialog create() {
            final CustomDialog dialog = new CustomDialog(mParent);
            init(dialog);
            return dialog;
        }

        /**
         * Initialize the given custom dialog.
         */
        void init(CustomDialog dialog) {
            LayoutInflater inflater = (LayoutInflater) mParent.getViewContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = inflater.inflate(R.layout.custom_dialog_layout, null);
            RelativeLayout layout = (RelativeLayout) view.findViewById(R.id.main_layout);

            // Set the background color
            view.setBackgroundColor(mParent.getViewTheme().getBackgroundColor(mParent.getViewContext()));

            // Set the title
            TextView titleLabel = (TextView) view.findViewById(R.id.title);
            if (mTitle != null) {
                titleLabel.setText(mTitle);
                titleLabel.setTextColor(mParent.getViewTheme().getForegroundColor(mParent.getViewContext()));
            } else
                titleLabel.setVisibility(View.GONE);

            // Set the internal view
            int margin = (int) mParent.getViewContext().getResources().getDimension(R.dimen.components_margin);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(margin, margin, margin, 0);
            params.addRule(RelativeLayout.CENTER_HORIZONTAL);
            params.addRule(RelativeLayout.BELOW, titleLabel.getId());

            View internalView;
            int viewMargin = margin;
            if (mMessage != null) {
                TextView messageLabel = new TextView(mParent.getViewContext());
                messageLabel.setText(mMessage);
                messageLabel.setGravity(Gravity.CENTER);
                messageLabel.setTextColor(mParent.getViewContext().getResources().getColor(R.color.text_color));
                layout.addView(messageLabel, params);
                internalView = messageLabel;
            } else if (mView != null) {
                layout.addView(mView, params);
                internalView = mView;
            } else {
                Log.e("CustomDialog", "No internal view provided!");
                return;
            }
            internalView.setId(R.id.internal_view);

            // Set the footer buttons
            margin = (int) mParent.getViewContext().getResources().getDimension(R.dimen.nav_button_margin);
            params = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            RelativeLayout.LayoutParams p1, p2, p3;
            params.addRule(RelativeLayout.BELOW, internalView.getId());
            params.setMargins(margin, margin, margin, margin);

            switch (mFooterButtonCount) {
                case 1:
                    params.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    addFooterButton(dialog, 0, layout, params);
                    break;
                case 2:
                    p1 = new RelativeLayout.LayoutParams(params);
                    p1.addRule(RelativeLayout.ALIGN_PARENT_START);
                    addFooterButton(dialog, 0, layout, p1);
                    p2 = new RelativeLayout.LayoutParams(params);
                    p2.setMargins(margin, margin, margin - viewMargin, margin); // Use the internal view as reference because bugs otherwise
                    p2.addRule(RelativeLayout.ALIGN_RIGHT, internalView.getId());
                    addFooterButton(dialog, 1, layout, p2);
                    break;
                case 3:
                    p1 = new RelativeLayout.LayoutParams(params);
                    p1.addRule(RelativeLayout.ALIGN_PARENT_START);
                    addFooterButton(dialog, 0, layout, p1);
                    p2 = new RelativeLayout.LayoutParams(params);
                    p2.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    addFooterButton(dialog, 1, layout, p2);
                    p3 = new RelativeLayout.LayoutParams(params);
                    p3.setMargins(margin, margin, margin - viewMargin, margin); // Use the internal view as reference because bugs otherwise
                    p3.addRule(RelativeLayout.ALIGN_RIGHT, internalView.getId());
                    addFooterButton(dialog, 2, layout, p3);
                    break;
            }

            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.addContentView(layout, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            if (mListener != null)
                dialog.setOnDismissListener(mListener);
            dialog.setCancelable(mCancelable);
            if (mOnCancelListener != null)
                dialog.setOnCancelListener(mOnCancelListener);
        }

        private void addFooterButton(final CustomDialog dialog,
                                     final int index, RelativeLayout layout,
                                     RelativeLayout.LayoutParams params) {
            final FooterButton data = mFooterButtons[index];

            MenuButton button = new MenuButton(mParent, false, true);
            int foregroundColor = mParent.getViewContext().getResources().getColor(R.color.text_color);
            button.setTextColor(foregroundColor);
            button.setBordersColor(foregroundColor);
            button.setText(data.text);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (data.listener != null)
                        data.listener.onClick(dialog, index);
                    dialog.dismiss();
                }
            });
            button.setMinWidth((int) mParent.getViewContext().getResources().getDimension(R.dimen.menu_button_min_width));
            layout.addView(button, params);
        }

        private static class FooterButton {
            final String text;
            final OnClickListener listener;

            FooterButton(String text, OnClickListener listener) {
                this.text = text;
                this.listener = listener;
            }
        }
    }

}
