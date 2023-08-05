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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import static java.lang.System.*;
import static com.jogamp.gluegen.BuildUtil.*;

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
        final String source = gluegenRoot + "/test/junit/com/jogamp/gluegen/StructValidator.java";
        compile(new File(source), testOutput + "/build/classes");

        // invoke test
        final Class<?> test = Class.forName("com.jogamp.gluegen.StructValidator");
        test.getDeclaredMethod("validate").invoke(null);
    }

    private void compile(final File file, final String dest) throws IOException {
        compile(new File[] {file}, dest);
    }

    // yeah, java 6 has even a compiler api...
    private void compile(final File[] files, final String destination) throws IOException {

        out.println("compiling files:\n    " + Arrays.asList(files));

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<JavaFileObject>();
        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(collector, null, null);

        final Iterable<? extends JavaFileObject> fileObj = fileManager.getJavaFileObjects(files);

        final boolean success = compiler.getTask( new OutputStreamWriter(out),
                                            fileManager,
                                            collector,
                                            Arrays.asList("-d", destination/*, "-verbose"*/),
                                            null,
                                            fileObj ).call();

        fileManager.close();

        final List<Diagnostic<? extends JavaFileObject>> list = collector.getDiagnostics();
        if(!list.isEmpty() || !success) {
            for (final Diagnostic<? extends JavaFileObject> d : list) {
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
