package com.mapbox.mapboxsdk.tileprovider;

import android.graphics.drawable.Drawable;
import com.mapbox.mapboxsdk.tileprovider.constants.TileLayerConstants;
import com.mapbox.mapboxsdk.tileprovider.util.LowMemoryException;
import com.mapbox.mapboxsdk.views.util.constants.MapViewConstants;
import com.mapbox.mapboxsdk.views.MapView;

import java.io.InputStream;

public class TileLayer implements ILayer, TileLayerConstants, MapViewConstants {

    private String mUrl;
    private MapView mv;
    protected float mMinimumZoomLevel = 1;
    protected float mMaximumZoomLevel = 16;
    private final int mTileSizePixels = DEFAULT_TILE_SIZE;

    public TileLayer(final String aUrl) {
        mUrl = aUrl;
    }

    public TileLayer addTo(MapView mv) {
        this.mv = mv;
        return this;
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

    public void update() {

    }

    final private String TAG = "OnlineTileSource";
}
