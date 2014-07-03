package com.jogamp.common.net;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import com.jogamp.common.net.AssetURLConnection;

/**
 * {@link URLStreamHandler} to handle the asset protocol.
 *
 * <p>
 * This is the <i>asset</i> URLStreamHandler variation
 * for manual use.
 * </p>
 * <p>
 * It requires passing a valid {@link AssetURLContext}
 * for construction, hence it's not suitable for the pkg factory model.
 * </p>
 */
public class AssetURLStreamHandler extends URLStreamHandler {
    AssetURLContext ctx;

    public AssetURLStreamHandler(final AssetURLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected URLConnection openConnection(final URL u) throws IOException {
        final AssetURLConnection c = new AssetURLConnection(u, ctx);
        c.connect();
        return c;
    }


}
