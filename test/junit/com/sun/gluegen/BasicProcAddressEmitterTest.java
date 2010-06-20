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
