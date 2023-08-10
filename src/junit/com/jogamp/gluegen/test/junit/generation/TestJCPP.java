/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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
import com.jogamp.gluegen.jcpp.JCPP;
import com.jogamp.gluegen.jcpp.LexerException;
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
public class TestJCPP extends SingletonJunitCase {

    @BeforeClass
    public static void init() {
        if(AndroidVersion.isAvailable) {
            // JCPP is n/a on Android - GlueGen Runtime only
            setTestSupported(false);
        }
    }

    @Test
    public void test01MacroAndIncWithoutPragmaOnce() {
        Exception ex = null;
        try {
            testMacroAndInc(false);
        } catch (IOException | LexerException e) {
            e.printStackTrace();
            ex = e;
        }
        assertNull(ex);
    }

    @Test
    public void test02MacroAndIncWithPragmaOnce() {
        Exception ex = null;
        try {
            testMacroAndInc(true);
        } catch (IOException | LexerException e) {
            e.printStackTrace();
            ex = e;
        }
        assertNull(ex);
    }

    public void testMacroAndInc(final boolean pragmaOnce) throws FileNotFoundException, IOException, LexerException {
        final String folderpath = BuildEnvironment.gluegenRoot + "/src/junit/com/jogamp/gluegen/test/junit/generation";
        final JCPP pp = new JCPP(Collections.<String>singletonList(folderpath), false, false, pragmaOnce);
        if( pragmaOnce ) {
            pp.addDefine("PRAGMA_ONCE_ENABLED", "1");
        }
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        pp.setOut(output);

        final String filename = "cpptest_1.h";
        final String filepath = folderpath + "/" + filename ;
        pp.run(new BufferedReader(new FileReader(filepath)), filename);

        final String expected =
                            "#line 1 \"cpptest_1.h\" 1"+
                            ""+
                            "typedef char cl_char;"+
                            "cl_char  GOOD_A;"+
                            "int GOOD_B;"+
                            "int GOOD_C;"+
                            ""+
                            "    int TEST_D_GOOD;"+
                            ""+
                            "/***"+
                            " ** STD API file .."+
                            " */"+
                            ""+
                            "int GOOD_F_1;"+
                            "int GOOD_F_2;"+
                            ""+
                            "int GOOD_G;"+
                            ""+
                            "#line 1 \""+folderpath+"/cpptest-included.h\" 1"+
                            ""+
                            ( pragmaOnce ?
                                    "    const int pragma_once_enabled = 1;"
                              :     "    const int pragma_once_enabled = 0;"
                            )+
                            ""+
                            "    // pragma-once or macro-defined test, i.e. should not be included recursively"+
                            "#line 1 \""+folderpath+"/cpptest-included.h\" 1"+
                            ""+
                            "#line 13 \""+folderpath+"/cpptest-included.h\" 2"+
                            ""+
                            "const int GOOD_H = 42;"+
                            ""+
                            "#line 136 \"cpptest_1.h\" 2"+
                            "#line 1 \""+folderpath+"/sub-inc/-cpptest-included2.h\" 1"+
                            ""+
                            "const int GOOD_I = 43;"+
                            "#line 137 \"cpptest_1.h\" 2"+
                            ""+
                            "typedef enum SomeEnum {"+
                            "  ConstEnumValue00 = 16,"+
                            "  ConstEnumValue01 = (1 << ConstEnumValue00) - 1,"+
                            "  ConstEnumValue02 = (10-1),"+
                            "  ConstEnumValue03 = (10 - 2),"+
                            "  ConstEnumValue04 = ( 10 - 3 ),"+
                            "  ConstEnumValue05 = 10-4,"+
                            "  ConstEnumValue06 = 10 - 11,"+
                            "  ConstEnumValue07 = -2,"+
                            "  ConstEnumValue08 = - 2,"+
                            "  ConstEnumValueXX = 0"+
                            "} SomeEnum;"+
                            ""+
                            "const int constInt00 = 16;"+
                            "const int constInt01 = ((1 << 16) - 1);"+
                            "const int constInt02 = (10-1);"+
                            "const int constInt03 = (10 - 2);"+
                            "const int constInt04 = ( 10 - 3 );"+
                            "const int constInt05 = 10-4;"+
                            "const int constInt06 = 10 - 11;"+
                            "const int constInt07 = -2;"+
                            "const int constInt08 = - 2;"+
                            "const int constIntXX = 0;"+
                            ""
        ;

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
        // System.err.println(result);
        System.err.println(killWhitespace(result));
        System.err.println("-------------------------------");
        System.err.println();

        assertEquals(killWhitespace(expected), killWhitespace(result));
    }

    private String killWhitespace(final String a) {
        return a.replaceAll("\\p{javaWhitespace}+", "");
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestJCPP.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
