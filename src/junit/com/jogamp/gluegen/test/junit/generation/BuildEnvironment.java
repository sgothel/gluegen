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
 
package com.jogamp.gluegen.test.junit.generation;

import java.io.File;
import java.net.URISyntaxException;

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
    }
}

