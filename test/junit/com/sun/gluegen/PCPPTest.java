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
