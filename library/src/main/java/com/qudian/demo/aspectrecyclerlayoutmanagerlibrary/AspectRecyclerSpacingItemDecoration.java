package com.qudian.demo.aspectrecyclerlayoutmanagerlibrary;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class AspectRecyclerSpacingItemDecoration extends RecyclerView.ItemDecoration {
    private static final String TAG = AspectRecyclerSpacingItemDecoration.class.getName();

    public static int DEFAULT_SPACING = 64;
    private int mSpacing;

    public AspectRecyclerSpacingItemDecoration() {
        this(DEFAULT_SPACING);
    }

    public AspectRecyclerSpacingItemDecoration(int spacing) {
        mSpacing = spacing;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if (!(parent.getLayoutManager() instanceof AspectRecyclerLayoutManager)) {
            throw new IllegalArgumentException(String.format("The %s must be used with a %s",
                    AspectRecyclerSpacingItemDecoration.class.getSimpleName(),
                    AspectRecyclerLayoutManager.class.getSimpleName()));
        }

        final AspectRecyclerLayoutManager layoutManager = (AspectRecyclerLayoutManager) parent.getLayoutManager();

        int childIndex = parent.getChildAdapterPosition(view);
        if (childIndex == RecyclerView.NO_POSITION) return;

        outRect.top    = 0;
        outRect.bottom = mSpacing;
        outRect.left   = 0;
        outRect.right  = mSpacing;

        // Add inter-item spacings
        if (isTopChild(childIndex, layoutManager)) {
            outRect.top = mSpacing;
        }

        if (isLeftChild(childIndex, layoutManager)) {
            outRect.left = mSpacing;
        }
    }

    private static boolean isTopChild(int position, AspectRecyclerLayoutManager layoutManager) {
        boolean isFirstViewHeader = layoutManager.isFirstViewHeader();
        final AspectRecyclerLayoutSizeCalculator sizeCalculator = layoutManager.getSizeCalculator();
        return sizeCalculator.getRowForChildPosition(position) == 0;
    }

    private static boolean isLeftChild(int position, AspectRecyclerLayoutManager layoutManager) {
        boolean isFirstViewHeader = layoutManager.isFirstViewHeader();
        final AspectRecyclerLayoutSizeCalculator sizeCalculator = layoutManager.getSizeCalculator();
        int rowForPosition = sizeCalculator.getRowForChildPosition(position);
        return sizeCalculator.getFirstChildPositionForRow(rowForPosition) == position;
    }
}
