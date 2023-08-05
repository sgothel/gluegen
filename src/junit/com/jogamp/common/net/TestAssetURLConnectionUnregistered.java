package com.jogamp.common.net;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.common.util.IOUtil;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
        final URLConnection c = IOUtil.getResource(test_asset_rt_entry, this.getClass().getClassLoader());
        testAssetConnection(c, test_asset_rt_entry);
    }

    @Test
    public void assetUnregisteredIOUtilGetResourceRel0_RT() throws IOException, URISyntaxException {
        final URLConnection urlConn0 = IOUtil.getResource(test_asset_test2_rel.get(), this.getClass().getClassLoader(), this.getClass());
        testAssetConnection(urlConn0, test_asset_test2_entry);

        final Uri uri1 = Uri.valueOf(urlConn0.getURL()).getRelativeOf(test_asset_test3_rel);
        Assert.assertNotNull(uri1); // JARFile URL ..
        testAssetConnection(uri1.toURL().openConnection(), test_asset_test3_entry);

        final Uri uri2 = Uri.valueOf(urlConn0.getURL()).getRelativeOf(test_asset_test4_rel);
        Assert.assertNotNull(uri2);
        testAssetConnection(uri2.toURL().openConnection(), test_asset_test4_entry);
    }

    protected static URLConnection createAssetURLConnection(final String path, final ClassLoader cl) throws IOException {
        final URL url = AssetURLContext.createURL(path, cl);
        final URLConnection c = url.openConnection();
        System.err.println("createAssetURL: "+path+" -> url: "+url+" -> conn: "+c+" / connURL "+(null!=c?c.getURL():null));
        return c;
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = AssetURLConnectionUnregisteredTest.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
