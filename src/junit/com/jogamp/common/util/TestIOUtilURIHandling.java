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

        new String[] {"jar:file://filehost/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class",
                      "jar:file://filehost/gluegen/build-x86_64 öä lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class" },

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

        new String[] {"jar:file:///C:/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class",
                      "jar:file:/C:/gluegen/build-x86_64 öä lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class" },

        new String[] {"jar:file://filehost/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class",
                      "jar:file://filehost/gluegen/build-x86_64 öä lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class" },

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

        new String[] {"\\\\filehost\\gluegen\\build-x86_64 öä lala\\gluegen-rt.jar",
                      "file://filehost/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar",
                      "file://filehost/gluegen/build-x86_64 öä lala/gluegen-rt.jar",
                      "\\\\filehost\\gluegen\\build-x86_64 öä lala\\gluegen-rt.jar"},

        /* No support for '#' fragment in URI path !
        new String[] {"C:/gluegen/#/gluegen-rt.jar",
                      "file:/C:/gluegen/%23/gluegen-rt.jar",
                      "file:/C:/gluegen/#/gluegen-rt.jar",
                      "C:\\gluegen\\#\\gluegen-rt.jar" }, */
    };

    @Test
    public void test00BasicCoding() throws IOException, URISyntaxException {
        final String string = "Hallo Welt öä";
        System.err.println("sp1 "+string);
        {
            String sp2 = IOUtil.encodeToURI(string);
            String sp3 = IOUtil.encodeToURI(sp2);
            System.err.println("sp2 "+sp2);
            System.err.println("sp3 "+sp3);
        }
        final File file = new File(string);
        System.err.println("file "+file);
        System.err.println("file.path.dec "+file.getPath());
        System.err.println("file.path.abs "+file.getAbsolutePath());
        System.err.println("file.path.can "+file.getCanonicalPath());
        final URI uri0 = file.toURI();
        System.err.println("uri0.string: "+uri0.toString());
        System.err.println("uri0.path  : "+uri0.getPath());
        System.err.println("uri0.ascii : "+uri0.toASCIIString());
        boolean ok = true;
        {
            final URI uri1 = IOUtil.toURISimple(file);
            final boolean equalString= uri0.toString().equals(uri1.toString());
            final boolean equalPath = uri0.getPath().equals(uri1.getPath());
            final boolean equalASCII= uri0.toASCIIString().equals(uri1.toASCIIString());
            System.err.println("uri1.string: "+uri1.toString()+" - "+(equalString?"OK":"ERROR"));
            System.err.println("uri1.path  : "+uri1.getPath()+" - "+(equalPath?"OK":"ERROR"));
            System.err.println("uri1.ascii : "+uri1.toASCIIString()+" - "+(equalASCII?"OK":"ERROR"));
            ok = equalString && equalPath && equalASCII && ok;
        }
        {
            final String s2 = IOUtil.slashify(file.getAbsolutePath(), true /* startWithSlash */, file.isDirectory() /* endWithSlash */);
            System.err.println("uri2.slashify: "+s2);
            {
                // Expected !equals due to double-escaping of space %20 -> %25%20
                // Double escaping is due to IOUtil.encodeToURI(s2).
                final String s3 = IOUtil.encodeToURI(s2);
                System.err.println("uri2.encoded: "+s3);
                final URI uri1 = new URI(IOUtil.FILE_SCHEME, null, s3, null);
                final boolean equalString= uri0.toString().equals(uri1.toString());
                final boolean equalPath = uri0.getPath().equals(uri1.getPath());
                final boolean equalASCII= uri0.toASCIIString().equals(uri1.toASCIIString());
                System.err.println("uri2.string: "+uri1.toString()+" - "+(equalString?"EQUAL":"NOT_EQUAL"));
                System.err.println("uri2.path  : "+uri1.getPath()+" - "+(equalPath?"EQUAL":"NOT_EQUAL"));
                System.err.println("uri2.ascii : "+uri1.toASCIIString()+" - "+(equalASCII?"EQUAL":"NOT_EQUAL"));
            }
            final URI uri1 = new URI(IOUtil.FILE_SCHEME, null, s2, null);
            final boolean equalString= uri0.toString().equals(uri1.toString());
            final boolean equalPath = uri0.getPath().equals(uri1.getPath());
            final boolean equalASCII= uri0.toASCIIString().equals(uri1.toASCIIString());
            System.err.println("uri2.string: "+uri1.toString()+" - "+(equalString?"OK":"ERROR"));
            System.err.println("uri2.path  : "+uri1.getPath()+" - "+(equalPath?"OK":"ERROR"));
            System.err.println("uri2.ascii : "+uri1.toASCIIString()+" - "+(equalASCII?"OK":"ERROR"));
            ok = equalString && equalPath && equalASCII && ok;
        }
        {
            final String s2 = "/"+string;
            System.err.println("uri3.orig: "+s2);
            final URI uri1 = new URI(IOUtil.FILE_SCHEME, s2, null);
            final String rString = "file:/Hallo%20Welt%20öä";
            final String rPath = s2;
            final String rASCII = "file:/Hallo%20Welt%20%C3%B6%C3%A4";
            final boolean equalString= rString.equals(uri1.toString());
            final boolean equalPath = rPath.equals(uri1.getPath());
            final boolean equalASCII= rASCII.equals(uri1.toASCIIString());
            System.err.println("uri3.string: "+uri1.toString()+" - "+(equalString?"OK":"ERROR"));
            System.err.println("uri3.path  : "+uri1.getPath()+" - "+(equalPath?"OK":"ERROR"));
            System.err.println("uri3.ascii : "+uri1.toASCIIString()+" - "+(equalASCII?"OK":"ERROR"));
            ok = equalString && equalPath && equalASCII && ok;
        }
        {
            final String s2 = "//lala.org/"+string;
            System.err.println("uri4.orig: "+s2);
            final URI uri1 = new URI(IOUtil.HTTP_SCHEME, s2, null);
            final String rString = "http://lala.org/Hallo%20Welt%20öä";
            final String rPath = "/"+string;
            final String rASCII = "http://lala.org/Hallo%20Welt%20%C3%B6%C3%A4";
            final boolean equalString= rString.equals(uri1.toString());
            final boolean equalPath = rPath.equals(uri1.getPath());
            final boolean equalASCII= rASCII.equals(uri1.toASCIIString());
            System.err.println("uri4.string: "+uri1.toString()+" - "+(equalString?"OK":"ERROR"));
            System.err.println("uri4.path  : "+uri1.getPath()+" - "+(equalPath?"OK":"ERROR"));
            System.err.println("uri4.ascii : "+uri1.toASCIIString()+" - "+(equalASCII?"OK":"ERROR"));
            ok = equalString && equalPath && equalASCII && ok;
        }
        {
            final String s2 = "http://lala.org/"+string;
            final String s2enc = IOUtil.encodeToURI(s2);
            System.err.println("uri5.orig: "+s2);
            System.err.println("uri5.enc : "+s2enc);
            final URI uri1 = new URI(s2enc);
            final String rString = "http://lala.org/Hallo%20Welt%20öä";
            final String rPath = "/"+string;
            final String rASCII = "http://lala.org/Hallo%20Welt%20%C3%B6%C3%A4";
            final boolean equalString= rString.equals(uri1.toString());
            final boolean equalPath = rPath.equals(uri1.getPath());
            final boolean equalASCII= rASCII.equals(uri1.toASCIIString());
            System.err.println("uri5.string: "+uri1.toString()+" - "+(equalString?"OK":"ERROR"));
            System.err.println("uri5.path  : "+uri1.getPath()+" - "+(equalPath?"OK":"ERROR"));
            System.err.println("uri5.ascii : "+uri1.toASCIIString()+" - "+(equalASCII?"OK":"ERROR"));
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
        {
            final URI uri0 = file.toURI();
            System.err.println("uri0.string: "+uri0.toString());
            System.err.println("uri0.ascii : "+uri0.toASCIIString());
        }
        final URI uri1 = IOUtil.toURISimple(file);
        System.err.println("uri1.string: "+uri1.toString());
        System.err.println("uri1.ascii : "+uri1.toASCIIString());
        showURI(uri1);
        showURL(uri1.toURL());

        final URL actualUrl = IOUtil.toURL(uri1);
        final String actualUrlS = actualUrl.toExternalForm();
        final String actualFileS = IOUtil.decodeURIIfFilePath(uri1);
        final boolean equalsFilePath = fileExpected.equals(actualFileS);
        System.err.println("actual_______uri  : "+actualUrlS);
        System.err.println("actual___file-path: "+actualFileS);
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
