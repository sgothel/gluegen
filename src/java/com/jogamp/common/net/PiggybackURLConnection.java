package com.jogamp.common.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Generic resource location protocol connection,
 * using another sub-protocol as the vehicle for a piggyback protocol.
 * <p>
 * The details of the sub-protocol can be queried using {@link #getSubProtocol()}.
 * </p>
 * <p>
 * See example in {@link AssetURLConnection}.
 * </p>
 */
public abstract class PiggybackURLConnection<I extends PiggybackURLContext> extends URLConnection {
    protected URL subUrl;
    protected URLConnection subConn;
    protected I context;

    /**
     * @param url the specific URL for this instance
     * @param context the piggyback context, defining state independent code and constants
     */
    protected PiggybackURLConnection(final URL url, final I context) {
        super(url);
        this.context = context;
    }

    /**
     * <p>
     * Resolves the URL via {@link PiggybackURLContext#resolve(String)},
     * see {@link AssetURLContext#resolve(String)} for an example.
     * </p>
     *
     * {@inheritDoc}
     */
    @Override
    public synchronized void connect() throws IOException {
        if(!connected) {
            subConn = context.resolve(url.getPath());
            subUrl = subConn.getURL();
            connected = true;
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if(!connected) {
            throw new IOException("not connected");
        }
        return subConn.getInputStream();
    }

    /**
     * Returns the <i>entry name</i> of the asset.
     * <pre>
     * Plain     asset:test/lala.txt
     * Resolved  asset:jar:file:/data/app/jogamp.test.apk!/assets/test/lala.txt
     * Result          test/lala.txt
     * </pre>
     * @throws IOException is not connected
     **/
    public abstract String getEntryName() throws IOException;

    /**
     * Returns the resolved <i>sub protocol</i> of the asset or null, ie:
     * <pre>
     * Plain     asset:test/lala.txt
     * Resolved  asset:jar:file:/data/app/jogamp.test.apk!/assets/test/lala.txt
     * Result          jar:file:/data/app/jogamp.test.apk!/assets/test/lala.txt
     * </pre>
     *
     * @throws IOException is not connected
     */
    public URL getSubProtocol() throws IOException {
        if(!connected) {
            throw new IOException("not connected");
        }
        return subUrl;
    }
}
