package com.sun.gluegen;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.Test;
import static java.lang.System.*;
import static com.sun.gluegen.BuildUtil.*;

/**
 *
 * @author Michael Bien
 */
public class StructAccessorTest {

    @Test
    public void generateStruct() {
        generate("struct");
    }

    @Test
    public void compileStructJava() {
        compileJava();
    }

    @Test
    public void compileStructNatives() {
        // this will only copy gluegen-rt to the right place
        compileNatives();
    }

    @Test
    public void validateGeneratedStructs() throws IOException, NoSuchMethodException, ClassNotFoundException, IllegalAccessException, InvocationTargetException {

        // compile testcase
        String source = gluegenRoot + "/test/junit/com/sun/gluegen/StructValidator.java";
        compile(new File(source), gluegenRoot+"/build/test/build/classes");

        // invoke test
        Class<?> test = Class.forName("com.sun.gluegen.StructValidator");
        test.getDeclaredMethod("validate").invoke(null);
    }

    private void compile(File file, String dest) throws IOException {
        compile(new File[] {file}, dest);
    }

    // yeah, java 6 has even a compiler api...
    private void compile(File[] files, String destination) throws IOException {

        out.println("compiling files:\n    " + Arrays.asList(files));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(collector, null, null);

        Iterable<? extends JavaFileObject> fileObj = fileManager.getJavaFileObjects(files);

        boolean success = compiler.getTask( new OutputStreamWriter(out),
                                            fileManager,
                                            collector,
                                            Arrays.asList("-d", destination/*, "-verbose"*/),
                                            null,
                                            fileObj ).call();

        fileManager.close();

        List<Diagnostic<? extends JavaFileObject>> list = collector.getDiagnostics();
        if(!list.isEmpty() || !success) {
            for (Diagnostic<? extends JavaFileObject> d : list) {
                out.println("Error on line "+ d.getLineNumber());
                out.println("Compiler Message:\n"+d.getMessage(Locale.ENGLISH));
            }
            Assert.fail("compilation failed");
        }

        out.println("done");

    }

    @AfterClass
    public static void tearDown() {
//        cleanGeneratedFiles();
    }

}
