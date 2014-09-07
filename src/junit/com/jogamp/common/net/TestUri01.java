package com.jogamp.common.net;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import jogamp.common.os.PlatformPropsImpl;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.common.net.URIDumpUtil;
import com.jogamp.common.os.Platform;
import com.jogamp.junit.util.JunitTracer;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestUri01 extends JunitTracer {

    @Test
    public void test00URIEscapeSpecialChars() throws IOException, URISyntaxException {
        {
            final String vanilla = "XXX ! # $ & ' ( ) * + , / : ; = ? @ [ ]";
            final Uri.Encoded escaped = Uri.Encoded.cast("XXX%20!%20%23%20%24%20%26%20%27%20%28%20%29%20%2A%20%2B%20%2C%20/%20%3A%20%3B%20%3D%20%3F%20%40%20%5B%20%5D");
            System.err.println("vanilla "+vanilla);
            final Uri.Encoded esc1 = new Uri.Encoded(vanilla, Uri.PATH_MIN_LEGAL);
            System.err.println("esc1 "+esc1);
            Assert.assertEquals(escaped, esc1);

            final String invEsc1 = esc1.decode();
            System.err.println("inv(esc1) "+invEsc1);
            Assert.assertEquals(vanilla, invEsc1);
        }
        {
            final String vanilla = "/XXX R!# R$&'()*+,/:;=?z@y[x]";
            final Uri.Encoded escaped = Uri.Encoded.cast("/XXX%20R!%23%20R%24%26%27%28%29%2A%2B%2C/%3A%3B%3D%3Fz%40y%5Bx%5D");
            System.err.println("vanilla "+vanilla);
            final Uri.Encoded esc1 = new Uri.Encoded(vanilla, Uri.PATH_MIN_LEGAL);
            System.err.println("esc1 "+esc1);
            Assert.assertEquals(escaped, esc1);

            final String invEsc1 = esc1.decode();
            System.err.println("inv(esc1) "+invEsc1);
            Assert.assertEquals(vanilla, invEsc1);
        }
        {
            // Bug 908:    $ ^ ~ # [ ]
            final String vanilla = "/XXX $ ^ ~ # [ ]";
            showDump0x(vanilla);
        }
        {
            // Windows invalid File characters: * ? " < > |
            final String vanilla = "/XXX ! & ' ( ) + , / ; = @ [ ]";
            showDump0x(vanilla);
        }
    }
    @Test
    public void test01URIEscapeCommonChars() throws IOException, URISyntaxException {
        {
            final String vanilla = "/XXX \"%-.<>\\^_`{|}~";
            final Uri.Encoded escaped = Uri.Encoded.cast("/XXX%20%22%25-.%3C%3E%5C%5E_%60%7B%7C%7D%7E");
            System.err.println("vanilla "+vanilla);
            final Uri.Encoded esc1 = new Uri.Encoded(vanilla, Uri.PATH_MIN_LEGAL);
            System.err.println("esc1 "+esc1);
            Assert.assertEquals(escaped, esc1);

            final String invEsc1 = esc1.decode();
            System.err.println("inv(esc1) "+invEsc1);
            Assert.assertEquals(vanilla, invEsc1);
            showDump0x(vanilla);
        }
    }
    private static void showDump0x(final String string) throws IOException, URISyntaxException {
        final File file = new File(string);
        System.err.println("file "+file);
        System.err.println("file.path.dec "+file.getPath());
        System.err.println("file.path.abs "+file.getAbsolutePath());
        System.err.println("file.path.can "+file.getCanonicalPath());

        System.err.println("File-path -> Uri:");
        final Uri uri0 = Uri.valueOfFilepath(string);
        URIDumpUtil.showUri(uri0);

        System.err.println("Uri -> File:");
        final Uri uri2 = Uri.valueOf(file);
        URIDumpUtil.showUri(uri2);

        System.err.println("Uri -> URI:");
        final URI uri3 = uri2.toURI();
        URIDumpUtil.showURI(uri3);

        System.err.println("URI -> Uri (keep encoding):");
        final Uri uri4 = Uri.valueOf(uri3, false);
        URIDumpUtil.showUri(uri4);

        System.err.println("URI -> Uri (re-encode):");
        final Uri uri5 = Uri.valueOf(uri3, true);
        URIDumpUtil.showUri(uri5);
    }

    @Test
    public void test03EqualsAndHashCode() throws IOException, URISyntaxException {
        {
            final Uri uri0 = Uri.cast("http://localhost/test01.html#tag01");
            final Uri uri1 = Uri.create("http", null, "localhost", -1, "/test01.html", null, "tag01");
            final Uri uri2 = Uri.create("http", "localhost", "/test01.html", "tag01");

            Assert.assertEquals(uri0, uri1);
            Assert.assertEquals(uri0.hashCode(), uri1.hashCode());

            Assert.assertEquals(uri0, uri2);
            Assert.assertEquals(uri0.hashCode(), uri2.hashCode());

            Assert.assertEquals(uri1, uri2);
            Assert.assertEquals(uri1.hashCode(), uri2.hashCode());

            final Uri uriA = Uri.create("http", null, "localhost", -1, "/test02.html", null, "tag01");
            final Uri uriB = Uri.create("http", null, "localhost", -1, "/test01.html", null, "tag02");
            final Uri uriC = Uri.create("http", null, "lalalhost", -1, "/test01.html", null, "tag01");
            final Uri uriD = Uri.create("sftp", null, "localhost", -1, "/test01.html", null, "tag01");

            Assert.assertNotEquals(uri1, uriA);
            Assert.assertNotEquals(uri1, uriB);
            Assert.assertNotEquals(uri1, uriC);
            Assert.assertNotEquals(uri1, uriD);
        }
        {   // 3 [scheme:][//[user-info@]host[:port]]path[?query][#fragment]
            final Uri uri0 = Uri.cast("http://user@localhost:80/test01.html?test=01&test=02#tag01");
            final Uri uri1 = Uri.create("http", "user", "localhost", 80, "/test01.html", "test=01&test=02", "tag01");

            Assert.assertEquals(uri0, uri1);
            Assert.assertEquals(uri0.hashCode(), uri1.hashCode());

            final Uri uriA = Uri.cast("http://user@localhost:80/test01.html?test=01&test=02#tag02");
            final Uri uriB = Uri.cast("http://user@localhost:80/test01.html?test=01&test=03#tag01");
            final Uri uriC = Uri.cast("http://user@localhost:80/test04.html?test=01&test=02#tag01");
            final Uri uriD = Uri.cast("http://user@localhost:88/test01.html?test=01&test=02#tag01");
            final Uri uriE = Uri.cast("http://user@lalalhost:80/test01.html?test=01&test=02#tag01");
            final Uri uriF = Uri.cast("http://test@localhost:80/test01.html?test=01&test=02#tag01");
            final Uri uriG = Uri.cast("sftp://user@localhost:80/test01.html?test=01&test=02#tag01");

            Assert.assertNotEquals(uri1, uriA);
            Assert.assertNotEquals(uri1, uriB);
            Assert.assertNotEquals(uri1, uriC);
            Assert.assertNotEquals(uri1, uriD);
            Assert.assertNotEquals(uri1, uriE);
            Assert.assertNotEquals(uri1, uriF);
            Assert.assertNotEquals(uri1, uriG);
        }
    }

    @Test
    public void test04ContainedUri() throws IOException, URISyntaxException {
        {
            final Uri input = Uri.cast("http://localhost/test01.html#tag01");
            final Uri contained = input.getContainedUri();
            Assert.assertNull(contained);
        }
        {
            final Uri input     = Uri.cast("jar:http://localhost/test01.jar!/com/jogamp/Lala.class#tag01");
            final Uri expected  = Uri.cast("http://localhost/test01.jar#tag01");
            final Uri contained = input.getContainedUri();
            Assert.assertEquals(expected, contained);
            Assert.assertEquals(expected.hashCode(), contained.hashCode());
        }
        {
            final Uri input     = Uri.cast("jar:file://localhost/test01.jar!/");
            final Uri expected  = Uri.cast("file://localhost/test01.jar");
            final Uri contained = input.getContainedUri();
            Assert.assertEquals(expected, contained);
            Assert.assertEquals(expected.hashCode(), contained.hashCode());
        }
        {
            final Uri input     = Uri.cast("sftp:http://localhost/test01.jar?lala=01#tag01");
            final Uri expected  = Uri.cast("http://localhost/test01.jar?lala=01#tag01");
            final Uri contained = input.getContainedUri();
            Assert.assertEquals(expected, contained);
            Assert.assertEquals(expected.hashCode(), contained.hashCode());
        }
    }

    @Test
    public void test05ParentUri() throws IOException, URISyntaxException {
        {
            final Uri input = Uri.cast("http://localhost/");
            final Uri parent = input.getParent();
            Assert.assertNull(parent);
        }
        {
            final Uri input     = Uri.cast("jar:http://localhost/test01.jar!/com/Lala.class");
            final Uri expected1 = Uri.cast("jar:http://localhost/test01.jar!/com/");
            final Uri expected2 = Uri.cast("jar:http://localhost/test01.jar!/");
            final Uri expected3 = Uri.cast("jar:http://localhost/");
            final Uri parent1 = input.getParent();
            final Uri parent2 = parent1.getParent();
            final Uri parent3 = parent2.getParent();
            Assert.assertEquals(expected1, parent1);
            Assert.assertEquals(expected1.hashCode(), parent1.hashCode());
            Assert.assertEquals(expected2, parent2);
            Assert.assertEquals(expected2.hashCode(), parent2.hashCode());
            Assert.assertEquals(expected3, parent3);
            Assert.assertEquals(expected3.hashCode(), parent3.hashCode());
        }
        {
            final Uri input     = Uri.cast("http://localhost/dir/test01.jar?lala=01#frag01");
            final Uri expected1 = Uri.cast("http://localhost/dir/");
            final Uri expected2 = Uri.cast("http://localhost/");
            final Uri parent1 = input.getParent();
            final Uri parent2 = parent1.getParent();
            Assert.assertEquals(expected1, parent1);
            Assert.assertEquals(expected1.hashCode(), parent1.hashCode());
            Assert.assertEquals(expected2, parent2);
            Assert.assertEquals(expected2.hashCode(), parent2.hashCode());
        }
    }

    @Test
    public void test10HttpUri2URL() throws IOException, URISyntaxException {
        testUri2URL(getSimpleTestName("."), TestUri03Resolving.uriHttpSArray);
    }

    @Test
    public void test20FileUnixUri2URL() throws IOException, URISyntaxException {
        testUri2URL(getSimpleTestName("."), TestUri03Resolving.uriFileSArrayUnix);
    }

    @Test
    public void test21FileWindowsUri2URL() throws IOException, URISyntaxException {
        testUri2URL(getSimpleTestName("."), TestUri03Resolving.uriFileSArrayWindows);
    }

    @Test
    public void test30FileUnixUri2URL() throws IOException, URISyntaxException {
        if( Platform.OSType.WINDOWS != PlatformPropsImpl.OS_TYPE ) {
            testFile2Uri(getSimpleTestName("."), TestUri03Resolving.fileSArrayUnix);
        }
    }

    @Test
    public void test31FileWindowsUri2URL() throws IOException, URISyntaxException {
        if( Platform.OSType.WINDOWS == PlatformPropsImpl.OS_TYPE ) {
            testFile2Uri(getSimpleTestName("."), TestUri03Resolving.fileSArrayWindows);
        }
    }

    static void testUri2URL(final String testname, final String[][] uriSArray) throws IOException, URISyntaxException {
        boolean ok = true;
        for(int i=0; i<uriSArray.length; i++) {
            final String[] uriSPair = uriSArray[i];
            final String uriSource = uriSPair[0];
            System.err.println("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS "+testname+": "+(i+1)+"/"+uriSArray.length);
            ok = testUri2URL(Uri.Encoded.cast(uriSource)) && ok;
            System.err.println("EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE "+testname+": "+(i+1)+"/"+uriSArray.length);
        }
        Assert.assertTrue("One or more errors occured see stderr above", ok);
    }

    static boolean testUri2URL(final Uri.Encoded uriSource) throws IOException, URISyntaxException {
        System.err.println("uriSource   : "+uriSource);
        final Uri uri0 = new Uri(uriSource);
        URIDumpUtil.showUri(uri0);

        final URI actualURI = uri0.toURI();
        URIDumpUtil.showURI(actualURI);
        final Uri.Encoded actualURIStr = Uri.Encoded.cast(actualURI.toString());

        final URL actualURL = uri0.toURL();
        URIDumpUtil.showURL(actualURL);
        final Uri.Encoded actualURLStr = Uri.Encoded.cast(actualURL.toExternalForm());

        System.err.println("expected_URX: "+uriSource);

        final boolean equalsURI = uriSource.equals(actualURIStr);
        System.err.println("actual   URI: "+actualURIStr+" - "+(equalsURI?"OK":"ERROR"));
        final boolean equalsURL = uriSource.equals(actualURLStr);
        System.err.println("actual   URL: "+actualURLStr+" - "+(equalsURL?"OK":"ERROR"));
        URIDumpUtil.showReencodedURIOfUri(uri0);
        URIDumpUtil.showReencodedUriOfURI(actualURI);

        final boolean ok = equalsURL && equalsURI;

        // now test open ..
        Throwable t = null;
        URLConnection con = null;
        try {
            con = actualURL.openConnection();
        } catch (final Throwable _t) {
            t = _t;
        }
        if( null != t ) {
            System.err.println("XXX: "+t.getClass().getName()+": "+t.getMessage());
            t.printStackTrace();
        } else {
            System.err.println("XXX: No openConnection() failure");
            System.err.println("XXX: "+con);
        }
        return ok;
    }

    static void testFile2Uri(final String testname, final String[][] uriSArray) throws IOException, URISyntaxException {
        boolean ok = true;
        for(int i=0; i<uriSArray.length; i++) {
            final String[] uriSPair = uriSArray[i];
            final String uriSource = uriSPair[0];
            final String uriEncExpected= uriSPair[1];
            final String fileExpected= uriSPair[2];
            System.err.println("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS "+testname+": "+(i+1)+"/"+uriSArray.length);
            ok = testFile2Uri(uriSource, Uri.Encoded.cast(uriEncExpected), fileExpected) && ok;
            System.err.println("EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE "+testname+": "+(i+1)+"/"+uriSArray.length);
        }
        Assert.assertTrue("One or more errors occured see stderr above", ok);
    }

    static boolean testFile2Uri(final String fileSource, final Uri.Encoded uriEncExpected, final String fileExpected) throws IOException, URISyntaxException {
        System.err.println("fileSource:         "+fileSource);
        final File file = new File(fileSource);
        System.err.println("file:               "+file.getAbsolutePath());
        final Uri uri1 = Uri.valueOf(file);
        System.err.println("uri1.string:        "+uri1.toString());
        URIDumpUtil.showUri(uri1);
        URIDumpUtil.showURL(uri1.toURL());
        URIDumpUtil.showReencodedURIOfUri(uri1);

        final URL actualUrl = uri1.toURL();
        final String actualFileS = uri1.getNativeFilePath();
        final boolean equalsFilePath = fileExpected.equals(actualFileS);
        System.err.println("expected_path:      "+fileExpected);
        System.err.println("actual___file-path: "+actualFileS+" - "+(equalsFilePath?"OK":"ERROR"));
        final boolean equalsEncUri = uriEncExpected.equals(uri1.input);
        System.err.println("expected__encUri:   "+uriEncExpected);
        System.err.println("actual_______Uri:   "+uri1.input+" - "+(equalsEncUri?"OK":"ERROR"));
        final boolean ok = equalsEncUri && equalsFilePath;

        System.err.println("actual_______URL:   "+actualUrl.toExternalForm());

        // now test open ..
        Throwable t = null;
        URLConnection con = null;
        try {
            con = actualUrl.openConnection();
        } catch (final Throwable _t) {
            t = _t;
        }
        if( null != t ) {
            System.err.println("XXX: "+t.getClass().getName()+": "+t.getMessage());
            t.printStackTrace();
        } else {
            System.err.println("XXX: No openConnection() failure");
            System.err.println("XXX: "+con);
        }
        return ok;
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestUri01.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
