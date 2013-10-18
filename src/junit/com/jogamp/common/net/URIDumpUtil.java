package com.jogamp.common.net;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class URIDumpUtil {
    public static void showURX(String urx) throws MalformedURLException, URISyntaxException {
        System.err.println("XXXXXX "+urx);
        showURL(new URL(urx));
        showURI(new URI(urx));
        System.err.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    }

    public static void showURL(URL url) {
        System.err.println("YYYYYY URL "+url);
        System.err.println("protocol: "+url.getProtocol());
        System.err.println("auth:     "+url.getAuthority());
        System.err.println("host:     "+url.getHost());
        System.err.println("port:     "+url.getPort() + " ( " + url.getDefaultPort() + " ) " );
        System.err.println("file:     "+url.getFile() + " ( path " + url.getPath() + ", query " + url.getQuery() + " ) " );
        System.err.println("ref:      "+url.getRef());
    }
    public static void showURI(URI uri) {
        System.err.println("ZZZZZZ URI "+uri);
        // 1 [scheme:]scheme-specific-part[#fragment]
        System.err.println("1 scheme:      "+uri.getScheme());
        System.err.println("1 scheme-part: "+uri.getRawSchemeSpecificPart());
        System.err.println("1 fragment:    "+uri.getRawFragment());

        // 2 [scheme:][//authority][path][?query][#fragment]
        System.err.println("2 scheme:      "+uri.getScheme());
        System.err.println("2 auth:        "+uri.getRawAuthority());
        System.err.println("2 path:        "+uri.getRawPath());
        System.err.println("2 query:       "+uri.getRawQuery());
        System.err.println("2 fragment:    "+uri.getRawFragment());

        // 3 [scheme:][//authority][path][?query][#fragment]
        //   authority: [user-info@]host[:port]
        System.err.println("3 scheme:      "+uri.getScheme());
        System.err.println("3 user-info:   "+uri.getRawUserInfo());
        System.err.println("3 host:        "+uri.getHost());
        System.err.println("3 port:        "+uri.getPort());
        System.err.println("3 path:        "+uri.getRawPath());
        System.err.println("3 query:       "+uri.getRawQuery());
        System.err.println("3 fragment:    "+uri.getRawFragment());
    }
}
