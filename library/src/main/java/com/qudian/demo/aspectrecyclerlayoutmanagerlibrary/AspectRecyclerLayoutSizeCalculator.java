package com.qudian.demo.aspectrecyclerlayoutmanagerlibrary;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class AspectRecyclerLayoutSizeCalculator {
    public interface SizeCalculatorListener {
        double aspectRatioForIndex(int index);
    }

    private static final int DEFAULT_MAX_ROW_HEIGHT = 600;
    private static int mMaxRowHeight = DEFAULT_MAX_ROW_HEIGHT;

    private static final int DEFAULT_SPAN_COUNT = 2;
    private static int mCertainSpanCount = DEFAULT_SPAN_COUNT;

    private static final int INVALID_CONTENT_WIDTH = -1;
    private int mContentWidth = INVALID_CONTENT_WIDTH;

    private SizeCalculatorListener mSizeCalculatorListener;

    private List<CeilSize> mSizeForChildAtPosition;
    private List<Integer> mFirstChildPositionForRow;
    private List<Integer> mRowForChildPosition;

    public AspectRecyclerLayoutSizeCalculator(SizeCalculatorListener sizeCalculatorDelegate) {
        mSizeCalculatorListener = sizeCalculatorDelegate;

        mSizeForChildAtPosition = new ArrayList<CeilSize>();
        mFirstChildPositionForRow = new ArrayList<Integer>();
        mRowForChildPosition = new ArrayList<Integer>();
    }

    public void setContentWidth(int contentWidth) {
        if (mContentWidth != contentWidth) {
            mContentWidth = contentWidth;
            reset();
        }
    }

    public void setMaxRowHeight(int maxRowHeight) {
        if (mMaxRowHeight != maxRowHeight) {
            mMaxRowHeight = maxRowHeight;
            reset();
        }
    }

    public void setCertainSpanCount(int spanCount) {
        mCertainSpanCount = spanCount;
    }

    public int getCertainSpanCount() {
        return mCertainSpanCount;
    }

    public CeilSize sizeForChildAtPosition(int position) throws IndexOutOfBoundsException {
        if (position >= mSizeForChildAtPosition.size()) {
            computeChildSizesUpToPosition(position);
        }

        return mSizeForChildAtPosition.get(position);
    }

    public int getFirstChildPositionForRow(int row) throws IndexOutOfBoundsException {
        if (row >= mFirstChildPositionForRow.size()) {
            computeFirstChildPositionsUpToRow(row);
        }
        return mFirstChildPositionForRow.get(row);
    }

    public int getRowForChildPosition(int position) throws IndexOutOfBoundsException {

        if (position >= mRowForChildPosition.size()) {
            computeChildSizesUpToPosition(position);
        }
        return mRowForChildPosition.get(position);
    }

    public void reset() {
        mSizeForChildAtPosition.clear();
        mFirstChildPositionForRow.clear();
        mRowForChildPosition.clear();
    }

    private void computeFirstChildPositionsUpToRow(int row) {
        // TODO: Rewrite this? Looks dangerous but in reality should be fine. I'd like something
        //       less alarming though.
        while (row >= mFirstChildPositionForRow.size()) {
            computeChildSizesUpToPosition(mSizeForChildAtPosition.size() + 1);
        }
    }

    private void computeChildSizesUpToPosition(int lastPosition) {
        if (mContentWidth == INVALID_CONTENT_WIDTH) {
            throw new RuntimeException("Invalid content width. Did you forget to set it?");
        }

        if (mSizeCalculatorListener == null) {
            throw new RuntimeException("CeilSize calculator delegate is missing. Did you forget to set it?");
        }

        int firstUncomputedChildPosition = mSizeForChildAtPosition.size();
        int row = mRowForChildPosition.size() > 0
                ? mRowForChildPosition.get(mRowForChildPosition.size() - 1) + 1 : 0;

        double currentRowAspectRatio = 0.0;
        List<Double> itemAspectRatios = new ArrayList<Double>();
        int currentRowHeight = Integer.MAX_VALUE;

        int rowChildSum = 0;

        int currentRowWidth = 0;
        int pos = firstUncomputedChildPosition;

        while (pos < lastPosition || (rowChildSum < mCertainSpanCount)) {//currentRowHeight > mMaxRowHeight
            double posAspectRatio = mSizeCalculatorListener.aspectRatioForIndex(pos);
            currentRowAspectRatio += posAspectRatio;
            itemAspectRatios.add(posAspectRatio);

            rowChildSum++;

            currentRowWidth = calculateWidth(currentRowHeight, currentRowAspectRatio);
            currentRowHeight = calculateHeight(mContentWidth, currentRowAspectRatio);

            Log.d("aspect", "rowChildSum:"+rowChildSum);

//            boolean isRowFull = currentRowHeight <= mMaxRowHeight;
            boolean isRowFull = rowChildSum == mCertainSpanCount;
            if (isRowFull) {
                int rowChildCount = itemAspectRatios.size();
                mFirstChildPositionForRow.add(pos - rowChildCount + 1);

                int availableSpace = mContentWidth;

                for (int i = 0; i < rowChildCount; i++) {
                    int itemWidth = calculateWidth(currentRowHeight, itemAspectRatios.get(i));
                    itemWidth = Math.min(availableSpace, itemWidth);

                    mSizeForChildAtPosition.add(new CeilSize(itemWidth, (currentRowHeight > mMaxRowHeight ? mMaxRowHeight : currentRowHeight)));
                    mRowForChildPosition.add(row);

                    availableSpace -= itemWidth;
                }

                itemAspectRatios.clear();
                currentRowAspectRatio = 0.0;
                row++;
            }

            pos++;
        }
        rowChildSum = 0;
    }

    private int calculateWidth(int itemHeight, double aspectRatio) {
        return (int) Math.ceil(itemHeight * aspectRatio);
    }

    private int calculateHeight(int itemWidth, double aspectRatio) {
        return (int) Math.ceil(itemWidth / aspectRatio);
    }
}
