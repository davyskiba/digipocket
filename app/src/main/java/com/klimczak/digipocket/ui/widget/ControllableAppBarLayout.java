package com.klimczak.digipocket.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import java.lang.ref.WeakReference;


public class ControllableAppBarLayout extends AppBarLayout implements AppBarLayout.OnOffsetChangedListener {

    private AppBarLayout.Behavior oBehavior;
    private WeakReference<CoordinatorLayout> oParent;
    private ToolbarChange oQueuedChange = ToolbarChange.NONE;

    private boolean oAfterFirstDraw = false;
    boolean oIsShow = false;
    int oScrollRange = -1;

    private State state;
    private OnStateChangeListener onStateChangeListener;


    public ControllableAppBarLayout(Context context) { super(context);}

    public ControllableAppBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!(getLayoutParams() instanceof CoordinatorLayout.LayoutParams)
                || !(getParent() instanceof CoordinatorLayout)) {
            throw new IllegalStateException(
                    "ControllableAppBarLayout must be a direct child of CoordinatorLayout.");
        }
        oParent = new WeakReference<>((CoordinatorLayout) getParent());
        addOnOffsetChangedListener(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (oBehavior == null) {
            oBehavior = (Behavior) ((CoordinatorLayout.LayoutParams) getLayoutParams()).getBehavior();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (r - l > 0 && b - t > 0 && oAfterFirstDraw && oQueuedChange != ToolbarChange.NONE) {
            analyzeQueuedChange();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!oAfterFirstDraw) {
            oAfterFirstDraw = true;
            if (oQueuedChange != ToolbarChange.NONE) {
                analyzeQueuedChange();
            }
        }
    }

    private synchronized void analyzeQueuedChange() {
        switch (oQueuedChange) {
            case COLLAPSE:
                performCollapsingWithoutAnimation();
                break;
            case COLLAPSE_WITH_ANIMATION:
                performCollapsingWithAnimation();
                break;
            case EXPAND:
                performExpandingWithoutAnimation();
                break;
            case EXPAND_WITH_ANIMATION:
                performExpandingWithAnimation();
                break;
        }

        oQueuedChange = ToolbarChange.NONE;
    }

    public void collapseToolbar() {
        collapseToolbar(false);
    }

    public void collapseToolbar(boolean withAnimation) {
        oQueuedChange = withAnimation ? ToolbarChange.COLLAPSE_WITH_ANIMATION : ToolbarChange.COLLAPSE;
        requestLayout();
    }

    public void expandToolbar() {
        expandToolbar(false);
    }

    public void expandToolbar(boolean withAnimation) {
        oQueuedChange = withAnimation ? ToolbarChange.EXPAND_WITH_ANIMATION : ToolbarChange.EXPAND;
        requestLayout();
    }

    private void performCollapsingWithoutAnimation() {
        if (oParent.get() != null) {
            oBehavior.onNestedPreScroll(oParent.get(), this, null, 0, getHeight(), new int[] { 0, 0 });
        }
    }

    private void performCollapsingWithAnimation() {
        if (oParent.get() != null) {
            oBehavior.onNestedFling(oParent.get(), this, null, 0, getHeight(), true);
        }
    }

    private void performExpandingWithoutAnimation() {
        if (oParent.get() != null) {
            oBehavior.setTopAndBottomOffset(0);
        }
    }

    private void performExpandingWithAnimation() {
        if (oParent.get() != null) {
            oBehavior.onNestedFling(oParent.get(), this, null, 0, -getHeight() * 5, false);
        }
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        if (oScrollRange == -1) {
            oScrollRange = appBarLayout.getTotalScrollRange();
            if (onStateChangeListener != null && state != State.IDLE) {
                onStateChangeListener.onStateChange(State.IDLE);
            }
        }
        if (oScrollRange + i <= 0) {
            if (onStateChangeListener != null && state != State.COLLAPSED) {
                onStateChangeListener.onStateChange(State.COLLAPSED);
            }
            state = State.COLLAPSED;
            oIsShow = true;
        } else if (oIsShow) {
            if (onStateChangeListener != null && state != State.EXPANDED) {
                onStateChangeListener.onStateChange(State.EXPANDED);
            }
            state = State.EXPANDED;
        }

    }

    public void setOnStateChangeListener(OnStateChangeListener listener) {
        this.onStateChangeListener = listener;
    }


    public interface OnStateChangeListener {
        void onStateChange(State toolbarChange);
    }

    public enum State {
        COLLAPSED,
        EXPANDED,
        IDLE
    }

    private enum ToolbarChange {
        COLLAPSE,
        COLLAPSE_WITH_ANIMATION,
        EXPAND,
        EXPAND_WITH_ANIMATION,
        NONE
    }
}
