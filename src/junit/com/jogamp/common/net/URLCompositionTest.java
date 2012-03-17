package com.jogamp.common.net;

import java.io.IOException;
import java.net.MalformedURLException;
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
    public void showURLComponents() throws IOException {
        testURLCompositioning(new URL("file:///rootDir/file1.txt"));
        testURLCompositioning(new URL("file://host/rootDir/file1.txt"));
        testURLCompositioning(new URL("jar:file:/web1/file1.jar!/rootDir/file1.txt"));
        testURLCompositioning(new URL("asset:gluegen-test/info.txt"));
        testURLCompositioning(new URL("asset:/gluegen-test/info.txt"));
        testURLCompositioning(new URL("asset:jar:file:/web1/file1.jar!/rootDir/file1.txt"));
        testURLCompositioning(new URL("http://domain.com:1234/web1/index.html?lala=23&lili=24#anchor"));
    }
    
    static void testURLCompositioning(URL u) throws MalformedURLException {
        final String scheme = u.getProtocol();
        final String auth = u.getAuthority();
        String path = u.getPath();
        String query = u.getQuery();
        String fragment = u.getRef();
        
        System.err.println("scheme <"+scheme+">, auth <"+auth+">, path <"+path+">, query <"+query+">, fragment <"+fragment+">");
        URL u2 = IOUtil.compose(scheme, auth, path, null, query, fragment);
        
        System.err.println("URL-equals: "+u.equals(u2));
        System.err.println("URL-same  : "+u.sameFile(u2));
        System.err.println("URL-orig  : <"+u+">");
        System.err.println("URL-comp  : <"+u2+">");
        Assert.assertEquals(u, u2);
        Assert.assertTrue(u.sameFile(u2));
    }
    
    public static void main(String args[]) throws IOException {
        String tstname = URLCompositionTest.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }    
}
