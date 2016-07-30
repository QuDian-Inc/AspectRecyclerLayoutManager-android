package com.qudian.demo;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.qudian.demo.aspectrecyclerlayoutmanagerlibrary.AspectRecyclerLayoutManager;
import com.qudian.demo.aspectrecyclerlayoutmanagerlibrary.AspectRecyclerSpacingItemDecoration;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static List<LocalImage> mDatas = new ArrayList<LocalImage>();
    private static ImageGridAdapter mStubAdapter;
    private AspectRecyclerLayoutManager mGridLayoutManager;

    private RecyclerView mRecyclerView;


    private ImageHandler mImageHandler = new ImageHandler(this);

    private static class ImageHandler extends android.os.Handler {

        WeakReference<Context> mWf;

        public ImageHandler(Context context){
            mWf = new WeakReference<Context>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (mWf.get() != null){
                Log.d(TAG, "handleMessage mDatas.size:"+ mDatas.size());
                mStubAdapter.setAdapterData(mDatas);
                mStubAdapter.notifyDataSetChanged();
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);

        mRecyclerView = (RecyclerView) findViewById(R.id.image_recycler_view);

        mStubAdapter = new ImageGridAdapter(this);
        mGridLayoutManager = new AspectRecyclerLayoutManager(mStubAdapter);
        mGridLayoutManager.setMaxRowHeight(MeasUtils.dpToPx(200.0f, this));
        mGridLayoutManager.setCertainSpanCount(2);

        mRecyclerView.addItemDecoration(new AspectRecyclerSpacingItemDecoration(MeasUtils.dpToPx(3.0f, this)));
        mRecyclerView.setLayoutManager(mGridLayoutManager);
        mRecyclerView.setAdapter(mStubAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);

        new Thread(new Runnable() {
            @Override
            public void run() {
                loadData();
            }
        }).start();
    }


    private void loadData(){
        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Images.Media.MIME_TYPE + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=?",
                new String[] { "image/jpeg", "image/png", "image/jpg" }, MediaStore.Images.Media.DATE_TAKEN + " desc");
        LocalImage localImage = null;
        ImageSize imageSize;
        while (cursor != null && cursor.moveToNext()) {
            // 获取图片的路径
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            if (!TextUtils.isEmpty(path)) {

                imageSize = ImageUtil.decordBitmapSize(path);
                if (imageSize.mWidth > 0 && imageSize.mHeight > 0) {
                    localImage = new LocalImage();
                    localImage.mImagePath = path;
                    localImage.mImageSize = imageSize;
                    mDatas.add(localImage);
                }
            }
        }

        Log.d(TAG, "mDatas.size:"+ mDatas.size());

        mImageHandler.sendEmptyMessage(0);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
