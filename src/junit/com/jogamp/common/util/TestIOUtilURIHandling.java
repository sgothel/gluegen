package com.jogamp.common.util;

import static com.jogamp.common.net.URIDumpUtil.showURX;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.IOUtil;
import com.jogamp.junit.util.JunitTracer;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestIOUtilURIHandling extends JunitTracer {

    static final String[][] uriHttpSArray = new String[][] {
        new String[] {"http://localhost/gluegen/build-x86_64/gluegen-rt.jar",
                      "http://localhost/gluegen/build-x86_64/gluegen-rt.jar"},

        new String[] {"http://localhost/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar",
                      "http://localhost/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar"
                   // "http://localhost/gluegen/build-x86_64 öä lala/gluegen-rt.jar"
                     },

        new String[] {"jar:http://localhost/gluegen/build-x86_64/gluegen-rt.jar!/",
                      "jar:http://localhost/gluegen/build-x86_64/gluegen-rt.jar!/" },

        new String[] {"jar:http://localhost/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar!/",
                      "jar:http://localhost/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar!/"
                   // "jar:http://localhost/gluegen/build-x86_64 öä lala/gluegen-rt.jar!/"
                     },

        new String[] {"jar:http://localhost/gluegen/build-x86_64/gluegen-rt.jar!/com/jogamp/common/os/Platform.class",
                      "jar:http://localhost/gluegen/build-x86_64/gluegen-rt.jar!/com/jogamp/common/os/Platform.class" },

        new String[] {"jar:http://localhost/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class",
                      "jar:http://localhost/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"
                   // "jar:http://localhost/gluegen/build-x86_64 öä lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class"
                     },
    };

    static final String[][] uriFileSArrayUnix = new String[][] {
        new String[] {"file:/gluegen/build-x86_64/gluegen-rt.jar",
                      "file:/gluegen/build-x86_64/gluegen-rt.jar"},

        new String[] {"file:/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar",
                      "file:/gluegen/build-x86_64 öä lala/gluegen-rt.jar"},

        new String[] {"jar:file:/gluegen/build-x86_64/gluegen-rt.jar!/",
                      "jar:file:/gluegen/build-x86_64/gluegen-rt.jar!/" },

        new String[] {"jar:file:/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar!/",
                      "jar:file:/gluegen/build-x86_64 öä lala/gluegen-rt.jar!/" },

        new String[] {"jar:file:/gluegen/build-x86_64/gluegen-rt.jar!/com/jogamp/common/os/Platform.class",
                      "jar:file:/gluegen/build-x86_64/gluegen-rt.jar!/com/jogamp/common/os/Platform.class" },

        new String[] {"jar:file:/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class",
                      "jar:file:/gluegen/build-x86_64 öä lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class" },
    };

    static final String[][] uriFileSArrayWindows = new String[][] {
        new String[] {"file:/C:/gluegen/build-x86_64/gluegen-rt.jar",
                      "file:/C:/gluegen/build-x86_64/gluegen-rt.jar"},

        new String[] {"file:/C:/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar",
                      "file:/C:/gluegen/build-x86_64 öä lala/gluegen-rt.jar"},

        new String[] {"jar:file:/C:/gluegen/build-x86_64/gluegen-rt.jar!/",
                      "jar:file:/C:/gluegen/build-x86_64/gluegen-rt.jar!/" },

        new String[] {"jar:file:/C:/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar!/",
                      "jar:file:/C:/gluegen/build-x86_64 öä lala/gluegen-rt.jar!/" },

        new String[] {"jar:file:/C:/gluegen/build-x86_64/gluegen-rt.jar!/com/jogamp/common/os/Platform.class",
                      "jar:file:/C:/gluegen/build-x86_64/gluegen-rt.jar!/com/jogamp/common/os/Platform.class" },

        new String[] {"jar:file:/C:/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class",
                      "jar:file:/C:/gluegen/build-x86_64 öä lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class" },
    };

    @Test
    public void test01HttpURI2URL() throws IOException, URISyntaxException {
        testURI2URL(uriHttpSArray, false /*usesFile*/, false /*matchOS*/);
    }

    @Test
    public void test02FileUnixURI2URL() throws IOException, URISyntaxException {
        testURI2URL(uriFileSArrayUnix, true /*usesFile*/, Platform.OSType.WINDOWS != Platform.getOSType() /*matchOS*/);
    }

    @Test
    public void test02FileWindowsURI2URL() throws IOException, URISyntaxException {
        testURI2URL(uriFileSArrayWindows, true /*usesFile*/, Platform.OSType.WINDOWS == Platform.getOSType() /*matchOS*/);
    }

    static void testURI2URL(String[][] uriSArray, boolean usesFile, boolean matchOS) throws IOException, URISyntaxException {
        for(int i=0; i<uriSArray.length; i++) {
            final String[] uriSPair = uriSArray[i];
            final String uriSource = uriSPair[0];
            final String uriExpected= uriSPair[1];
            System.err.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
            showURX(uriSource);
            testURI2URL(uriSource, uriExpected, usesFile, matchOS);
            System.err.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        }
    }

    static void testURI2URL(String source, String expected, boolean usesFile, boolean matchOS) throws IOException, URISyntaxException {
        final URI uri0 = new URI(source);
        System.err.println("uri: "+uri0.toString());

        final URL actualUrl = IOUtil.toURL(uri0);
        final String actualUrlS = actualUrl.toExternalForm();
        System.err.println("url: "+actualUrlS);
        final boolean equalsA = expected.equals(actualUrlS);
        System.err.println("expected___: "+expected+" - ok "+equalsA);
        System.err.println("actual     : "+actualUrlS);
        Assert.assertTrue("No match, expected    w/ orig url", equalsA);

        // now test open ..
        Throwable t = null;
        URLConnection con = null;
        try {
            con = actualUrl.openConnection();
        } catch (Throwable _t) {
            t = _t;
        }
        if( null != t ) {
            System.err.println("XXX: "+t.getClass().getName()+": "+t.getMessage());
            t.printStackTrace();
        } else {
            System.err.println("XXX: No openConnection() failure");
            System.err.println("XXX: "+con);
        }
    }

    public static void main(String args[]) throws IOException {
        String tstname = TestIOUtilURIHandling.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
