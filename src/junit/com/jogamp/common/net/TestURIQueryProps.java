package com.jogamp.common.net;

import static com.jogamp.common.net.URIDumpUtil.showURI;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.junit.util.JunitTracer;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestURIQueryProps extends JunitTracer {
    
    @Test
    public void test() throws IOException, URISyntaxException {
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
           URIQueryProps data = URIQueryProps.create(uri1, ';');
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
        String tstname = TestURIQueryProps.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }    
}
