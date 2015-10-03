package com.jogamp.common.net;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import com.jogamp.common.os.AndroidVersion;
import com.jogamp.common.util.IOUtil;

/**
 * See {@link PiggybackURLConnection} for description and examples.
 */
public abstract class AssetURLContext implements PiggybackURLContext {
    private static final boolean DEBUG = IOUtil.DEBUG;

    /** The <i>asset URL</i> protocol name <code>asset</code> */
    public static final String asset_protocol = "asset";

    /** The <i>asset URL</i> protocol prefix <code>asset:</code> */
    public static final String asset_protocol_prefix = "asset:";

    /**
     * The <i>optional</i> <i>asset</i> folder name with ending slash <code>assets/</code>.
     * <p>
     * Note that the <i>asset</i> folder is not used on all platforms using the <i>asset</i> protocol
     * and you should not rely on it, use {@link AssetURLConnection#getEntryName()}.
     * </p>
     **/
    public static final String assets_folder = "assets/";

    public static AssetURLContext create(final ClassLoader cl) {
        return new AssetURLContext() {
            @Override
            public ClassLoader getClassLoader() {
                return cl;
            }
        };
    }

    public static AssetURLStreamHandler createHandler(final ClassLoader cl) {
        return new AssetURLStreamHandler(create(cl));
    }

    /**
     * Create an <i>asset</i> URL, suitable even w/o the registered <i>asset</i> URLStreamHandler.
     * <p>
     * This is equivalent with:
     * <pre>
     *   return new URL(null, path.startsWith("asset:") ? path : "asset:" + path, new AssetURLStreamHandler(cl));
     * </pre>
     * </p>
     * @param path resource path, with or w/o <code>asset:</code> prefix
     * @param cl the ClassLoader used to resolve the location, see {@link #getClassLoader()}.
     * @return
     * @throws MalformedURLException
     */
    public static URL createURL(final String path, final ClassLoader cl) throws MalformedURLException {
        return new URL(null, path.startsWith(asset_protocol_prefix) ? path : asset_protocol_prefix + path, createHandler(cl));
    }

    /**
     * Create an <i>asset</i> URL, suitable only with the registered <i>asset</i> URLStreamHandler.
     * <p>
     * This is equivalent with:
     * <pre>
     *   return new URL(path.startsWith("asset:") ? path : "asset:" + path);
     * </pre>
     * </p>
     * @param path resource path, with or w/o <code>asset:</code> prefix
     * @return
     * @throws MalformedURLException
     */
    public static URL createURL(final String path) throws MalformedURLException {
        return new URL(path.startsWith(asset_protocol_prefix) ? path : asset_protocol_prefix + path);
    }

    /**
     * Returns the <i>asset</i> handler previously set via {@link #registerHandler(ClassLoader)},
     * or null if none was set.
     */
    public static URLStreamHandler getRegisteredHandler() {
        final GenericURLStreamHandlerFactory f = GenericURLStreamHandlerFactory.register();
        return ( null != f ) ? f.getHandler(asset_protocol) : null;
    }

    /**
     * Registers the generic URLStreamHandlerFactory via {@link GenericURLStreamHandlerFactory#register()}
     * and if successful sets the <i>asset</i> <code>handler</code> for the given ClassLoader <code>cl</code>.
     *
     * @return true if successful, otherwise false
     */
    public static boolean registerHandler(final ClassLoader cl) {
        final GenericURLStreamHandlerFactory f = GenericURLStreamHandlerFactory.register();
        if( null != f ) {
            f.setHandler(asset_protocol, createHandler(cl));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns an <i>asset</i> aware ClassLoader.
     * <p>
     * The ClassLoader is required to find the <i>asset</i> resource
     * via it's <code>URL findResource(String)</code> implementation.
     * </p>
     * <p>
     * It's <code>URL findResource(String)</code> implementation shall return either
     * an <i>asset</i> URL <code>asset:sub-protocol</code> or just the sub-protocol URL.
     * </p>
     * <p>
     * For example, on Android, we <i>redirect</i> all <code>path</code> request to <i>assets/</i><code>path</code>.
     * </p>
     */
    public abstract ClassLoader getClassLoader();

    @Override
    public String getImplementedProtocol() {
        return asset_protocol;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation attempts to resolve <code>path</code> in the following order:
     * <ol>
     *   <li> as a valid URL: <code>new URL(path)</code>, use sub-protocol if <i>asset</i> URL</li>
     *   <li> via ClassLoader: {@link #getClassLoader()}.{@link ClassLoader#getResource(String) getResource(path)}, use sub-protocol if <i>asset</i> URL </li>
     *   <li> as a File: <code>new File(path).toURI().toURL()</code>
     * </ol>
     * </p>
     * <p>
     * In case of using the ClassLoader (2) <b>and</b> if running on Android,
     * the {@link #assets_folder} is being prepended to <code>path</code> if missing.
     * </p>
     **/
    @Override
    public URLConnection resolve(final String path) throws IOException {
        return resolve(path, getClassLoader());
    }

    public static URLConnection resolve(String path, final ClassLoader cl) throws IOException {
        URL url = null;
        URLConnection conn = null;
        int type = -1;

        if(DEBUG) {
            System.err.println("AssetURLContext.resolve: <"+path+">");
        }
        try {
            path = IOUtil.cleanPathString(path);
        } catch (final URISyntaxException uriEx) {
            throw new IOException(uriEx);
        }

        try {
            // lookup as valid sub-protocol
            url = new URL(path);
            conn = open(url);
            type = null != conn ? 1 : -1;
        } catch(final MalformedURLException e1) { if(DEBUG) { System.err.println("FAIL(1): "+e1.getMessage()); } }

        if(null == conn && null != cl) {
            // lookup via ClassLoader .. cleanup leading '/'
            String cpath = path;
            while(cpath.startsWith("/")) {
                cpath = cpath.substring(1);
            }
            if(AndroidVersion.isAvailable) {
                cpath = cpath.startsWith(assets_folder) ? cpath : assets_folder + cpath;
            }
            url = cl.getResource(cpath);
            conn = open(url);
            type = null != conn ? 2 : -1;
        }

        if(null == conn) {
            // lookup as File
            try {
                final File file = new File(path);
                if(file.exists()) {
                    url = Uri.valueOf(file).toURL();
                    conn = open(url);
                    type = null != conn ? 3 : -1;
                }
            } catch (final Throwable e) { if(DEBUG) { System.err.println("FAIL(3): "+e.getMessage()); } }
        }

        if(DEBUG) {
            System.err.println("AssetURLContext.resolve: type "+type+": url <"+url+">, conn <"+conn+">, connURL <"+(null!=conn?conn.getURL():null)+">");
        }
        if(null == conn) {
            throw new FileNotFoundException("Could not look-up: "+path+" as URL, w/ ClassLoader or as File");
        }
        return conn;
    }

    private static URLConnection open(final URL url) {
        if(null==url) {
            return null;
        }
        try {
            final URLConnection c = url.openConnection();
            c.connect(); // redundant
            return c;
        } catch (final IOException ioe) { if(DEBUG) { System.err.println("FAIL(2): "+ioe.getMessage()); } }
        return null;
    }

}
