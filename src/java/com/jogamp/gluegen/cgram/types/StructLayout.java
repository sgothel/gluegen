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

package com.jogamp.gluegen.cgram.types;

import com.jogamp.common.os.MachineDataInfo;
import com.jogamp.gluegen.GlueGen;

/** Encapsulates algorithm for laying out data structures. Note that
    this ends up embedding code in various places via SizeThunks. If
    the 32-bit and 64-bit ports on a given platform differ
    fundamentally in their handling of struct layout then this code
    will need to be updated and, most likely, two versions of the
    SizeThunks maintained in various places. */

public class StructLayout {
  private final int baseOffset;

  protected StructLayout(final int baseOffset) {
    this.baseOffset = baseOffset;
  }

  public void layout(final CompoundType t) {
    /**
     * - 1) align offset for the new data type,
     * - 2) add the aligned size of the new data type
     * - 3) add trailing padding (largest element size)
     */
    final int n = t.getNumFields();
    SizeThunk curOffset = SizeThunk.constant(baseOffset);
    SizeThunk maxSize   = SizeThunk.constant(0);

    final MachineDataInfo dbgMD;
    if( GlueGen.debug() ) {
        dbgMD = MachineDataInfo.StaticConfig.LP64_UNIX.md;
        System.err.printf("SL.__: o %03d, s %03d, t %s{%d}%n", curOffset.computeSize(dbgMD), 0, t, t.getNumFields());
    } else {
        dbgMD = null;
    }

    for (int i = 0; i < n; i++) {
      final Field f = t.getField(i);
      final Type ft = f.getType();
      if (ft.isInt() || ft.isFloat() || ft.isDouble() || ft.isPointer()) {
        final SizeThunk sz = ft.getSize();
        curOffset = SizeThunk.align(curOffset, sz);
        f.setOffset(curOffset);
        if (t.isUnion()) {
          maxSize = SizeThunk.max(maxSize, sz);
        } else {
          curOffset = SizeThunk.add(curOffset, sz);
        }
      } else if (ft.isCompound()) {
        final CompoundType ct = ft.asCompound();
        if(!ct.isLayouted()) {
            StructLayout.layout(0, ct);
        }
        final SizeThunk sz = ct.getSize();
        curOffset = SizeThunk.align(curOffset, sz);
        f.setOffset(curOffset);
        if (t.isUnion()) {
          maxSize = SizeThunk.max(maxSize, sz);
        } else {
          curOffset = SizeThunk.add(curOffset, sz);
        }
      } else if (ft.isArray()) {
        final ArrayType arrayType = ft.asArray();
        if(!arrayType.isLayouted()) {
            final CompoundType compoundElementType = arrayType.getBaseElementType().asCompound();
            if (compoundElementType != null) {
              if(!compoundElementType.isLayouted()) {
                  StructLayout.layout(0, compoundElementType);
              }
              arrayType.recomputeSize();
            }
            arrayType.setLayouted();
        }
        final SizeThunk sz = ft.getSize();
        curOffset = SizeThunk.align(curOffset, sz);
        f.setOffset(curOffset);
        curOffset = SizeThunk.add(curOffset, sz);
      } else {
        // FIXME
        String name = t.getName();
        if (name == null) {
          name = t.toString();
        }
        throw new RuntimeException("Complicated field types (" + ft +
                                   " " + f.getName() +
                                   " in type " + name +
                                   ") not implemented yet");
      }
      if( GlueGen.debug() ) {
        System.err.printf("SL.%02d: o %03d, s %03d: %s, %s%n", (i+1), f.getOffset(dbgMD), ft.getSize(dbgMD), f, ft.getDebugString());
      }
    }
    if (t.isUnion()) {
      t.setSize(maxSize);
    } else {
      // trailing struct padding ..
      curOffset = SizeThunk.align(curOffset, curOffset);
      t.setSize(curOffset);
    }
    if( GlueGen.debug() ) {
        System.err.printf("SL.XX: o %03d, s %03d, t %s{%d}%n%n", curOffset.computeSize(dbgMD), t.getSize(dbgMD), t, t.getNumFields());
    }
    t.setLayouted();
  }

  public static StructLayout create(final int baseOffset) {
      return new StructLayout(baseOffset);
  }

  public static void layout(final int baseOffset, final CompoundType t) {
      create(baseOffset).layout(t);
  }
}
