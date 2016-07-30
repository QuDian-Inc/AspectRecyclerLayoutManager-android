package com.qudian.demo;

/**
 * Created by edgardo on 16/7/30.
 */
public class LocalImage {

    public String mImagePath;
    public ImageSize mImageSize;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mImagePath == null) ? 0 : mImagePath.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LocalImage other = (LocalImage) obj;
        if (mImagePath == null) {
            if (other.mImagePath != null)
                return false;
        } else if (!mImagePath.equals(other.mImagePath))
            return false;
        return true;
    }


}
