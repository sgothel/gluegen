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

import java.util.*;
import java.io.*;

import com.jogamp.gluegen.cgram.types.FunctionSymbol;
import com.jogamp.gluegen.cgram.types.Type;

public abstract class FunctionEmitter {

  public static final EmissionModifier STATIC = new EmissionModifier("static");

  private final boolean isInterface;
  private final ArrayList<EmissionModifier> modifiers;
  private CommentEmitter commentEmitter = null;
  private final PrintWriter defaultOutput;
  // Only present to provide more clear comments
  protected final JavaConfiguration cfg;

  /**
   * Constructs the FunctionEmitter with a CommentEmitter that emits nothing.
   */
  public FunctionEmitter(final PrintWriter defaultOutput, final boolean isInterface, final JavaConfiguration configuration)  {
    assert(defaultOutput != null);
    this.isInterface = isInterface;
    this.modifiers = new ArrayList<EmissionModifier>();
    this.defaultOutput = defaultOutput;
    this.cfg = configuration;
  }

  /**
   * Makes this FunctionEmitter a copy of the passed one.
   */
  public FunctionEmitter(final FunctionEmitter arg) {
    isInterface = arg.isInterface;
    modifiers      = new ArrayList<EmissionModifier>(arg.modifiers);
    commentEmitter = arg.commentEmitter;
    defaultOutput  = arg.defaultOutput;
    cfg            = arg.cfg;
  }

  public boolean isInterface() { return isInterface; }

  public PrintWriter getDefaultOutput() { return defaultOutput; }

  public void addModifiers(final Iterator<EmissionModifier> mi)  {
    while (mi.hasNext())  {
      modifiers.add(mi.next());
    }
  }
  public void addModifier(final EmissionModifier m) { modifiers.add(m); }

  public boolean removeModifier(final EmissionModifier m) { return modifiers.remove(m); }

  public void clearModifiers() { modifiers.clear(); }

  public boolean hasModifier(final EmissionModifier m) { return modifiers.contains(m); }

  public Iterator<EmissionModifier> getModifiers() { return modifiers.iterator(); }

  public abstract String getInterfaceName();
  public abstract String getImplName();
  public abstract String getNativeName();

  public abstract FunctionSymbol getCSymbol();

  /**
   * Emit the function to the specified output (instead of the default
   * output).
   */
  public void emit(final PrintWriter output)  {
    emitDocComment(output);
    //output.println("  // Emitter: " + getClass().getName());
    emitSignature(output);
    emitBody(output);
  }

  /**
   * Emit the function to the default output (the output that was passed to
   * the constructor)
   */
  public final void emit()  {
    emit(getDefaultOutput());
  }

  /** Returns, as a String, whatever {@link #emit} would output. */
  @Override
  public String toString()  {
    final StringWriter sw = new StringWriter(500);
    final PrintWriter w = new PrintWriter(sw);
    emit(w);
    return sw.toString();
  }

  /**
   * Set the object that will emit the comment for this function. If the
   * parameter is null, no comment will be emitted.
   */
  public void setCommentEmitter(final CommentEmitter cEmitter)  {
    commentEmitter = cEmitter;
  }

  /**
   * Get the comment emitter for this FunctionEmitter. The return value may be
   * null, in which case no comment emitter has been set.
   */
  public CommentEmitter getCommentEmitter() { return commentEmitter; }

  protected void emitDocComment(final PrintWriter writer) {

    if (commentEmitter != null)    {
      writer.print(getBaseIndentString()); //indent

      writer.print(getCommentStartString());

      commentEmitter.emit(this, writer);

      writer.print(getBaseIndentString()); //indent

      writer.println(getCommentEndString());
    }
  }

  protected void emitSignature(final PrintWriter writer)  {

    writer.print(getBaseIndentString()); // indent method

    final int numEmitted = emitModifiers(writer);
    if (numEmitted > 0)  {
      writer.print(" ");
    }

    emitReturnType(writer);
    writer.print(" ");

    emitName(writer);
    writer.print("(");

    emitArguments(writer);
    writer.print(")");
  }

  protected int emitModifiers(final PrintWriter writer)  {
    int numEmitted = 0;
    for (final Iterator<EmissionModifier> it = getModifiers(); it.hasNext(); )   {
      writer.print(it.next());
      ++numEmitted;
      if (it.hasNext())  {
        writer.print(" ");
      }
    }
    return numEmitted;
  }

  protected String getBaseIndentString() { return ""; }

  protected String getCommentStartString() { return "/* "; }
  protected String getCommentEndString() { return " */"; }

  protected abstract void emitReturnType(PrintWriter writer);
  protected abstract void emitName(PrintWriter writer);
  /** Returns the number of arguments emitted. */
  protected abstract int emitArguments(PrintWriter writer);
  protected abstract void emitBody(PrintWriter writer);

  public static class EmissionModifier  {

    @Override
    public final String toString() { return emittedForm; }

    private final String emittedForm;

    @Override
    public int hashCode() {
      return emittedForm.hashCode();
    }

    @Override
    public boolean equals(final Object arg) {
      if (arg == null || (!(arg instanceof EmissionModifier))) {
        return false;
      }

      return emittedForm.equals(((EmissionModifier) arg).emittedForm);
    }

    protected EmissionModifier(final String emittedForm) { this.emittedForm = emittedForm; }
  }
}

