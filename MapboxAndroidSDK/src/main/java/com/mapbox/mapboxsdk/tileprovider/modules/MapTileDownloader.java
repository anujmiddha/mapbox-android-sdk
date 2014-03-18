package com.mapbox.mapboxsdk.tileprovider.modules;

import android.widget.ImageView;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import com.mapbox.mapboxsdk.tileprovider.MapTileRequestState;
import com.mapbox.mapboxsdk.tileprovider.tilesource.ITileLayer;
import com.squareup.okhttp.HttpResponseCache;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.tileprovider.tilesource.TileLayer;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import com.squareup.picasso.Picasso;

/**
 * The {@link MapTileDownloader} loads tiles from an HTTP server.
 */
public class MapTileDownloader {
    private static final String TAG = "Tile downloader";

    private final AtomicReference<TileLayer> mTileSource = new AtomicReference<TileLayer>();

    private final INetworkAvailabilityCheck mNetworkAvailablityCheck;
    private MapView mapView;
    HttpResponseCache cache;
    boolean hdpi;

    ArrayList<Boolean> threadControl = new ArrayList<Boolean>();

    public MapTileDownloader(final ITileLayer pTileSource,
                             final INetworkAvailabilityCheck pNetworkAvailablityCheck,
                             final MapView mapView) {
        this.mapView = mapView;

        hdpi = mapView.getContext().getResources()
                .getDisplayMetrics().densityDpi > DisplayMetrics.DENSITY_HIGH;

        mNetworkAvailablityCheck = pNetworkAvailablityCheck;
        setTileSource(pTileSource);
    }

    public void setTileSource(final ITileLayer tileSource) {
        // We are only interested in TileLayer tile sources
        if (tileSource instanceof TileLayer) {
            mTileSource.set((TileLayer) tileSource);
        } else {
            // Otherwise shut down the tile downloader
            mTileSource.set(null);
        }
    }

    protected class TileLoader extends MapTileModuleLayerBase.TileLoader {

        @Override
        public Drawable loadTile(final MapTileRequestState aState) throws CantContinueException {
            threadControl.add(false);
            TileLayer tileLayer = mTileSource.get();
            String url = tileLayer.getTileURL(aState.getMapTile(), hdpi);
            ImageView imageView = new ImageView(mapView.getContext());

            Picasso.with(mapView.getContext()).load(url).into(imageView);

            return null;
        }

        @Override
        protected void tileLoaded(final MapTileRequestState pState, Drawable pDrawable) {
            removeTileFromQueues(pState.getMapTile());
            pState.getCallback().mapTileRequestCompleted(pState, pDrawable);
        }
    }
}
