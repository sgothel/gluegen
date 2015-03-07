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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
 * @author Michael Bien, et al.
 */
@SupportedAnnotationTypes(value = {"com.jogamp.gluegen.structgen.CStruct", "com.jogamp.gluegen.structgen.CStructs"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class CStructAnnotationProcessor extends AbstractProcessor {
    private static final String DEFAULT = "_default_";
    static final boolean DEBUG;

    static {
        Debug.initSingleton();
        DEBUG = PropertyAccess.isPropertyDefined("jogamp.gluegen.structgen.debug", true);
    }

    private static final String STRUCTGENOUTPUT_OPTION = "structgen.output";
    private static final String STRUCTGENOUTPUT = PropertyAccess.getProperty("jogamp.gluegen."+STRUCTGENOUTPUT_OPTION, true, "gensrc");

    private Filer filer;
    private Messager messager;
    private Elements eltUtils;
    private String outputPath;

    private final static Set<String> generatedStructs = new HashSet<String>();


    @Override
    public void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        eltUtils = processingEnv.getElementUtils();

        outputPath = processingEnv.getOptions().get(STRUCTGENOUTPUT_OPTION);
        outputPath = outputPath == null ? STRUCTGENOUTPUT : outputPath;
    }

    private File locateSource(final String packageName, final String relativeName) {
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
        } catch (final IOException e) {
            if(DEBUG) {
                System.err.println("Caught "+e.getClass().getSimpleName()+": "+e.getMessage()); /* e.printStackTrace(); */
            }
        }
        return null;
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment env) {
        final String user_dir = System.getProperty("user.dir");

        final Set<? extends Element> cStructsElements = env.getElementsAnnotatedWith(CStructs.class);
        for (final Element structsElement : cStructsElements) {
            final String packageName = eltUtils.getPackageOf(structsElement).toString();
            final CStructs cstructs = structsElement.getAnnotation(CStructs.class);
            if( null != cstructs ) {
                final CStruct[] cstructArray = cstructs.value();
                for(final CStruct cstruct : cstructArray) {
                    processCStruct(cstruct, structsElement, packageName, user_dir);
                }
            }
        }

        final Set<? extends Element> cStructElements = env.getElementsAnnotatedWith(CStruct.class);
        for (final Element structElement : cStructElements) {
            final String packageName = eltUtils.getPackageOf(structElement).toString();
            final CStruct cstruct = structElement.getAnnotation(CStruct.class);
            if( null != cstruct ) {
                processCStruct(cstruct, structElement, packageName, user_dir);
            }
        }
        return true;
    }

    private void processCStruct(final CStruct struct, final Element element, final String packageName, final String user_dir) {
        try {
            final String headerRelPath = struct.header();
            final Element enclElement = element.getEnclosingElement();
            final boolean isPackageOrType = null == enclElement;

            System.err.println("CStruct: "+struct+", package "+packageName+", header "+headerRelPath);
            if(DEBUG) {
                System.err.println("CStruct.0: user.dir: "+user_dir);
                System.err.println("CStruct.0: element: "+element+", .simpleName "+element.getSimpleName());
                System.err.print("CStruct.0: isPackageOrType "+isPackageOrType+", enclElement: "+enclElement);
                if( !isPackageOrType ) {
                    System.err.println(", .simpleName "+enclElement.getSimpleName()+", .package "+eltUtils.getPackageOf(enclElement).toString());
                } else {
                    System.err.println("");
                }
            }
            if( isPackageOrType && struct.name().equals(DEFAULT) ) {
                throw new IllegalArgumentException("CStruct annotation on package or type must have name specified: "+struct+" @ "+element);
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

            final String rootOut, headerParent;
            {
                final String root0 = headerFile.getAbsolutePath();
                headerParent = root0.substring(0, root0.length()-headerFile.getName().length()-1);
                rootOut = headerParent.substring(0, headerParent.length()-packageName.length()) + "..";
            }
            System.err.println("CStruct: "+headerFile+", abs: "+headerFile.isAbsolute()+", headerParent "+headerParent+", rootOut "+rootOut);

            generateStructBinding(element, struct, isPackageOrType, rootOut, packageName, headerFile, headerParent);
        } catch (final IOException ex) {
            throw new RuntimeException("IOException while processing!", ex);
        }
    }

    private void generateStructBinding(final Element element, final CStruct struct, final boolean isPackageOrType, final String rootOut, final String pakage, final File header, final String headerParent) throws IOException {
        final String declaredType = element.asType().toString();
        final boolean useStructName = !struct.name().equals(DEFAULT);
        final String structName = useStructName ? struct.name() : declaredType;
        final boolean useJavaName = !struct.jname().equals(DEFAULT);

        final String finalType = useJavaName ? struct.jname() : ( !isPackageOrType ? declaredType : structName );
        System.err.println("CStruct: Generating struct accessor for struct: "+structName+" -> "+finalType+" [struct.name "+struct.name()+", struct.jname "+struct.jname()+", declaredType "+declaredType+"]");
        if( generatedStructs.contains(finalType) ) {
            messager.printMessage(Kind.NOTE, "struct "+structName+" already defined elsewhere, skipping.", element);
            return;
        }

        final boolean outputDirAbs;
        {
            final File outputDirFile = new File(outputPath);
            outputDirAbs = outputDirFile.isAbsolute();
        }
        final String outputPath1 = outputDirAbs ? outputPath : rootOut + File.separator + outputPath;
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
            if( finalType != structName ) {
                // We allow renaming the structType to the element's declaredType (FIELD annotation only)
                writer.write("RenameJavaType " + struct.name()+" " + declaredType +"\n");
            }
        } finally {
            if( null != writer ) {
                writer.close();
            }
        }
        final List<String> cfgFiles = new ArrayList<String>();
        cfgFiles.add(config);
        final List<String> includePaths = new ArrayList<String>();
        includePaths.add(headerParent);
        includePaths.add(outputPath1);
        final Reader reader;
        final String filename = header.getPath();
        try {
            reader = new BufferedReader(new FileReader(filename));
        } catch (final FileNotFoundException ex) {
            throw new RuntimeException("input file not found", ex);
        }
        if( DEBUG  ) {
            GlueGen.setDebug(true);
        }
        new GlueGen().run(reader, filename, AnnotationProcessorJavaStructEmitter.class,
                          includePaths, cfgFiles, outputPath1, false /* copyCPPOutput2Stderr */);

        configFile.delete();
        generatedStructs.add(finalType);
    }

    public static class AnnotationProcessorJavaStructEmitter extends JavaEmitter {

        @Override
        protected PrintWriter openFile(final String filename, final String simpleClassName) throws IOException {

            if( generatedStructs.contains(simpleClassName) ) {
                System.err.println("skipping -> " + simpleClassName);
                return null;
            }

            // look for recursive generated structs... keep it DRY
            if( !simpleClassName.endsWith("32") &&
                !simpleClassName.endsWith("64") ) {
                System.err.println("generating -> " + simpleClassName);
                generatedStructs.add(simpleClassName);
            }
            return super.openFile(filename, simpleClassName);
        }

    }

}
