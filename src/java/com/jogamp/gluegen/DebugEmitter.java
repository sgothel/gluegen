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

package com.jogamp.gluegen;

import java.io.IOException;
import java.util.*;

import com.jogamp.gluegen.cgram.types.*;

/** Debug emitter which prints the parsing results to standard output. */

public class DebugEmitter implements GlueEmitter {
  protected JavaConfiguration cfg;

  @Override
  public void readConfigurationFile(final String filename) throws IOException {
      cfg = createConfig();
      cfg.read(filename);
  }

  @Override
  public JavaConfiguration getConfiguration() { return cfg; }

  @Override
  public void beginEmission(final GlueEmitterControls controls) {
    System.out.println("----- BEGIN EMISSION OF GLUE CODE -----");
  }

  @Override
  public void endEmission() {
    System.out.println("----- END EMISSION OF GLUE CODE -----");
  }

  @Override
  public void beginDefines() {}

  @Override
  public void emitDefine(final ConstantDefinition def, final String optionalComment) {
    final String name = def.getName();
    final String value = def.getNativeExpr();
    System.out.println("#define " + name + " " + value +
                       (optionalComment != null ? ("// " + optionalComment) : ""));
  }
  @Override
  public void endDefines() {}

  @Override
  public void beginFunctions(final TypeDictionary typedefDictionary,
                             final TypeDictionary structDictionary,
                             final Map<Type, Type> canonMap) {
    final Set<String> keys = typedefDictionary.keySet();
    for (final String key: keys) {
      final Type value = typedefDictionary.get(key);
      System.out.println("typedef " + value + " " + key + ";");
    }
  }

  @Override
  public Iterator<FunctionSymbol> emitFunctions(final List<FunctionSymbol> originalCFunctions) throws Exception {
    for (final FunctionSymbol sym : originalCFunctions) {
      emitSingleFunction(sym);
    }
    return originalCFunctions.iterator();
  }
  public void emitSingleFunction(final FunctionSymbol sym) {
    System.out.println(sym);
    System.out.println(" -> " + sym.toString());
  }
  @Override
  public void endFunctions() {}

  @Override
  public void beginStructLayout() throws Exception {}
  @Override
  public void layoutStruct(final CompoundType t) throws Exception {}
  @Override
  public void endStructLayout() throws Exception {}

  @Override
  public void beginStructs(final TypeDictionary typedefDictionary, final TypeDictionary structDictionary, final Map<Type, Type> canonMap) {
  }

  @Override
  public void emitStruct(final CompoundType t, final Type typedefType) {
    String name = t.getName();
    if (name == null && typedefType != null) {
      name = typedefType.getName();
    }

    System.out.println("Referenced type \"" + name + "\"");
  }

  @Override
  public void endStructs() {}

  /**
   * Create the object that will read and store configuration information for
   * this JavaEmitter.
   */
  protected JavaConfiguration createConfig() {
    return new JavaConfiguration();
  }

}
