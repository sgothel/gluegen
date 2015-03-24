/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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

package com.jogamp.gluegen.pcpp;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.jogamp.gluegen.ASTLocusTag;
import com.jogamp.gluegen.ConstantDefinition;
import com.jogamp.gluegen.GenericCPP;
import com.jogamp.gluegen.GlueGenException;
import com.jogamp.gluegen.Logging;
import com.jogamp.gluegen.Logging.LoggerIf;

import static java.util.logging.Level.*;

/** A minimal pseudo-C-preprocessor designed in particular to preserve
    #define statements defining constants so they can be observed by a
    glue code generator. */

public class PCPP implements GenericCPP {

    private final LoggerIf LOG;

    /** Map containing the results of #define statements. We must
        evaluate certain very simple definitions (to properly handle
        OpenGL's gl.h) but preserve the text of definitions evaluating
        to constants.  Macros and multi-line defines (which typically
        contain either macro definitions or expressions) are currently
        not handled. */
    private final Map<String, String> defineMap          = new HashMap<String, String>(128);
    private final Map<String, Macro>  macroMap           = new HashMap<String, Macro>(128);
    private final Set<String>         nonConstantDefines = new HashSet<String>(128);

    /** List containing the #include paths as Strings */
    private final List<String> includePaths;

    private ParseState  state;

    private final boolean enableDebugPrint;
    private final boolean enableCopyOutput2Stderr;

    public PCPP(final List<String> includePaths, final boolean debug, final boolean copyOutput2Stderr) {
        LOG = Logging.getLogger(PCPP.class.getPackage().getName(), PCPP.class.getSimpleName());
        this.includePaths = includePaths;
        setOut(System.out);
        enableDebugPrint = debug;
        enableCopyOutput2Stderr = copyOutput2Stderr;
    }

    @Override
    public void run(final Reader reader, final String filename) throws GlueGenException {
        StreamTokenizer tok = null;
        BufferedReader bufReader = null;
        if (reader instanceof BufferedReader) {
            bufReader = (BufferedReader) reader;
        } else {
            bufReader = new BufferedReader(reader);
        }

        tok = new StreamTokenizer(new ConcatenatingReader(bufReader));
        initTokenizer(tok);

        final ParseState curState = new ParseState(tok, filename);
        final ParseState oldState = state;
        state = curState;
        lineDirective();
        try {
            parse();
        } catch (final Exception e) {
            final StringBuilder buf = new StringBuilder("Preprocessor failed");
            LOG.log(Level.SEVERE, buf.toString(), e);
            if( e instanceof GlueGenException ) {
                throw (GlueGenException)e;
            } else {
                throw new GlueGenException("Preprocessor failed",
                                           new ASTLocusTag(filename(), lineNumber(), -1, null), e);
            }
        }
        state = oldState;
        if (state != null) {
            lineDirective();
        }
    }

    @Override
    public List<ConstantDefinition> getConstantDefinitions() throws GlueGenException {
        return new ArrayList<ConstantDefinition>(); // NOP
    }

    private void initTokenizer(final StreamTokenizer tok) {
        tok.resetSyntax();
        tok.wordChars('a', 'z');
        tok.wordChars('A', 'Z');
        tok.wordChars('0', '9');
        tok.wordChars('_', '_');
        tok.wordChars('-', '.');
        tok.wordChars(128, 255);
        tok.whitespaceChars(0, ' ');
        tok.quoteChar('"');
        tok.quoteChar('\'');
        tok.eolIsSignificant(true);
        tok.slashSlashComments(true);
        tok.slashStarComments(true);
    }

    @Override
    public String findFile(final String filename) {
        final String sep = File.separator;
        for (final String inclPath : includePaths) {
            final String fullPath = inclPath + sep + filename;
            final File file = new File(fullPath);
            if (file.exists()) {
                return fullPath;
            }
        }
        return null;
    }

    @Override
    public OutputStream out() {
        return out;
    }

    @Override
    public void setOut(final OutputStream out) {
        this.out = out;
        writer = new PrintWriter(out);
    }

    // State
    static class ParseState {

        private final StreamTokenizer tok;
        private final String filename;
        private boolean startOfLine;
        private boolean startOfFile;

        ParseState(final StreamTokenizer tok, final String filename) {
            this.tok = tok;
            this.filename = filename;
            startOfLine = true;
            startOfFile = true;
        }

        void pushBackToken() throws IOException {
            tok.pushBack();
        }

        int curToken() {
            return tok.ttype;
        }

        int nextToken() throws IOException {
            return tok.nextToken();
        }

        String curWord() {
            return tok.sval;
        }

        String filename() {
            return filename;
        }

        int lineNumber() {
            return tok.lineno();
        }

        boolean startOfLine() {
            return startOfLine;
        }

        void setStartOfLine(final boolean val) {
            startOfLine = val;
        }

        boolean startOfFile() {
            return startOfFile;
        }

        void setStartOfFile(final boolean val) {
            startOfFile = val;
        }

    }

    private static class Macro {

        private final List<String> values;
        private final List<String> params;

        Macro(final List<String> params, final List<String> values) {
            this.values = values;
            this.params = params;
        }

        @Override
        public String toString() {
            return "params: "+params+" values: "+values;
        }

    }

    // Accessors

    /** Equivalent to nextToken(false) */
    private int nextToken() throws IOException {
        return nextToken(false);
    }

    private int nextToken(final boolean returnEOLs) throws IOException {
        final int lineno = lineNumber();
        // Check to see whether the previous call to nextToken() left an
        // EOL on the stream
        if (state.curToken() == StreamTokenizer.TT_EOL) {
            state.setStartOfLine(true);
        } else if (!state.startOfFile()) {
            state.setStartOfLine(false);
        }
        state.setStartOfFile(false);
        int val = state.nextToken();
        if (!returnEOLs) {
            if (val == StreamTokenizer.TT_EOL) {
                do {
                    // Consume and return next token, setting state appropriately
                    val = state.nextToken();
                    state.setStartOfLine(true);
                    println();
                } while (val == StreamTokenizer.TT_EOL);
            }
        }
        if (lineNumber() > lineno + 1) {
            // This is a little noisier than it needs to be, but does handle
            // the case of multi-line comments properly
            lineDirective();
        }
        return val;
    }

    /**
     * Reads the next token and throws an IOException if it is not the specified
     * token character.
     */
    private void nextRequiredToken(final int requiredToken) throws IOException {
        final int nextTok = nextToken();
        if (nextTok != requiredToken) {
            String msg = "Expected token '" + requiredToken + "' but got ";
            switch (nextTok) {
                case StreamTokenizer.TT_EOF: msg += "<EOF>"; break;
                case StreamTokenizer.TT_EOL: msg += "<EOL>"; break;
                default: msg += "'" + curTokenAsString() + "'"; break;
            }
            msg += " at file " + filename() + ", line " + lineNumber();
            throw new IOException(msg);
        }
    }


    private String curTokenAsString() {
        final int t = state.curToken();
        if (t == StreamTokenizer.TT_WORD) {
            return state.curWord();
        }
        if (t == StreamTokenizer.TT_EOL) {
            throw new RuntimeException("Should not be converting EOL characters to strings at file " + filename() + ", line " + lineNumber());
        }
        final char c = (char) t;
        if (c == '"' || c == '\'') {
            final StringBuilder sb = new StringBuilder();
            sb.append(c);
            sb.append(state.curWord());
            sb.append(c);
            return sb.toString();
        }
        return new String(new char[] { c });
    }

    private String nextWordOrString() throws IOException {
        nextToken();
        return curTokenAsString();
    }

    private String nextWord() throws IOException {
        final int val = nextToken();
        if (val != StreamTokenizer.TT_WORD) {
            throw new RuntimeException("Expected word at file " + filename() +
                                       ", line " + lineNumber());
        }
        return state.curWord();
    }

    private boolean startOfLine() {
        return state.startOfLine();
    }

    private String filename() {
        return (null != state) ? state.filename() : null;
    }

    private int lineNumber() {
        return (null != state) ? state.lineNumber() : -1;
    }

    /////////////
    // Parsing //
    /////////////

    private void parse() throws IOException {
        int tok = 0;
        while ((tok = nextToken()) != StreamTokenizer.TT_EOF) {
            // A '#' at the beginning of a line is a preprocessor directive
            if (startOfLine() && (tok == '#')) {
                preprocessorDirective();
            } else {
                // Output white space plus current token, handling #defines
                // (though not properly -- only handling #defines to constants and the empty string)

                // !!HACK!! - print space only for word tokens. This way multicharacter
                // operators such as ==, != etc. are property printed.
                if (tok == StreamTokenizer.TT_WORD) {
                    print(" ");
                }
                final String s = curTokenAsString();
                String newS = defineMap.get(s);
                if (newS == null) {
                    newS = s;
                }

                final Macro macro = macroMap.get(newS);
                if(macro != null) {
                    newS = "";
                    final List<String> args = new ArrayList<String>();
                    while (nextToken() != StreamTokenizer.TT_EOL) {
                        final String token = curTokenAsString();
                        if(")".equals(token)) {
                            break;
                        }else if(!",".equals(token) && !"(".equals(token)) {
                            args.add(token);
                        }
                    }

                    for (int i = 0; i < macro.values.size(); i++) {
                        String value = macro.values.get(i);

                        for (int j = 0; j < macro.params.size(); j++) {
                            final String param = macro.params.get(j);
                            if(param.equals(value)) {
                                value = args.get(j);
                                break;
                            }
                        }

                        if(ConstantDefinition.isIdentifier(value)) {
                            newS +=" ";
                        }

                        newS += value;

                    }

                }

                print(newS);
            }
        }
        flush();
    }

    private void preprocessorDirective() throws IOException {
        final String w = nextWord();
        boolean shouldPrint = true;
        if (w.equals("warning")) {
            handleWarning();
            shouldPrint = false;
        } else if (w.equals("error")) {
            handleError();
            shouldPrint = false;
        } else if (w.equals("define")) {
            handleDefine();
            shouldPrint = false;
        } else if (w.equals("undef")) {
            handleUndefine();
            shouldPrint = false;
        } else if (w.equals("if") || w.equals("elif")) {
            handleIf(w.equals("if"));
            shouldPrint = false;
        } else if (w.equals("ifdef") || w.equals("ifndef")) {
            handleIfdef(w.equals("ifdef"));
            shouldPrint = false;
        } else if (w.equals("else")) {
            handleElse();
            shouldPrint = false;
        } else if (w.equals("endif")) {
            handleEndif();
            shouldPrint = false;
        } else if (w.equals("include")) {
            handleInclude();
            shouldPrint = false;
        } else {
            int line = -1;
            try {
                // try '# <line> "<filename>"' case
                line = Integer.parseInt(w);
                final String filename = nextWordOrString();
                print("# " + line + " " + filename);
                println();
                shouldPrint = false;
            } catch (final NumberFormatException nfe) {
                // Unknown preprocessor directive (#pragma?) -- ignore
            }
        }
        if (shouldPrint) {
            print("# ");
            printToken();
        }
    }

    ////////////////////////////////////
    // Handling of #define directives //
    ////////////////////////////////////

    private void handleUndefine() throws IOException {
        // Next token is the name of the #undef
        final String name = nextWord();

        debugPrint(true, "UNDEF " + name);

        // there shouldn't be any extra symbols after the name, but just in case...
        final List<String> values = new ArrayList<String>();
        while (nextToken(true) != StreamTokenizer.TT_EOL) {
            values.add(curTokenAsString());
        }

        if (enabled()) {
            final String oldDef = defineMap.remove(name);
            if (oldDef == null) {
                LOG.log(WARNING, new ASTLocusTag(filename(), lineNumber(), -1, name),
                        "ignoring redundant \"#undef {0}\" - was not previously defined",
                        name);
            } else {
                // System.err.println("UNDEFINED: '" + name + "'  (line " + lineNumber() + " file " + filename() + ")");
            }
            nonConstantDefines.remove(name);
        } else {
            LOG.log(INFO, new ASTLocusTag(filename(), lineNumber(), -1, name),
                    "DISABLED UNDEFINE: ''{0}''", name);
        }
    }

    private void handleWarning() throws IOException {
        final String msg = nextWordOrString();
        if (enabled()) {
            LOG.log(WARNING, new ASTLocusTag(filename(), lineNumber(), -1, null), msg);
        }
    }

    private void handleError() throws IOException, GlueGenException {
        final String msg = nextWordOrString();
        if (enabled()) {
            throw new GlueGenException(msg, new ASTLocusTag(filename(), lineNumber(), -1, null));
        }
    }

    private void handleDefine() throws IOException {

        // (workaround for not having a lookahead)
        // macro functions have no space between identifier and '('
        // since whitespace is our delimiter we can't determine wether we are dealing with
        // macros or normal defines starting with a brace.
        // this will glue the brace to the token if there is no whitespace between both
        state.tok.wordChars('(', '(');

        // Next token is the name of the #define
        String name = nextWord();

        final boolean macroDefinition = name.contains("(");

        //System.err.println("IN HANDLE_DEFINE: '" + name + "'  (line " + lineNumber() + " file " + filename() + ")");
        // (Note that this is not actually proper handling for multi-line #defines)
        final List<String> values = new ArrayList<String>();

        if(macroDefinition) {
            final int index = name.indexOf('(');
            final String var = name.substring(index+1);
            name = name.substring(0, index);

            values.add("(");
            values.add(var);
        }

        // restore normal syntax
        state.tok.ordinaryChar('(');

        while (nextToken(true) != StreamTokenizer.TT_EOL) {
            values.add(curTokenAsString());
        }
        addDefine(name, macroDefinition, values);
    }

    @Override
    public void addDefine(final String name, final String value) {
        final List<String> values = new ArrayList<String>();
        values.add(value);
        addDefine(name, false, values);
    }

    private void addDefine(final String name, final boolean nameIsMacro, List<String> values) {
        // if we're not within an active block of code (like inside an "#ifdef
        // FOO" where FOO isn't defined), then don't actually alter the definition
        // map.
        debugPrint(true, "DEFINE " + name);
        if (enabled()) {
            boolean emitDefine = true;

            // Handle #definitions to nothing or to a constant value
            final int sz = values.size();
            if (sz == 0) {
                // definition to nothing, like "#define FOO"
                final String value = "";
                final String oldDef = defineMap.put(name, value);
                if (oldDef != null && !oldDef.equals(value)) {
                    LOG.log(WARNING, new ASTLocusTag(filename(), lineNumber(), -1, null),
                            "\"{0}\" redefined from \"{1}\" to \"\"", name, oldDef);
                }
                // We don't want to emit the define, because it would serve no purpose
                // and cause GlueGen errors (confuse the GnuCParser)
                emitDefine = false;
                //System.err.println("//---DEFINED: " + name + "to \"\"");
            } else if (sz == 1) {
                // See whether the value is a constant
                final String value = values.get(0);

                if (ConstantDefinition.isNumber(value)) {
                    // Value is numeric constant like "#define FOO 5".
                    // Put it in the #define map
                    final String oldDef = defineMap.put(name, value);
                    if (oldDef != null && !oldDef.equals(value)) {
                        LOG.log(WARNING, new ASTLocusTag(filename(), lineNumber(), -1, null),
                                "\"{0}\" redefined from \"{1}\" to \"{2}\"", name, oldDef, value);
                    }
                    debugPrint(true, "DEFINE " + name + " ["+oldDef+" ] -> "+value + " CONST");
                    //System.err.println("//---DEFINED: " + name + " to \"" + value + "\"");
                } else {
                    // Value is a symbolic constant like "#define FOO BAR".
                    // Try to look up the symbol's value
                    final String newValue = resolveDefine(value, true);
                    debugPrint(true, "DEFINE " + name + " -> "+value + " -> <" + newValue + "> SYMB");
                    if (newValue != null) {
                        // Set the value to the value of the symbol.
                        //
                        // TO DO: Is this correct? Why not output the symbol unchanged?
                        // I think that it's a good thing to see that some symbols are
                        // defined in terms of others. -chris
                        final boolean valueIsMacro  = newValue.contains("(");
                        if(valueIsMacro) {
                            // parser can't dig this currently
                            emitDefine = false;
                        } else {
                            values.set(0, newValue);
                        }
                    } else {
                        // Still perform textual replacement
                        defineMap.put(name, value);
                        nonConstantDefines.add(name);
                        emitDefine = false;
                    }
                }

            } else if (nameIsMacro) {
                // list parameters
                final List<String> params = new ArrayList<String>();
                for (int i = 1; i < values.size(); i++) {
                    final String v = values.get(i);
                    if(")".equals(v)) { // end of params
                        if(i != values.size()-1) {
                            values = values.subList(i+1, values.size());
                        }else{
                            values = Collections.emptyList();
                        }
                        break;
                    }else if(!",".equals(v)) {
                        params.add(v);
                    }
                }

                final Macro macro = new Macro(params, values);
                final Macro oldDef = macroMap.put(name, macro);
                if (oldDef != null) {
                    LOG.log(WARNING, new ASTLocusTag(filename(), lineNumber(), -1, null),
                            "\"{0}\" redefined from \"{1}\" to \"{2}\"", name, oldDef, macro);
                }
                emitDefine = false;

            }else{

                // find constant expressions like (1 << 3)
                // if found just pass them through, they will most likely work in java too
                // expressions containing identifiers are currently ignored (casts too)

                boolean containsIdentifier = false;
                for (final String value : values) {
                    if(ConstantDefinition.isIdentifier(value)) {
                        containsIdentifier = true;
                        break;
                    }
                }

                //TODO more work here e.g casts are currently not handled
                if(containsIdentifier) { //skip

                    // Non-constant define; try to do reasonable textual substitution anyway
                    // (FIXME: should identify some of these, like (-1), as constants)
                    emitDefine = false;
                    final StringBuilder val = new StringBuilder();
                    for (int i = 0; i < sz; i++) {
                        if (i != 0) {
                            val.append(" ");
                        }
                        val.append(resolveDefine(values.get(i), false));
                    }
                    if (defineMap.get(name) != null) {
                        // This is probably something the user should investigate.
                        throw new RuntimeException("Cannot redefine symbol \"" + name +
                                                   " from \"" + defineMap.get(name) + "\" to non-constant " +
                                                   " definition \"" + val.toString() + "\"" +
                                                   " at file \"" + filename() + ", line " + lineNumber() );
                    }
                    defineMap.put(name, val.toString());
                    nonConstantDefines.add(name);

                }else{ // constant expression -> pass through

                    final StringBuilder sb = new StringBuilder();
                    for (final String v : values) {
                        sb.append(v);
                    }
                    final String value = sb.toString();

                    final String oldDef = defineMap.put(name, value);
                    if (oldDef != null && !oldDef.equals(value)) {
                        LOG.log(WARNING, new ASTLocusTag(filename(), lineNumber(), -1, null),
                                "\"{0}\" redefined from \"{1}\" to \"{2}\"", name, oldDef, value);
                    }
                    debugPrint(true, "DEFINE " + name + " ["+oldDef+" ] -> "+value + " CONST");
//                    System.err.println("#define " + name +" "+value + " CONST EXPRESSION");
                }

            }

            if (emitDefine) {
                // Print name and value
                print("# define ");
                print(name);
                print(" ");
                for (final String v : values) {
                    print(v);
                }
                println();
            }

        } // end if (enabled())

        //System.err.println("OUT HANDLE_DEFINE: " + name);
    }

    private String resolveDefine(final String word, final boolean returnNullIfNotFound) {
        String lastWord = defineMap.get(word);
        if (lastWord == null) {
            if (returnNullIfNotFound) {
                return null;
            }
            return word;
        }
        String nextWord = null;
        do {
            nextWord = defineMap.get(lastWord);
            if (nextWord != null) {
                lastWord = nextWord;
            }
        } while (nextWord != null);
        return lastWord;
    }

    /**
     * Handling of #if/#ifdef/ifndef/endif directives
     *
     * condition      - the actual if-elif condition
     * whole-block    - the whole if-else-endif block
     * inside-block   - the inner block between if-elif-else-endif
     *
     * Outside        - reflects the state at entering the whole-block
     * Condition      - reflects the state of the condition
     * Inside         - reflects the state within the inside-block
     */

    /**
     * @param isIfdef if true, we're processing #ifdef; if false, we're
     * processing #ifndef.
     */
    private void handleIfdef(final boolean isIfdef) throws IOException {
        // Next token is the name of the #ifdef
        final String symbolName = nextWord();

        final boolean enabledOutside = enabled();
        final boolean symbolIsDefined = defineMap.get(symbolName) != null;

        debugPrint(false, (isIfdef ? "IFDEF " : "IFNDEF ") + symbolName + ", enabledOutside " + enabledOutside + ", isDefined " + symbolIsDefined + ", file \"" + filename() + " line " + lineNumber());

        final boolean enabledNow = enabled() && symbolIsDefined == isIfdef ;
        pushEnableBit( enabledNow ) ; // StateCondition
        pushEnableBit( enabledNow ) ; // StateInside
    }

    /** Handles #else directives */
    private void handleElse() throws IOException {
        popEnableBit(); // Inside
        final boolean enabledCondition = enabled();
        popEnableBit(); // Condition
        final boolean enabledOutside = enabled();

        debugPrint(false, "ELSE, enabledOutside " + enabledOutside + ", file \"" + filename() + " line " + lineNumber());
        pushEnableBit(enabledOutside && !enabledCondition); // Condition - don't care
        pushEnableBit(enabledOutside && !enabledCondition); // Inside
    }

    private void handleEndif() {
        popEnableBit(); // Inside
        popEnableBit(); // Condition
        final boolean enabledOutside = enabled();

        // print the endif if we were enabled prior to popEnableBit() (sending
        // false to debugPrint means "print regardless of current enabled() state).
        debugPrint(false, "ENDIF, enabledOutside " + enabledOutside);
    }

    /**
     * @param isIf if true, we're processing #if; if false, we're
     * processing #elif.
     */
    private void handleIf(final boolean isIf) throws IOException {
        boolean enabledCondition = false;
        boolean enabledOutside;

        if (!isIf) {
            popEnableBit(); // Inside
            enabledCondition = enabled();
            popEnableBit(); // Condition
        }
        enabledOutside = enabled();

        final boolean defineEvaluatedToTrue = handleIfRecursive(true);

        debugPrint(false, (isIf ? "IF" : "ELIF") + ", enabledOutside " + enabledOutside + ", eval " + defineEvaluatedToTrue + ", file \"" + filename() + " line " + lineNumber());

        boolean enabledNow;

        if(isIf) {
            enabledNow = enabledOutside && defineEvaluatedToTrue ;
            pushEnableBit( enabledNow ) ; // Condition
            pushEnableBit( enabledNow ) ; // Inside
        } else {
            enabledNow = enabledOutside && !enabledCondition && defineEvaluatedToTrue ;
            pushEnableBit( enabledCondition || enabledNow ) ; // Condition
            pushEnableBit( enabledNow ) ;                     // Inside
        }
    }

    //static int tmp = -1;

    /**
     * This method is called recursively to process nested sub-expressions such as:
     * <pre>
     *   #if !defined(OPENSTEP) && !(defined(NeXT) || !defined(NeXT_PDO))
     *</pre>
     *
     * @param greedy if true, continue evaluating sub-expressions until EOL is
     * reached. If false, return as soon as the first sub-expression is
     * processed.
     * @return the value of the sub-expression or (if greedy==true)
     * series of sub-expressions.
     */
    private boolean handleIfRecursive(final boolean greedy) throws IOException {
        //System.err.println("IN HANDLE_IF_RECURSIVE (" + ++tmp + ", greedy = " + greedy + ")"); System.err.flush();

        // ifValue keeps track of the current value of the potentially nested
        // "defined()" expressions as we process them.
        boolean ifValue = true;
        int openParens = 0;
        int tok;
        do {
            tok = nextToken(true);
            //System.err.println("-- READ: [" + (tok == StreamTokenizer.TT_EOL ? "<EOL>" :curTokenAsString()) + "]");
            switch (tok) {
                case '(':
                    ++openParens;
                    //System.err.println("OPEN PARENS = " + openParens);
                    ifValue = ifValue && handleIfRecursive(true);
                    break;
                case ')':
                    --openParens;
                    //System.err.println("OPEN PARENS = " + openParens);
                    break;
                case '!':
                    {
                        //System.err.println("HANDLE_IF_RECURSIVE HANDLING !");
                        final boolean rhs = handleIfRecursive(false);
                        ifValue = !rhs;
                        //System.err.println("HANDLE_IF_RECURSIVE HANDLED OUT !, RHS = " + rhs);
                    }
                    break;
                case '&':
                    {
                        nextRequiredToken('&');
                        //System.err.println("HANDLE_IF_RECURSIVE HANDLING &&, LHS = " + ifValue);
                        final boolean rhs = handleIfRecursive(true);
                        //System.err.println("HANDLE_IF_RECURSIVE HANDLED &&, RHS = " + rhs);
                        ifValue = ifValue && rhs;
                    }
                    break;
                case '|':
                    {
                        nextRequiredToken('|');
                        //System.err.println("HANDLE_IF_RECURSIVE HANDLING ||, LHS = " + ifValue);
                        final boolean rhs = handleIfRecursive(true);
                        //System.err.println("HANDLE_IF_RECURSIVE HANDLED ||, RHS = " + rhs);
                        ifValue = ifValue || rhs;
                    }
                    break;
                case '>':
                    {
                        // NOTE: we don't handle expressions like this properly
                        final boolean rhs = handleIfRecursive(true);
                        ifValue = false;
                    }
                    break;
                case '<':
                    {
                        // NOTE: we don't handle expressions like this properly
                        final boolean rhs = handleIfRecursive(true);
                        ifValue = false;
                    }
                    break;
                case '*':
                    {
                        // NOTE: we don't handle expressions like this properly
                        final boolean rhs = handleIfRecursive(false);
                        ifValue = false;
                    }
                    break;
                case '+':
                    {
                        // NOTE: we don't handle expressions like this properly
                        final boolean rhs = handleIfRecursive(false);
                        ifValue = false;
                    }
                    break;
                case '-':
                    {
                        // NOTE: we don't handle expressions like this properly
                        final boolean rhs = handleIfRecursive(false);
                        ifValue = false;
                    }
                    break;
                case '=':
                    {
                        // NOTE: we don't handle expressions like this properly
                        final boolean rhs = handleIfRecursive(true);
                        ifValue = false;
                    }
                    break;
                case StreamTokenizer.TT_WORD:
                    {
                        final String word = curTokenAsString();
                        if (word.equals("defined")) {
                            // Handle things like #if defined(SOMESYMBOL)
                            nextRequiredToken('(');
                            final String symbol = nextWord();
                            final boolean isDefined = defineMap.get(symbol) != null;
                            //System.err.println("HANDLE_IF_RECURSIVE HANDLING defined(" + symbol + ") = " + isDefined);
                            ifValue = ifValue && isDefined;
                            nextRequiredToken(')');
                        } else {
                            // Handle things like #if SOME_SYMBOL.
                            final String symbolValue = defineMap.get(word);

                            // See if the statement is "true"; i.e., a non-zero expression
                            if (symbolValue != null) {
                                // The statement is true if the symbol is defined and is a constant expression
                                return (!nonConstantDefines.contains(word));
                            } else {
                                // The statement is true if the symbol evaluates to a non-zero value
                                //
                                // NOTE: This doesn't yet handle evaluable expressions like "#if
                                // SOME_SYMBOL > 5" or "#if SOME_SYMBOL == 0", both of which are
                                // valid syntax. It only handles numeric symbols like "#if 1"

                                try {
                                    // see if it's in decimal form
                                    return Double.parseDouble(word) != 0;
                                } catch (final NumberFormatException nfe1) {
                                    try {
                                        // ok, it's not a valid decimal value, try hex/octal value
                                        return Long.parseLong(word) != 0;
                                    } catch (final NumberFormatException nfe2) {
                                        // ok, it's not a valid hex/octal value, try boolean last
                                        return Boolean.valueOf(word).booleanValue();
                                    }
                                }
                            }
                        }
                    } // end case TT_WORD
                    break;
                case StreamTokenizer.TT_EOL:
                    //System.err.println("HANDLE_IF_RECURSIVE HIT <EOL>!");
                    state.pushBackToken(); // so caller hits EOL as well if we're recursing
                    break;
                case StreamTokenizer.TT_EOF:
                    throw new RuntimeException("Unexpected end of file while parsing " +
                                               "#if statement at file " + filename() + ", line " + lineNumber());

                default:
                    throw new RuntimeException("Unexpected token (" + curTokenAsString() +
                                               ") while parsing " + "#if statement at file " + filename() +
                                               ", line " + lineNumber());
            }
            //System.err.println("END OF WHILE: greedy = " + greedy + " parens = " +openParens + " not EOL = " + (tok != StreamTokenizer.TT_EOL) + " --> " + ((greedy && openParens >= 0) && tok != StreamTokenizer.TT_EOL));
        } while ((greedy && openParens >= 0) && tok != StreamTokenizer.TT_EOL);
        //System.err.println("OUT HANDLE_IF_RECURSIVE (" + tmp-- + ", returning " + ifValue + ")");
        //System.err.flush();
        return ifValue;
    }

    /////////////////////////////////////
    // Handling of #include directives //
    /////////////////////////////////////

    private void handleInclude() throws IOException {
        // Two kinds of #includes: one with quoted string for argument,
        // one with angle brackets surrounding argument
        int t = nextToken();
        String filename = null;
        if (t == '"') {
            filename = state.curWord();
        } else if (t == '<') {
            // Components of path name are coming in as separate tokens;
            // concatenate them
            final StringBuilder buf = new StringBuilder();
            while ((t = nextToken()) != '>' && (t != StreamTokenizer.TT_EOF)) {
                buf.append(curTokenAsString());
            }
            if (t == StreamTokenizer.TT_EOF) {
                LOG.warning(new ASTLocusTag(filename(), lineNumber(), -1, null),
                            "unexpected EOF while processing #include directive");
            }
            filename = buf.toString();
        }
        // if we're not within an active block of code (like inside an "#ifdef
        // FOO" where FOO isn't defined), then don't actually process the
        // #included file.
        debugPrint(true, "INCLUDE [" + filename + "]");
        if (enabled()) {
            // Look up file in known #include path
            final String fullname = findFile(filename);
            //System.err.println("ACTIVE BLOCK, LOADING " + filename);
            if (fullname == null) {
                throw new RuntimeException("Can't find #include file \"" + filename + "\" at file " + filename() + ", line " + lineNumber());
            }
            // Process this file in-line
            final Reader reader = new BufferedReader(new FileReader(fullname));
            run(reader, fullname);
        } else {
            //System.err.println("INACTIVE BLOCK, SKIPPING " + filename);
        }
    }

    ////////////
    // Output //
    ////////////

    private OutputStream out;
    private PrintWriter  writer;
    private final List<Boolean> enabledBits = new ArrayList<Boolean>();

    private static int debugPrintIndentLevel = 0;

    private void debugPrint(final boolean onlyPrintIfEnabled, final String msg) {
        if (!enableDebugPrint) {
            return;
        }

        if (!onlyPrintIfEnabled || (onlyPrintIfEnabled && enabled())) {
            for (int i = debugPrintIndentLevel; --i > 0;) {
                System.err.print("  ");
            }
            System.err.println("STATE: " + msg + "  (line " + lineNumber() + " file " + filename() + ")");
            System.err.flush();
        }
    }

    private void pushEnableBit(final boolean enabled) {
        enabledBits.add(enabled);
        ++debugPrintIndentLevel;
        debugPrint(false, "PUSH_ENABLED, NOW: " + enabled());
    }

    private void popEnableBit() {
        if (enabledBits.isEmpty()) {
            throw new RuntimeException("mismatched #ifdef/endif pairs at file " + filename() + ", line " + lineNumber());
        }
        enabledBits.remove(enabledBits.size() - 1);
        --debugPrintIndentLevel;
        debugPrint(false, "POP_ENABLED, NOW: " + enabled());
    }

    private boolean enabled() {
        return (enabledBits.isEmpty() || enabledBits.get(enabledBits.size() - 1));
    }

    private void print(final String s) {
        if (enabled()) {
            writer.print(s);
            if (enableCopyOutput2Stderr) {
                System.err.print(s);
                System.err.flush();
                return;
            }
        }
    }

    private void print(final char c) {
        if (enabled()) {
            writer.print(c);
            if (enableCopyOutput2Stderr) {
                System.err.print(c);
                System.err.flush();
                return;
            }
        }
    }

    private void println() {
        if (enabled()) {
            writer.println();
            if (enableCopyOutput2Stderr) {
                System.err.println();
                System.err.flush();
                return;
            }
        }
    }

    private void printToken() {
        print(curTokenAsString());
    }

    private void flush() {
        if (enabled()) {
            writer.flush();
            if (enableCopyOutput2Stderr) {
                System.err.flush();
                return;
            }
        }
    }

    private void lineDirective() {
        print("# " + lineNumber() + " \"" + filename() + "\"");
        println();
    }

    private static void usage() {
        System.err.println("Usage: java PCPP [filename | -]");
        System.err.println("Minimal pseudo-C-preprocessor.");
        System.err.println("Output goes to standard output. Standard input can be used as input");
        System.err.println("by passing '-' as the argument.");
        System.err.println("  --debug enables debug mode");
        System.exit(1);
    }

    public static void main(final String[] args) throws IOException {
        Reader reader = null;
        String filename = null;
        boolean debug = false;

        if (args.length == 0) {
            usage();
        }

        final List<String> includePaths = new ArrayList<String>();
        for (int i = 0; i < args.length; i++) {
            if (i < args.length - 1) {
                final String arg = args[i];
                if (arg.startsWith("-I")) {
                    final String[] paths = arg.substring(2).split(System.getProperty("path.separator"));
                    for (int j = 0; j < paths.length; j++) {
                        includePaths.add(paths[j]);
                    }
                } else if (arg.equals("--debug")) {
                    debug = true;
                } else {
                    usage();
                }
            } else {
                final String arg = args[i];
                if (arg.equals("-")) {
                    reader = new InputStreamReader(System.in);
                    filename = "standard input";
                } else {
                    if (arg.startsWith("-")) {
                        usage();
                    }
                    filename = arg;
                    reader = new BufferedReader(new FileReader(filename));
                }
            }
        }

        new PCPP(includePaths, debug, debug).run(reader, filename);
    }

}
