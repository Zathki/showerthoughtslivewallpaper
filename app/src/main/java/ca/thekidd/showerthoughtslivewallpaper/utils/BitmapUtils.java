package ca.thekidd.showerthoughtslivewallpaper.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;


/**
 *  Utilities for loading a bitmap from a URL
 *
 */
public class BitmapUtils {

    private static final String TAG = "Panoramio";

    private static final int IO_BUFFER_SIZE = 4 * 1024;

    /**
     * Loads a bitmap from the specified url. This can take a while, so it should not
     * be called from the UI thread.
     *
     * @param url The location of the bitmap asset
     *
     * @return The bitmap, or null if it could not be loaded
     */
    public static Bitmap loadBitmap(String url, int height) {
        InputStream is;
        try {
            is = new BufferedInputStream(new URL(url).openStream(), IO_BUFFER_SIZE);
        } catch (Exception e) {
            Log.e(TAG, "Image not found.", e);
            return null;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, opts);

        // scale the image
        float scaleFactor = ((float)height) / opts.outHeight;
        // do not upscale!
        //if (scaleFactor < 1) {
            opts.inDensity = 10000;
            opts.inTargetDensity = (int) ((float) opts.inDensity * scaleFactor);
        Log.d("Scale", opts.inDensity + " " + opts.inTargetDensity + " " + scaleFactor);
        //}
        opts.inJustDecodeBounds = false;

        try {
            is.close();
        } catch (IOException e) {
            // ignore
        }
        try {
            is = new BufferedInputStream(new URL(url).openStream(), IO_BUFFER_SIZE);
        } catch (Exception e) {
            Log.e(TAG, "Image not found.", e);
            return null;
        }
        Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);
        try {
            is.close();
        } catch (IOException e) {
            // ignore
        }

        return bitmap;
    }

    /**
     * Closes the specified stream.
     *
     * @param stream The stream to close.
     */
    private static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                android.util.Log.e(TAG, "Could not close stream", e);
            }
        }
    }

    /**
     * Copy the content of the input stream into the output stream, using a
     * temporary byte array buffer whose size is defined by
     * {@link #IO_BUFFER_SIZE}.
     *
     * @param in The input stream to copy from.
     * @param out The output stream to copy to.
     * @throws IOException If any error occurs during the copy.
     */
    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] b = new byte[IO_BUFFER_SIZE];
        int read;
        while ((read = in.read(b)) != -1) {
            out.write(b, 0, read);
        }
    }

}