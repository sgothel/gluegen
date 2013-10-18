/*
 * Copyright (c) 2010, Michael Bien. All rights reserved.
 * Copyright (c) 2013 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Michael Bien nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Michael Bien BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.jogamp.gluegen.structgen;

import com.jogamp.common.util.PropertyAccess;
import com.jogamp.gluegen.GlueGen;
import com.jogamp.gluegen.JavaEmitter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import jogamp.common.Debug;

/**
 * <p>
 * If the <i>header file</i> is absolute, the <i>root path</i> is the parent folder of the folder containing the package source, i.e.:
 * <pre>
 *  Header: /gluegen/src/junit/com/jogamp/test/structgen/TestStruct01.h
 *  Root:   /gluegen/src/junit/..
 *  Root:   /gluegen/src
 * </pre>
 * Otherwise the <i>user.dir</i> is being used as the <i>root path</i>
 * and the relative <i>header file</i> is appended to it.
 * </p>
 * The property <code>jogamp.gluegen.structgen.output</code> allows setting a default <i>outputPath</i>
 * for the generated sources, if the {@link ProcessingEnvironment}'s <code>structgen.output</code> option is not set.
 * <p>
 * If the <i>outputPath</i> is relative, it is appended to the <i>root path</i>,
 * otherwise it is taken as-is.
 * </p>
 * <p>
 * User can enable DEBUG while defining property <code>jogamp.gluegen.structgen.debug</code>.
 * </p>
 *
 * @author Michael Bien
 * @author Sven Gothel, et.al.
 */
@SupportedAnnotationTypes(value = {"com.jogamp.gluegen.structgen.CStruct"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class CStructAnnotationProcessor extends AbstractProcessor {
    private static final String DEFAULT = "_default_";
    private static final boolean DEBUG;

    static {
        Debug.initSingleton();
        DEBUG = Debug.isPropertyDefined("jogamp.gluegen.structgen.debug", true);
    }

    private static final String STRUCTGENOUTPUT_OPTION = "structgen.output";
    private static final String STRUCTGENOUTPUT = PropertyAccess.getProperty("jogamp.gluegen."+STRUCTGENOUTPUT_OPTION, true, "gensrc");

    private Filer filer;
    private Messager messager;
    private Elements eltUtils;
    private String outputPath;

    private final static Set<String> generatedStructs = new HashSet<String>();


    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        eltUtils = processingEnv.getElementUtils();

        outputPath = processingEnv.getOptions().get(STRUCTGENOUTPUT_OPTION);
        outputPath = outputPath == null ? STRUCTGENOUTPUT : outputPath;
    }

    private File locateSource(String packageName, String relativeName) {
        try {
            if( DEBUG ) {
                System.err.println("CStruct.locateSource.0: p "+packageName+", r "+relativeName);
            }
            final FileObject h = filer.getResource(StandardLocation.SOURCE_PATH, packageName, relativeName);
            if( DEBUG ) {
                System.err.println("CStruct.locateSource.1: h "+h.toUri());
            }
            final File f = new File( h.toUri().getPath() ); // URI is incomplete (no scheme), hence use path only!
            if( f.exists() ) {
                return f;
            }
        } catch (IOException e) { if(DEBUG) { System.err.println("Catched "+e.getClass().getSimpleName()+": "+e.getMessage()); /* e.printStackTrace(); */ } }
        return null;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        final String user_dir = System.getProperty("user.dir");

        final Set<? extends Element> elements = env.getElementsAnnotatedWith(CStruct.class);

        for (Element element : elements) {

            final String packageName = eltUtils.getPackageOf(element).toString();

            try {
                final CStruct struct = element.getAnnotation(CStruct.class);
                final String headerRelPath = struct.header();
                final Element enclElement = element.getEnclosingElement();

                System.err.println("CStruct: "+struct+", package "+packageName+", header "+headerRelPath);
                if(DEBUG) {
                    System.err.println("CStruct.0: user.dir: "+user_dir);
                    System.err.println("CStruct.0: element: "+element+", .simpleName "+element.getSimpleName());
                    System.err.println("CStruct.0: enclElement: "+enclElement+", .simpleName "+enclElement.getSimpleName()+", .package "+eltUtils.getPackageOf(enclElement).toString());
                }

                final File headerFile;
                {
                    File f = locateSource(packageName, headerRelPath);
                    if( null == f ) {
                        f = locateSource("", headerRelPath);
                        if( null == f ) {
                            // bail out
                            throw new RuntimeException("Could not locate header "+headerRelPath+", package "+packageName);
                        }
                    }
                    headerFile = f;
                }

                final String root;
                {
                    String root0 = headerFile.getAbsolutePath();
                    root0 = root0.substring(0, root0.length()-headerFile.getName().length()-1);
                    root = root0.substring(0, root0.length()-packageName.length()) +"..";
                }
                System.err.println("CStruct: "+headerFile+", abs: "+headerFile.isAbsolute()+", root "+root);

                generateStructBinding(element, struct, root, packageName, headerFile);
            } catch (IOException ex) {
                throw new RuntimeException("IOException while processing!", ex);
            }
        }
        return true;
    }

    private void generateStructBinding(Element element, CStruct struct, String root, String pakage, File header) throws IOException {
        final String declaredType = element.asType().toString();
        final String structName   = struct.name().equals(DEFAULT) ? declaredType : struct.name();

        if( generatedStructs.contains(structName) ) {
            messager.printMessage(Kind.WARNING, "struct "+structName+" already defined elsewhere.", element);
            return;
        }
        System.out.println("generating struct accessor for struct: "+structName);

        generatedStructs.add(structName);

        final boolean outputDirAbs;
        {
            final File outputDirFile = new File(outputPath);
            outputDirAbs = outputDirFile.isAbsolute();
        }
        final String outputPath1 = outputDirAbs ? outputPath : root + File.separator + outputPath;
        final String config = outputPath1 + File.separator + header.getName() + ".cfg";
        final File configFile = new File(config);
        if(DEBUG) {
            System.err.println("CStruct: OutputDir: "+outputPath+", is-abs "+outputDirAbs);
            System.err.println("CStruct: OutputPath: "+outputPath1);
            System.err.println("CStruct: ConfigFile: "+configFile);
        }

        FileWriter writer = null;
        try{
            writer = new FileWriter(configFile);
            writer.write("Package "+pakage+"\n");
            writer.write("EmitStruct "+structName+"\n");
            if(!struct.name().equals(DEFAULT)) {
                writer.write("RenameJavaType " + struct.name()+" " + declaredType +"\n");
            }
        } finally {
            if( null != writer ) {
                writer.close();
            }
        }

        // TODO: Handle exceptions .. suppressed by Gluegen.main(..) ?
        GlueGen.main(
                //                "-I"+path+"/build/",
                "-O" + outputPath1,
                "-E" + AnnotationProcessorJavaStructEmitter.class.getName(),
                "-C" + config,
                header.getPath());

        configFile.delete();
    }

    public static class AnnotationProcessorJavaStructEmitter extends JavaEmitter {

        @Override
        protected PrintWriter openFile(String filename, String simpleClassName) throws IOException {

            // look for recursive generated structs... keep it DRY
            if( !simpleClassName.endsWith("32") &&
                !simpleClassName.endsWith("64") ) {

                System.out.println("generating -> " + simpleClassName);
                generatedStructs.add(simpleClassName);
            }

            return super.openFile(filename, simpleClassName);
        }

    }

}
