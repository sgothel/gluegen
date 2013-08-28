package com.jogamp.common.net;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.common.util.IOUtil;
import com.jogamp.junit.util.JunitTracer;

public class URLCompositionTest extends JunitTracer {
    
    @BeforeClass
    public static void assetRegistration() throws Exception {
        try {
            System.err.println("******* Asset URL Stream Handler Registration: PRE");
            Assert.assertTrue("GenericURLStreamHandlerFactory.register() failed", AssetURLContext.registerHandler(URLCompositionTest.class.getClassLoader()));
            Assert.assertNotNull(AssetURLContext.getRegisteredHandler());
            System.err.println("******* Asset URL Stream Handler Registration: POST");
        } catch (Exception e) {
            setTestSupported(false);
            throw e;
        }        
    }
    
    @Test
    public void showURLComponents0() throws IOException, URISyntaxException {
        showURX("file:///rootDir/file1.txt");
        showURX("file://host/rootDir/file1.txt");
        showURX("jar:file:/web1/file1.jar!/rootDir/file1.txt");
        showURX("asset:gluegen-test/info.txt");
        showURX("asset:/gluegen-test/info.txt");
        showURX("http://domain.com/web1/index.html?lala=23&lili=24#anchor");
        showURX("http://domain.com:1234/web1/index.html?lala=23&lili=24#anchor");
        showURX("asset:jar:file:/web1/file1.jar!/rootDir/file1.txt");
        showURX("asset:jar:file:/web1/file1.jar!/rootDir/./file1.txt");
        showURX("asset:jar:file:/web1/file1.jar!/rootDir/dummyParent/../file1.txt");
    }
    
    static void showURX(String urx) throws MalformedURLException, URISyntaxException {
        System.err.println("XXXXXX "+urx);
        showURL(new URL(urx));
        showURI(new URI(urx)); 
        System.err.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    }
    
    static void showURL(URL url) {
        System.err.println("YYYYYY URL "+url);
        System.err.println("protocol: "+url.getProtocol());
        System.err.println("auth:     "+url.getAuthority());
        System.err.println("host:     "+url.getHost());
        System.err.println("port:     "+url.getPort() + " ( " + url.getDefaultPort() + " ) " );
        System.err.println("file:     "+url.getFile() + " ( path " + url.getPath() + ", query " + url.getQuery() + " ) " );
        System.err.println("ref:      "+url.getRef());
    }
    static void showURI(URI uri) {
        System.err.println("ZZZZZZ URI "+uri);
        // 1 [scheme:]scheme-specific-part[#fragment]  
        System.err.println("1 scheme:      "+uri.getScheme());
        System.err.println("1 scheme-part: "+uri.getRawSchemeSpecificPart());
        System.err.println("1 fragment:    "+uri.getRawFragment());
        
        // 2 [scheme:][//authority][path][?query][#fragment]
        System.err.println("2 scheme:      "+uri.getScheme());
        System.err.println("2 auth:        "+uri.getRawAuthority());
        System.err.println("2 path:        "+uri.getRawPath());
        System.err.println("2 query:       "+uri.getRawQuery());
        System.err.println("2 fragment:    "+uri.getRawFragment());
        
        // 3 [scheme:][//authority][path][?query][#fragment]
        //   authority: [user-info@]host[:port]
        System.err.println("3 scheme:      "+uri.getScheme());
        System.err.println("3 user-info:   "+uri.getRawUserInfo());
        System.err.println("3 host:        "+uri.getHost());
        System.err.println("3 port:        "+uri.getPort());
        System.err.println("3 path:        "+uri.getRawPath());
        System.err.println("3 query:       "+uri.getRawQuery());
        System.err.println("3 fragment:    "+uri.getRawFragment());
    }    
    
    
    @Test
    public void showURLComponents1() throws IOException, URISyntaxException {
        testURI2URL("jar:file:/usr/local/projects/JOGL/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class",
                    "jar:file:/usr/local/projects/JOGL/gluegen/build-x86_64 öä lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class", true);
        
        testURI2URL("jar:file:/usr/local/projects/JOGL/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar!/",
                    "jar:file:/usr/local/projects/JOGL/gluegen/build-x86_64 öä lala/gluegen-rt.jar!/", true);
        
        testURI2URL("file:/usr/local/projects/JOGL/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar", 
                    "file:/usr/local/projects/JOGL/gluegen/build-x86_64 öä lala/gluegen-rt.jar", true);
        
        testURI2URL("jar:http:/usr/local/projects/JOGL/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class",
                    "jar:http:/usr/local/projects/JOGL/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class", false);
        
        testURI2URL("jar:http:/usr/local/projects/JOGL/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar!/",
                    "jar:http:/usr/local/projects/JOGL/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar!/", false);
        
        testURI2URL("http:/usr/local/projects/JOGL/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar", 
                    "http:/usr/local/projects/JOGL/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar", false);
        
        testURI2URL("jar:ftp:/usr/local/projects/JOGL/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class",
                    "jar:ftp:/usr/local/projects/JOGL/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar!/com/jogamp/common/os/Platform.class", false);
                
        testURI2URL("ftp:/usr/local/projects/JOGL/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar",
                    "ftp:/usr/local/projects/JOGL/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar", false);
    }
    
    void testURI2URL(String source, String expected, boolean usesFile) throws IOException, URISyntaxException {
        System.err.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        final URI uri0 = new URI(source);
        System.err.println("uri: "+uri0.toString());
        
        final URL url0 = IOUtil.toURL(uri0);
        final String actual = url0.toExternalForm();
        System.err.println("url: "+actual);
        final String expectedOS;
        if( usesFile ) {
            final int jarSepI = expected.lastIndexOf("!");
            final int replB = 0 <= jarSepI ? jarSepI : expected.length();
            final String partA = expected.substring(0, replB).replace('/', File.separatorChar);
            final String partB = expected.substring(replB);
            expectedOS = partA+partB; 
        } else {
            expectedOS = expected;
        }
        System.err.println("expected: orig "+expected+", OS "+expectedOS);
        final boolean equalsA = expected.equals(actual);
        final boolean equalsB = expectedOS.equals(actual);
        Assert.assertTrue("No match, neither expected orig or OS w/ url", equalsA || equalsB);
        System.err.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    }
    
    @Test
    public void showURLComponents2() throws IOException, URISyntaxException {
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
        
    static void testURNCompositioning(String urn) throws MalformedURLException, URISyntaxException {
        testURICompositioning( new URI(urn) );
        testURLCompositioning( new URL(urn) );
    }
    
    static void testURICompositioning(URI uri) throws MalformedURLException, URISyntaxException {    
        testURICompositioning(uri, uri);
    }    
    static void testURICompositioning(URI refURI, URI uri1) throws MalformedURLException, URISyntaxException {
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
    
    static void testURLCompositioning(URL url) throws MalformedURLException, URISyntaxException {    
        testURLCompositioning(url, url);
    }
    static void testURLCompositioning(URL refURL, URL url1) throws MalformedURLException, URISyntaxException {
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
    
    @Test
    public void testURLProps() throws IOException, URISyntaxException {
       final String SCHEME = "camera";
       final String HOST = "somewhere";
       final String PATH = "0";
       String[] args = new String[] {
           SCHEME+"://"+HOST+"/"+PATH,
           SCHEME+"://"+HOST+"/"+PATH+"?p1=1",
       };
       for(int i=0; i<args.length-1; i+=2) {
           String uri_s0 = args[i];
           String uri_s1 = args[i+1];
           URI uri0 = new URI(uri_s0);
           URI uri1 = new URI(uri_s1);
           showURI(uri0);
           showURI(uri1);
           URIQueryProps data = URIQueryProps.create(uri1);
           if(null == data) {
               System.err.println("Error: NULL: <"+uri_s1+"> -> "+uri1+" -> NULL");
           } else {
               URI uri1T = data.appendQuery(uri0);
               showURI(uri1T);
               Assert.assertEquals(uri1, uri1T);
           }
       }
    }
    public static void main(String args[]) throws IOException {
        String tstname = URLCompositionTest.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }    
}
