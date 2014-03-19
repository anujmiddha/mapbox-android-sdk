package com.mapbox.mapboxsdk.tileprovider;

import android.graphics.drawable.Drawable;
import com.mapbox.mapboxsdk.tileprovider.constants.TileLayerConstants;
import com.mapbox.mapboxsdk.tileprovider.util.LowMemoryException;
import com.mapbox.mapboxsdk.views.util.constants.MapViewConstants;

import java.io.InputStream;

public class TileLayer implements ITileLayer, TileLayerConstants, MapViewConstants {

    private String mUrl;
    protected float mMinimumZoomLevel = 1;
    protected float mMaximumZoomLevel = 16;
    private final int mTileSizePixels = DEFAULT_TILE_SIZE;

    public TileLayer(final String aUrl) {
        mUrl = aUrl;
    }

    public TileLayer setURL(final String aUrl) {
        mUrl = aUrl;
        return this;
    }

    public String getTileURL(final MapTile aTile, boolean hdpi) {
        return mUrl
                .replace("{z}", String.valueOf(aTile.getZ()))
                .replace("{x}", String.valueOf(aTile.getX()))
                .replace("{y}", String.valueOf(aTile.getY()))
                .replace("{2x}", hdpi ? "@2x" : "");
    }

    @Override
    public float getMinimumZoomLevel() {
        return mMinimumZoomLevel;
    }

    @Override
    public float getMaximumZoomLevel() {
        return mMaximumZoomLevel;
    }

    @Override
    public int getTileSizePixels() {
        return mTileSizePixels;
    }

    @Override
    public Drawable getDrawable(final InputStream aFileInputStream) throws LowMemoryException {
        return null;
    }

    final private String TAG = "OnlineTileSource";
}
