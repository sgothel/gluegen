/**
 * Copyright 2023 JogAmp Community. All rights reserved.
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

import com.jogamp.common.os.AndroidVersion;
import com.jogamp.gluegen.ConstantDefinition;
import com.jogamp.gluegen.JavaConfiguration;
import com.jogamp.gluegen.cgram.CToken;
import com.jogamp.gluegen.cgram.Define;
import com.jogamp.gluegen.cgram.GNUCTokenTypes;
import com.jogamp.gluegen.cgram.GnuCLexer;
import com.jogamp.gluegen.cgram.GnuCParser;
import com.jogamp.gluegen.cgram.HeaderParser;
import com.jogamp.gluegen.cgram.TNode;
import com.jogamp.gluegen.cgram.types.EnumType;
import com.jogamp.gluegen.cgram.types.TypeDictionary;
import com.jogamp.gluegen.jcpp.JCPP;
import com.jogamp.gluegen.jcpp.LexerException;
import com.jogamp.gluegen.jcpp.Macro;
import com.jogamp.junit.util.SingletonJunitCase;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.TokenStreamRecognitionException;
import junit.framework.Assert;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * serves mainly as entry point for debugging purposes.
 * @author Sven Gothel, Michael Bien
 */
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestCParser extends SingletonJunitCase {

    static final String sourcePath = BuildEnvironment.gluegenRoot + "/src/junit/com/jogamp/gluegen/test/junit/generation/";
    static final boolean debug = false;

    @BeforeClass
    public static void init() {
        if(AndroidVersion.isAvailable) {
            // JCPP is n/a on Android - GlueGen Runtime only
            setTestSupported(false);
        }
    }

    @Test
    public void test01_cpp_cc() {
        if( null != test10CCFileName ) {
            return;
        }
        Exception ex = null;
        try {
            final String cppResultPath = cpp("cpptest_1", ".h", debug);
            cc(cppResultPath, debug);
        } catch (RecognitionException | IOException | LexerException e) {
            e.printStackTrace();
            ex = e;
        }
        assertNull(ex);
    }

    // @Test
    public void test10_cc() {
        Exception ex = null;
        try {
            if( null != test10CCFileName ) {
                cc(test10CCFileName, debug);
            } else {
                cc(sourcePath + "cpptest_10.hpp", debug);
            }
        } catch (RecognitionException | IOException | LexerException e) {
            e.printStackTrace();
            ex = e;
        }
        assertNull(ex);
    }

    public String cpp(final String cSourceBasename, final String cSourceSuffix, final boolean debug) throws FileNotFoundException, IOException, LexerException, RecognitionException {
        final String cSourcePath = sourcePath + cSourceBasename + cSourceSuffix;
        final FileReader cSourceReader = new FileReader(cSourcePath);

        final String cppResultPath = BuildEnvironment.testOutput + "/" + cSourceBasename + ".hpp";
        final File cppResultFile = new File( cppResultPath );
        if( cppResultFile.exists() ) {
            cppResultFile.delete();
        }

        System.err.println("XXX JCPP: "+cSourcePath);
        System.err.println("XXX cpp result-file "+cppResultFile);
        try( final FileOutputStream cppResultOStream = new FileOutputStream(cppResultFile) ) {
            final JCPP pp = new JCPP(Collections.<String>singletonList(sourcePath), debug, false, true /* default */);
            pp.addDefine("__GLUEGEN__", "2");
            pp.setOut(cppResultOStream);
            pp.run(new BufferedReader(cSourceReader), cSourceBasename);
            cppResultOStream.flush();
            cppResultOStream.close();
            {
                int macroCount = 0;
                for (final Macro cdef : pp.cpp.getMacros(true)) {
                    System.err.println("XXX cpp Macr "+macroCount+" <"+cdef+">, isFunc "+
                            cdef.isFunctionLike()+", isConstExpr "+
                            ConstantDefinition.isConstantExpression(cdef.getText()));
                    ++macroCount;
                }
            }
            {
                int defCount = 0;
                for (final ConstantDefinition cdef : pp.getConstantDefinitions()) {
                    System.err.println("XXX cpp Defn "+defCount+" <"+cdef+">");
                    ++defCount;
                }
            }
        }

        return cppResultPath;
    }

    public void cc(final String cppResultPath, final boolean debug) throws FileNotFoundException, IOException, LexerException, RecognitionException {
        final File cppResultFile = new File( cppResultPath );
        System.err.println("XXX C Parser: "+cppResultPath);
        try( final FileInputStream inStream = new FileInputStream(cppResultFile) ) {
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
            parser.setDebug(debug);
            try {
                parser.translationUnit();
            } catch (final RecognitionException e) {
                throw new RuntimeException(String.format(
                        "Fatal error during translation (Localisation : %s:%s:%s)",
                        e.getFilename(), e.getLine(), e.getColumn()
                ), e);
            } catch (final TokenStreamRecognitionException e) {
                throw new RuntimeException(String.format(
                        "Fatal error during translation (Localisation : %s:%s:%s)",
                        e.recog.getFilename(), e.recog.getLine(), e.recog.getColumn()
                ), e);
            } catch (final TokenStreamException e) {
                throw new RuntimeException("Fatal IO error", e);
            }

            System.err.println("XXX C Header Tree Parser ...");
            final JavaConfiguration cfg = new JavaConfiguration();
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

            {
                int enumCount = 0;
                for (final EnumType enumeration : headerParser.getEnums()) {
                    String enumName = enumeration.getName();
                    if (enumName.equals("<anonymous>")) {
                        enumName = null;
                    }
                    // iterate over all values in the enumeration
                    for (int i = 0; i < enumeration.getNumEnumerates(); ++i) {
                        final EnumType.Enumerator enumerate = enumeration.getEnum(i);
                        final ConstantDefinition cdef =
                            new ConstantDefinition(enumerate.getName(), enumerate.getExpr(),
                                                   enumerate.getNumber(),
                                                   enumName, enumeration.getASTLocusTag());
                        System.err.println("XXX cc_ Enum "+enumCount+":"+i+" <"+cdef+">");
                    }
                    ++enumCount;
                }
            }
            {
                int defCount = 0;
                for (final Object elem : lexer.getDefines()) {
                    final Define def = (Define) elem;
                    final ConstantDefinition cdef =
                            new ConstantDefinition(def.getName(), def.getValue(), null, def.getASTLocusTag());
                    System.err.println("XXX cc_ Defn "+defCount+" <"+cdef+">");
                    ++defCount;
                }
            }
        }
    }

    static String test10CCFileName = null;
    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; ++i ) {
            if( "-in".equals(args[i]) ) {
                test10CCFileName = args[++i];
            }
        }
        final String tstname = TestCParser.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
