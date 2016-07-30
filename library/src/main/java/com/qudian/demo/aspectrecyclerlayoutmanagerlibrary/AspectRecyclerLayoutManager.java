package com.qudian.demo.aspectrecyclerlayoutmanagerlibrary;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

public class AspectRecyclerLayoutManager extends RecyclerView.LayoutManager {

	private static final String TAG = AspectRecyclerLayoutManager.class.getSimpleName();

    // TODO: Can we do away with this?
    private enum Direction { NONE, UP, DOWN }

    // First (top-left) position visible at any point
    private int mFirstVisiblePosition;

    // First (top) row position at any given point
    private int mFirstVisibleRow;

    // Flag to force current scroll offsets to be ignored on re-layout
    private boolean mForceClearOffsets;

    // Scroll position offset that will be applied on the next layout pass
    private int mPendingScrollPositionOffset = 0;

    // The size of the header view. This is calculated in {@code preFillGrid}.
    private CeilSize mHeaderViewSize;

    private AspectRecyclerLayoutSizeCalculator mSizeCalculator;

    public AspectRecyclerLayoutManager(AspectRecyclerLayoutSizeCalculator.SizeCalculatorListener sizeCalculatorDelegate) {
		 mSizeCalculator = new AspectRecyclerLayoutSizeCalculator(sizeCalculatorDelegate);
	}

    /**
     * The max height a row could be. If fixed height is enabled via {@code setFixedHeight(boolean)}
     * the given max row height value will be used as the fixed row height.
     *
     * @param maxRowHeight Max height a row can grow to.
     */
    public void setMaxRowHeight(int maxRowHeight) {
        mSizeCalculator.setMaxRowHeight(maxRowHeight);
    }

    public void setCertainSpanCount(int spanCount){
        mSizeCalculator.setCertainSpanCount(spanCount);
    }

    /**
     * Set to true if you want the first view to act as a header. It's height will be obtained from
     * the view itself, and the width will be equal to the content width.
     *
     * @param isFirstViewHeader true to have the first view act as a header.
     */
    public void setFirstViewAsHeader(boolean isFirstViewHeader) {
//        mIsFirstViewHeader = isFirstViewHeader;
    }

    public boolean isFirstViewHeader() {
        return false;//mIsFirstViewHeader;
    }

    // The initial call from the framework, received when we need to start laying out the initial
    // set of views, or when the user changes the data set
    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        // We have nothing to show for an empty data set but clear any existing views
        if (getItemCount() == 0) {
            detachAndScrapAttachedViews(recycler);
            return;
        }

        mSizeCalculator.setContentWidth(getContentWidth());
        mSizeCalculator.reset();

        int initialTopOffset = 0;
        if (getChildCount() == 0) { // First or empty layout
            mFirstVisiblePosition = 0;
            mFirstVisibleRow = 0;
        } else { // Adapter data set changes
            // Keep the existing initial position, and save off the current scrolled offset.
            final View topChild = getChildAt(0);
            if (mForceClearOffsets) {
                initialTopOffset = 0;
                mForceClearOffsets = false;
            } else {
                initialTopOffset = getDecoratedTop(topChild);
            }
        }

        detachAndScrapAttachedViews(recycler);
        try {
			preFillGrid(Direction.NONE, 0, initialTopOffset, recycler, state);
		} catch (Exception e) {
			e.printStackTrace();
		}
        mPendingScrollPositionOffset = 0;
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        removeAllViews();
        mSizeCalculator.reset();
    }

    /**
     * Find first visible position, scrap all children, and then layout all visible views returning
     * the number of pixels laid out, which could be greater than the entire view (useful for scroll
     * functions).
     * @param direction The direction we are filling the grid in
     * @param dy Vertical offset, creating a gap that we need to fill
     * @param emptyTop Offset we begin filling at
     * @return Number of vertical pixels laid out
     */
    private int preFillGrid(Direction direction, int dy, int emptyTop,
                            RecyclerView.Recycler recycler, RecyclerView.State state) throws Exception{
        Log.d("aspect", "preFillGrid");
        int newFirstVisiblePosition = firstChildPositionForRow(mFirstVisibleRow);

        // First, detach all existing views from the layout. detachView() is a lightweight operation
        //      that we can use to quickly reorder views without a full add/remove.
        SparseArray<View> viewCache = new SparseArray<View>(getChildCount());
        int startLeftOffset = getPaddingLeft();
        int startTopOffset  = getPaddingTop() + emptyTop;

        if (getChildCount() != 0) {
            startTopOffset = getDecoratedTop(getChildAt(0));
            if (mFirstVisiblePosition != newFirstVisiblePosition) {
                switch (direction) {
                    case UP: // new row above may be shown
                        double previousTopRowHeight = sizeForChildAtPosition(
                                mFirstVisiblePosition - 1).getHeight();
                        startTopOffset -= previousTopRowHeight;
                        break;
                    case DOWN: // row may have gone off screen
                        double topRowHeight = sizeForChildAtPosition(
                                mFirstVisiblePosition).getHeight();
                        startTopOffset += topRowHeight;
                        break;
                }
            }

            // Cache all views by their existing position, before updating counts
            for (int i = 0; i < getChildCount(); i++) {
                int position = mFirstVisiblePosition + i;
                final View child = getChildAt(i);
                viewCache.put(position, child);
            }

            // Temporarily detach all cached views. Views we still need will be added back at the proper index
            for (int i = 0; i < viewCache.size(); i++) {
                final View cachedView = viewCache.valueAt(i);
                detachView(cachedView);
            }
        }

        mFirstVisiblePosition = newFirstVisiblePosition;

        // Next, supply the grid of items that are deemed visible. If they were previously there,
        //      they will simply be re-attached. New views that must be created are obtained from
        //      the Recycler and added.
        int leftOffset = startLeftOffset;
        int topOffset  = startTopOffset + mPendingScrollPositionOffset;
        int nextPosition = mFirstVisiblePosition;

        while (nextPosition >= 0 && nextPosition < state.getItemCount()) {

            boolean isViewCached = true;
            View view = viewCache.get(nextPosition);
            if (view == null) {
                view = recycler.getViewForPosition(nextPosition);
                isViewCached = false;
            }

            // Overflow to next row if we don't fit
            CeilSize viewSize = sizeForChildAtPosition(nextPosition);
            if ((leftOffset + viewSize.getWidth()) > getContentWidth()) {
                leftOffset = startLeftOffset;
                CeilSize previousViewSize = sizeForChildAtPosition(nextPosition - 1);
                topOffset += previousViewSize.getHeight();
            }

            // These next children would no longer be visible, stop here
            boolean isAtEndOfContent;
            switch (direction) {
                case DOWN: isAtEndOfContent = topOffset >= getContentHeight() + dy; break;
                default:   isAtEndOfContent = topOffset >= getContentHeight();      break;
            }
            if (isAtEndOfContent) break;

            if (isViewCached) {
                // Re-attach the cached view at its new index
                attachView(view);
                viewCache.remove(nextPosition);
            } else {
                addView(view);
                measureChildWithMargins(view, 0, 0);

                int right  = leftOffset + viewSize.getWidth();
                int bottom = topOffset  + viewSize.getHeight();
                layoutDecorated(view, leftOffset, topOffset, right, bottom);
            }

            leftOffset += viewSize.getWidth();

            nextPosition++;
        }

        // Scrap and store views that were not re-attached (no longer visible).
        for (int i = 0; i < viewCache.size(); i++) {
            final View removingView = viewCache.valueAt(i);
            recycler.recycleView(removingView);
        }

        // Calculate pixels laid out during fill
        int pixelsFilled = 0;
        if (getChildCount() > 0) {
            pixelsFilled = getChildAt(getChildCount() - 1).getBottom();
        }

        return pixelsFilled;
    }

    private int getContentWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private int getContentHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    //region SizeCalculator proxy methods
    private CeilSize sizeForChildAtPosition(int position) throws IndexOutOfBoundsException{
        return mSizeCalculator.sizeForChildAtPosition(position);
    }

    private int rowForChildPosition(int position) throws IndexOutOfBoundsException{
        int offset = 0;
        return mSizeCalculator.getRowForChildPosition(position) + offset;
    }

    private int firstChildPositionForRow(int row) throws IndexOutOfBoundsException{
        int offset = 0;
        return mSizeCalculator.getFirstChildPositionForRow(row) + offset;
    }
    //endregion

    /**
     * {@inheritDoc}
     */
    @Override
    public void scrollToPosition(int position) {

        if (position >= getItemCount()) {
            Log.w(TAG, String.format("Cannot scroll to %d, item count is %d", position, getItemCount()));
            return;
        }

        try {
			mForceClearOffsets = true; // Ignore current scroll offset
			mFirstVisibleRow = rowForChildPosition(position);
			mFirstVisiblePosition = firstChildPositionForRow(mFirstVisibleRow);
			requestLayout();
		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
            try {
                mForceClearOffsets = true;
                if (position % mSizeCalculator.getCertainSpanCount() == 0) {
                    mFirstVisibleRow = (position + 1) / mSizeCalculator.getCertainSpanCount();
                } else {
                    mFirstVisibleRow = (position + 1) / mSizeCalculator.getCertainSpanCount() + 1;
                }
                mFirstVisiblePosition = firstChildPositionForRow(mFirstVisibleRow);
                requestLayout();
            } catch (IndexOutOfBoundsException e1) {
                e1.printStackTrace();
            }
		}

    }

    /**
     * Scroll to the specified adapter position with the given offset. Note that the scroll position
     * change will not be reflected until the next layout call. If you are just trying to make a
     * position visible, use {@link #scrollToPosition(int)}.
     *
     * @param position Index (starting at 0) of the reference item.
     * @param offset   The distance (in pixels) between the start edge of the item view and
     *                 start edge of the RecyclerView.
     * @see #scrollToPosition(int)
     */
    public void scrollToPositionWithOffset(int position, int offset) {
        mPendingScrollPositionOffset = offset;
        scrollToPosition(position);
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
    	
        if (getChildCount() == 0 || dy == 0) {
            return 0;
        }

        final View topLeftView = getChildAt(0);
        final View bottomRightView = getChildAt(getChildCount() - 1);
        int pixelsFilled = getContentHeight();
        // TODO: Split into methods, or a switch case?
        if (dy > 0) {
            boolean isLastChildVisible = (mFirstVisiblePosition + getChildCount()) >= getItemCount();

            if (isLastChildVisible) {
                // Is at end of content
                pixelsFilled = Math.max(getDecoratedBottom(bottomRightView) - getContentHeight(), 0);

            } else if (getDecoratedBottom(topLeftView) - dy <= 0) {
                // Top row went offscreen
                mFirstVisibleRow++;
                try {
					pixelsFilled = preFillGrid(Direction.DOWN, Math.abs(dy), 0, recycler, state);
				} catch (Exception e) {
					e.printStackTrace();
				}

            } else if (getDecoratedBottom(bottomRightView) - dy < getContentHeight()) {
                // New bottom row came on screen
                try {
					pixelsFilled = preFillGrid(Direction.DOWN, Math.abs(dy), 0, recycler, state);
				} catch (Exception e) {
					e.printStackTrace();
				}
            }
        } else {
            if (mFirstVisibleRow == 0 && getDecoratedTop(topLeftView) - dy >= 0) {
                // Is scrolled to top
                pixelsFilled = -getDecoratedTop(topLeftView);

            } else if (getDecoratedTop(topLeftView) - dy >= 0) {
                // New top row came on screen
                mFirstVisibleRow--;
                try {
					pixelsFilled = preFillGrid(Direction.UP, Math.abs(dy), 0, recycler, state);
				} catch (Exception e) {
					e.printStackTrace();
				}

            } else if (getDecoratedTop(bottomRightView) - dy > getContentHeight()) {
                // Bottom row went offscreen
                try {
					pixelsFilled = preFillGrid(Direction.UP, Math.abs(dy), 0, recycler, state);
				} catch (Exception e) {
					e.printStackTrace();
				}
            }
        }

        final int scrolled = Math.abs(dy) > pixelsFilled ? (int) Math.signum(dy) * pixelsFilled : dy;
        offsetChildrenVertical(-scrolled);

        // Return value determines if a boundary has been reached (for edge effects and flings). If
        //      returned value does not match original delta (passed in), RecyclerView will draw an
        //      edge effect.
        return scrolled;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    /**
     * Returns the adapter position of the first visible view.
     *
     * @return The adapter position of the first visible view or {@link RecyclerView#NO_POSITION} if
     * there aren't any visible items.
     */
    public int findFirstVisibleItemPosition() {
        if (getItemCount() == 0) {
            return RecyclerView.NO_POSITION;
        } else {
            return mFirstVisiblePosition;
        }
    }

    /**
     * Returns the adapter position of the last visible view.
     *
     * @return The adapter position of the last visible view or {@link RecyclerView#NO_POSITION} if
     * there aren't any visible items.
     */
    public int findLastVisibleItemPosition() {
        if (getItemCount() == 0) {
            return RecyclerView.NO_POSITION;
        } else {
            return mFirstVisiblePosition + getChildCount();
        }
    }

    public AspectRecyclerLayoutSizeCalculator getSizeCalculator() {
        return mSizeCalculator;
    }
}