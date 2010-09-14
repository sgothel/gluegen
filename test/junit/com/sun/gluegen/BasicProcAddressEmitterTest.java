/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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
 
package com.sun.gluegen;

import com.sun.gluegen.procaddress.ProcAddressEmitter;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.Test;
import static java.util.Arrays.*;
import static com.sun.gluegen.BuildUtil.*;
import static org.junit.Assert.*;

/**
 * Basic test using ProcAddressEmitter.
 * @author Michael Bien
 */
public class BasicProcAddressEmitterTest {

    @Test
    public void generateBindingTest() {
        generate("dyntest", "test", ProcAddressEmitter.class.getName());
    }

    /**
     * fails if ant script fails (which is a good thing).
     * executeTarget throws RuntimeException on failure
     */
    @Test
    public void compileJavaTest() {
        compileJava();
    }

    /*
     * fails if ant script fails (which is a good thing)
     * executeTarget throws RuntimeException on failure
     */
    @Test
    public void compileNativesTest() {
        compileNatives();
    }

    @Test
    public void renameTest() throws Exception {

        Class<?> binding = Class.forName("test.DynBindingTest");
        Class<?> table = Class.forName("test.Table");
        
        Field[] fields = table.getDeclaredFields();


        Set<String> expected = new HashSet<String>(
                asList("arrayTest", "bufferTest", "pbTest", "manyBuffersTest", "mixedTest", "doubleTest"));

        for (Field field : fields) {
            System.out.println("address field: "+field);

            String function = field.getName().substring("_addressoff_".length()-1);
            assertTrue("unexpected field: '"+function+"'",expected.contains(function));
        }

    }

    @AfterClass
    public static void tearDown() {
//        cleanGeneratedFiles();
    }

}
