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

import java.io.IOException;

/**
 * Java code unit (a generated Java source file),
 * covering multiple {@link FunctionEmitter} allowing to unify output, decoration and dynamic helper code injection per unit.
 **/
public class JavaCodeUnit extends CodeUnit {
    /** Package name of this Java unit */
    public final String pkgName;
    /** Simple class name of this Java unit */
    public final String className;

    /**
     * @param filename the class's full filename to open w/ write access
     * @param packageName the package name of the class
     * @param simpleClassName the simple class name, i.e. w/o package name or c-file basename
     * @param generator informal optional object that is creating this unit, used to be mentioned in a warning message if not null.
     * @throws IOException
     */
    public JavaCodeUnit(final String filename, final String packageName, final String simpleClassName, final Object generator) throws IOException {
        super(filename, generator);
        this.pkgName = packageName;
        this.className = simpleClassName;
        CodeGenUtils.emitAutogeneratedWarning(output, generator, "Java-Unit: [pkg "+packageName+", cls "+simpleClassName+"], "+filename);
    }

    @Override
    public String toString() { return "JavaCodeUnit[unit "+pkgName+"."+className+", file "+filename+"]"; }
}
