package com.mapbox.mapboxsdk;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import com.mapbox.mapboxsdk.util.BitmapUtils;
import com.mapbox.mapboxsdk.views.util.constants.MapViewConstants;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

/**
 * Default implementation of {@link ResourceProxy} that returns fixed string to get
 * string resources and reads the jar package to get bitmap resources.
 */
public class DefaultResourceProxyImpl implements ResourceProxy, MapViewConstants {

    private Resources mResources;
    private DisplayMetrics mDisplayMetrics;

    /**
     * Constructor.
     *
     * @param pContext Used to get the display metrics that are used for scaling the bitmaps returned by
     *                 {@link #getBitmap} and {@link #getDrawable}.
     *                 Can be null, in which case the bitmaps are not scaled.
     */
    public DefaultResourceProxyImpl(final Context pContext) {
        if (pContext != null) {
            mResources = pContext.getResources();
            mDisplayMetrics = mResources.getDisplayMetrics();
            if (DEBUGMODE) {
                Log.i(TAG, "mDisplayMetrics=" + mDisplayMetrics);
            }
        }
    }

    @Override
    public String getString(final string pResId) {
        switch (pResId) {
            case format_distance_meters:
                return "%s m";
            case format_distance_kilometers:
                return "%s km";
            case format_distance_miles:
                return "%s mi";
            case format_distance_nautical_miles:
                return "%s nm";
            case format_distance_feet:
                return "%s ft";
            case online_mode:
                return "Online mode";
            case offline_mode:
                return "Offline mode";
            case my_location:
                return "My location";
            case compass:
                return "Compass";
            case map_mode:
                return "Map mode";
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public String getString(final string pResId, final Object... formatArgs) {
        return String.format(getString(pResId), formatArgs);
    }

    @Override
    public Bitmap getBitmap(final bitmap pResId) {
        InputStream is = null;
        try {
            final String resName = pResId.name() + ".png";
            is = ResourceProxy.class.getResourceAsStream(resName);
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + resName);
            }
            BitmapFactory.Options options = null;
            if (mDisplayMetrics != null) {
                options = BitmapUtils.getBitmapOptions(mDisplayMetrics);
            }
            return BitmapFactory.decodeStream(is, null, options);
        } catch (final OutOfMemoryError e) {
            Log.e(TAG, "OutOfMemoryError getting bitmap resource: " + pResId);
            System.gc();
            // there's not much we can do here
            // - when we load a bitmap from resources we expect it to be found
            throw e;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (final IOException ignore) {
                }
            }
        }
    }

    @Override
    public Drawable getDrawable(final bitmap pResId) {
        return mResources != null
                ? new BitmapDrawable(mResources, getBitmap(pResId))
                : new BitmapDrawable(getBitmap(pResId));
    }

    @Override
    public float getDisplayMetricsDensity() {
        return mDisplayMetrics.density;
    }

    private static final String TAG = "DefaultResourceProxyImpl";
}
