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

package com.jogamp.gluegen.test.junit.generation;

import com.jogamp.common.os.AndroidVersion;
import com.jogamp.gluegen.pcpp.PCPP;
import com.jogamp.junit.util.SingletonJunitCase;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * serves mainly as entry point for debugging purposes.
 * @author Sven Gothel, Michael Bien
 */
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PCPPTest extends SingletonJunitCase {

    @BeforeClass
    public static void init() {
        if(AndroidVersion.isAvailable) {
            // PCPP is n/a on Android - GlueGen Runtime only
            setTestSupported(false);
        }
    }

    @Test
    public void pcppMacroDefinitionTest() throws FileNotFoundException, IOException {
        final PCPP pp = new PCPP(Collections.<String>emptyList(), false, false);
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        pp.setOut(output);

        final String filename = "pcpptest.h";
        final String filepath = BuildEnvironment.gluegenRoot + "/src/junit/com/jogamp/gluegen/test/junit/generation/" + filename ;
        pp.run(new BufferedReader(new FileReader(filepath)), filename);

        final String expected =   "# 1 \"pcpptest.h\""+
                            "# define CL_SCHAR_MIN (-127-1)"+
                            "# define __YES__ 1"+
                            "# 16 \"pcpptest.h\""+
                            "# 26 \"pcpptest.h\""+
                            "# 36 \"pcpptest.h\""+
                            " cl_char  GOOD_A;"+
                            " int GOOD_B;"+
                            " int GOOD_C;"+
                            "# 40 \"pcpptest.h\""+
                            "#54\"pcpptest.h\""+
                            " int TEST_D_GOOD;"+
                            "#60\"pcpptest.h\""+
                            "#70\"pcpptest.h\""+
                            "#77\"pcpptest.h\""+
                            "#105\"pcpptest.h\""+
                            "#123\"pcpptest.h\""+
                            " int GOOD_F_1;"+
                            " int GOOD_F_2;"+
                            "#126\"pcpptest.h\""+
                            " int GOOD_G;"+
                            "#128\"pcpptest.h\""+
                            "#130\"pcpptest.h\""+
                            "#134\"pcpptest.h\""+
                            "#136\"pcpptest.h\"";


        output.flush();
        final String result = output.toString();
        output.close();

        System.err.println("Expected: ");
        System.err.println("-------------------------------");
        System.err.println(killWhitespace(expected));
        System.err.println("-------------------------------");
        System.err.println();
        System.err.println("Result: ");
        System.err.println("-------------------------------");
        System.err.println(killWhitespace(result));
        System.err.println("-------------------------------");
        System.err.println();

        assertEquals(killWhitespace(expected), killWhitespace(result));

    }

    private String killWhitespace(final String a) {
        return a.replaceAll("\\p{javaWhitespace}+", "");
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = PCPPTest.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
