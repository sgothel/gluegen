package com.jogamp.common.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URLConnection;

import org.junit.Assert;

import com.jogamp.common.os.AndroidVersion;
import com.jogamp.common.util.IOUtil;
import com.jogamp.junit.util.SingletonJunitCase;

public abstract class AssetURLConnectionBase extends SingletonJunitCase {

    /** In gluegen-rt.jar */
    protected static final String test_asset_rt_url      = "asset:gluegen/info.txt";
    protected static final String test_asset_rt_entry    = "gluegen/info.txt";

    protected static final String test_asset_rt2_url     = "asset:/gluegen/info.txt";

    /** In gluegen.test.jar */
    protected static final String test_asset_test1_url   = "asset:gluegen-test/info.txt";
    protected static final String test_asset_test1_entry = "gluegen-test/info.txt";
    protected static final Uri.Encoded test_asset_test2_rel   = Uri.Encoded.cast("data/AssetURLConnectionTest.txt");
    protected static final String test_asset_test2a_url  = "asset:com/jogamp/common/net/data/AssetURLConnectionTest.txt";
    protected static final String test_asset_test2b_url  = "asset:/com/jogamp/common/net/data/AssetURLConnectionTest.txt";
    protected static final String test_asset_test2_entry = "com/jogamp/common/net/data/AssetURLConnectionTest.txt";
    protected static final Uri.Encoded test_asset_test3_rel   = Uri.Encoded.cast("RelativeData.txt");
    protected static final String test_asset_test3a_url  = "asset:com/jogamp/common/net/data/RelativeData.txt";
    protected static final String test_asset_test3b_url  = "asset:/com/jogamp/common/net/data/RelativeData.txt";
    protected static final String test_asset_test3_entry = "com/jogamp/common/net/data/RelativeData.txt";
    protected static final Uri.Encoded test_asset_test4_rel   = Uri.Encoded.cast("../data2/RelativeData2.txt");
    protected static final String test_asset_test4a_url  = "asset:com/jogamp/common/net/data2/RelativeData2.txt";
    protected static final String test_asset_test4b_url  = "asset:/com/jogamp/common/net/data2/RelativeData2.txt";
    protected static final String test_asset_test4_entry = "com/jogamp/common/net/data2/RelativeData2.txt";

    protected static void testAssetConnection(final URLConnection c, final String entry_name) throws IOException {
        Assert.assertNotNull(c);
        if(c instanceof AssetURLConnection) {
            final AssetURLConnection ac = (AssetURLConnection) c;
            Assert.assertEquals(entry_name, ac.getEntryName());
        } else if(c instanceof JarURLConnection) {
            final JarURLConnection jc = (JarURLConnection) c;
            if(AndroidVersion.isAvailable) {
                Assert.assertEquals("assets/"+entry_name, jc.getEntryName());
            } else {
                Assert.assertEquals(entry_name, jc.getEntryName());
            }
        }

        final BufferedReader reader = new BufferedReader(new InputStreamReader(c.getInputStream()));
        try {
            String line = null;
            int l = 0;
            while ((line = reader.readLine()) != null) {
                System.err.println(c.getURL()+":"+l+"> "+line);
                l++;
            }
        } finally {
            IOUtil.close(reader, false);
        }
    }
}