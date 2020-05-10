package com.bignerdranch.android.locatr.model;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

/**
 * Class for a basic representation of a data class to encapsulate all variables and methods
 * for working with individual photo objects.
 */
public class GalleryItem {

    @SerializedName("title")
    private String mCaption;
    @SerializedName("id")
    private String mId;
    @SerializedName("url_s")
    private String mUrl;
    @SerializedName("url_o")
    private String mUrlBig;
    @SerializedName("owner")
    private String mOwner;

    public String getCaption() {
        return mCaption;
    }

    public String getId() {
        return mId;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getUrlBig() {
        return mUrlBig;
    }

    public String getOwner() {
        return mOwner;
    }

    public void setCaption(String caption) {
        mCaption = caption;
    }

    public void setId(String id) {
        mId = id;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public void setUrlBig(String mUrlBig) {
        this.mUrlBig = mUrlBig;
    }

    public void setOwner(String owner) {
        mOwner = owner;
    }

    @NonNull
    @Override
    public String toString() {
        return mCaption;
    }

    /**
     * Get public URI for photo's web page address, like:
     * https://www.flickr.com/photos/owner_id/photo_id
     *
     * @return URI to the photo's webpage
     */
    public Uri getPhotoPageUri() {
        return Uri.parse("https://www.flickr.com/photos")
                .buildUpon()
                .appendPath(mOwner)
                .appendPath(mId)
                .build();
    }
}
