/**
 * Copyright 2012 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
 
package com.jogamp.common.util;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import com.jogamp.junit.util.JunitTracer;

public class TestSystemPropsAndEnvs extends JunitTracer {

    @Test
    public void dumpProperties() {
        int i=0;
        Properties props = System.getProperties();
        Iterator<Map.Entry<Object,Object>> iter = props.entrySet().iterator();
        while (iter.hasNext()) {
          i++;
          Map.Entry<Object, Object> entry = iter.next();
          System.out.format("%4d: %s = %s%n", i, entry.getKey(), entry.getValue());
        }
        System.out.println("Property count: "+i);
    }
    
    @Test
    public void dumpEnvironment() {
        int i=0;
        Map<String, String> env = System.getenv();
        for (String envName : env.keySet()) {
            i++;
            System.out.format("%4d: %s = %s%n",
                              i, envName,
                              env.get(envName));
        }        
        System.out.println("Environment count: "+i);
    }
    
    public static void main(String args[]) throws IOException {
        String tstname = TestSystemPropsAndEnvs.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
