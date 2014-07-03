package com.jogamp.common.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.common.util.IOUtil;
import com.jogamp.junit.util.JunitTracer;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestIOUtilURICompose extends JunitTracer {

    @Test
    public void test01URLCompositioning() throws IOException, URISyntaxException {
        testURNCompositioning("file:///rootDir/file1.txt");
        testURNCompositioning("file://host/rootDir/file1.txt");
        testURNCompositioning("jar:file:/web1/file1.jar!/rootDir/file1.txt");
        testURNCompositioning("asset:gluegen-test/info.txt");
        testURNCompositioning("asset:/gluegen-test/info.txt");
        testURNCompositioning("http://domain.com/web1/index.html?lala=23&lili=24#anchor");
        testURNCompositioning("http://domain.com:1234/web1/index.html?lala=23&lili=24#anchor");

        final URI file1URI = new URI("asset:jar:file:/web1/file1.jar!/rootDir/file1.txt");
        testURICompositioning(file1URI);
        testURICompositioning(file1URI, new URI("asset:jar:file:/web1/file1.jar!/rootDir/./file1.txt"));
        testURICompositioning(file1URI, new URI("asset:jar:file:/web1/file1.jar!/rootDir/dummyParent/../file1.txt"));

        final URL file1URL = new URL("asset:jar:file:/web1/file1.jar!/rootDir/file1.txt");
        testURLCompositioning(file1URL);
        testURLCompositioning(file1URL, new URL("asset:jar:file:/web1/file1.jar!/rootDir/./file1.txt"));
        testURLCompositioning(file1URL, new URL("asset:jar:file:/web1/file1.jar!/rootDir/dummyParent/../file1.txt"));
    }

    static void testURNCompositioning(final String urn) throws MalformedURLException, URISyntaxException {
        testURICompositioning( new URI(urn) );
        testURLCompositioning( new URL(urn) );
    }

    static void testURICompositioning(final URI uri) throws MalformedURLException, URISyntaxException {
        testURICompositioning(uri, uri);
    }
    static void testURICompositioning(final URI refURI, final URI uri1) throws MalformedURLException, URISyntaxException {
        final String scheme = uri1.getScheme();
        final String ssp = uri1.getRawSchemeSpecificPart();
        final String fragment = uri1.getRawFragment();

        System.err.println("scheme <"+scheme+">, ssp <"+ssp+">, fragment <"+fragment+">");
        final URI uri2 = IOUtil.compose(scheme, ssp, null, fragment);

        System.err.println("URL-equals: "+refURI.equals(uri2));
        System.err.println("URL-ref   : <"+refURI+">");
        System.err.println("URL-orig  : <"+uri1+">");
        System.err.println("URL-comp  : <"+uri2+">");
        Assert.assertEquals(refURI, uri2);
    }

    static void testURLCompositioning(final URL url) throws MalformedURLException, URISyntaxException {
        testURLCompositioning(url, url);
    }
    static void testURLCompositioning(final URL refURL, final URL url1) throws MalformedURLException, URISyntaxException {
        final URI uri1 = url1.toURI();
        final String scheme = uri1.getScheme();
        final String ssp = uri1.getRawSchemeSpecificPart();
        final String fragment = uri1.getRawFragment();

        System.err.println("scheme <"+scheme+">, ssp <"+ssp+">, fragment <"+fragment+">");
        final URI uri2 = IOUtil.compose(scheme, ssp, null, fragment);

        System.err.println("URL-equals(1): "+refURL.toURI().equals(uri2));
        System.err.println("URL-equals(2): "+refURL.equals(uri2.toURL()));
        System.err.println("URL-same  : "+refURL.sameFile(uri2.toURL()));
        System.err.println("URL-ref   : <"+refURL+">");
        System.err.println("URL-orig  : <"+url1+">");
        System.err.println("URL-comp  : <"+uri2+">");
        Assert.assertEquals(refURL.toURI(), uri2);
        Assert.assertEquals(refURL, uri2.toURL());
        Assert.assertTrue(refURL.sameFile(uri2.toURL()));
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestIOUtilURICompose.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
