package com.sun.gluegen;

import java.io.File;
import java.net.URISyntaxException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;

import static java.lang.System.*;

/**
 * @author Michael Bien
 */
@Ignore
public abstract class AbstractTest {

    static final Project project = new Project();
    
    protected static String gluegenRoot;
    protected static String path;
    protected static String output;

    
    @BeforeClass
    public static void setUp() throws Exception {

        out.println(" - - - System info - - - ");
        out.println("OS: " + System.getProperty("os.name"));
        out.println("VM: " + System.getProperty("java.vm.name"));

        // setup paths
        try {
            File executionRoot = new File(AbstractTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            out.println("execution root: " + executionRoot);
            gluegenRoot = executionRoot.getParentFile().getParentFile().getParentFile().getParentFile().toString();
            out.println("gluegen project root: " + gluegenRoot);
        } catch (URISyntaxException ex) {
            throw new RuntimeException("can not determine gluegen root", ex);
        }

        path     = gluegenRoot + "/test/junit/com/sun/gluegen";
        output   = gluegenRoot + "/build/test";

        out.println("path: "+path);
        out.println("output: "+output);
        out.println(" - - - - - - - - - - - - ");

        deleteDirectory(new File(output+"/gensrc"));

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

    @AfterClass
    public static void tearDown() {
//        deleteDirectory(new File(output));
    }

    /**
     * fails if ant script fails (which is a good thing).
     * executeTarget throws RuntimeException on failure
     */
    public final void compileJava() {
        project.executeTarget("compile.java");
    }

    /**
     * fails if ant script fails (which is a good thing)
     * executeTarget throws RuntimeException on failure
     */
    public final void compileNatives() {
        project.executeTarget("compile.native");
    }

    static final void generate(String config) {
        out.println("generate: "+config);
        GlueGen.main(
            new String[] {
                "-I"+path,
                "-O"+output+"/gensrc",
//                "-Ecom.sun.gluegen.DebugEmitter",
                "-C"+path+"/"+config+".cfg",
                path+"/"+config+".h"
            }
        );
        out.println("done");
    }

    static final void deleteDirectory(File path) {
        if(path.exists()) {

            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
            
            path.delete();
        }
    }


}
