package com.jogamp.common.net;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.common.util.IOUtil;

public class AssetURLConnectionUnregisteredTest extends AssetURLConnectionBase {
    @Test
    public void assetUnregisteredURLConnection_RT2() throws IOException {
        testAssetConnection(createAssetURLConnection(test_asset_rt2_url, this.getClass().getClassLoader()), test_asset_rt_entry);
    }
    
    @Test
    public void assetUnregisteredURLConnection_RT() throws IOException {
        testAssetConnection(createAssetURLConnection(test_asset_rt_url, this.getClass().getClassLoader()), test_asset_rt_entry);
    }
    
    @Test
    public void assetUnregisteredURLConnection_Test() throws IOException {
        testAssetConnection(createAssetURLConnection(test_asset_test1_url, this.getClass().getClassLoader()), test_asset_test1_entry);
    }
    
    @Test
    public void assetUnregisteredIOUtilGetResourceAbs_RT() throws IOException {
        URLConnection c = IOUtil.getResource(test_asset_rt_entry, this.getClass().getClassLoader());
        testAssetConnection(c, test_asset_rt_entry);
    }
    
    @Test
    public void assetUnregisteredIOUtilGetResourceRel0_RT() throws IOException {
        final URLConnection urlConn0 = IOUtil.getResource(this.getClass(), test_asset_test2_rel);
        testAssetConnection(urlConn0, test_asset_test2_entry);
        
        final URL url1 = IOUtil.getRelativeOf(urlConn0.getURL(), test_asset_test3_rel);
        Assert.assertNotNull(url1); // JARFile URL ..
        testAssetConnection(url1.openConnection(), test_asset_test3_entry);
    }
    
    protected static URLConnection createAssetURLConnection(String path, ClassLoader cl) throws IOException {
        URL url = AssetURLContext.createURL(path, cl);
        URLConnection c = url.openConnection();
        System.err.println("createAssetURL: "+path+" -> url: "+url+" -> conn: "+c+" / connURL "+(null!=c?c.getURL():null));
        return c;        
    }
        
    public static void main(String args[]) throws IOException {
        String tstname = AssetURLConnectionUnregisteredTest.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }    
}
