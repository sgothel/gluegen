/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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
 * Neither the name of Sun Microsystems, Inc. or the names of
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
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 *
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */
package com.jogamp.gluegen;

import com.jogamp.common.GlueGenVersion;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

import antlr.*;

import com.jogamp.gluegen.cgram.*;
import com.jogamp.gluegen.cgram.types.*;
import com.jogamp.gluegen.jcpp.JCPP;

import static java.lang.System.*;

/**
 * Glue code generator for C functions and data structures.<br>
 */
public class GlueGen implements GlueEmitterControls {

    static{
        Logging.init();
    }

    private final List<String> forcedStructNames = new ArrayList<String>();
    private GenericCPP preprocessor;

    // State for SymbolFilters
    private List<ConstantDefinition> allConstants;
    private List<FunctionSymbol> allFunctions;

    private static boolean debug = false;

    private static Level logLevel = null;

    public static void setDebug(final boolean v) { debug=v; }
    public static void setLogLevel(final Level l) { logLevel=l; }
    public static boolean debug() { return debug; }

    @Override
    public void forceStructEmission(final String typedefName) {
        forcedStructNames.add(typedefName);
    }

    @Override
    public String findHeaderFile(final String headerFileName) {
        return preprocessor.findFile(headerFileName);
    }

    @Override
    public void runSymbolFilter(final SymbolFilter filter) {
        filter.filterSymbols(allConstants, allFunctions);
        final List<ConstantDefinition> newConstants = filter.getConstants();
        final List<FunctionSymbol> newFunctions = filter.getFunctions();
        if (newConstants != null) {
            allConstants = newConstants;
        }
        if (newFunctions != null) {
            allFunctions = newFunctions;
        }
    }

    /** GlueGen's build in macro name {@value}, when compiling w/ GlueGen. */
    public static final String __GLUEGEN__ = "__GLUEGEN__";

    @SuppressWarnings("unchecked")
    public void run(final Reader reader, final String filename, final Class<?> emitterClass, final List<String> includePaths, final List<String> cfgFiles, final String outputRootDir, final boolean copyCPPOutput2Stderr) {

        try {
            if(debug) {
                Logging.getLogger().setLevel(Level.ALL);
            } else if( null != logLevel ) {
                Logging.getLogger().setLevel(logLevel);
            }
            final GlueEmitter emit;
            if (emitterClass == null) {
                emit = new JavaEmitter();
            } else {
                try {
                    emit = (GlueEmitter) emitterClass.newInstance();
                } catch (final Exception e) {
                    throw new RuntimeException("Exception occurred while instantiating emitter class.", e);
                }
            }

            for (final String config : cfgFiles) {
                emit.readConfigurationFile(config);
            }
            final JavaConfiguration cfg = emit.getConfiguration();

            final File out = File.createTempFile("CPPTemp", ".cpp");
            final FileOutputStream outStream = new FileOutputStream(out);

            // preprocessor = new PCPP(includePaths, debug, copyCPPOutput2Stderr);
            preprocessor = new JCPP(includePaths, debug, copyCPPOutput2Stderr);
            final String cppName = preprocessor.getClass().getSimpleName();
            if(debug) {
                System.err.println("CPP <"+cppName+"> output at (persistent): " + out.getAbsolutePath());
            } else {
                out.deleteOnExit();
            }

            preprocessor.addDefine(__GLUEGEN__, "2");
            preprocessor.setOut(outStream);

            preprocessor.run(reader, filename);
            outStream.flush();
            outStream.close();
            if(debug) {
                System.err.println("CPP <"+cppName+"> done");
            }

            final FileInputStream inStream = new FileInputStream(out);
            final DataInputStream dis = new DataInputStream(inStream);

            final GnuCLexer lexer = new GnuCLexer(dis);
            lexer.setTokenObjectClass(CToken.class.getName());
            lexer.initialize();
            // Parse the input expression.
            final GnuCParser parser = new GnuCParser(lexer);

            // set AST node type to TNode or get nasty cast class errors
            parser.setASTNodeClass(TNode.class.getName());
            TNode.setTokenVocabulary(GNUCTokenTypes.class.getName());

            // invoke parser
            try {
                parser.translationUnit();
            } catch (final RecognitionException e) {
                throw new RuntimeException("Fatal IO error", e);
            } catch (final TokenStreamException e) {
                throw new RuntimeException("Fatal IO error", e);
            }

            final HeaderParser headerParser = new HeaderParser();
            headerParser.setDebug(debug);
            headerParser.setJavaConfiguration(cfg);
            final TypeDictionary td = new TypeDictionary();
            headerParser.setTypedefDictionary(td);
            final TypeDictionary sd = new TypeDictionary();
            headerParser.setStructDictionary(sd);
            // set AST node type to TNode or get nasty cast class errors
            headerParser.setASTNodeClass(TNode.class.getName());
            // walk that tree
            headerParser.translationUnit(parser.getAST());
            dis.close();
            inStream.close();

            /**
            // For debugging: Dump type dictionary and struct dictionary to System.err
            if(debug) {
                td.dumpDictionary(err, "All Types");
                sd.dumpDictionary(err, "All Structs");
            } */

            // At this point we have all of the pieces we need in order to
            // generate glue code: the #defines to constants, the set of
            // typedefs, and the set of functions.

            if (null != outputRootDir && outputRootDir.trim().length() > 0) {
                if (emit instanceof JavaEmitter) {
                    // FIXME: hack to interfere with the *Configuration setting via commandlines
                    final JavaEmitter jemit = (JavaEmitter) emit;
                    if (null != jemit.getConfig()) {
                        jemit.getConfig().setOutputRootDir(outputRootDir);
                    }
                }
            }

            // Repackage the enum and #define statements from the parser into a common format
            // so that SymbolFilters can operate upon both identically
            allConstants = new ArrayList<ConstantDefinition>();
            for (final EnumType enumeration : headerParser.getEnums()) {
                String enumName = enumeration.getName();
                if (enumName.equals("<anonymous>")) {
                    enumName = null;
                }
                // iterate over all values in the enumeration
                for (int i = 0; i < enumeration.getNumEnumerates(); ++i) {
                    final EnumType.Enumerator enumerate = enumeration.getEnum(i);
                    final ConstantDefinition def =
                            new ConstantDefinition(enumerate.getName(), enumerate.getExpr(),
                                                   enumerate.getNumber(),
                                                   enumName, enumeration.getASTLocusTag());
                    allConstants.add(def);
                }
            }
            for (final Object elem : lexer.getDefines()) {
                final Define def = (Define) elem;
                allConstants.add(new ConstantDefinition(def.getName(), def.getValue(), null, def.getASTLocusTag()));
            }
            allConstants.addAll(preprocessor.getConstantDefinitions());

            allFunctions = headerParser.getParsedFunctions();

            // begin emission of glue code,
            // incl. firing up 'runSymbolFilter(SymbolFilter)' calls, which:
            //    - filters all ConstantDefinition
            //    - filters all FunctionSymbol
            emit.beginEmission(this);

            if( debug() ) {
                int i=0;
                System.err.println("Filtered Constants: "+allConstants.size());
                for (final ConstantDefinition def : allConstants) {
                    if( debug() ) {
                        System.err.println("Filtered ["+i+"]: "+def.getAliasedString());
                        i++;
                    }
                }
                i=0;
                System.err.println("Filtered Functions: "+allFunctions.size());
                for (final FunctionSymbol cFunc : allFunctions) {
                    System.err.println("Filtered ["+i+"]: "+cFunc.getAliasedString());
                    i++;
                }
            }

            if ( !cfg.structsOnly() ) {
                emit.beginDefines();
                final Set<String> emittedDefines = new HashSet<String>(100);
                // emit java equivalent of enum { ... } statements
                final StringBuilder comment = new StringBuilder();
                for (final ConstantDefinition def : allConstants) {
                    if (!emittedDefines.contains(def.getName())) {
                        emittedDefines.add(def.getName());
                        final Set<String> aliases = cfg.getAliasedDocNames(def);
                        if (aliases != null && aliases.size() > 0 ) {
                            int i=0;
                            comment.append("Alias for: <code>");
                            for (final String alias : aliases) {
                                if(0 < i) {
                                    comment.append("</code>, <code>");
                                }
                                comment.append(alias);
                                i++;
                            }
                            comment.append("</code>");
                        }
                        if (def.getEnumName() != null) {
                            if (comment.length() > 0)
                                comment.append("<br>\n");

                            comment.append("Defined as part of enum type \"");
                            comment.append(def.getEnumName());
                            comment.append("\"");
                        }
                        if (comment.length() > 0) {
                            emit.emitDefine(def, comment.toString());
                            comment.setLength(0);
                        }
                        else {
                            emit.emitDefine(def, null);
                        }
                    }
                }
                emit.endDefines();
            }

            // Iterate through the functions finding structs that are referenced in
            // the function signatures; these will be remembered for later emission
            final ReferencedStructs referencedStructs = new ReferencedStructs();
            for (final FunctionSymbol sym : allFunctions) {
                // FIXME: this doesn't take into account the possibility that some of
                // the functions we send to emitMethodBindings() might not actually be
                // emitted (e.g., if an Ignore directive in the JavaEmitter causes it to be skipped).
                sym.getType().visit(referencedStructs);
            }

            // Normally only referenced types will be emitted. The user can force a
            // type to be emitted via a .cfg file directive. Those directives are
            // processed here.
            for (final String name : forcedStructNames) {
                final Type type = td.get(name);
                if (type == null) {
                    err.println("WARNING: during forced struct emission: struct \"" + name + "\" not found");
                } else if (!type.isCompound()) {
                    err.println("WARNING: during forced struct emission: type \"" + name + "\" was not a struct");
                } else {
                    type.visit(referencedStructs);
                }
            }

            // Lay out structs
            emit.beginStructLayout();
            for (final Iterator<CompoundType> iter = referencedStructs.layouts(); iter.hasNext();) {
                final CompoundType c = iter.next();
                if( !c.isLayouted() ) {
                    emit.layoutStruct(c);
                }
            }
            emit.endStructLayout();

            // Emit structs
            emit.beginStructs(td, sd, headerParser.getCanonMap());
            for (final Iterator<Type> iter = referencedStructs.results(); iter.hasNext();) {
                final Type t = iter.next();
                if (t.isCompound()) {
                    assert t.isTypedef() && t.getName() == null : "ReferencedStructs incorrectly recorded compound type " + t;
                    emit.emitStruct(t.asCompound(), null);
                } else if (t.isPointer()) {
                    final PointerType p = t.asPointer();
                    final CompoundType c = p.getTargetType().asCompound();
                    assert p.isTypedef() && c.getName() == null : "ReferencedStructs incorrectly recorded pointer type " + p;
                    emit.emitStruct(c, p);
                }
            }
            emit.endStructs();

            if ( !cfg.structsOnly() ) {
                // emit java and C code to interface with the native functions
                emit.beginFunctions(td, sd, headerParser.getCanonMap());
                emit.emitFunctions(allFunctions);
                emit.endFunctions();
            }

            // end emission of glue code
            emit.endEmission();

        } catch (final Exception e) {
            throw new RuntimeException("Exception occurred while generating glue code.", e);
        }
    }

    public static void main(final String... args) {

        if (args.length == 0) {
            System.err.println(GlueGenVersion.getInstance());
            usage();
        }

        Reader reader = null;
        String filename = null;
        String emitterFQN = null;
        String outputRootDir = null;
        final List<String> cfgFiles = new ArrayList<String>();
        boolean copyCPPOutput2Stderr = false;

        final List<String> includePaths = new ArrayList<String>();
        for (int i = 0; i < args.length; i++) {
            if (i < args.length - 1) {
                final String arg = args[i];
                if (arg.startsWith("-I")) {
                    final String[] paths = arg.substring(2).split(getProperty("path.separator"));
                    includePaths.addAll(Arrays.asList(paths));
                } else if (arg.startsWith("-O")) {
                    outputRootDir = arg.substring(2);
                } else if (arg.startsWith("-E")) {
                    emitterFQN = arg.substring(2);
                } else if (arg.startsWith("-C")) {
                    cfgFiles.add(arg.substring(2));
                } else if (arg.equals("--logLevel")) {
                    i++;
                    logLevel = Level.parse(args[i]);
                } else if (arg.equals("--debug")) {
                    debug=true;
                } else if (arg.equals("--dumpCPP")) {
                    copyCPPOutput2Stderr=true;
                } else {
                    usage();
                }
            } else {
                final String arg = args[i];
                if (arg.equals("-")) {
                    reader = new InputStreamReader(in);
                    filename = "standard input";
                } else {
                    if (arg.startsWith("-")) {
                        usage();
                    }
                    filename = arg;
                    try {
                        reader = new BufferedReader(new FileReader(filename));
                    } catch (final FileNotFoundException ex) {
                        throw new RuntimeException("input file not found", ex);
                    }
                }
            }
        }

        try {
            final Class<?> emitterClass = emitterFQN == null ? null : Class.forName(emitterFQN);
            new GlueGen().run(reader, filename, emitterClass, includePaths, cfgFiles, outputRootDir, copyCPPOutput2Stderr);
        } catch (final ClassNotFoundException ex) {
            throw new RuntimeException("specified emitter class was not in the classpath", ex);
        }

    }

    //----------------------------------------------------------------------
    // Internals only below this point
    //
    private static void usage() {
        out.println("Usage: java GlueGen [-I...] [-Eemitter_class_name] [-Ccfg_file_name...] <filename | ->");
        out.println();
        out.println("Runs C header parser on input file or standard input, first");
        out.println("passing input through minimal pseudo-C-preprocessor. Use -I");
        out.println("command-line arguments to specify the search path for #includes.");
        out.println("Emitter class name can be specified with -E option: i.e.,");
        out.println("-Ecom.jogamp.gluegen.JavaEmitter (the default). Use");
        out.println("-Ecom.jogamp.gluegen.DebugEmitter to print recognized entities");
        out.println("(#define directives to constant numbers, typedefs, and function");
        out.println("declarations) to standard output. Emitter-specific configuration");
        out.println("file or files can be specified with -C option; e.g,");
        out.println("-Cjava-emitter.cfg.");
        out.println("  --debug enables debug mode");
        out.println("  --dumpCPP directs CPP to dump all output to stderr as well");
        exit(1);
    }
}
