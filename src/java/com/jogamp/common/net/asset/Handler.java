package com.jogamp.common.net.asset;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import com.jogamp.common.net.AssetURLConnection;
import com.jogamp.common.net.AssetURLContext;

/**
 * {@link URLStreamHandler} to handle the asset protocol.
 *
 * <p>
 * This is the <i>asset</i> URLStreamHandler variation
 * using this class ClassLoader for the pkg factory model.
 * </p>
 */
public class Handler extends URLStreamHandler {
    static final AssetURLContext localCL = new AssetURLContext() {
            @Override
            public ClassLoader getClassLoader() {
                return Handler.class.getClassLoader();
            }
        };

    public Handler() {
        super();
    }

    @Override
    protected URLConnection openConnection(final URL u) throws IOException {
        final AssetURLConnection c = new AssetURLConnection(u, localCL);
        c.connect();
        return c;
    }

}
