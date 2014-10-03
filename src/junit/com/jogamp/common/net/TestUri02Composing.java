package com.jogamp.common.net;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestUri02Composing extends SingletonJunitCase {

    @BeforeClass
    public static void assetRegistration() throws Exception {
        try {
            System.err.println("******* Asset URL Stream Handler Registration: PRE");
            Assert.assertTrue("GenericURLStreamHandlerFactory.register() failed", AssetURLContext.registerHandler(AssetURLConnectionRegisteredTest.class.getClassLoader()));
            Assert.assertNotNull(AssetURLContext.getRegisteredHandler());
            System.err.println("******* Asset URL Stream Handler Registration: POST");
        } catch (final Exception e) {
            setTestSupported(false);
            throw e;
        }
    }

    @Test
    public void test01URLCompositioning() throws IOException, URISyntaxException {
        testURNCompositioning("file:///rootDir/file1.txt");
        testURNCompositioning("file://host/rootDir/file1.txt");
        testURNCompositioning("jar:file:/web1/file1.jar!/rootDir/file1.txt");
        testURNCompositioning("asset:gluegen-test/info.txt");
        testURNCompositioning("asset:/gluegen-test/info.txt");
        testURNCompositioning("http://domain.com/web1/index.html?lala=23&lili=24#anchor");
        testURNCompositioning("http://domain.com:1234/web1/index.html?lala=23&lili=24#anchor");

        final Uri file1URI = Uri.cast("asset:jar:file:/web1/file1.jar!/rootDir/file1.txt");
        testURICompositioning(file1URI);
        testUriCompositioning(file1URI, Uri.cast("asset:jar:file:/web1/file1.jar!/rootDir/./file1.txt"));
        testUriCompositioning(file1URI, Uri.cast("asset:jar:file:/web1/file1.jar!/rootDir/dummyParent/../file1.txt"));

        final URL file1URL = new URL("asset:jar:file:/web1/file1.jar!/rootDir/file1.txt");
        testURLCompositioning(file1URL);
        testURLCompositioning(file1URL, new URL("asset:jar:file:/web1/file1.jar!/rootDir/./file1.txt"));
        testURLCompositioning(file1URL, new URL("asset:jar:file:/web1/file1.jar!/rootDir/dummyParent/../file1.txt"));
    }

    static void testURNCompositioning(final String urn) throws MalformedURLException, URISyntaxException {
        testURICompositioning( Uri.cast(urn) );
        testURLCompositioning( new URL(urn) );
    }

    static void testURICompositioning(final Uri uri) throws MalformedURLException, URISyntaxException {
        testUriCompositioning(uri, uri);
    }
    static void testUriCompositioning(final Uri refURI, final Uri uri1) throws MalformedURLException, URISyntaxException {
        System.err.println("scheme <"+uri1.scheme+">, ssp <"+uri1.schemeSpecificPart+">, fragment <"+uri1.fragment+">");
        final Uri uri2 = uri1.getRelativeOf(null);

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
        final Uri uri1 = Uri.valueOf(url1);
        System.err.println("scheme <"+uri1.scheme+">, ssp <"+uri1.schemeSpecificPart+">, fragment <"+uri1.fragment+">");
        final Uri uri2 = uri1.getRelativeOf(null);

        System.err.println("URL-equals(1): "+refURL.toURI().equals(uri2));
        System.err.println("URL-equals(2): "+refURL.equals(uri2.toURL()));
        System.err.println("URL-same  : "+refURL.sameFile(uri2.toURL()));
        System.err.println("URL-ref   : <"+refURL+">");
        System.err.println("URL-orig  : <"+url1+">");
        System.err.println("URL-comp  : <"+uri2+">");
        Assert.assertEquals(Uri.valueOf(refURL), uri2);
        Assert.assertEquals(refURL, uri2.toURL());
        Assert.assertTrue(refURL.sameFile(uri2.toURL()));
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestUri02Composing.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
