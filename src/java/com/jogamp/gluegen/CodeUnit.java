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
package com.jogamp.gluegen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * General code unit (a generated C or Java source file),
 * covering multiple {@link FunctionEmitter} allowing to unify output, decoration and dynamic helper code injection per unit.
 **/
public class CodeUnit {
    public final String filename;
    public final PrintWriter output;
    private final Set<String> tailCode = new HashSet<String>();

    /**
     * @param filename the class's full filename to open w/ write access
     * @param generator informal optional object that is creating this unit, used to be mentioned in a warning message if not null.
     * @throws IOException
     */
    protected CodeUnit(final String filename, final Object generator) throws IOException {
        this.filename = filename;
        this.output = openFile(filename);
    }

    private static PrintWriter openFile(final String filename) throws IOException {
        final File file = new File(filename);
        final String parentDir = file.getParent();
        if (parentDir != null) {
            new File(parentDir).mkdirs();
        }
        return new PrintWriter(new BufferedWriter(new FileWriter(file)));
    }

    /**
     * Add a tail code to this unit
     * @param c the code to be added to the tail of this code unit
     * @return true if the `tailCode` set did not already contain the specified code `c`
     */
    public boolean addTailCode(final String c) {
        return tailCode.add(c);
    }

    public void emitln() {
        output.println();
    }
    public void emitln(final String s) {
        output.println(s);
    }
    public void emit(final String s) {
        output.print(s);
    }
    public void emitf(final String s, final Object... args) {
        output.printf(s, args);
    }
    public void emitTailCode() {
        tailCode.forEach( (final String t) -> { output.write(t); output.println(); } );
        tailCode.clear();
    }
    public void close() {
        emitTailCode();
        output.flush();
        output.close();
    }

    @Override
    public String toString() { return "CodeUnit[file "+filename+"]"; }
}
