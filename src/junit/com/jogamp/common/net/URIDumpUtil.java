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

    public static void showUri(final Uri uri) throws URISyntaxException {
        showUri("ZZZZZZ Uri "+uri+", isOpaque "+uri.opaque+", isAbs "+uri.absolute+", hasAuth "+uri.hasAuthority, uri);
    }

    public static void showUri(final String message, final Uri uri) throws URISyntaxException {
        System.err.println(message);

        System.err.println("0.0.0 string:      "+uri.toString());
        System.err.println("0.0.0 ascii :      "+uri.toASCIIString());
        System.err.println("0.0.0 native-file: "+uri.toFile());
        System.err.println("0.0.0 contained:   "+uri.getContainedUri());

        System.err.println("1.0.0 scheme:      "+uri.scheme);
        System.err.println("2.0.0 scheme-part: "+uri.schemeSpecificPart+" (raw), "+Uri.decode(uri.schemeSpecificPart)+" (dec)");
        System.err.println("2.1.0 auth:        "+uri.authority+" (raw), "+Uri.decode(uri.authority)+" (dec)");
        System.err.println("2.1.1 user-info:   "+uri.userInfo+" (raw), "+Uri.decode(uri.userInfo)+" (dec)");
        System.err.println("2.1.1 host:        "+uri.host);
        System.err.println("2.1.1 port:        "+uri.port);
        System.err.println("2.2.0 path:        "+uri.path+" (raw), "+Uri.decode(uri.path)+" (dec)");
        System.err.println("2.3.0 query:       "+uri.query+" (raw), "+Uri.decode(uri.query)+" (dec)");
        System.err.println("3.0.0 fragment:    "+uri.fragment+" (raw), "+Uri.decode(uri.fragment)+" (dec)");
    }

    /**
     * Just showing different encoding of Uri -> URI
     *
     * @param uri
     * @throws URISyntaxException
     */
    public static void showReencodedURIOfUri(final Uri uri) throws URISyntaxException {
        final URI recomposedURI = uri.toURIReencoded();
        showURI("YYYYYY Recomposed URI "+recomposedURI+", isOpaque "+recomposedURI.isOpaque()+", isAbs "+recomposedURI.isAbsolute(), recomposedURI);
        final String recomposedURIStr = recomposedURI.toString();
        final boolean equalsRecompURI = uri.input.equals(recomposedURIStr);
        System.err.println("source   Uri: "+uri.input);
        System.err.println("recomp   URI: "+recomposedURIStr+" - "+(equalsRecompURI?"EQUAL":"UNEQUAL"));
    }

   /**
     * Just showing different encoding of URI -> Uri
     *
     * @param uri
     * @throws URISyntaxException
     */
    public static void showReencodedUriOfURI(final URI uri) throws URISyntaxException {
        final Uri recomposedUri = Uri.valueOf(uri);
        showUri("ZZZZZZ Recomposed Uri "+recomposedUri+", isOpaque "+recomposedUri.opaque+", isAbs "+recomposedUri.absolute+", hasAuth "+recomposedUri.hasAuthority, recomposedUri);
        final String recomposedUriStr = recomposedUri.toString();
        final boolean equalsRecompUri = uri.toString().equals(recomposedUriStr);
        System.err.println("source   URI: "+uri.toString());
        System.err.println("recomp   Uri: "+recomposedUriStr+" - "+(equalsRecompUri?"EQUAL":"UNEQUAL"));
     }
}
