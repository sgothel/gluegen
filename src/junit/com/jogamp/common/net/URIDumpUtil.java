package com.jogamp.common.net;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class URIDumpUtil {
    public static void showURX(final String urx) throws MalformedURLException, URISyntaxException {
        System.err.println("WWWWWW "+urx);
        showURL(new URL(urx));
        showURI(new URI(urx));
        System.err.println("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW");
    }

    public static void showURL(final URL url) {
        System.err.println("XXXXXX URL "+url.toString());
        System.err.println("protocol: "+url.getProtocol());
        System.err.println("auth:     "+url.getAuthority());
        System.err.println("host:     "+url.getHost());
        System.err.println("port:     "+url.getPort() + " ( " + url.getDefaultPort() + " ) " );
        System.err.println("file:     "+url.getFile() + " ( path " + url.getPath() + ", query " + url.getQuery() + " ) " );
        System.err.println("ref:      "+url.getRef());
    }

    public static void showURI(final URI uri) {
        showURI("YYYYYY URI "+uri+", isOpaque "+uri.isOpaque()+", isAbs "+uri.isAbsolute(), uri);
    }
    public static void showURI(final String message, final URI uri) {
        System.err.println(message);

        System.err.println("0.0.0 string:      "+uri.toString());
        System.err.println("0.0.0 ascii :      "+uri.toASCIIString());

        System.err.println("1.0.0 scheme:      "+uri.getScheme());
        System.err.println("2.0.0 scheme-part: "+uri.getRawSchemeSpecificPart()+" (raw), "+uri.getSchemeSpecificPart()+" (dec)");
        System.err.println("2.1.0 auth:        "+uri.getRawAuthority()+" (raw), "+uri.getAuthority()+" (dec)");
        System.err.println("2.1.1 user-info:   "+uri.getRawUserInfo()+" (raw), "+uri.getUserInfo()+" (dec)");
        System.err.println("2.1.1 host:        "+uri.getHost());
        System.err.println("2.1.1 port:        "+uri.getPort());
        System.err.println("2.2.0 path:        "+uri.getRawPath()+" (raw), "+uri.getPath()+" (dec)");
        System.err.println("2.3.0 query:       "+uri.getRawQuery()+" (raw), "+uri.getQuery()+" (dec)");
        System.err.println("3.0.0 fragment:    "+uri.getRawFragment()+" (raw), "+uri.getFragment()+" (dec)");
    }
}
