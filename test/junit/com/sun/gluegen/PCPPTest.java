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

import com.sun.gluegen.pcpp.PCPP;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * serves mainly as entry point for debugging purposes.
 * @author Michael Bien
 */
public class PCPPTest {

    @Test
    public void pcppMacroDefinitionTest() throws FileNotFoundException, IOException {
        
        PCPP pp = new PCPP(Collections.<String>emptyList());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        pp.setOut(output);

        String filename = "pcpptest.h";
        String filepath = BuildUtil.path + "/" + filename;

        pp.run(new BufferedReader(new FileReader(filepath)), filename);

        String expected =   "# 1 \"pcpptest.h\""+
                            "# define CL_SCHAR_MIN (-127-1)"+
                            " cl_char  __attribute__(( aligned(2))) s[ 2];"+
                            "# 7 \"pcpptest.h\"";
        output.flush();
        String result = output.toString();
        output.close();

        assertEquals(killWhitespace(expected), killWhitespace(result));

    }

    private String killWhitespace(String a) {
        return a.replaceAll("\\p{javaWhitespace}+", "");
    }


}
