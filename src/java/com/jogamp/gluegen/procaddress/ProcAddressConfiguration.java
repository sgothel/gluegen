/*
 * Copyright (c) 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
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
package com.jogamp.gluegen.procaddress;

import static java.util.logging.Level.INFO;

import com.jogamp.gluegen.JavaConfiguration;
import com.jogamp.gluegen.cgram.types.AliasedSymbol;
import com.jogamp.gluegen.cgram.types.FunctionSymbol;

import java.io.*;
import java.text.*;
import java.util.*;

public class ProcAddressConfiguration extends JavaConfiguration {

    private boolean emitProcAddressTable = false;
    private boolean forceProcAddressGen4All = false;

    private String tableClassPackage;
    private String tableClassName = "ProcAddressTable";
    private String getProcAddressTableExpr;
    private String localProcAddressCallingConvention4All = null;

    private ConvNode procAddressNameConverter;
    private final Set<String> skipProcAddressGen = new HashSet<String>();
    private final List<String> forceProcAddressGen = new ArrayList<String>();
    private final Set<String> forceProcAddressGenSet = new HashSet<String>();

    // This is needed only on Windows. Ideally we would modify the
    // HeaderParser and PCPP to automatically pick up the calling
    // convention from the headers
    private final Map<String, String> localProcAddressCallingConventionMap = new HashMap<String, String>();

    @Override
    protected void dispatch(final String cmd, final StringTokenizer tok, final File file, final String filename, final int lineNo) throws IOException {
        if (cmd.equalsIgnoreCase("EmitProcAddressTable")) {
            emitProcAddressTable = readBoolean("EmitProcAddressTable", tok, filename, lineNo).booleanValue();
        } else if (cmd.equalsIgnoreCase("ProcAddressTablePackage")) {
            tableClassPackage = readString("ProcAddressTablePackage", tok, filename, lineNo);
        } else if (cmd.equalsIgnoreCase("ProcAddressTableClassName")) {
            tableClassName = readString("ProcAddressTableClassName", tok, filename, lineNo);
        } else if (cmd.equalsIgnoreCase("SkipProcAddressGen")) {
            final String sym = readString("SkipProcAddressGen", tok, filename, lineNo);
            skipProcAddressGen.add(sym);
        } else if (cmd.equalsIgnoreCase("ForceProcAddressGen")) {
            final String funcName = readString("ForceProcAddressGen", tok, filename, lineNo);
            if (funcName.equals("__ALL__")) {
                forceProcAddressGen4All = true;
            } else {
                addForceProcAddressGen(funcName);
            }
        } else if (cmd.equalsIgnoreCase("GetProcAddressTableExpr")) {
            setProcAddressTableExpr( readGetProcAddressTableExpr(tok, filename, lineNo) );
        } else if (cmd.equalsIgnoreCase("ProcAddressNameExpr")) {
            readProcAddressNameExpr(tok, filename, lineNo);
        } else if (cmd.equalsIgnoreCase("LocalProcAddressCallingConvention")) {
            readLocalProcAddressCallingConvention(tok, filename, lineNo);
        } else {
            super.dispatch(cmd, tok, file, filename, lineNo);
        }
    }

    protected String readGetProcAddressTableExpr(final StringTokenizer tok, final String filename, final int lineNo) {
        try {
            final String restOfLine = tok.nextToken("\n\r\f");
            return restOfLine.trim();
        } catch (final NoSuchElementException e) {
            throw new RuntimeException("Error parsing \"GetProcAddressTableExpr\" command at line " + lineNo
                    + " in file \"" + filename + "\"", e);
        }
    }

    protected void setProcAddressNameExpr(final String expr) {
        // Parse this into something allowing us to map from a function
        // name to the typedef'ed function pointer name
        final List<String> tokens = new ArrayList<String>();
        final StringTokenizer tok1 = new StringTokenizer(expr);
        while (tok1.hasMoreTokens()) {
            final String sstr = tok1.nextToken();
            final StringTokenizer tok2 = new StringTokenizer(sstr, "$()", true);
            while (tok2.hasMoreTokens()) {
                tokens.add(tok2.nextToken());
            }
        }

        // Now that the string is flattened out, convert it to nodes
        procAddressNameConverter = makeConverter(tokens.iterator());
        if (procAddressNameConverter == null) {
            throw new NoSuchElementException("Error creating converter from string");
        }
    }

    protected void readProcAddressNameExpr(final StringTokenizer tok, final String filename, final int lineNo) {
        try {
            String restOfLine = tok.nextToken("\n\r\f");
            restOfLine = restOfLine.trim();
            setProcAddressNameExpr(restOfLine);
        } catch (final NoSuchElementException e) {
            throw new RuntimeException("Error parsing \"ProcAddressNameExpr\" command at line " + lineNo
                    + " in file \"" + filename + "\"", e);
        }
    }

    protected void readLocalProcAddressCallingConvention(final StringTokenizer tok, final String filename, final int lineNo) throws IOException {
        try {
            final String functionName = tok.nextToken();
            final String callingConvention = tok.nextToken();
            if (functionName.equals("__ALL__")) {
                localProcAddressCallingConvention4All = callingConvention;
            } else {
                localProcAddressCallingConventionMap.put(functionName, callingConvention);
            }
        } catch (final NoSuchElementException e) {
            throw new RuntimeException("Error parsing \"LocalProcAddressCallingConvention\" command at line " + lineNo
                    + " in file \"" + filename + "\"", e);
        }
    }

    private static ConvNode makeConverter(final Iterator<String> iter) {
        final List<ConvNode> result = new ArrayList<ConvNode>();

        while (iter.hasNext()) {
            final String str = iter.next();
            if (str.equals("$")) {
                final String command = iter.next();
                final String openParen = iter.next();
                if (!openParen.equals("(")) {
                    throw new NoSuchElementException("Expected \"(\"");
                }
                boolean uppercase = false;
                if (command.equalsIgnoreCase("UPPERCASE")) {
                    uppercase = true;
                } else if (!command.equalsIgnoreCase("LOWERCASE")) {
                    throw new NoSuchElementException("Unknown ProcAddressNameExpr command \"" + command + "\"");
                }
                result.add(new CaseNode(uppercase, makeConverter(iter)));
            } else if (str.equals(")")) {
                // Fall through and return
            } else if (str.indexOf('{') >= 0) {
                result.add(new FormatNode(str));
            } else {
                result.add(new ConstStringNode(str));
            }
        }
        if (result.isEmpty()) {
            return null;
        } else if (result.size() == 1) {
            return result.get(0);
        } else {
            return new ConcatNode(result);
        }
    }

    /** Helper class for converting a function name to the typedef'ed
    function pointer name */
    static abstract class ConvNode {
        abstract String convert(String funcName);
    }

    static class FormatNode extends ConvNode {

        private final MessageFormat msgFmt;

        FormatNode(final String fmt) {
            msgFmt = new MessageFormat(fmt);
        }

        @Override
        String convert(final String funcName) {
            final StringBuffer buf = new StringBuffer();
            msgFmt.format(new Object[]{funcName}, buf, null);
            return buf.toString();
        }
    }

    static class ConstStringNode extends ConvNode {

        private final String str;

        ConstStringNode(final String str) {
            this.str = str;
        }

        @Override
        String convert(final String funcName) {
            return str;
        }
    }

    static class ConcatNode extends ConvNode {

        private final List<ConvNode> children;

        ConcatNode(final List<ConvNode> children) {
            this.children = children;
        }

        @Override
        String convert(final String funcName) {
            final StringBuilder res = new StringBuilder();
            for (final ConvNode node : children) {
                res.append(node.convert(funcName));
            }
            return res.toString();
        }
    }

    static class CaseNode extends ConvNode {

        private final boolean upperCase;
        private final ConvNode child;

        CaseNode(final boolean upperCase, final ConvNode child) {
            this.upperCase = upperCase;
            this.child = child;
        }

        @Override
        public String convert(final String funcName) {
            if (upperCase) {
                return child.convert(funcName).toUpperCase();
            } else {
                return child.convert(funcName).toLowerCase();
            }
        }
    }

    public boolean emitProcAddressTable() {
        return emitProcAddressTable;
    }

    public String tableClassPackage() {
        return tableClassPackage;
    }

    public String tableClassName() {
        return tableClassName;
    }

    public boolean skipProcAddressGen(final FunctionSymbol symbol) {
      if ( skipProcAddressGen.contains( symbol.getName() ) ||
           oneInSet(skipProcAddressGen, symbol.getAliasedNames())
         )
      {
          LOG.log(INFO, symbol.getASTLocusTag(), "Skip ProcAddress: {0}", symbol);
          return true;
      }
      return false;
    }

    public boolean isForceProcAddressGen4All() {
        return forceProcAddressGen4All;
    }

    public List<String> getForceProcAddressGen() {
        return forceProcAddressGen;
    }

    public String getProcAddressTableExpr() {
        if (getProcAddressTableExpr == null) {
            throw new RuntimeException("GetProcAddressTableExpr was not defined in .cfg file");
        }
        return getProcAddressTableExpr;
    }
    protected void setProcAddressTableExpr(final String s) {
        getProcAddressTableExpr = s;
    }

    public String convertToFunctionPointerName(final String funcName) {
        if (procAddressNameConverter == null) {
            throw new RuntimeException("ProcAddressNameExpr was not defined in .cfg file");
        }
        return procAddressNameConverter.convert(funcName);
    }

    public boolean forceProcAddressGen(final FunctionSymbol symbol) {
        if( forceProcAddressGen4All ) {
            if(!forceProcAddressGen4AllOnce) {
                forceProcAddressGen4AllOnce = true;
                LOG.log(INFO, symbol.getASTLocusTag(), "Force ALL ProcAddress");
            }
            return true;
        }

        if ( forceProcAddressGenSet.contains( symbol.getName() ) ||
             oneInSet(forceProcAddressGenSet, symbol.getAliasedNames())
           )
        {
            LOG.log(INFO, symbol.getASTLocusTag(), "Force ProcAddress: {0}", symbol);
            return true;
        }
        return false;
    }
    private static boolean forceProcAddressGen4AllOnce = false;

    public void addForceProcAddressGen(final String funcName) {
        forceProcAddressGen.add(funcName);
        forceProcAddressGenSet.add(funcName);
    }

    public void addLocalProcAddressCallingConvention(final String funcName, final String callingConvention) {
        localProcAddressCallingConventionMap.put(funcName, callingConvention);
    }

    public String getLocalProcAddressCallingConvention(final FunctionSymbol symbol) {
        if ( isLocalProcAddressCallingConvention4All() ) {
            return getLocalProcAddressCallingConvention4All();
        }
        final String res = localProcAddressCallingConventionMap.get(symbol.getName());
        if( null != res ) {
            return res;
        }
        return oneInMap(localProcAddressCallingConventionMap, symbol.getAliasedNames());
    }

    public boolean isLocalProcAddressCallingConvention4All() {
        return localProcAddressCallingConvention4All != null;
    }

    public String getLocalProcAddressCallingConvention4All() {
        return localProcAddressCallingConvention4All;
    }
}
