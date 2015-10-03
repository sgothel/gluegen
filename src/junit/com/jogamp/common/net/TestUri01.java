package com.jogamp.common.net;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.common.net.URIDumpUtil;
import com.jogamp.common.util.IOUtil;
import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestUri01 extends SingletonJunitCase {

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
            final Uri uri1 = Uri.create(Uri.FILE_SCHEME, null, s2, null);
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
            final Uri uri1 = Uri.create(Uri.FILE_SCHEME, s2, null);
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
            final Uri uri1 = Uri.create(Uri.HTTP_SCHEME, s2, null);
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
    public void test02URIEscapeSpecialChars() throws IOException, URISyntaxException {
        {
            final String vanilla = "XXX ! # $ & ' ( ) * + , / : ; = ? @ [ ]";
            final Uri.Encoded escaped = Uri.Encoded.cast("XXX%20!%20%23%20%24%20%26%20%27%20%28%20%29%20%2A%20%2B%20%2C%20/%20%3A%20%3B%20%3D%20%3F%20%40%20%5B%20%5D");
            System.err.println("vanilla "+vanilla);
            final Uri.Encoded esc1 = new Uri.Encoded(vanilla, Uri.PATH_LEGAL);
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
            final Uri.Encoded esc1 = new Uri.Encoded(vanilla, Uri.PATH_LEGAL);
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
    public void test03URIEscapeCommonChars() throws IOException, URISyntaxException {
        {
            final String vanilla = "/XXX \"%-.<>\\^_`{|}~";
            final Uri.Encoded escaped = Uri.Encoded.cast("/XXX%20%22%25-.%3C%3E%5C%5E_%60%7B%7C%7D~");
            System.err.println("vanilla "+vanilla);
            final Uri.Encoded esc1 = new Uri.Encoded(vanilla, Uri.PATH_LEGAL);
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
        final Uri uri4 = Uri.valueOf(uri3);
        URIDumpUtil.showUri(uri4);

        System.err.println("URI -> Uri (re-encode):");
        final Uri uri5 = Uri.valueOf(uri3);
        URIDumpUtil.showUri(uri5);
    }

    @Test
    public void test04EqualsAndHashCode() throws IOException, URISyntaxException {
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
    public void test05Contained() throws IOException, URISyntaxException {
        {
            final Uri input = Uri.cast("http://localhost/test01.html#tag01");
            final Uri contained = input.getContainedUri();
            Assert.assertNull(contained);
        }
        {
            final Uri input     = Uri.cast("jar:http://localhost/test01.jar!/com/jogamp/Lala.class#tag01");
            final Uri expected  = Uri.cast("http://localhost/test01.jar#tag01");
            final Uri contained = input.getContainedUri();
            URIDumpUtil.showUri(input);
            URIDumpUtil.showUri(contained);
            Assert.assertEquals(expected, contained);
            Assert.assertEquals(expected.hashCode(), contained.hashCode());
        }
        {
            final Uri input     = Uri.cast("jar:file://localhost/test01.jar!/");
            final Uri expected  = Uri.cast("file://localhost/test01.jar");
            final Uri contained = input.getContainedUri();
            URIDumpUtil.showUri(input);
            URIDumpUtil.showUri(contained);
            Assert.assertEquals(expected, contained);
            Assert.assertEquals(expected.hashCode(), contained.hashCode());
        }
        {
            final Uri input     = Uri.cast("sftp:http://localhost/test01.jar?lala=01#tag01");
            final Uri expected  = Uri.cast("http://localhost/test01.jar?lala=01#tag01");
            final Uri contained = input.getContainedUri();
            URIDumpUtil.showUri(input);
            URIDumpUtil.showUri(contained);
            Assert.assertEquals(expected, contained);
            Assert.assertEquals(expected.hashCode(), contained.hashCode());
        }
    }

    @Test
    public void test08NormalizedHierarchy() throws IOException, URISyntaxException {
        {
            final Uri input    = Uri.cast("./dummy/nop/../a.txt");
            final Uri expected = Uri.cast("dummy/a.txt");
            URIDumpUtil.showUri(input);
            final Uri normal = input.getNormalized();
            Assert.assertEquals(expected, normal);
        }
        {
            final Uri input    = Uri.cast("../dummy/nop/../a.txt");
            final Uri expected = Uri.cast("../dummy/a.txt");
            URIDumpUtil.showUri(input);
            final Uri normal = input.getNormalized();
            Assert.assertEquals(expected, normal);
        }
        {
            final Uri input    = Uri.cast("http://localhost/dummy/../");
            final Uri expected = Uri.cast("http://localhost/");
            URIDumpUtil.showUri(input);
            final Uri normal = input.getNormalized();
            Assert.assertEquals(expected, normal);
        }
        {
            final Uri input    = Uri.cast("http://localhost/dummy/./../");
            final Uri expected = Uri.cast("http://localhost/");
            URIDumpUtil.showUri(input);
            final Uri normal = input.getNormalized();
            Assert.assertEquals(expected, normal);
        }
        {
            final Uri input    = Uri.cast("http://localhost/dummy/../aa/././../");
            final Uri expected = Uri.cast("http://localhost/");
            URIDumpUtil.showUri(input);
            final Uri normal = input.getNormalized();
            Assert.assertEquals(expected, normal);
        }
        {
            final Uri input    = Uri.cast("http://localhost/test/dummy/./../text.txt");
            final Uri expected = Uri.cast("http://localhost/test/text.txt");
            URIDumpUtil.showUri(input);
            final Uri normal = input.getNormalized();
            Assert.assertEquals(expected, normal);
        }
        {
            final Uri input    = Uri.cast("http://localhost/test/dummy/../text.txt?lala=01&lili=02#frag01");
            final Uri expected = Uri.cast("http://localhost/test/text.txt?lala=01&lili=02#frag01");
            URIDumpUtil.showUri(input);
            final Uri normal = input.getNormalized();
            Assert.assertEquals(expected, normal);
        }
    }

    @Test
    public void test09NormalizedOpaque() throws IOException, URISyntaxException {
        {
            final Uri input    = Uri.cast("jar:http://localhost/dummy/../abc.jar!/");
            final Uri expected = Uri.cast("jar:http://localhost/abc.jar!/");
            URIDumpUtil.showUri(input);
            final Uri normal = input.getNormalized();
            Assert.assertEquals(expected, normal);
        }
        {
            final Uri input    = Uri.cast("jar:http://localhost/test/./dummy/../abc.jar!/");
            final Uri expected = Uri.cast("jar:http://localhost/test/abc.jar!/");
            URIDumpUtil.showUri(input);
            final Uri normal = input.getNormalized();
            Assert.assertEquals(expected, normal);
        }
        {
            final Uri input    = Uri.cast("jar:http://localhost/test/dummy/../abc.jar!/a/b/C.class");
            final Uri expected = Uri.cast("jar:http://localhost/test/abc.jar!/a/b/C.class");
            URIDumpUtil.showUri(input);
            final Uri normal = input.getNormalized();
            Assert.assertEquals(expected, normal);
        }
        {
            final Uri input    = Uri.cast("jar:http://localhost/test/dummy/../abc.jar!/a/b/C.class?lala=01&lili=02#frag01");
            final Uri expected = Uri.cast("jar:http://localhost/test/abc.jar!/a/b/C.class?lala=01&lili=02#frag01");
            URIDumpUtil.showUri(input);
            final Uri normal = input.getNormalized();
            Assert.assertEquals(expected, normal);
        }
    }

    @Test
    public void test10ParentAndDirHierarchy() throws IOException, URISyntaxException {
        {
            final Uri input = Uri.cast("http://localhost/");
            URIDumpUtil.showUri(input);
            final Uri directory = input.getDirectory();
            Assert.assertEquals(input, directory);
            final Uri parent = input.getParent();
            Assert.assertNull(parent);
        }
        {
            final Uri input    = Uri.cast("http://localhost/dummy/../test/");
            final Uri expectedD = Uri.cast("http://localhost/test/");
            final Uri expectedP = Uri.cast("http://localhost/");
            URIDumpUtil.showUri(input);
            final Uri directory = input.getDirectory();
            Assert.assertEquals(expectedD, directory);
            final Uri parent = input.getParent();
            Assert.assertEquals(expectedP, parent);
        }
        {
            final Uri input    = Uri.cast("http://localhost/dummy/../test/dummy/../");
            final Uri expectedD = Uri.cast("http://localhost/test/");
            final Uri expectedP = Uri.cast("http://localhost/");
            URIDumpUtil.showUri(input);
            final Uri directory = input.getDirectory();
            Assert.assertEquals(expectedD, directory);
            final Uri parent = input.getParent();
            Assert.assertEquals(expectedP, parent);
        }
        {
            final Uri input     = Uri.cast("http://localhost/dir/test01.jar?lala=01#frag01");
            final Uri expParen1 = Uri.cast("http://localhost/dir/?lala=01#frag01");
            final Uri expFolde1 = expParen1;
            final Uri expParen2 = Uri.cast("http://localhost/?lala=01#frag01");
            final Uri expFolde2 = expParen1; // is folder already
            final Uri expParen3 = null;
            final Uri expFolde3 = expParen2;
            Assert.assertNotEquals(input, expParen1);
            Assert.assertNotEquals(expParen1, expParen2);
            Assert.assertNotEquals(expParen1, expParen3);
            URIDumpUtil.showUri(input);

            final Uri parent1 = input.getParent();
            Assert.assertEquals(expParen1, parent1);
            Assert.assertEquals(expParen1.hashCode(), parent1.hashCode());
            final Uri folder1 = input.getDirectory();
            Assert.assertEquals(expFolde1, folder1);

            final Uri parent2 = parent1.getParent();
            Assert.assertEquals(expParen2, parent2);
            Assert.assertEquals(expParen2.hashCode(), parent2.hashCode());
            final Uri folder2 = parent1.getDirectory();
            Assert.assertEquals(expFolde2, folder2);

            final Uri parent3 = parent2.getParent();
            Assert.assertEquals(expParen3, parent3); // NULL!
            final Uri folder3 = parent2.getDirectory();
            Assert.assertEquals(expFolde3, folder3); // NULL!
        }
    }

    @Test
    public void test11ParentAndDirOpaque() throws IOException, URISyntaxException {
        {
            final Uri input = Uri.cast("jar:http://localhost/test.jar!/");
            URIDumpUtil.showUri(input);
            final Uri directory = input.getDirectory();
            Assert.assertEquals(input, directory);
            final Uri parent = input.getParent();
            Assert.assertNull(parent);
        }
        {
            final Uri input    = Uri.cast("jar:http://localhost/dummy/../test/test.jar!/");
            final Uri expectedD = Uri.cast("jar:http://localhost/test/test.jar!/");
            final Uri expectedP = null;
            URIDumpUtil.showUri(input);
            final Uri directory = input.getDirectory();
            Assert.assertEquals(expectedD, directory);
            final Uri parent = input.getParent();
            Assert.assertEquals(expectedP, parent);
        }
        {
            final Uri input    = Uri.cast("jar:http://localhost/dummy/../test/dummy/../test.jar!/a/b/C.class");
            final Uri expectedD = Uri.cast("jar:http://localhost/test/test.jar!/a/b/");
            final Uri expectedP = Uri.cast("jar:http://localhost/test/test.jar!/a/b/");
            URIDumpUtil.showUri(input);
            final Uri directory = input.getDirectory();
            Assert.assertEquals(expectedD, directory);
            final Uri parent = input.getParent();
            Assert.assertEquals(expectedP, parent);
        }
        {
            final Uri input     = Uri.cast("jar:http://localhost/test01.jar!/com/Lala.class?lala=01#frag01");
            final Uri expParen1 = Uri.cast("jar:http://localhost/test01.jar!/com/?lala=01#frag01");
            final Uri expFolde1 = expParen1;
            final Uri expParen2 = Uri.cast("jar:http://localhost/test01.jar!/?lala=01#frag01");
            final Uri expFolde2 = expParen1; // is folder already
            final Uri expParen3 = null;
            final Uri expFolde3 = expParen2; // is folder already
            Assert.assertNotEquals(input, expParen1);
            Assert.assertNotEquals(expParen1, expParen2);
            Assert.assertNotEquals(expParen1, expParen3);
            URIDumpUtil.showUri(input);

            final Uri parent1 = input.getParent();
            Assert.assertEquals(expParen1, parent1);
            Assert.assertEquals(expParen1.hashCode(), parent1.hashCode());
            final Uri folder1 = input.getDirectory();
            Assert.assertEquals(expFolde1, folder1);

            final Uri parent2 = parent1.getParent();
            Assert.assertEquals(expParen2, parent2);
            Assert.assertEquals(expParen2.hashCode(), parent2.hashCode());
            final Uri folder2 = parent1.getDirectory();
            Assert.assertEquals(expFolde2, folder2);

            final Uri parent3 = parent2.getParent();
            Assert.assertEquals(expParen3, parent3); // NULL
            final Uri folder3 = parent2.getDirectory();
            Assert.assertEquals(expFolde3, folder3);
        }
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestUri01.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
