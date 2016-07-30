package com.qudian.demo;

/**
 * Created by edgardo on 16/7/30.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.qudian.demo.aspectrecyclerlayoutmanagerlibrary.AspectRecyclerLayoutSizeCalculator;

import java.util.ArrayList;
import java.util.List;

public class ImageGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements AspectRecyclerLayoutSizeCalculator.SizeCalculatorListener {

    private static final String TAG = ImageGridAdapter.class.getSimpleName();

    private List<LocalImage> mAdapterData = new ArrayList<LocalImage>();
    private double[] mImageAspectRatios = null;

    private Context mContext;
    private LayoutInflater mInflater;

    private DisplayImageOptions mOpts;

    public ImageGridAdapter(Context context) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(mContext);

        mOpts = new DisplayImageOptions.Builder().cacheInMemory(true).considerExifParams(true)
                .showImageOnLoading(R.drawable.drawable_image_loading)
                .displayer(new FadeInBitmapDisplayer(500, true, true, true)).bitmapConfig(Bitmap.Config.RGB_565)
                .imageScaleType(ImageScaleType.IN_SAMPLE_INT).cacheOnDisk(true).build();

    }

    public void setAdapterData(List<LocalImage> list) {
        mAdapterData.clear();
        mAdapterData.addAll(list);

        if (mImageAspectRatios == null) {
            mImageAspectRatios = new double[mAdapterData.size()];

            for (int i = 0; i < mAdapterData.size(); i++) {
                mImageAspectRatios[i] = (double) mAdapterData.get(i).mImageSize.mWidth / (double) mAdapterData.get(i).mImageSize.mHeight;
            }
        }
    }

    private int getLoopedIndex(int index) {
        return index % mAdapterData.size();
    }

    @Override
    public int getItemCount() {
        return mAdapterData.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private LocalImage getItem(int position) {
        return mAdapterData.isEmpty() ? null : mAdapterData.get(position);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final LocalImage localImage = getItem(getLoopedIndex(position));
        if (localImage != null) {
            LocalImageHolder localImageHolder = (LocalImageHolder) holder;
            Log.d(TAG, "imagePath >>>> " + localImage.mImagePath);
            ImageLoader.getInstance().displayImage("file://" + localImage.mImagePath, localImageHolder.mImageView, mOpts);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        ImageView imageView = new ImageView(mContext);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        return new LocalImageHolder(imageView);
    }

    private class LocalImageHolder extends RecyclerView.ViewHolder {

        private ImageView mImageView;

        public LocalImageHolder(View view) {
            super(view);
            mImageView = (ImageView) view;
        }
    }

    @Override
    public double aspectRatioForIndex(int index) {
        if (index >= getItemCount()) return 1.0;
        return mImageAspectRatios[getLoopedIndex(index)];
    }

}

