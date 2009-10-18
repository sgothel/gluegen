package com.sun.gluegen;

import com.sun.gluegen.runtime.BufferFactory;
import com.sun.gluegen.runtime.PointerBuffer;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.lang.System.*;

/**
 * @author Michael Bien
 */
public class GlueGenTest {

    private static final Project project = new Project();
    
    private static String gluegenRoot;
    private static String path;
    private static String output;

    
    @BeforeClass
    public static void setUpTest() throws Exception {

        out.println("System info: ");
        out.println("OS: " + System.getProperty("os.name"));
        out.println("VM: " + System.getProperty("java.vm.name"));

        // setup paths
        try {
            File executionRoot = new File(GlueGenTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            System.out.println("execution root: " + executionRoot);
            gluegenRoot = executionRoot.getParentFile().getParentFile().getParentFile().getParentFile().toString();
            System.out.println("gluegen project root: " + gluegenRoot);
        } catch (URISyntaxException ex) {
            Logger.getLogger(GlueGenTest.class.getName()).log(Level.SEVERE, "can not determine gluegen root", ex);
            Assert.fail();
        }

        path     = gluegenRoot + "/test/junit/com/sun/gluegen";
        output   = gluegenRoot + "/build/test";

        //setup ant build file
        project.setBaseDir(new File(gluegenRoot));

        DefaultLogger logger = new DefaultLogger();
        logger.setErrorPrintStream(err);
        logger.setOutputPrintStream(out);
        logger.setMessageOutputLevel(Project.MSG_INFO);
        project.addBuildListener(logger);

        project.init();

        File buildFile = new File(path, "build.xml");
        ProjectHelper.configureProject(project, buildFile);
    }

    @Test
    public void generateBindingTest() {

        out.println("path: "+path);
        out.println("output: "+output);

        String name = "test";
        
        GlueGen.main(
            new String[] {
                "-I"+path,
                "-O"+output+"/gensrc",
//                "-Ecom.sun.gluegen.DebugEmitter",
                "-C"+path+"/"+name+".cfg",
                path+"/"+name+".h"
            }
        );
    }

    /* yeah, java 6 has even a compiler api...
    @Test
    public void compileJavaTest() throws IOException {
        
        out.println("compiling generated files...");

        String source = output+"/gensrc/java/test/BindingTest.java";

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(collector, null, null);

        Iterable<? extends JavaFileObject> fileObj = fileManager.getJavaFileObjects(source);

        compiler.getTask(   new OutputStreamWriter(out),
                            fileManager,
                            collector,
                            Arrays.asList("-d",output+"/build/java","-verbose"),
                            null,
                            fileObj ).call();

        List<Diagnostic<? extends JavaFileObject>> list = collector.getDiagnostics();
        if(!list.isEmpty()) {
            for (Diagnostic<? extends JavaFileObject> d : list) {
                out.println("Error on line "+ d.getLineNumber());
                out.println("Compiler Message:\n"+d.getMessage(Locale.ENGLISH));
            }
            Assert.fail("compilation test failed");
        }

        fileManager.close();

        out.println("done");

    }
*/

    /*
     * fails when ant script fails (which is a good thing).
     * executeTarget throws RuntimeException on failure
     */
    @Test
    public void compileJavaTest() {
        project.executeTarget("compile.java");
    }
    
    /*
     * fails when ant script fails (which is a good thing)
     * executeTarget throws RuntimeException on failure
     */
    @Test
    public void compileNativesTest() {
        project.executeTarget("compile.native");
    }

    @Test
    public void bindingTest() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, InstantiationException {
        
        String nativesPath = output + "/build/natives";
        System.load(nativesPath + "/libgluegen-rt.so");
        System.load(nativesPath + "/librofl.so");

        Object bindingTest = Class.forName("test.BindingTest").newInstance();

        // test values
        ByteBuffer dbb = BufferFactory.newDirectByteBuffer(32);
        ByteBuffer bb  = ByteBuffer.allocate(32).order(ByteOrder.nativeOrder());
        
        PointerBuffer dpb = PointerBuffer.allocateDirect(32);
        PointerBuffer pb  = PointerBuffer.allocate(32);

        long[] array = new long[] {1,2,3,4,5,6,7,8,9};
        int offset = 0;
        long id = 42;

        // invoke everything public
        Method[] methods = bindingTest.getClass().getDeclaredMethods();

        for (Method method : methods) {

            // prepare method parameters
            Class<?>[] paramTypes = method.getParameterTypes();
            Object[] paramInstances = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> paramType = paramTypes[i];
                if(paramType.isInstance(dbb)) {
                    paramInstances[i] = dbb;
                }else if(paramType.isInstance(bb)) {
                    paramInstances[i] = bb;
                }else if(paramType.isInstance(dpb)) {
                    paramInstances[i] = dpb;
                }else if(paramType.isInstance(pb)) {
                    paramInstances[i] = pb;
                }else if(paramType.isPrimitive()) { // TODO primitive types
                    paramInstances[i] = offset;
                }else if(paramType.isArray()) {     // TODO array types
                    paramInstances[i] = array;
                }
            }
            
            out.println("invoking: "+method);
            out.println("with params: ");
            for (Object param : paramInstances)
                out.print(param+", ");
            out.println();

            // TODO fix Exception: ...Caused by: java.lang.UnsatisfiedLinkError: test.BindingTest.arrayTest0(JLjava/lang/Object;I)I

            Object result = method.invoke(bindingTest, paramInstances);
            out.println("result: "+result);
            out.println("success");
        }

    }


}
