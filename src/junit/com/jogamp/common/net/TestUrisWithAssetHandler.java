package com.jogamp.common.net;

import static com.jogamp.common.net.URIDumpUtil.showURX;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.junit.util.SingletonJunitCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestUrisWithAssetHandler extends SingletonJunitCase {

    @BeforeClass
    public static void assetRegistration() throws Exception {
        try {
            System.err.println("******* Asset URL Stream Handler Registration: PRE");
            Assert.assertTrue("GenericURLStreamHandlerFactory.register() failed", AssetURLContext.registerHandler(TestUrisWithAssetHandler.class.getClassLoader()));
            Assert.assertNotNull(AssetURLContext.getRegisteredHandler());
            System.err.println("******* Asset URL Stream Handler Registration: POST");
        } catch (final Exception e) {
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

    public static void main(final String args[]) throws IOException {
        final String tstname = TestUrisWithAssetHandler.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
