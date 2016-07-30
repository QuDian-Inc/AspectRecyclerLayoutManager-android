package com.qudian.demo;

import android.graphics.BitmapFactory;

/**
 * Created by edgardo on 16/7/30.
 */
public class ImageUtil {

    public static ImageSize decordBitmapSize(String path) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opts);

        return new ImageSize(opts.outWidth, opts.outHeight);
    }

}
