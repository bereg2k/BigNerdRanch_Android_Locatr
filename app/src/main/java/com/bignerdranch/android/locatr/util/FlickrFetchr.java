package com.bignerdranch.android.locatr.util;

import android.location.Location;
import android.net.Uri;
import android.util.Log;

import com.bignerdranch.android.locatr.model.GalleryItem;
import com.bignerdranch.android.locatr.util.dto.RootJsonDTO;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to fetch data from Flickr:
 * <p> - download JSON files with image's data </p>
 * <p> - download images as byte objects </p>
 */
public class FlickrFetchr {

    private static final String TAG = "FlickrFetchr";
    private static final String API_KEY = "33675dac38a8a33ccdf1a30b0e67f21b";
    private static final String SECRET = "6e3ad27ab28b33d3";

    private static final String FLICKR_REST_API_URL = "https://www.flickr.com/services/rest/";
    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri ENDPOINT = Uri
            .parse(FLICKR_REST_API_URL)
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    /**
     * Download objects via provided URL and receive them in raw byte form.
     *
     * @param urlSpec URL to download data from
     * @return byte array with downloaded object's data
     * @throws IOException exception, in case a client messes up the URL somehow.
     */
    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[4096];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
            return new byte[]{};
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Download objects via provided URL and receive them in string format.
     * Normally, this is a request for JSON files to get data for {@link GalleryItem} objects.
     *
     * @param urlSpec URL to download data from
     * @return string representation of downloaded object's data
     * @throws IOException exception, in case a client messes up the URL somehow.
     */
    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    /**
     * GET request to fetch the most recent uploaded photos on Flickr.
     *
     * @param page number of current page (the results come in packs of 100 or so, hence pages)
     * @return list of all the recent photos in the form of collection of GalleryItem objects
     */
    public List<GalleryItem> fetchRecentPhotos(int page) {
        String url = buildUrl(FETCH_RECENTS_METHOD, null, String.valueOf(page));
        return downloadGalleryItems(url);
    }

    /**
     * GET request to search for photos on Flickr by user-defined query request.
     *
     * @param query search query by which to search relevant photos (e.g. "cars", "guns", "nature"...)
     * @param page  number of current page (the results come in packs of 100 or so, hence pages)
     * @return list of all the recent photos in the form of collection of GalleryItem objects
     */
    public List<GalleryItem> searchPhotos(String query, int page) {
        String url = buildUrl(SEARCH_METHOD, query, String.valueOf(page));
        return downloadGalleryItems(url);
    }

    /**
     * GET request to search for photos on Flickr near her current location.
     *
     * @param location current user's global location
     * @return list of all the recent photos in the form of collection of GalleryItem objects
     */
    public List<GalleryItem> searchPhotos(Location location) {
        String url = buildUrl(location);
        return downloadGalleryItems(url);
    }

    /**
     * Download photo's data via provided URL.
     *
     * @param url URL to request a data from
     * @return collection of {@link GalleryItem} objects parsed from URL request
     */
    private List<GalleryItem> downloadGalleryItems(String url) {
        List<GalleryItem> items = new ArrayList<>();

        try {
            // Url with page was here...
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);

            parseItemsGson(items, jsonString);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items!", ioe);
        }

        return items;
    }

    /**
     * Getting a standard URL request string for Flickr with some required parameters.
     *
     * @param method one of the set Flickr method like "flickr.photos.getRecent"
     * @param query  value for search query (optional)
     * @param page   number of page or a pack of data (optional)
     * @return
     */
    private String buildUrl(String method, String query, String page) {
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("method", method);

        if (method.equals(SEARCH_METHOD)) {
            uriBuilder.appendQueryParameter("text", query);
        }

        uriBuilder.appendQueryParameter("page", page);

        return uriBuilder.build().toString();
    }

    private String buildUrl(Location location) {
        return ENDPOINT.buildUpon()
                .appendQueryParameter("method", SEARCH_METHOD)
                .appendQueryParameter("lat", "" + location.getLatitude())
                .appendQueryParameter("lon", "" + location.getLongitude())
                .build().toString();

    }

    /**
     * Convert a data object in the JSON form into collection of {@link GalleryItem} objects.
     * <p></p>
     * Warning! For parsing JSON this method uses special DTO objects
     * to represent how to map out JSON data to actual Java objects.
     * (see {@link com.bignerdranch.android.locatr.util.dto.PhotosDTO} for reference)
     * It's required by Gson API design that is being used here.
     *
     * @param items      collection of model objects
     * @param jsonString string representation of JSON data to parse
     */
    private void parseItemsGson(List<GalleryItem> items, String jsonString) {
        Gson gson = new Gson();
        RootJsonDTO rootJsonDTO = gson.fromJson(jsonString, RootJsonDTO.class);
        GalleryItem[] galleryItemsArray = rootJsonDTO.getPhotos().getGalleryItems();

        // filter out all the items with empty URLs
        for (GalleryItem arrayItem : galleryItemsArray) {
            if (arrayItem.getUrl() == null || arrayItem.getUrl().isEmpty()) {
                continue;
            }
            items.add(arrayItem);
        }
    }

    /**
     * Convert a data object in the JSON form into collection of {@link GalleryItem} objects.
     *
     * @param items    collection of model objects
     * @param jsonBody JSON object with photo's data
     * @throws JSONException exception to throw if JSON data is messed up somehow.
     * @deprecated use {@link FlickrFetchr#parseItemsGson}
     */
    @Deprecated
    private void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws
    JSONException {
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

        for (int i = 0; i < photoJsonArray.length(); i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);

            if (!photoJsonObject.has("url_s")) {
                continue;
            }

            GalleryItem item = new GalleryItem();
            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("title"));
            item.setUrl(photoJsonObject.getString("url_s"));

            items.add(item);
        }
    }
}
