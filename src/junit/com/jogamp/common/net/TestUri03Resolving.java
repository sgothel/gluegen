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

import com.jogamp.common.net.Uri;
import com.jogamp.common.os.Platform;
import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestUri03Resolving extends SingletonJunitCase {

    // Bug 908, issues w/ windows file path char: $ ^ ~ # [ ]

    private static final String[][] uriHttpSArray = new String[][] {
        new String[] {"http://localhost/gluegen/build-x86_64/gluegen-rt.jar"},

        new String[] {"http://localhost/gluegen/"+'\u0394'+"/gluegen-rt.jar"},

        new String[] {"http://localhost/gluegen/build-x86_64%20lala/gluegen-rt.jar"},

        new String[] {"http://localhost/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar"},

        new String[] {"jar:http://localhost/gluegen/build-x86_64/gluegen-rt.jar!/"},

        new String[] {"jar:http://localhost/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar!/"},

        new String[] {"jar:http://localhost/gluegen/build-x86_64/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"},

        new String[] {"jar:http://localhost/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"},

        new String[] {"jar:http://localhost/gluegen/R%23/gluegen-rt.jar!/"},

        new String[] {"jar:http://localhost/gluegen/A%24/B%5E/C~/D%23/E%5B/F%5D/gluegen-rt.jar!/"},

        new String[] {"jar:http://localhost/gluegen/%24/%5E/~/%23/%5B/%5D/gluegen-rt.jar!/"},

        new String[] {"jar:http://localhost/gluegen/"+'\u0394'+"/gluegen-rt.jar!/"},
    };

    private static final String[][] uriFileSArrayUnix = new String[][] {
        new String[] {"file:/gluegen/build-x86_64/gluegen-rt.jar"},

        new String[] {"file:/gluegen/"+'\u0394'+"/gluegen-rt.jar"},

        new String[] {"file:/gluegen/build-x86_64%20lala/gluegen-rt.jar"},

        new String[] {"file:/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar"},

        new String[] {"jar:file:/gluegen/build-x86_64/gluegen-rt.jar!/"},

        new String[] {"jar:file:/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar!/"},

        new String[] {"jar:file:/gluegen/build-x86_64/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"},

        new String[] {"jar:file:/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"},

        new String[] {"jar:file://filehost/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"},

        new String[] {"jar:file:/gluegen/R%23/gluegen-rt.jar!/"},

        new String[] {"jar:file:/gluegen/A%24/B%5E/C~/D%23/E%5B/F%5D/gluegen-rt.jar!/"},

        new String[] {"jar:file:/gluegen/%24/%5E/~/%23/%5B/%5D/gluegen-rt.jar!/"},

        new String[] {"jar:file:/gluegen/"+'\u0394'+"/gluegen-rt.jar!/"},
    };

    private static final String[][] uriFileSArrayWindows = new String[][] {
        new String[] {"file:/C%3A/gluegen/build-x86_64/gluegen-rt.jar"},

        new String[] {"file:/C%3A/gluegen/"+'\u0394'+"/gluegen-rt.jar"},

        new String[] {"file:/C%3A/gluegen/build-x86_64%20lala/gluegen-rt.jar"},

        new String[] {"file:/C%3A/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar"},

        new String[] {"jar:file:/C%3A/gluegen/build-x86_64/gluegen-rt.jar!/"},

        new String[] {"jar:file:/C%3A/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar!/"},

        new String[] {"jar:file:/C%3A/gluegen/build-x86_64/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"},

        new String[] {"jar:file:/C%3A/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"},

        new String[] {"jar:file:///C%3A/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"},

        new String[] {"jar:file://filehost/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"},

        new String[] {"jar:file:/C%3A/gluegen/R%23/gluegen-rt.jar!/"},

        new String[] {"jar:file:/C%3A/gluegen/A%24/B%5E/C~/D%23/E%5B/F%5D/gluegen-rt.jar!/"},

        new String[] {"jar:file:/C%3A/gluegen/%24/%5E/~/%23/%5B/%5D/gluegen-rt.jar!/"},

        new String[] {"jar:file:/C%3A/gluegen/"+'\u0394'+"/gluegen-rt.jar!/"},
    };

    private static final String[][] urlFileSArrayWindows = new String[][] {
        new String[] {"file:/C:/gluegen/build-x86_64/gluegen-rt.jar"},

        new String[] {"file:/C:/gluegen/"+'\u0394'+"/gluegen-rt.jar"},

        new String[] {"file:/C:/gluegen/build-x86_64%20lala/gluegen-rt.jar"},

        new String[] {"file:/C:/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar"},

        new String[] {"jar:file:/C:/gluegen/build-x86_64/gluegen-rt.jar!/"},

        new String[] {"jar:file:/C:/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar!/"},

        new String[] {"jar:file:/C:/gluegen/build-x86_64/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"},

        new String[] {"jar:file:/C:/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"},

        new String[] {"jar:file:///C:/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"},

        new String[] {"jar:file://filehost/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"},

        new String[] {"jar:file:/C:/gluegen/R%23/gluegen-rt.jar!/"},

        new String[] {"jar:file:/C:/gluegen/A%24/B%5E/C~/D%23/E%5B/F%5D/gluegen-rt.jar!/"},

        new String[] {"jar:file:/C:/gluegen/%24/%5E/~/%23/%5B/%5D/gluegen-rt.jar!/"},

        new String[] {"jar:file:/C:/gluegen/"+'\u0394'+"/gluegen-rt.jar!/"},
    };

    public static final String[][] fileSArrayUnix = new String[][] {
        new String[] {"/gluegen/build-x86_64/gluegen-rt.jar",
                      "file:/gluegen/build-x86_64/gluegen-rt.jar",
                      "/gluegen/build-x86_64/gluegen-rt.jar"},

        new String[] {"/gluegen/"+'\u0394'+"/gluegen-rt.jar",
                      "file:/gluegen/"+'\u0394'+"/gluegen-rt.jar",
                      "/gluegen/"+'\u0394'+"/gluegen-rt.jar"},

        new String[] {"/gluegen/build-x86_64 lala/gluegen-rt.jar",
                      "file:/gluegen/build-x86_64%20lala/gluegen-rt.jar",
                      "/gluegen/build-x86_64 lala/gluegen-rt.jar"},

        new String[] {"/gluegen/build-x86_64 öä lala/gluegen-rt.jar",
                      "file:/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar",
                      "/gluegen/build-x86_64 öä lala/gluegen-rt.jar"},

        new String[] {"/gluegen/A$/B^/C~/D#/E[/F]/gluegen-rt.jar",
                      "file:/gluegen/A%24/B%5E/C~/D%23/E%5B/F%5D/gluegen-rt.jar",
                      "/gluegen/A$/B^/C~/D#/E[/F]/gluegen-rt.jar" },

        new String[] {"/gluegen/$/^/~/#/[/]/gluegen-rt.jar",
                      "file:/gluegen/%24/%5E/~/%23/%5B/%5D/gluegen-rt.jar",
                      "/gluegen/$/^/~/#/[/]/gluegen-rt.jar" },
    };

    public static final String[][] fileSArrayWindows = new String[][] {
        new String[] {"C:/gluegen/build-x86_64/gluegen-rt.jar",
                      "file:/C%3A/gluegen/build-x86_64/gluegen-rt.jar",
                      "C:\\gluegen\\build-x86_64\\gluegen-rt.jar"},

        new String[] {"C:/gluegen/"+'\u0394'+"/gluegen-rt.jar",
                      "file:/C%3A/gluegen/"+'\u0394'+"/gluegen-rt.jar",
                      "C:\\gluegen\\"+'\u0394'+"\\gluegen-rt.jar"},

        new String[] {"C:/gluegen/build-x86_64 lala/gluegen-rt.jar",
                      "file:/C%3A/gluegen/build-x86_64%20lala/gluegen-rt.jar",
                      "C:\\gluegen\\build-x86_64 lala\\gluegen-rt.jar"},

        new String[] {"C:/gluegen/build-x86_64 öä lala/gluegen-rt.jar",
                      "file:/C%3A/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar",
                      "C:\\gluegen\\build-x86_64 öä lala\\gluegen-rt.jar"},

        new String[] {"C:\\gluegen\\build-x86_64 öä lala\\gluegen-rt.jar",
                      "file:/C%3A/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar",
                      "C:\\gluegen\\build-x86_64 öä lala\\gluegen-rt.jar"},

        new String[] {"\\\\filehost\\gluegen\\build-x86_64 öä lala\\gluegen-rt.jar",
                      "file://filehost/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar",
                      "\\\\filehost\\gluegen\\build-x86_64 öä lala\\gluegen-rt.jar"},

        new String[] {"C:/gluegen/A$/B^/C~/D#/E[/F]/gluegen-rt.jar",
                      "file:/C%3A/gluegen/A%24/B%5E/C~/D%23/E%5B/F%5D/gluegen-rt.jar",
                      "C:\\gluegen\\A$\\B^\\C~\\D#\\E[\\F]\\gluegen-rt.jar" },

        new String[] {"C:/gluegen/$/^/~/#/[/]/gluegen-rt.jar",
                      "file:/C%3A/gluegen/%24/%5E/~/%23/%5B/%5D/gluegen-rt.jar",
                      "C:\\gluegen\\$\\^\\~\\#\\[\\]\\gluegen-rt.jar" },
    };

    @Test
    public void test01HttpUri2URL() throws IOException, URISyntaxException {
        testUri2URL(getSimpleTestName("."), uriHttpSArray);
    }

    @Test
    public void test02FileUnixUri2URL() throws IOException, URISyntaxException {
        testUri2URL(getSimpleTestName("."), uriFileSArrayUnix);
    }

    @Test
    public void test03FileWindowsUri2URL() throws IOException, URISyntaxException {
        testUri2URL(getSimpleTestName("."), uriFileSArrayWindows);
    }

    @Test
    public void test11HttpURL2Uri() throws IOException, URISyntaxException {
        testURL2Uri(getSimpleTestName("."), uriHttpSArray);
    }

    @Test
    public void test12FileUnixURL2Uri() throws IOException, URISyntaxException {
        testURL2Uri(getSimpleTestName("."), uriFileSArrayUnix);
    }

    @Test
    public void test13FileWindowsURL2Uri() throws IOException, URISyntaxException {
        testURL2Uri(getSimpleTestName("."), urlFileSArrayWindows);
    }

    @Test
    public void test24FileUnixURI2URL() throws IOException, URISyntaxException {
        if( Platform.OSType.WINDOWS != PlatformPropsImpl.OS_TYPE ) {
            testFile2Uri(getSimpleTestName("."), fileSArrayUnix);
        }
    }

    @Test
    public void test25FileWindowsURI2URL() throws IOException, URISyntaxException {
        if( Platform.OSType.WINDOWS == PlatformPropsImpl.OS_TYPE ) {
            testFile2Uri(getSimpleTestName("."), fileSArrayWindows);
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

        boolean ok = equalsURL && equalsURI;

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

        if( uri0.scheme.equals(Uri.JAR_SCHEME) ) {
            // Extended tests on JAR Uri
            final Uri uriSub0 = uri0.getContainedUri();
            Assert.assertNotNull(uriSub0);
            System.err.println("EXT JAR contained:");
            URIDumpUtil.showUri(uriSub0);
            final Uri uriSubDir0 = uriSub0.getDirectory();
            final Uri uriSubParent0 = uriSub0.getParent();
            System.err.println("EXT JAR contained Dir:");
            URIDumpUtil.showUri(uriSubDir0);
            System.err.println("EXT JAR contained Parent:");
            URIDumpUtil.showUri(uriSubParent0);
            ok = uriSubDir0.equals(uriSubParent0) && ok;
        }
        return ok;
    }

    static void testURL2Uri(final String testname, final String[][] urlSArray) throws IOException, URISyntaxException {
        boolean ok = true;
        for(int i=0; i<urlSArray.length; i++) {
            final String[] uriSPair = urlSArray[i];
            final String uriSource = uriSPair[0];
            System.err.println("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS "+testname+": "+(i+1)+"/"+urlSArray.length);
            ok = testURL2Uri(new URL(uriSource)) && ok;
            System.err.println("EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE "+testname+": "+(i+1)+"/"+urlSArray.length);
        }
        Assert.assertTrue("One or more errors occured see stderr above", ok);
    }

    static boolean testURL2Uri(final URL urlSource) throws IOException, URISyntaxException {
        System.err.println("URL Source   : "+urlSource);
        URIDumpUtil.showURL(urlSource);

        final URI uriSource = urlSource.toURI();
        URIDumpUtil.showURI(uriSource);

        final Uri uri0 = Uri.valueOf(urlSource);
        URIDumpUtil.showUri(uri0);

        final URL uriToURL = uri0.toURL();
        URIDumpUtil.showURL(uriToURL);

        // now test open ..
        Throwable t = null;
        URLConnection con = null;
        try {
            con = uriToURL.openConnection();
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

        boolean ok = true;

        if( uri0.scheme.equals(Uri.JAR_SCHEME) ) {
            // Extended tests on JAR Uri
            final Uri uriSub0 = uri0.getContainedUri();
            Assert.assertNotNull(uriSub0);
            System.err.println("EXT JAR contained:");
            URIDumpUtil.showUri(uriSub0);
            final Uri uriSubDir0 = uriSub0.getDirectory();
            final Uri uriSubParent0 = uriSub0.getParent();
            System.err.println("EXT JAR contained Dir:");
            URIDumpUtil.showUri(uriSubDir0);
            System.err.println("EXT JAR contained Parent:");
            URIDumpUtil.showUri(uriSubParent0);
            ok = uriSubDir0.equals(uriSubParent0) && ok;
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

        final Uri uri0 = Uri.valueOf(file);
        URIDumpUtil.showReencodedURIOfUri(uri0);

        final URL actualUrl = uri0.toURL();
        final File actualFile = uri0.toFile();
        final boolean equalsFilePath = fileExpected.equals(actualFile.getPath());
        System.err.println("expected_path:      "+fileExpected);
        System.err.println("actual___file-path: "+actualFile+" - "+(equalsFilePath?"OK":"ERROR"));
        final boolean equalsEncUri = uriEncExpected.equals(uri0.getEncoded());
        System.err.println("expected__encUri:   "+uriEncExpected);
        System.err.println("actual_______Uri:   "+uri0.getEncoded()+" - "+(equalsEncUri?"OK":"ERROR"));
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
        final String tstname = TestUri03Resolving.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
