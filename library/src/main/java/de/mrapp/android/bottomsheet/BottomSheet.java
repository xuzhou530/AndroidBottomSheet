/*
 * AndroidBottomSheet Copyright 2016 Michael Rapp
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package de.mrapp.android.bottomsheet;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.TextView;

import java.util.Collection;
import java.util.LinkedList;

import de.mrapp.android.bottomsheet.adapter.BottomSheetAdapter;
import de.mrapp.android.bottomsheet.model.MenuItem;
import de.mrapp.android.bottomsheet.model.Separator;
import de.mrapp.android.bottomsheet.view.DraggableView;

import static de.mrapp.android.util.Condition.ensureAtLeast;
import static de.mrapp.android.util.Condition.ensureAtMaximum;
import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * A bottom sheet, which is designed according to the Android 5's Material Design guidelines even on
 * pre-Lollipop devices. Such a bottom sheet appears at the bottom of the window and consists of a
 * title and multiple menu items. It is possible to customize the appearance of the bottom sheet or
 * to replace its title and menu items with custom views.
 *
 * For creating or showing such bottom sheets, the methods {@link Builder#create()} or {@link
 * Builder#show()} of the builder {@link de.mrapp.android.bottomsheet.BottomSheet.Builder} can be
 * used.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class BottomSheet extends Dialog implements DialogInterface, DraggableView.Callback {

    /**
     * A builder, which allows to create and show bottom sheets, which are designed according to
     * Android 5's Material Design guidelines even on pre-Lollipop devices. Such a bottom sheet
     * appears at the bottom of the window and consists of a title and multiple menu items. It is
     * possible to customize the appearance of the bottom sheet or to replace its title and menu
     * items with custom views.
     */
    public static class Builder {

        /**
         * The context, which is used by the builder.
         */
        private final Context context;

        /**
         * The resource id of the theme, which should be used by the bottom sheet, which is created
         * by the builder.
         */
        private final int themeResourceId;

        /**
         * True, if the dialog, which is created by the builder, should be cancelable, false
         * otherwise.
         */
        private boolean cancelable = true;

        /**
         * The style of the bottom sheet, which is created by the builder.
         */
        private Style style = Style.LIST;

        /**
         * The listener, which should be notified, when the bottom sheet, which is created by the
         * builder, is canceled.
         */
        private OnCancelListener cancelListener;

        /**
         * The listener, which should be notified, when the bottom sheet, which is created by the
         * builder, is dismissed.
         */
        private OnDismissListener dismissListener;

        /**
         * The listener, which should be notified, if a key is dispatched to the bottom sheet, which
         * is created by the builder.
         */
        private OnKeyListener keyListener;

        /**
         * The title of the bottom sheet, which is created by the builder.
         */
        private CharSequence title;

        /**
         * The icon of the bottom sheet, which is created by the builder.
         */
        private Drawable icon;

        /**
         * The color of the title of the bottom sheet, which is created by the builder.
         */
        private int titleColor = -1;

        /**
         * The color of the menu items of the bottom sheet, which is created by the builder.
         */
        private int itemColor = -1;

        /**
         * The background of the bottom sheet, which is created by the builder.
         */
        private Drawable background;

        /**
         * The custom content view of the bottom sheet, which is created by the builder.
         */
        private View customView;

        /**
         * The resource id of the custom content view of the bottom sheet, which is created by the
         * builder.
         */
        private int customViewId = -1;

        /**
         * The custom title view of the bottom sheet, which is created by the builder.
         */
        private View customTitleView;

        /**
         * The sensitivity, which specifies the distance after which dragging has an effect on the
         * bottom sheet
         */
        private float dragSensitivity = 0.25f;

        /**
         * The items of the bottom sheet, which is created by the builder.
         */
        private Collection<Parcelable> items = new LinkedList<>();

        /**
         * Inflates the bottom sheet's layout.
         *
         * @return The root view of the layout, which has been inflated, as an instance of the class
         * {@link DraggableView}
         */
        @SuppressWarnings("deprecation")
        private DraggableView inflateLayout() {
            DraggableView root = (DraggableView) View.inflate(context, R.layout.bottom_sheet, null);

            if (background != null) {
                root.setBackgroundDrawable(background);
            }

            if (style == Style.LIST) {
                int paddingTop = getContext().getResources()
                        .getDimensionPixelSize(R.dimen.bottom_sheet_list_padding_top);
                root.setPadding(0, paddingTop, 0, 0);
            } else {
                int paddingTop = getContext().getResources()
                        .getDimensionPixelSize(R.dimen.bottom_sheet_grid_padding_top);
                root.setPadding(0, paddingTop, 0, 0);
            }

            return root;
        }

        /**
         * Inflates the bottom sheet's title view, which may either be the default view or a custom
         * view, if one has been set before.
         *
         * @param root
         *         The root view of the bottom sheet's layout as an instance of the class {@link
         *         DraggableView}
         */
        private void inflateTitleView(@NonNull final DraggableView root) {
            ViewGroup titleContainer = (ViewGroup) root.findViewById(R.id.title_container);

            if (customTitleView != null) {
                titleContainer.setVisibility(View.VISIBLE);
                titleContainer.addView(customTitleView, ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            } else {
                View.inflate(context, R.layout.bottom_sheet_title, titleContainer);

                if (style == Style.LIST) {
                    int padding = getContext().getResources().getDimensionPixelSize(
                            R.dimen.bottom_sheet_list_item_horizontal_padding);
                    titleContainer.setPadding(padding, 0, padding, 0);
                } else {
                    int padding = getContext().getResources().getDimensionPixelSize(
                            R.dimen.bottom_sheet_grid_item_horizontal_padding);
                    titleContainer.setPadding(padding, 0, padding, 0);
                }
            }

            initializeTitle(root, titleContainer);
        }

        /**
         * Initializes the bottom sheet's title and icon.
         *
         * @param root
         *         The root view of the bottom sheet's layout as an instance of the class {@link
         *         DraggableView}
         * @param titleContainer
         *         The parent view of the title view as an instance of the class {@link ViewGroup}
         */
        private void initializeTitle(@NonNull final DraggableView root,
                                     @NonNull final ViewGroup titleContainer) {
            View titleView = titleContainer.findViewById(android.R.id.title);

            if (titleView != null && titleView instanceof TextView) {
                TextView titleTextView = (TextView) titleView;

                if (titleColor != -1) {
                    titleTextView.setTextColor(titleColor);
                }

                if (!TextUtils.isEmpty(title) || icon != null) {
                    titleContainer.setVisibility(View.VISIBLE);
                    titleTextView.setText(title);
                    root.setPadding(root.getPaddingLeft(), 0, root.getPaddingRight(),
                            root.getPaddingBottom());

                    if (icon != null) {
                        titleTextView
                                .setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
                    }

                }
            }
        }

        /**
         * Inflates the bottom sheet's content view, which may either be the default view or a
         * custom view, if one has been set before.
         *
         * @param root
         *         The root view of the bottom sheet's layout as an instance of the class {@link
         *         DraggableView}
         * @return The grid view, which can be used to show the bottom sheet's items, as an instance
         * of the class {@link GridView} or null, if the bottom sheet contains a custom view
         * instead
         */
        private GridView inflateContentView(@NonNull final DraggableView root) {
            ViewGroup contentContainer = (ViewGroup) root.findViewById(R.id.content_container);

            if (customView != null) {
                contentContainer.setVisibility(View.VISIBLE);
                contentContainer.addView(customView, ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            } else if (customViewId != -1) {
                contentContainer.setVisibility(View.VISIBLE);
                View.inflate(context, customViewId, contentContainer);
            } else {
                View.inflate(context, R.layout.bottom_sheet_grid_view, contentContainer);
            }

            return initializeContent(contentContainer);
        }

        /**
         * Initializes the bottom sheet's content.
         *
         * @param contentContainer
         *         The parent view of the content view as an instance of the class  {@link
         *         ViewGroup}
         * @return The grid view, which can be used to show the bottom sheet's items, as an instance
         * of the class {@link GridView} or null, if the bottom sheet contains a custom view
         * instead
         */
        private GridView initializeContent(@NonNull final ViewGroup contentContainer) {
            GridView gridView =
                    (GridView) contentContainer.findViewById(R.id.bottom_sheet_grid_view);

            if (gridView != null) {
                contentContainer.setVisibility(View.VISIBLE);
                gridView.setNumColumns(1);

                if (style == Style.LIST) {
                    int paddingBottom = getContext().getResources()
                            .getDimensionPixelSize(R.dimen.bottom_sheet_list_padding_bottom);
                    gridView.setPadding(0, 0, 0, paddingBottom);
                } else {
                    int paddingBottom = getContext().getResources()
                            .getDimensionPixelSize(R.dimen.bottom_sheet_grid_padding_bottom);
                    gridView.setPadding(0, 0, 0, paddingBottom);
                }
            }

            return gridView;
        }

        /**
         * Creates a new builder, which allows to create bottom sheets, which are designed according
         * to Android 5's Material Design guidelines even on pre-Lollipop devices.
         *
         * @param context
         *         The context, which should be used by the builder, as an instance of the class
         *         {@link Context}. The context may not be null
         */
        public Builder(@NonNull final Context context) {
            this(context, -1);
        }

        /**
         * Creates a new builder, which allows to create bottom sheets, which are designed according
         * to Android 5's Material Design guidelines even on pre-Lollipop devices.
         *
         * @param context
         *         The context, which should be used by the builder, as an instance of the class
         *         {@link Context}. The context may not be null
         * @param themeResourceId
         *         The resource id of the theme, which should be used by the bottom sheet, as an
         *         {@link Integer} value. The resource id must correspond to a valid theme
         */
        public Builder(@NonNull final Context context, @StyleRes final int themeResourceId) {
            ensureNotNull(context, "The context may not be null");
            this.context = context;
            this.themeResourceId = themeResourceId;
        }

        /**
         * Returns the context, which is used by the builder.
         *
         * @return The context, which is used by the builder, as an instance of the class {@link
         * Context}
         */
        public final Context getContext() {
            return context;
        }

        /**
         * Sets, whether the bottom sheet, which is created by the builder, should be cancelable, or
         * not.
         *
         * @param cancelable
         *         True, if the bottom sheet, which is created by the builder, should be cancelable,
         *         false otherwise
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder setCancelable(final boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        /**
         * Sets the style of the bottom sheet, which is created by the builder.
         *
         * @param style
         *         The style, which should be set, as a value of the enum {@link Style}. The value
         *         may either be <code>List</code> or <code>GRID</code>
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder setStyle(@NonNull final Style style) {
            ensureNotNull(style, "The style may not be null");
            this.style = style;
            return this;
        }

        /**
         * Sets the listener, which should be notified, when the bottom sheet, which is created by
         * the builder, is canceled.
         *
         * If you are interested in listening for all cases where the bottom sheet is dismissed and
         * not just when it is canceled, see {@link #setOnDismissListener(android.content.DialogInterface.OnDismissListener)
         * setOnDismissListener}.
         *
         * @param listener
         *         The listener, which should be set, as an instance of the type {@link
         *         OnCancelListener}, or null, if no listener should be set
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         * @see #setOnDismissListener(android.content.DialogInterface.OnDismissListener)
         */
        public Builder setOnCancelListener(@Nullable final OnCancelListener listener) {
            this.cancelListener = listener;
            return this;
        }

        /**
         * Sets the listener, which should be notified, when the bottom sheet, which is created by
         * the builder, is dismissed for any reason.
         *
         * @param listener
         *         The listener, which should be set, as an instance of the type {@link
         *         OnDismissListener}, or null, if no listener should be set
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder setOnDismissListener(@Nullable final OnDismissListener listener) {
            this.dismissListener = listener;
            return this;
        }

        /**
         * Sets the listener, which should be notified, if a key is dispatched to the bottom sheet,
         * which is created by the builder.
         *
         * @param listener
         *         The listener, which should be set, as an instance of the type {@link
         *         OnKeyListener}, or null, if no listener should be set
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder setOnKeyListener(@Nullable final OnKeyListener listener) {
            this.keyListener = listener;
            return this;
        }

        /**
         * Sets the color of the title of the bottom sheet, which is created by the builder.
         *
         * @param color
         *         The color, which should be set, as an {@link Integer} value or -1, if no custom
         *         color should be set
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder setTitleColor(@ColorInt final int color) {
            this.titleColor = color;
            return this;
        }

        /**
         * Sets the color of the menu items of the bottom sheet, which is created by the builder.
         *
         * @param color
         *         The color, which should be set, as an {@link Integer} value or -1, if no custom
         *         color should be set
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder setItemColor(@ColorInt final int color) {
            this.itemColor = color;
            return this;
        }

        /**
         * Sets the background of the bottom sheet, which is created by the builder.
         *
         * @param background
         *         The background, which should be set, as an instance of the class {@link Drawable}
         *         or null, if no custom background should be set
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder setBackground(@Nullable final Drawable background) {
            this.background = background;
            return this;
        }

        /**
         * Sets the background of the bottom sheet, which is created by the builder.
         *
         * @param resourceId
         *         The resource id of the background, which should be set, as an {@link Integer}
         *         value. The resource id must correspond to a valid drawable resource
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        @SuppressWarnings("deprecation")
        public final Builder setBackground(@DrawableRes final int resourceId) {
            this.background = context.getResources().getDrawable(resourceId);
            return this;
        }

        /**
         * Sets the background color of the bottom sheet, which is created by the builder.
         *
         * @param color
         *         The background color, which should be set, as an {@link Integer} value or -1, if
         *         no custom background color should be set
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder setBackgroundColor(@ColorInt final int color) {
            this.background = color != -1 ? new ColorDrawable(color) : null;
            return this;
        }

        /**
         * Sets the title of the bottom sheet, which is created by the builder.
         *
         * @param title
         *         The title, which should be set, as an instance of the type {@link CharSequence}
         *         or null, if no title should be shown
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder setTitle(@Nullable final CharSequence title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the title of the bottom sheet, which is created by the builder.
         *
         * @param resourceId
         *         The resource id of the title, which should be set, as an {@link Integer} value.
         *         The resource id must correspond to a valid string resource
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder setTitle(@StringRes final int resourceId) {
            return setTitle(context.getText(resourceId));
        }

        /**
         * Sets the icon of the bottom sheet, which is created by the builder.
         *
         * @param icon
         *         The icon, which should be set, as an instance of the class {@link Drawable} or
         *         null, if no icon should be shown
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder setIcon(@Nullable final Drawable icon) {
            this.icon = icon;
            return this;
        }

        /**
         * Sets the icon of the bottom sheet, which is created by the builder.
         *
         * @param resourceId
         *         The resource id of the icon, which should be set, as an {@link Integer} value.
         *         The resource id must correspond to a valid drawable resource
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        @SuppressWarnings("deprecation")
        public final Builder setIcon(@DrawableRes final int resourceId) {
            return setIcon(context.getResources().getDrawable(resourceId));
        }

        /**
         * Set the icon of the bottom sheet, which is created by the builder.
         *
         * @param attributeId
         *         The id of the theme attribute, which supplies the icon, which should be set, as
         *         an {@link Integer} value. The id must point to a valid drawable resource
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder setIconAttribute(@AttrRes final int attributeId) {
            TypedArray typedArray =
                    context.getTheme().obtainStyledAttributes(new int[]{attributeId});
            return setIcon(typedArray.getDrawable(0));
        }

        /**
         * Sets the custom view, which should be shown by the bottom sheet, which is created by the
         * builder.
         *
         * @param view
         *         The view, which should be set, as an instance of the class {@link View} or null,
         *         if no custom view should be shown
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder setView(@Nullable final View view) {
            customView = view;
            customViewId = 0;
            return this;
        }

        /**
         * Sets the custom view, which should be shown by the bottom sheet, which is created by the
         * builder.
         *
         * @param resourceId
         *         The resource id of the view, which should be set, as an {@link Integer} value.
         *         The resource id must correspond to a valid layout resource
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder setView(@LayoutRes final int resourceId) {
            customViewId = resourceId;
            customView = null;
            return this;
        }

        /**
         * Sets the custom view, which should be used to show the title of the bottom sheet, which
         * is created by the builder.
         *
         * @param view
         *         The view, which should be set, as an instance of the class {@link View} or null,
         *         if no custom view should be used to show the title
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder setCustomTitle(@Nullable final View view) {
            customTitleView = view;
            return this;
        }

        /**
         * Sets the sensitivity, which specifies the distance after which dragging has an effect on
         * the bottom sheet, in relation to an internal value range.
         *
         * @param dragSensitivity
         *         The drag sensitivity, which should be set, as a {@link Float} value. The drag
         *         sensitivity must be at lest 0 and at maximum 1
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder setDragSensitivity(final float dragSensitivity) {
            ensureAtLeast(dragSensitivity, 0, "The drag sensitivity must be at least 0");
            ensureAtMaximum(dragSensitivity, 1, "The drag sensitivity must be at maximum 1");
            this.dragSensitivity = dragSensitivity;
            return this;
        }

        /**
         * Adds a new item to the bottom sheet, which is created by the builder.
         *
         * @param title
         *         The title of the item, which should be added, as an instance of the type {@link
         *         CharSequence}. The title may neither be null, nor empty
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder addItem(@NonNull final CharSequence title) {
            MenuItem item = new MenuItem(title);
            items.add(item);
            return this;
        }

        /**
         * Adds a new item to the bottom sheet, which is created by the builder.
         *
         * @param title
         *         The title of the item, which should be added, as an instance of the type {@link
         *         CharSequence}. The title may neither be null, nor empty
         * @param icon
         *         The icon of the item, which should be added, as an instance of the class {@link
         *         Drawable}, or null, if no item should be used
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder addItem(@NonNull final CharSequence title,
                                     @Nullable final Drawable icon) {
            MenuItem item = new MenuItem(title);
            item.setIcon(icon);
            items.add(item);
            return this;
        }

        /**
         * Adds a new item to the bottom sheet, which is created by the builder.
         *
         * @param titleId
         *         The resource id of the title of the item, which should be added, as an {@link
         *         Integer} value. The resource id must correspond to a valid string resource
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder addItem(@StringRes final int titleId) {
            MenuItem item = new MenuItem(getContext(), titleId);
            items.add(item);
            return this;
        }

        /**
         * Adds a new item to the bottom sheet, which is created by the builder.
         *
         * @param titleId
         *         The resource id of the title of the item, which should be added, as an {@link
         *         Integer} value. The resource id must correspond to a valid string resource
         * @param iconId
         *         The resource id of the icon of the item, which should be added, as an {@link
         *         Integer} value. The resource id must correspond to a valid drawable resource
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder addItem(@StringRes final int titleId, @DrawableRes final int iconId) {
            MenuItem item = new MenuItem(getContext(), titleId);
            item.setIcon(getContext(), iconId);
            items.add(item);
            return this;
        }

        /**
         * Adds a new separator to the bottom sheet, which is created by the builder.
         *
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder addSeparator() {
            items.add(new Separator());
            return this;
        }

        /**
         * Adds a new separator to the bottom sheet, which is created by the builder.
         *
         * @param title
         *         The title of the separator, which should be added, as an instance of the type
         *         {@link CharSequence}, or null, if no title should be used
         */
        public final Builder addSeparator(@Nullable final CharSequence title) {
            Separator separator = new Separator();
            separator.setTitle(title);
            items.add(separator);
            return this;
        }

        /**
         * Adds a new separator to the bottom sheet, which is created by the builder.
         *
         * @param titleId
         *         The resource id of the title, which should be added, as an {@link Integer} value.
         *         The resource id must correspond to a valid string resource
         * @return The builder, the method has been called upon, as an instance of the class {@link
         * Builder}
         */
        public final Builder addSeparator(@StringRes final int titleId) {
            Separator separator = new Separator();
            separator.setTitle(getContext(), titleId);
            items.add(separator);
            return this;
        }

        /**
         * Creates a bottom sheet with the arguments, which have been supplied to the builder.
         * Calling this method does not display the bottom sheet.
         *
         * @return The bottom sheet, which has been create as an instance of the class {@link
         * BottomSheet}
         */
        public final BottomSheet create() {
            DraggableView root = inflateLayout();
            inflateTitleView(root);
            GridView gridView = inflateContentView(root);
            int themeResourceId =
                    this.themeResourceId != -1 ? this.themeResourceId : R.style.BottomSheet;
            BottomSheet bottomSheet =
                    new BottomSheet(context, themeResourceId, gridView != null ? items : null);
            bottomSheet.requestWindowFeature(Window.FEATURE_NO_TITLE);
            FrameLayout.LayoutParams layoutParams =
                    new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT);
            layoutParams.gravity = Gravity.BOTTOM;
            bottomSheet.setContentView(root, layoutParams);
            bottomSheet.setOnCancelListener(cancelListener);
            bottomSheet.setOnDismissListener(dismissListener);
            bottomSheet.setOnKeyListener(keyListener);
            bottomSheet.setCancelable(cancelable);
            bottomSheet.setCanceledOnTouchOutside(true);
            bottomSheet.setDragSensitivity(dragSensitivity);
            return bottomSheet;
        }

        /**
         * Creates a bottom sheet with the arguments, which have been supplied to the builder and
         * immediately displays it.
         *
         * @return The bottom sheet, which has been shown, as an instance of the class {@link
         * BottomSheet}
         */
        public final BottomSheet show() {
            BottomSheet bottomSheet = create();
            bottomSheet.show();
            return bottomSheet;
        }

    }

    /**
     * Contains all possible styles of a {@link BottomSheet}.
     */
    public enum Style {

        /**
         * If the bottom sheet's items should be shown in a list.
         */
        LIST,

        /**
         * If the bottom sheet's items should be shown in a grid.
         */
        GRID;

    }

    /**
     * The minimum value of the internal value range, which specifies after which distance dragging
     * has an effect on the bottom sheet.
     */
    private static final int MIN_DRAG_SENSITIVITY = 10;

    /**
     * The maximum value of the internal value range, which specifies after which distance dragging
     * has an effect on the bottom sheet.
     */
    private static final int MAX_DRAG_SENSITIVITY = 260;

    /**
     * The root view of the bottom sheet.
     */
    private DraggableView rootView;

    /**
     * The grid view, which is used to show the bottom sheet's menu items.
     */
    private GridView gridView;

    /**
     * The adapter, which is used to manage the bottom sheet's menu items.
     */
    private BottomSheetAdapter adapter;

    /**
     * True, if the bottom sheet is cancelable, false otherwise.
     */
    private boolean cancelable;

    /**
     * True, if the bottom sheet is canceled, when the decor view is touched, false otherwise.
     */
    private boolean canceledOnTouchOutside;

    /**
     * The sensitivity, which specifies the distance after which dragging has an effect on the
     * bottom sheet, in relation to an internal value range.
     */
    private float dragSensitivity;

    /**
     * Initializes the bottom sheet.
     *
     * @param items
     *         A collection, which contains the items, which should be added to the bottom sheet, as
     *         an instance of the type {@link Collection} or null, if a custom view is shown by the
     *         bottom sheet instead
     */
    private void initialize(@Nullable final Collection<Parcelable> items) {
        if (items != null) {
            adapter = new BottomSheetAdapter(getContext());

            for (Parcelable item : items) {
                if (item instanceof MenuItem) {
                    adapter.add((MenuItem) item);
                } else if (item instanceof Separator) {
                    adapter.add((Separator) item);
                }
            }
        }
    }

    /**
     * Creates and returns the layout params, which should be used to show the bottom sheet.
     *
     * @return The layout params, which have been created, as an instance of the class {@link
     * android.view.WindowManager.LayoutParams}
     */
    private WindowManager.LayoutParams createLayoutParams() {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.gravity = Gravity.BOTTOM;
        return layoutParams;
    }

    /**
     * Creates and returns a listener, which allows to cancel the bottom sheet, when the decor view
     * is touched.
     *
     * @return The listener, which has been created, as an instance of the type {@link
     * View.OnTouchListener}
     */
    private View.OnTouchListener createCancelOnTouchListener() {
        return new View.OnTouchListener() {

            @Override
            public boolean onTouch(final View v, final MotionEvent event) {
                if (cancelable && canceledOnTouchOutside) {
                    cancel();
                    return true;
                }

                return false;
            }

        };
    }

    /**
     * Calculates and returns the distance after which dragging has an effect on the bottom sheet in
     * pixels. The distance depends on the current set drag sensitivity, which corresponds to an
     * internal value range.
     *
     * @return The distance after which dragging has an effect on the bottom sheet in pixels as an
     * {@link Integer} value
     */
    private int calculateDragSensitivity() {
        int range = MAX_DRAG_SENSITIVITY - MIN_DRAG_SENSITIVITY;
        return Math.round((1 - getDragSensitivity()) * range + MIN_DRAG_SENSITIVITY);
    }

    /**
     * Creates a bottom sheet, which is designed according to Android 5's Material Design guidelines
     * even on pre-Lollipop devices.
     *
     * @param context
     *         The context, which should be used by the bottom sheet, as an instance of the class
     *         {@link Context}. The context may not be null
     * @param items
     *         A collection, which contains the items, which should be added to the bottom sheet, as
     *         an instance of the type {@link Collection} or null, if a custom view is shown by the
     *         bottom sheet instead
     */
    protected BottomSheet(@NonNull final Context context,
                          @Nullable final Collection<Parcelable> items) {
        super(context);
        initialize(items);
    }

    /**
     * Creates a bottom sheet, which is designed according to Android 5's Material Design guidelines
     * even on pre-Lollipop devices.
     *
     * @param context
     *         The context, which should be used by the bottom sheet, as an instance of the class
     *         {@link Context}. The context may not be null
     * @param themeResourceId
     *         The resource id of the theme, which should be used by the bottom sheet, as an {@link
     *         Integer} value. The resource id must correspond to a valid theme
     * @param items
     *         A collection, which contains the items, which should be added to the bottom sheet, as
     *         an instance of the type {@link Collection} or null, if a custom view is shown by the
     *         bottom sheet instead
     */
    protected BottomSheet(@NonNull final Context context, @StyleRes final int themeResourceId,
                          @Nullable final Collection<Parcelable> items) {
        super(context, themeResourceId);
        initialize(items);
    }

    /**
     * Returns the sensitivity, which specifies the distance after which dragging has an effect on
     * the bottom sheet, in relation to an internal value range.
     *
     * @return The drag sensitivity as a {@link Float} value. The drag sensitivity must be at lest 0
     * and at maximum 1
     */
    public final float getDragSensitivity() {
        return dragSensitivity;
    }

    /**
     * Sets the sensitivity, which specifies the distance after which dragging has an effect on the
     * bottom sheet, in relation to an internal value range.
     *
     * @param dragSensitivity
     *         The drag sensitivity, which should be set, as a {@link Float} value. The drag
     *         sensitivity must be at lest 0 and at maximum 1
     */
    public final void setDragSensitivity(final float dragSensitivity) {
        ensureAtLeast(dragSensitivity, 0, "The drag sensitivity must be at least 0");
        ensureAtMaximum(dragSensitivity, 1, "The drag sensitivity must be at maximum 1");
        this.dragSensitivity = dragSensitivity;

        if (rootView != null) {
            rootView.setDragSensitivity(calculateDragSensitivity());
        }
    }

    /**
     * Adds a new item to the bottom sheet.
     *
     * @param title
     *         The title of the item, which should be added, as an instance of the type {@link
     *         CharSequence}. The title may neither be null, nor empty
     */
    public final void addItem(@NonNull final CharSequence title) {
        ensureNotNull(adapter, "No items are shown by the bottom sheet",
                IllegalStateException.class);
        MenuItem item = new MenuItem(title);
        adapter.add(item);
    }

    /**
     * Adds a new item to the bottom sheet.
     *
     * @param title
     *         The title of the item, which should be added, as an instance of the type {@link
     *         CharSequence}. The title may neither be null, nor empty
     * @param icon
     *         The icon of the item, which should be added, as an instance of the class {@link
     *         Drawable}, or null, if no item should be used
     */
    public final void addItem(@NonNull final CharSequence title, @Nullable final Drawable icon) {
        ensureNotNull(adapter, "No items are shown by the bottom sheet",
                IllegalStateException.class);
        MenuItem item = new MenuItem(title);
        item.setIcon(icon);
        adapter.add(item);
    }

    /**
     * Adds a new item to the bottom sheet.
     *
     * @param titleId
     *         The resource id of the title of the item, which should be added, as an {@link
     *         Integer} value. The resource id must correspond to a valid string resource
     */
    public final void addItem(@StringRes final int titleId) {
        ensureNotNull(adapter, "No items are shown by the bottom sheet",
                IllegalStateException.class);
        MenuItem item = new MenuItem(getContext(), titleId);
        adapter.add(item);
    }

    /**
     * Adds a new item to the bottom sheet.
     *
     * @param titleId
     *         The resource id of the title of the item, which should be added, as an {@link
     *         Integer} value. The resource id must correspond to a valid string resource
     * @param iconId
     *         The resource id of the icon of the item, which should be added, as an {@link Integer}
     *         value. The resource id must correspond to a valid drawable resource
     */
    public final void addItem(@StringRes final int titleId, @DrawableRes final int iconId) {
        ensureNotNull(adapter, "No items are shown by the bottom sheet",
                IllegalStateException.class);
        MenuItem item = new MenuItem(getContext(), titleId);
        item.setIcon(getContext(), iconId);
        adapter.add(item);
    }

    /**
     * Adds a new separator to the bottom sheet.
     */
    public final void addSeparator() {
        ensureNotNull(adapter, "No items are shown by the bottom sheet",
                IllegalStateException.class);
        adapter.add(new Separator());
    }

    /**
     * Adds a new separator to the bottom sheet.
     *
     * @param title
     *         The title of the separator, which should be added, as an instance of the type {@link
     *         CharSequence}, or null, if no title should be used
     */
    public final void addSeparator(@Nullable final CharSequence title) {
        ensureNotNull(adapter, "No items are shown by the bottom sheet",
                IllegalStateException.class);
        Separator separator = new Separator();
        separator.setTitle(title);
        adapter.add(separator);
    }

    /**
     * Adds a new separator to the bottom sheet.
     *
     * @param titleId
     *         The resource id of the title, which should be added, as an {@link Integer} value. The
     *         resource id must correspond to a valid string resource
     */
    public final void addSeparator(@StringRes final int titleId) {
        ensureNotNull(adapter, "No items are shown by the bottom sheet",
                IllegalStateException.class);
        Separator separator = new Separator();
        separator.setTitle(getContext(), titleId);
        adapter.add(separator);
    }

    /**
     * Removes the item at a specific index from the bottom sheet.
     *
     * @param index
     *         The index of the item, which should be removed, as an {@link Integer} value
     */
    public final void removeItem(final int index) {
        ensureNotNull(adapter, "No items are shown by the bottom sheet",
                IllegalStateException.class);
        adapter.remove(index);
    }

    /**
     * Removes all items from the bottom sheet.
     */
    public final void removeAllItems() {
        ensureNotNull(adapter, "No items are shown by the bottom sheet",
                IllegalStateException.class);
        adapter.clear();
    }

    /**
     * Returns the number of items, which are currently contained by the bottom sheet.
     *
     * @return The number of items, which are currently contained by the bottom sheet, as an {@link
     * Integer} value
     */
    public final int getItemCount() {
        ensureNotNull(adapter, "No items are shown by the bottom sheet",
                IllegalStateException.class);
        return adapter.getCount();
    }

    /**
     * Invalidates the bottom sheet. This method must be called in order to update the appearance of
     * the bottom sheet, when its items have been changed.
     */
    public final void invalidate() {
        ensureNotNull(adapter, "No items are shown by the bottom sheet",
                IllegalStateException.class);
        adapter.notifyDataSetChanged();
    }

    /**
     * Sets, whether the bottom sheet should automatically be invalidated, when its items have been
     * changed, or not.
     *
     * @param invalidateOnChange
     *         True, if the bottom sheet should automatically be invalidated, when its items have
     *         been changed, false otherwise
     */
    public final void invalidateOnChange(final boolean invalidateOnChange) {
        ensureNotNull(adapter, "No items are shown by the bottom sheet",
                IllegalStateException.class);
        adapter.notifyOnChange(invalidateOnChange);
    }

    @Override
    public final void setCancelable(final boolean cancelable) {
        super.setCancelable(cancelable);
        this.cancelable = cancelable;
    }

    @Override
    public final void setCanceledOnTouchOutside(final boolean canceledOnTouchOutside) {
        super.setCanceledOnTouchOutside(canceledOnTouchOutside);
        this.canceledOnTouchOutside = canceledOnTouchOutside;
    }

    @Override
    public final void onMaximized() {

    }

    @Override
    public final void onHidden() {
        dismiss();
    }

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setAttributes(createLayoutParams());
        getWindow().getDecorView().setOnTouchListener(createCancelOnTouchListener());
        rootView = (DraggableView) findViewById(R.id.root);
        rootView.setDragSensitivity(calculateDragSensitivity());
        rootView.setCallback(this);
        gridView = (GridView) findViewById(R.id.bottom_sheet_grid_view);

        if (gridView != null) {
            gridView.setAdapter(adapter);
        }
    }

}