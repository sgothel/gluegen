package com.jogamp.common.net;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

public class GenericURLStreamHandlerFactory implements URLStreamHandlerFactory {
    private static GenericURLStreamHandlerFactory factory = null;

    private final Map<String, URLStreamHandler> protocolHandlers;

    private GenericURLStreamHandlerFactory() {
        protocolHandlers = new HashMap<String, URLStreamHandler>();
    }

    /**
     * Sets the <code>handler</code> for <code>protocol</code>.
     *
     * @return the previous set <code>handler</code>, or null if none was set.
     */
    public synchronized final URLStreamHandler setHandler(final String protocol, final URLStreamHandler handler) {
        return protocolHandlers.put(protocol, handler);
    }

    /**
     * Returns the <code>protocol</code> handler previously set via {@link #setHandler(String, URLStreamHandler)},
     * or null if none was set.
     */
    public synchronized final URLStreamHandler getHandler(final String protocol) {
        return protocolHandlers.get(protocol);
    }

    @Override
    public synchronized final URLStreamHandler createURLStreamHandler(final String protocol) {
        return getHandler(protocol);
    }

    /**
     * Returns the singleton instance of the registered GenericURLStreamHandlerFactory
     * or null if registration was not successful.
     * <p>
     * Registration is only performed once.
     * </p>
     */
    public synchronized static GenericURLStreamHandlerFactory register() {
        if(null == factory) {
            factory = AccessController.doPrivileged(new PrivilegedAction<GenericURLStreamHandlerFactory>() {
                @Override
                public GenericURLStreamHandlerFactory run() {
                    boolean ok = false;
                    final GenericURLStreamHandlerFactory f = new GenericURLStreamHandlerFactory();
                    try {
                        URL.setURLStreamHandlerFactory(f);
                        ok = true;
                    } catch (final Throwable e) {
                        System.err.println("GenericURLStreamHandlerFactory: Setting URLStreamHandlerFactory failed: "+e.getMessage());
                    }
                    return ok ? f : null;
                } } );
        }
        return factory;
    }
}
