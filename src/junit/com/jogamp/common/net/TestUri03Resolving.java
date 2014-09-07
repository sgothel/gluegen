package com.jogamp.common.net;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import jogamp.common.os.PlatformPropsImpl;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.common.net.Uri;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.IOUtil;
import com.jogamp.junit.util.JunitTracer;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestUri03Resolving extends JunitTracer {

    // Bug 908, issues w/ windows file path char: $ ^ ~ # [ ]

    public static final String[][] uriHttpSArray = new String[][] {
        new String[] {"http://localhost/gluegen/build-x86_64/gluegen-rt.jar"},

        new String[] {"http://localhost/gluegen/"+'\u0394'+"/gluegen-rt.jar"},

        new String[] {"http://localhost/gluegen/build-x86_64%20lala/gluegen-rt.jar"},

        new String[] {"http://localhost/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar"},

        new String[] {"jar:http://localhost/gluegen/build-x86_64/gluegen-rt.jar!/"},

        new String[] {"jar:http://localhost/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar!/"},

        new String[] {"jar:http://localhost/gluegen/build-x86_64/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"},

        new String[] {"jar:http://localhost/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"},

        new String[] {"jar:http://localhost/gluegen/R%23/gluegen-rt.jar!/"},

        new String[] {"jar:http://localhost/gluegen/"+'\u0394'+"/gluegen-rt.jar!/"},
    };

    public static final String[][] uriFileSArrayUnix = new String[][] {
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

        new String[] {"jar:file:/gluegen/"+'\u0394'+"/gluegen-rt.jar!/"},
    };

    public static final String[][] uriFileSArrayWindows = new String[][] {
        new String[] {"file:/C%3A/gluegen/build-x86_64/gluegen-rt.jar"},

        new String[] {"file:/C%3A/gluegen/"+'\u0394'+"/gluegen-rt.jar"},

        new String[] {"file:/C%3A/gluegen/build-x86_64%20lala/gluegen-rt.jar"},

        new String[] {"file:/C%3A/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar"},

        new String[] {"jar:file:/C%3A/gluegen/build-x86_64/gluegen-rt.jar!/"},

        new String[] {"jar:file:/C%3A/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar!/"},

        new String[] {"jar:file:/C%3A/gluegen/build-x86_64/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"},

        new String[] {"jar:file:/C%3A/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"},
        new String[] {"jar:file:/C%3A/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"},

        new String[] {"jar:file:///C%3A/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"},

        new String[] {"jar:file://filehost/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"},

        new String[] {"jar:file:/C%3A/gluegen/R%23/gluegen-rt.jar!/"},

        new String[] {"jar:file:/C%3A/gluegen/"+'\u0394'+"/gluegen-rt.jar!/"},
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
    public void test00BasicCoding() throws IOException, URISyntaxException {
        final String string = "Hallo Welt öä";
        System.err.println("sp1 "+string);
        final File file = new File(string);
        System.err.println("file "+file);
        System.err.println("file.path.dec "+file.getPath());
        System.err.println("file.path.abs "+file.getAbsolutePath());
        System.err.println("file.path.can "+file.getCanonicalPath());
        final Uri uri0 = Uri.valueOf(file);
        URIDumpUtil.showUri(uri0);
        URIDumpUtil.showReencodedURIOfUri(uri0);

        boolean ok = true;
        {
            final String s2 = IOUtil.slashify(file.getAbsolutePath(), true /* startWithSlash */, file.isDirectory() /* endWithSlash */);
            System.err.println("uri2.slashify: "+s2);
            final Uri uri1 = Uri.create(IOUtil.FILE_SCHEME, null, s2, null);
            final boolean equalEncoded= uri0.getEncoded().equals(uri1.getEncoded());
            final boolean equalPath = uri0.path.decode().equals(uri1.path.decode());
            final boolean equalASCII= uri0.toASCIIString().equals(uri1.toASCIIString().get());
            System.err.println("uri2.enc   : "+uri1.getEncoded()+" - "+(equalEncoded?"OK":"ERROR"));
            System.err.println("uri2.pathD : "+uri1.path.decode()+" - "+(equalPath?"OK":"ERROR"));
            System.err.println("uri2.asciiE: "+uri1.toASCIIString()+" - "+(equalASCII?"OK":"ERROR"));
            ok = equalEncoded && equalPath && equalASCII && ok;
        }
        {
            final String s2 = "/"+string;
            System.err.println("uri3.orig: "+s2);
            final Uri uri1 = Uri.create(IOUtil.FILE_SCHEME, s2, null);
            final String rString = "file:/Hallo%20Welt%20öä";
            final String rPath = s2;
            final String rASCII = "file:/Hallo%20Welt%20%C3%B6%C3%A4";
            final boolean equalEncoded = rString.equals(uri1.toString());
            final boolean equalPath = rPath.equals(uri1.path.decode());
            final boolean equalASCII= rASCII.equals(uri1.toASCIIString().get());
            System.err.println("uri3.enc   : "+uri1.toString()+" - "+(equalEncoded?"OK":"ERROR"));
            System.err.println("uri3.pathD : "+uri1.path.decode()+" - "+(equalPath?"OK":"ERROR"));
            System.err.println("uri3.asciiE: "+uri1.toASCIIString()+" - "+(equalASCII?"OK":"ERROR"));
            ok = equalEncoded && equalPath && equalASCII && ok;
        }
        {
            final String s2 = "//lala.org/"+string;
            System.err.println("uri4.orig: "+s2);
            final Uri uri1 = Uri.create(IOUtil.HTTP_SCHEME, s2, null);
            final String rString = "http://lala.org/Hallo%20Welt%20öä";
            final String rPath = "/"+string;
            final String rASCII = "http://lala.org/Hallo%20Welt%20%C3%B6%C3%A4";
            final boolean equalString= rString.equals(uri1.toString());
            final boolean equalPath = rPath.equals(uri1.path.decode());
            final boolean equalASCII= rASCII.equals(uri1.toASCIIString().get());
            System.err.println("uri4.enc   : "+uri1.toString()+" - "+(equalString?"OK":"ERROR"));
            System.err.println("uri4.pathD : "+uri1.path.decode()+" - "+(equalPath?"OK":"ERROR"));
            System.err.println("uri4.asciiE: "+uri1.toASCIIString()+" - "+(equalASCII?"OK":"ERROR"));
            ok = equalString && equalPath && equalASCII && ok;
        }
        Assert.assertTrue("One or more errors occured see stderr above", ok);
    }

    @Test
    public void test01HttpURI2URL() throws IOException, URISyntaxException {
        testURI2URL(getSimpleTestName("."), uriHttpSArray);
    }

    @Test
    public void test02FileUnixURI2URL() throws IOException, URISyntaxException {
        testURI2URL(getSimpleTestName("."), uriFileSArrayUnix);
    }

    @Test
    public void test03FileWindowsURI2URL() throws IOException, URISyntaxException {
        testURI2URL(getSimpleTestName("."), uriFileSArrayWindows);
    }

    @Test
    public void test04FileUnixURI2URL() throws IOException, URISyntaxException {
        if( Platform.OSType.WINDOWS != PlatformPropsImpl.OS_TYPE ) {
            testFile2URI(getSimpleTestName("."), fileSArrayUnix);
        }
    }

    @Test
    public void test05FileWindowsURI2URL() throws IOException, URISyntaxException {
        if( Platform.OSType.WINDOWS == PlatformPropsImpl.OS_TYPE ) {
            testFile2URI(getSimpleTestName("."), fileSArrayWindows);
        }
    }

    static void testURI2URL(final String testname, final String[][] uriSArray) throws IOException, URISyntaxException {
        boolean ok = true;
        for(int i=0; i<uriSArray.length; i++) {
            final String[] uriSPair = uriSArray[i];
            final String uriSource = uriSPair[0];
            System.err.println("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS "+testname+": "+(i+1)+"/"+uriSArray.length);
            ok = testURI2URL(Uri.Encoded.cast(uriSource)) && ok;
            System.err.println("EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE "+testname+": "+(i+1)+"/"+uriSArray.length);
        }
        Assert.assertTrue("One or more errors occured see stderr above", ok);
    }

    static boolean testURI2URL(final Uri.Encoded uriEncodedSource) throws IOException, URISyntaxException {
        final Uri uri0 = new Uri(uriEncodedSource);
        URIDumpUtil.showUri(uri0);

        final String expected1 = uriEncodedSource.toString();
        System.err.println("expected__s0: "+uriEncodedSource);
        System.err.println("expected__d1: "+expected1);

        final URL actualURL = uri0.toURL();
        final String actualURLStr = actualURL.toString();
        final boolean equalsURLSrc = uriEncodedSource.equals(actualURLStr);
        final boolean equalsURLDec1 = expected1.equals(actualURLStr);
        final boolean equalsURL = equalsURLSrc || equalsURLDec1;
        System.err.println("actual      : "+actualURLStr+" - "+(equalsURL?"OK":"ERROR")+
                                          " - equalSrc "+equalsURLSrc+", equalDec1 "+equalsURLDec1);

        final boolean ok = equalsURL;

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

    static void testFile2URI(final String testname, final String[][] uriSArray) throws IOException, URISyntaxException {
        boolean ok = true;
        for(int i=0; i<uriSArray.length; i++) {
            final String[] uriSPair = uriSArray[i];
            final String uriSource = uriSPair[0];
            final String uriEncExpected= uriSPair[1];
            final String fileExpected= uriSPair[2];
            System.err.println("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS "+testname+": "+(i+1)+"/"+uriSArray.length);
            ok = testFile2URI(uriSource, Uri.Encoded.cast(uriEncExpected), fileExpected) && ok;
            System.err.println("EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE "+testname+": "+(i+1)+"/"+uriSArray.length);
        }
        Assert.assertTrue("One or more errors occured see stderr above", ok);
    }

    static boolean testFile2URI(final String fileSource, final Uri.Encoded uriEncExpected, final String fileExpected) throws IOException, URISyntaxException {
        System.err.println("fileSource:         "+fileSource);
        final File file = new File(fileSource);
        System.err.println("file:               "+file.getAbsolutePath());

        final Uri uri0 = Uri.valueOf(file);
        URIDumpUtil.showReencodedURIOfUri(uri0);

        final URL actualUrl = uri0.toURL();
        final String actualFileS = uri0.getNativeFilePath();
        final boolean equalsFilePath = fileExpected.equals(actualFileS);
        System.err.println("expected_path:      "+fileExpected);
        System.err.println("actual___file-path: "+actualFileS+" - "+(equalsFilePath?"OK":"ERROR"));
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
