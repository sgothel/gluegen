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

package com.jogamp.gluegen;

import com.jogamp.gluegen.JavaEmitter;
import com.jogamp.gluegen.GlueGen;
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
            final File executionRoot = new File(BuildUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            out.println("execution root: " + executionRoot);
            gluegenRoot = executionRoot.getParentFile().getParentFile().getParentFile().getParentFile().toString();
            out.println("gluegen project root: " + gluegenRoot);
        } catch (final URISyntaxException ex) {
            throw new RuntimeException("can not determine gluegen root", ex);
        }

        path       = gluegenRoot + "/test/junit/com/jogamp/gluegen";
        testOutput = gluegenRoot + "/" + rootrel_build + "/test";

        out.println("path: "+path);
        out.println("testOutput: "+testOutput);
        out.println(" - - - - - - - - - - - - ");

        cleanGeneratedFiles();

        //setup ant build file
        project = new Project();
        project.setProperty("rootrel.build", rootrel_build);
        passSystemProperty(project, "gluegen-cpptasks.file");
        passSystemProperty(project, "os.arch");

        final DefaultLogger logger = new DefaultLogger();
        logger.setErrorPrintStream(out);
        logger.setOutputPrintStream(out);
        logger.setMessageOutputLevel(Project.MSG_WARN);
        project.addBuildListener(logger);

        project.init();

        final File buildFile = new File(path, "build.xml");
        if(!buildFile.exists()) {
            throw new RuntimeException("buildfile "+buildFile+" does not exist");
        }

        ProjectHelper.configureProject(project, buildFile);
    }

    public static Project passSystemProperty(final Project p, final String name) {
        final String tmp = System.getProperty(name);
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

    public static void generate(final String bindingName) {
        generate(bindingName, JavaEmitter.class.getName());
//        generate(bindingName, DebugEmitter.class.getName());
    }

    public static void generate(final String bindingName, final String emitter) {
        generate(bindingName, bindingName, emitter);
    }
    public static void generate(final String bindingName, final String header, final String emitter) {

        out.println("generate binding to '" + bindingName+"' using '"+emitter+"'");

        GlueGen.main(  "-I"+path,
                       "-O"+testOutput+"/gensrc",
                       "-E"+emitter,
                       "-C"+path+"/"+bindingName+".cfg",
                       path+"/"+header+".h"   );

        out.println("done");
    }

    public static void deleteDirectory(final File path) {
        if(path.exists()) {

            final File[] files = path.listFiles();
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
