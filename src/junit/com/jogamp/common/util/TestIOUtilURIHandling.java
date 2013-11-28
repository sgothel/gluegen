package com.jogamp.common.util;

import static com.jogamp.common.net.URIDumpUtil.showURX;
import static com.jogamp.common.net.URIDumpUtil.showURI;
import static com.jogamp.common.net.URIDumpUtil.showURL;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import jogamp.common.os.PlatformPropsImpl;

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

        new String[] {"http://localhost/gluegen/build-x86_64%20lala/gluegen-rt.jar",
                      "http://localhost/gluegen/build-x86_64%20lala/gluegen-rt.jar"
                   // "http://localhost/gluegen/build-x86_64 lala/gluegen-rt.jar"
                     },

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
        /** Not possible, '#' is fragment in URI
        new String[] {"jar:http://localhost/gluegen/%23/gluegen-rt.jar!/",
                      "jar:http://localhost/gluegen/%23/gluegen-rt.jar!/"
                      // "jar:http://localhost/gluegen/#/gluegen-rt.jar!/"
                     }, */
    };

    static final String[][] uriFileSArrayUnix = new String[][] {
        new String[] {"file:/gluegen/build-x86_64/gluegen-rt.jar",
                      "file:/gluegen/build-x86_64/gluegen-rt.jar"},

        new String[] {"file:/gluegen/build-x86_64%20lala/gluegen-rt.jar",
                      "file:/gluegen/build-x86_64 lala/gluegen-rt.jar"},

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

        /** Not possible, '#' is fragment in URI
        new String[] {"jar:file:/gluegen/%23/gluegen-rt.jar!/",
                      "jar:file:/gluegen/#/gluegen-rt.jar!/" }, */
    };

    static final String[][] uriFileSArrayWindows = new String[][] {
        new String[] {"file:/C:/gluegen/build-x86_64/gluegen-rt.jar",
                      "file:/C:/gluegen/build-x86_64/gluegen-rt.jar"},

        new String[] {"file:/C:/gluegen/build-x86_64%20lala/gluegen-rt.jar",
                      "file:/C:/gluegen/build-x86_64 lala/gluegen-rt.jar"},

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

        /** Not possible, '#' is fragment in URI
        new String[] {"jar:file:/C:/gluegen/%23/gluegen-rt.jar!/",
                      "jar:file:/C:/gluegen/#/gluegen-rt.jar!/" }, */
    };

    static final String[][] fileSArrayUnix = new String[][] {
        new String[] {"/gluegen/build-x86_64/gluegen-rt.jar",
                      "file:/gluegen/build-x86_64/gluegen-rt.jar",
                      "file:/gluegen/build-x86_64/gluegen-rt.jar",
                      "/gluegen/build-x86_64/gluegen-rt.jar"},

        new String[] {"/gluegen/build-x86_64 lala/gluegen-rt.jar",
                      "file:/gluegen/build-x86_64%20lala/gluegen-rt.jar",
                      "file:/gluegen/build-x86_64 lala/gluegen-rt.jar",
                      "/gluegen/build-x86_64 lala/gluegen-rt.jar"},

        new String[] {"/gluegen/build-x86_64 öä lala/gluegen-rt.jar",
                      "file:/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar",
                      "file:/gluegen/build-x86_64 öä lala/gluegen-rt.jar",
                      "/gluegen/build-x86_64 öä lala/gluegen-rt.jar"},

        /* No support for '#' fragment in URI path !
        new String[] {"/gluegen/#/gluegen-rt.jar",
                      "file:/gluegen/%23/gluegen-rt.jar",
                      "file:/gluegen/#/gluegen-rt.jar",
                      "/gluegen/#/gluegen-rt.jar" }, */
    };

    static final String[][] fileSArrayWindows = new String[][] {
        new String[] {"C:/gluegen/build-x86_64/gluegen-rt.jar",
                      "file:/C:/gluegen/build-x86_64/gluegen-rt.jar",
                      "file:/C:/gluegen/build-x86_64/gluegen-rt.jar",
                      "C:\\gluegen\\build-x86_64\\gluegen-rt.jar"},

        new String[] {"C:/gluegen/build-x86_64 lala/gluegen-rt.jar",
                      "file:/C:/gluegen/build-x86_64%20lala/gluegen-rt.jar",
                      "file:/C:/gluegen/build-x86_64 lala/gluegen-rt.jar",
                      "C:\\gluegen\\build-x86_64 lala\\gluegen-rt.jar"},

        new String[] {"C:/gluegen/build-x86_64 öä lala/gluegen-rt.jar",
                      "file:/C:/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar",
                      "file:/C:/gluegen/build-x86_64 öä lala/gluegen-rt.jar",
                      "C:\\gluegen\\build-x86_64 öä lala\\gluegen-rt.jar"},

        new String[] {"C:\\gluegen\\build-x86_64 öä lala\\gluegen-rt.jar",
                      "file:/C:/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar",
                      "file:/C:/gluegen/build-x86_64 öä lala/gluegen-rt.jar",
                      "C:\\gluegen\\build-x86_64 öä lala\\gluegen-rt.jar"},

        /* No support for '#' fragment in URI path !
        new String[] {"C:/gluegen/#/gluegen-rt.jar",
                      "file:/C:/gluegen/%23/gluegen-rt.jar",
                      "file:/C:/gluegen/#/gluegen-rt.jar",
                      "C:\\gluegen\\#\\gluegen-rt.jar" }, */
    };

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

    static void testURI2URL(String testname, String[][] uriSArray) throws IOException, URISyntaxException {
        boolean ok = true;
        for(int i=0; i<uriSArray.length; i++) {
            final String[] uriSPair = uriSArray[i];
            final String uriSource = uriSPair[0];
            final String uriExpected= uriSPair[1];
            System.err.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX "+testname+": "+(i+1)+"/"+uriSArray.length);
            ok = testURI2URL(uriSource, uriExpected) && ok;
            System.err.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX "+testname+": "+(i+1)+"/"+uriSArray.length);
        }
        Assert.assertTrue("One or more errors occured see stderr above", ok);
    }

    static boolean testURI2URL(String uriSource, String uriExpected) throws IOException, URISyntaxException {
        showURX(uriSource);
        final URI uri0 = new URI(uriSource);
        System.err.println("uri.string: "+uri0.toString());
        System.err.println("uri.ascii : "+uri0.toASCIIString());

        final URL actualUrl = IOUtil.toURL(uri0);
        final String actualUrlS = actualUrl.toExternalForm();
        final boolean equalsA = uriExpected.equals(actualUrlS);
        System.err.println("actual     : "+actualUrlS);
        System.err.println("expected___: "+uriExpected+" - "+(equalsA?"OK":"ERROR"));
        final boolean ok = equalsA;

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
        return ok;
    }

    static void testFile2URI(String testname, String[][] uriSArray) throws IOException, URISyntaxException {
        boolean ok = true;
        for(int i=0; i<uriSArray.length; i++) {
            final String[] uriSPair = uriSArray[i];
            final String uriSource = uriSPair[0];
            final String uriEncExpected= uriSPair[1];
            final String uriDecExpected= uriSPair[2];
            final String fileExpected= uriSPair[3];
            System.err.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX "+testname+": "+(i+1)+"/"+uriSArray.length);
            ok = testFile2URI(uriSource, uriEncExpected, uriDecExpected, fileExpected) && ok;
            System.err.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX "+testname+": "+(i+1)+"/"+uriSArray.length);
        }
        Assert.assertTrue("One or more errors occured see stderr above", ok);
    }

    static boolean testFile2URI(String fileSource, String uriEncExpected, String uriDecExpected, String fileExpected) throws IOException, URISyntaxException {
        final File file = new File(fileSource);
        final URI uri0 = IOUtil.toURISimple(file);
        System.err.println("uri.string: "+uri0.toString());
        System.err.println("uri.ascii : "+uri0.toASCIIString());
        showURI(uri0);
        showURL(uri0.toURL());

        final URL actualUrl = IOUtil.toURL(uri0);
        final String actualUrlS = actualUrl.toExternalForm();
        final String actualUrlPathS = actualUrl.getPath();
        final String actualFilePathS = IOUtil.decodeURIToFilePath( actualUrlPathS );
        final boolean equalsFilePath = fileExpected.equals(actualFilePathS);
        System.err.println("actual____url-path: "+actualUrlPathS);
        System.err.println("actual___file-path: "+actualFilePathS);
        System.err.println("expected_path: "+fileExpected+" - "+(equalsFilePath?"OK":"ERROR"));
        final boolean equalsDecUri = uriDecExpected.equals(actualUrlS);
        System.err.println("actual_______uri: "+actualUrlS);
        System.err.println("expected__encUri: "+uriEncExpected);
        System.err.println("expected__decUri: "+uriDecExpected+" - "+(equalsDecUri?"OK":"ERROR"));
        final boolean ok = equalsDecUri && equalsFilePath;

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
        return ok;
    }

    public static void main(String args[]) throws IOException {
        String tstname = TestIOUtilURIHandling.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
