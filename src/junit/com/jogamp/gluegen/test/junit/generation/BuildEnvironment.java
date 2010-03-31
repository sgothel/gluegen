/*
 * Copyright (c) 2010 Sven Gothel. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name Sven Gothel or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SVEN GOTHEL HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

package com.jogamp.gluegen.test.junit.generation;

import java.io.File;
import java.net.URISyntaxException;

import org.apache.tools.ant.DefaultLogger;

import static java.lang.System.*;

/**
 * @author Michael Bien
 * @author Sven Gothel
 */
public final class BuildEnvironment {

    public static final String gluegenRoot;
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
            File executionRoot = new File(BuildEnvironment.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            out.println("execution root: " + executionRoot);
            gluegenRoot = executionRoot.getParentFile().getParentFile().getParentFile().getParentFile().toString();
            out.println("gluegen project root: " + gluegenRoot);
        } catch (URISyntaxException ex) {
            throw new RuntimeException("can not determine gluegen root", ex);
        }

        testOutput   = gluegenRoot + "/" + rootrel_build + "/test";

        out.println("testOutput: "+testOutput);
        out.println(" - - - - - - - - - - - - ");

        DefaultLogger logger = new DefaultLogger();
        logger.setErrorPrintStream(out);
        logger.setOutputPrintStream(out);
    }
}

