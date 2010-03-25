package com.sun.gluegen;

import java.io.File;
import java.net.URISyntaxException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

import static java.lang.System.*;

/**
 * @author Michael Bien
 */
public final class BuildUtil {

    private static final Project project;
    
    public static final String gluegenRoot;
    public static final String path;
    public static final String testOutput;
    public static final String rootrel_build;
    
    static {

        out.println(" - - - System info - - - ");
        out.println("OS: " + System.getProperty("os.name"));
        out.println("VM: " + System.getProperty("java.vm.name"));

        String rootrel_build_tmp = System.getProperty("rootrel.build");
        if(null==rootrel_build_tmp || rootrel_build_tmp.length()==0) {
            rootrel_build_tmp = "build" ;
        }
        rootrel_build = rootrel_build_tmp;
        out.println("rootrel.build: " + rootrel_build);

        // setup paths
        try {
            File executionRoot = new File(BuildUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            out.println("execution root: " + executionRoot);
            gluegenRoot = executionRoot.getParentFile().getParentFile().getParentFile().getParentFile().toString();
            out.println("gluegen project root: " + gluegenRoot);
        } catch (URISyntaxException ex) {
            throw new RuntimeException("can not determine gluegen root", ex);
        }

        path     = gluegenRoot + "/test/junit/com/sun/gluegen";
        testOutput   = gluegenRoot + "/" + rootrel_build + "/test";

        out.println("path: "+path);
        out.println("testOutput: "+testOutput);
        out.println(" - - - - - - - - - - - - ");

        cleanGeneratedFiles();

        //setup ant build file
        project = new Project();
        project.setBaseDir(new File(gluegenRoot));
        project.setProperty("rootrel.build", rootrel_build);
        passSystemProperty(project, "gluegen-cpptasks.file");
        passSystemProperty(project, "os.arch");

        DefaultLogger logger = new DefaultLogger();
        logger.setErrorPrintStream(out);
        logger.setOutputPrintStream(out);
        logger.setMessageOutputLevel(Project.MSG_INFO);
        project.addBuildListener(logger);

        project.init();

        File buildFile = new File(path, "build.xml");
        if(!buildFile.exists()) {
            throw new RuntimeException("buildfile "+buildFile+" does not exist");
        }

        ProjectHelper.configureProject(project, buildFile);
    }

    public static Project passSystemProperty(Project p, String name) {
        String tmp = System.getProperty(name);
        if(null!=tmp && tmp.length()>0) {
            p.setProperty(name, tmp);
        }
        return p;
    }

    public static void cleanGeneratedFiles() {
        out.println("cleaning generated files");
        deleteDirectory(new File(testOutput+"/gensrc"));
        out.println("done");
    }

    /**
     * fails if ant script fails (which is a good thing).
     * executeTarget throws RuntimeException on failure
     */
    public static void compileJava() {
        out.println("compiling java files");
        project.executeTarget("compile.java");
        out.println("done");
    }

    /**
     * fails if ant script fails (which is a good thing)
     * executeTarget throws RuntimeException on failure
     */
    public static void compileNatives() {
        out.println("compiling native files");
        project.executeTarget("compile.native");
        out.println("done");
    }

    public static void generate(String bindingName) {

        out.println("generate binding: " + bindingName);

        GlueGen.main(  "-I"+path,
                       "-O"+testOutput+"/gensrc",
                    // "-Ecom.sun.gluegen.DebugEmitter",
                       "-C"+path+"/"+bindingName+".cfg",
                       path+"/"+bindingName+".h"   );

        out.println("done");
    }

    public static void deleteDirectory(File path) {
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
