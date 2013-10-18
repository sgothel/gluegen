package com.jogamp.common.net;

import java.io.IOException;
import java.net.URLConnection;

/**
 * See {@link PiggybackURLConnection} for description and examples.
 */
public interface PiggybackURLContext {

    /** Returns the specific protocol, constant for this implementation. */
    public String getImplementedProtocol();

    /**
     * Resolving path to a URL sub protocol and return it's open URLConnection
     **/
    public URLConnection resolve(String path) throws IOException;
}
