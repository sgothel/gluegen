package com.jogamp.common.net;

import static com.jogamp.common.net.URIDumpUtil.showUri;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestUriQueryProps extends SingletonJunitCase {

    @Test
    public void test() throws IOException, URISyntaxException {
       final String SCHEME = "camera";
       final String HOST = "somewhere";
       final String PATH = "0";
       final String[] args = new String[] {
           SCHEME+"://"+HOST+"/"+PATH,
           SCHEME+"://"+HOST+"/"+PATH+"?p1=1",
       };
       for(int i=0; i<args.length-1; i+=2) {
           final String uri_s0 = args[i];
           final String uri_s1 = args[i+1];
           final Uri uri0 = Uri.cast(uri_s0);
           final Uri uri1 = Uri.cast(uri_s1);
           showUri(uri0);
           showUri(uri1);
           final UriQueryProps data = UriQueryProps.create(uri1, ';');
           if(null == data) {
               System.err.println("Error: NULL: <"+uri_s1+"> -> "+uri1+" -> NULL");
           } else {
               final Uri uri1T = data.appendQuery(uri0);
               showUri(uri1T);
               Assert.assertEquals(uri1, uri1T);
           }
       }
    }
    public static void main(final String args[]) throws IOException {
        final String tstname = TestUriQueryProps.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
