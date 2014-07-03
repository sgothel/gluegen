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

package com.jogamp.common.os;

import jogamp.common.os.PlatformPropsImpl;

/**
 * For alignment and size see {@link com.jogamp.gluegen}
 */
public class MachineDescription {
  public enum ID {
      /** {@link Platform.CPUType#ARM} EABI Little Endian */
      ARMle_EABI(Platform.CPUType.ARM),
      /** {@link Platform.CPUType#X86_32} Little Endian Unix */
      X86_32_UNIX(Platform.CPUType.X86_32),
      /** {@link Platform.CPUType#X86_64} Little Endian Unix */
      X86_64_UNIX(Platform.CPUType.X86_64),
      /** {@link Platform.CPUType#X86_32} Little Endian MacOS (Special case gcc4/OSX) */
      X86_32_MACOS(Platform.CPUType.X86_32),
      /** {@link Platform.CPUType#X86_64} Little Endian MacOS */
      X86_32_WINDOWS(Platform.CPUType.X86_32),
      /** {@link Platform.CPUType#X86_64} Little Endian Windows */
      X86_64_WINDOWS(Platform.CPUType.X86_64),
      /** {@link Platform.CPUType#SPARC_32} Big Endian Solaris */
      SPARC_32_SUNOS(Platform.CPUType.SPARC_32);

      public final Platform.CPUType cpu;

      ID(final Platform.CPUType cpu){
          this.cpu = cpu;
      }
  }

  /*                              arch   os          int, long, float, doubl, ldoubl,  ptr,   page */
  private final static int[] size_armeabi         =  { 4,    4,     4,     8,      8,    4,   4096 };
  private final static int[] size_x86_32_unix     =  { 4,    4,     4,     8,     12,    4,   4096 };
  private final static int[] size_x86_32_macos    =  { 4,    4,     4,     8,     16,    4,   4096 };
  private final static int[] size_x86_32_windows  =  { 4,    4,     4,     8,     12,    4,   4096 };
  private final static int[] size_x86_64_unix     =  { 4,    8,     4,     8,     16,    8,   4096 };
  private final static int[] size_x86_64_windows  =  { 4,    4,     4,     8,     16,    8,   4096 };
  private final static int[] size_sparc_32_sunos  =  { 4,    4,     4,     8,     16,    4,   8192 };

  /*                               arch   os          i8, i16, i32, i64, int, long, float, doubl, ldoubl, ptr */
  private final static int[] align_armeabi        =  { 1,   2,   4,   8,   4,    4,     4,     8,      8,   4 };
  private final static int[] align_x86_32_unix    =  { 1,   2,   4,   4,   4,    4,     4,     4,      4,   4 };
  private final static int[] align_x86_32_macos   =  { 1,   2,   4,   4,   4,    4,     4,     4,     16,   4 };
  private final static int[] align_x86_32_windows =  { 1,   2,   4,   8,   4,    4,     4,     8,      4,   4 };
  private final static int[] align_x86_64_unix    =  { 1,   2,   4,   8,   4,    8,     4,     8,     16,   8 };
  private final static int[] align_x86_64_windows =  { 1,   2,   4,   8,   4,    4,     4,     8,     16,   8 };
  private final static int[] align_sparc_32_sunos =  { 1,   2,   4,   8,   4,    4,     4,     8,      8,   4 };

  public enum StaticConfig {
      /** {@link MachineDescription.ID#ARMle_EABI } */
      ARMle_EABI(ID.ARMle_EABI,         true,  size_armeabi,        align_armeabi),
      /** {@link MachineDescription.ID#X86_32_UNIX } */
      X86_32_UNIX(ID.X86_32_UNIX,       true,  size_x86_32_unix,    align_x86_32_unix),
      /** {@link MachineDescription.ID#X86_64_UNIX } */
      X86_64_UNIX(ID.X86_64_UNIX,       true,  size_x86_64_unix,    align_x86_64_unix),
      /** {@link MachineDescription.ID#X86_32_MACOS } */
      X86_32_MACOS(ID.X86_32_MACOS,     true,  size_x86_32_macos,   align_x86_32_macos),
      /** {@link MachineDescription.ID#X86_32_WINDOWS } */
      X86_32_WINDOWS(ID.X86_32_WINDOWS, true,  size_x86_32_windows, align_x86_32_windows),
      /** {@link MachineDescription.ID#X86_64_WINDOWS } */
      X86_64_WINDOWS(ID.X86_64_WINDOWS, true,  size_x86_64_windows, align_x86_64_windows),
      /** {@link MachineDescription.ID#SPARC_32_SUNOS } */
      SPARC_32_SUNOS(ID.SPARC_32_SUNOS, false, size_sparc_32_sunos, align_sparc_32_sunos);

      public final ID id;
      public final MachineDescription md;

      StaticConfig(final ID id, final boolean littleEndian, final int[] sizes, final int[] alignments) {
          this.id = id;
          int i=0, j=0;
          this.md = new MachineDescription(false, littleEndian,
                                           sizes[i++],
                                           sizes[i++],
                                           sizes[i++],
                                           sizes[i++],
                                           sizes[i++],
                                           sizes[i++],
                                           sizes[i++],
                                           alignments[j++],
                                           alignments[j++],
                                           alignments[j++],
                                           alignments[j++],
                                           alignments[j++],
                                           alignments[j++],
                                           alignments[j++],
                                           alignments[j++],
                                           alignments[j++],
                                           alignments[j++]);
      }

      public StringBuilder toString(StringBuilder sb) {
        if(null==sb) {
            sb = new StringBuilder();
        }
        sb.append("MachineDescriptionStatic: ").append(this.name()).append("(").append(this.ordinal()).append("): ");
        md.toString(sb);
        return sb;
      }

      @Override
      public String toString() {
        return toString(null).toString();
      }
  }


  final private boolean runtimeValidated;

  final private boolean littleEndian;

  final private int int8SizeInBytes = 1;
  final private int int16SizeInBytes = 2;
  final private int int32SizeInBytes = 4;
  final private int int64SizeInBytes = 8;

  final private int intSizeInBytes;
  final private int longSizeInBytes;
  final private int floatSizeInBytes;
  final private int doubleSizeInBytes;
  final private int ldoubleSizeInBytes;
  final private int pointerSizeInBytes;
  final private int pageSizeInBytes;
  final private boolean is32Bit;

  final private int int8AlignmentInBytes;
  final private int int16AlignmentInBytes;
  final private int int32AlignmentInBytes;
  final private int int64AlignmentInBytes;
  final private int intAlignmentInBytes;
  final private int longAlignmentInBytes;
  final private int floatAlignmentInBytes;
  final private int doubleAlignmentInBytes;
  final private int ldoubleAlignmentInBytes;
  final private int pointerAlignmentInBytes;

  public MachineDescription(final boolean runtimeValidated,
                            final boolean littleEndian,

                            final int intSizeInBytes,
                            final int longSizeInBytes,
                            final int floatSizeInBytes,
                            final int doubleSizeInBytes,
                            final int ldoubleSizeInBytes,
                            final int pointerSizeInBytes,
                            final int pageSizeInBytes,

                            final int int8AlignmentInBytes,
                            final int int16AlignmentInBytes,
                            final int int32AlignmentInBytes,
                            final int int64AlignmentInBytes,
                            final int intAlignmentInBytes,
                            final int longAlignmentInBytes,
                            final int floatAlignmentInBytes,
                            final int doubleAlignmentInBytes,
                            final int ldoubleAlignmentInBytes,
                            final int pointerAlignmentInBytes) {
    this.runtimeValidated = runtimeValidated;
    this.littleEndian = littleEndian;

    this.intSizeInBytes     = intSizeInBytes;
    this.longSizeInBytes    = longSizeInBytes;
    this.floatSizeInBytes   = floatSizeInBytes;
    this.doubleSizeInBytes  = doubleSizeInBytes;
    this.ldoubleSizeInBytes = ldoubleSizeInBytes;
    this.pointerSizeInBytes = pointerSizeInBytes;
    this.pageSizeInBytes    = pageSizeInBytes;
    this.is32Bit            = 4 == pointerSizeInBytes;

    this.int8AlignmentInBytes    = int8AlignmentInBytes;
    this.int16AlignmentInBytes   = int16AlignmentInBytes;
    this.int32AlignmentInBytes   = int32AlignmentInBytes;
    this.int64AlignmentInBytes   = int64AlignmentInBytes;
    this.intAlignmentInBytes     = intAlignmentInBytes;
    this.longAlignmentInBytes    = longAlignmentInBytes;
    this.floatAlignmentInBytes   = floatAlignmentInBytes;
    this.doubleAlignmentInBytes  = doubleAlignmentInBytes;
    this.ldoubleAlignmentInBytes = ldoubleAlignmentInBytes;
    this.pointerAlignmentInBytes = pointerAlignmentInBytes;
  }

  /**
   * @return true if all values are validated at runtime, otherwise false (i.e. for static compilation w/ preset values)
   */
  public final boolean isRuntimeValidated() {
      return runtimeValidated;
  }

  /**
   * Returns true only if this system uses little endian byte ordering.
   */
  public final boolean isLittleEndian() {
      return littleEndian;
  }

  /**
   * Returns true if this JVM/ARCH is 32bit.
   */
  public final boolean is32Bit() {
    return is32Bit;
  }

  /**
   * Returns true if this JVM/ARCH is 64bit.
   */
  public final  boolean is64Bit() {
    return !is32Bit;
  }

  public final int intSizeInBytes()     { return intSizeInBytes;    }
  public final int longSizeInBytes()    { return longSizeInBytes;   }
  public final int int8SizeInBytes()    { return int8SizeInBytes;  }
  public final int int16SizeInBytes()   { return int16SizeInBytes;  }
  public final int int32SizeInBytes()   { return int32SizeInBytes;  }
  public final int int64SizeInBytes()   { return int64SizeInBytes;  }
  public final int floatSizeInBytes()   { return floatSizeInBytes;  }
  public final int doubleSizeInBytes()  { return doubleSizeInBytes; }
  public final int ldoubleSizeInBytes() { return ldoubleSizeInBytes; }
  public final int pointerSizeInBytes() { return pointerSizeInBytes; }
  public final int pageSizeInBytes()    { return pageSizeInBytes; }

  public final int intAlignmentInBytes()     { return intAlignmentInBytes;    }
  public final int longAlignmentInBytes()    { return longAlignmentInBytes;   }
  public final int int8AlignmentInBytes()    { return int8AlignmentInBytes;  }
  public final int int16AlignmentInBytes()   { return int16AlignmentInBytes;  }
  public final int int32AlignmentInBytes()   { return int32AlignmentInBytes;  }
  public final int int64AlignmentInBytes()   { return int64AlignmentInBytes;  }
  public final int floatAlignmentInBytes()   { return floatAlignmentInBytes;  }
  public final int doubleAlignmentInBytes()  { return doubleAlignmentInBytes; }
  public final int ldoubleAlignmentInBytes() { return ldoubleAlignmentInBytes; }
  public final int pointerAlignmentInBytes() { return pointerAlignmentInBytes; }

  /**
   * @return number of pages required for size in bytes
   */
  public int pageCount(final int size) {
    return ( size + ( pageSizeInBytes - 1) ) / pageSizeInBytes ; // integer arithmetic
  }

  /**
   * @return page aligned size in bytes
   */
  public int pageAlignedSize(final int size) {
    return pageCount(size) * pageSizeInBytes;
  }

  /**
   * Checks whether two size objects are equal. Two instances
   * of <code>MachineDescription</code> are considered equal if all components
   * match but {@link #runtimeValidated},  {@link #isRuntimeValidated()}.
   * @return  <code>true</code> if the two MachineDescription are equal;
   *          otherwise <code>false</code>.
   */
  @Override
  public final boolean equals(final Object obj) {
      if (this == obj) { return true; }
      if ( !(obj instanceof MachineDescription) ) { return false; }
      final MachineDescription md = (MachineDescription) obj;

      return pageSizeInBytes == md.pageSizeInBytes &&
             compatible(md);
  }

  /**
   * Checks whether two size objects are equal. Two instances
   * of <code>MachineDescription</code> are considered equal if all components
   * match but {@link #isRuntimeValidated()} and {@link #pageSizeInBytes()}.
   * @return  <code>true</code> if the two MachineDescription are equal;
   *          otherwise <code>false</code>.
   */
  public final boolean compatible(final MachineDescription md) {
      return littleEndian == md.littleEndian &&

             intSizeInBytes == md.intSizeInBytes &&
             longSizeInBytes == md.longSizeInBytes &&
             floatSizeInBytes == md.floatSizeInBytes &&
             doubleSizeInBytes == md.doubleSizeInBytes &&
             ldoubleSizeInBytes == md.ldoubleSizeInBytes &&
             pointerSizeInBytes == md.pointerSizeInBytes &&
             is32Bit == md.is32Bit &&

             int8AlignmentInBytes == md.int8AlignmentInBytes &&
             int16AlignmentInBytes == md.int16AlignmentInBytes &&
             int32AlignmentInBytes == md.int32AlignmentInBytes &&
             int64AlignmentInBytes == md.int64AlignmentInBytes &&
             intAlignmentInBytes == md.intAlignmentInBytes &&
             longAlignmentInBytes == md.longAlignmentInBytes &&
             floatAlignmentInBytes == md.floatAlignmentInBytes &&
             doubleAlignmentInBytes == md.doubleAlignmentInBytes &&
             ldoubleAlignmentInBytes == md.ldoubleAlignmentInBytes &&
             pointerAlignmentInBytes == md.pointerAlignmentInBytes ;
  }

  public StringBuilder toString(StringBuilder sb) {
    if(null==sb) {
        sb = new StringBuilder();
    }
    sb.append("MachineDescription: runtimeValidated ").append(isRuntimeValidated()).append(", littleEndian ").append(isLittleEndian()).append(", 32Bit ").append(is32Bit()).append(", primitive size / alignment:").append(PlatformPropsImpl.NEWLINE);
    sb.append("  int8    ").append(int8SizeInBytes)   .append(" / ").append(int8AlignmentInBytes);
    sb.append(", int16   ").append(int16SizeInBytes)  .append(" / ").append(int16AlignmentInBytes).append(Platform.getNewline());
    sb.append("  int     ").append(intSizeInBytes)    .append(" / ").append(intAlignmentInBytes);
    sb.append(", long    ").append(longSizeInBytes)   .append(" / ").append(longAlignmentInBytes).append(Platform.getNewline());
    sb.append("  int32   ").append(int32SizeInBytes)  .append(" / ").append(int32AlignmentInBytes);
    sb.append(", int64   ").append(int64SizeInBytes)  .append(" / ").append(int64AlignmentInBytes).append(Platform.getNewline());
    sb.append("  float   ").append(floatSizeInBytes)  .append(" / ").append(floatAlignmentInBytes);
    sb.append(", double  ").append(doubleSizeInBytes) .append(" / ").append(doubleAlignmentInBytes);
    sb.append(", ldouble ").append(ldoubleSizeInBytes).append(" / ").append(ldoubleAlignmentInBytes).append(Platform.getNewline());
    sb.append("  pointer ").append(pointerSizeInBytes).append(" / ").append(pointerAlignmentInBytes);
    sb.append(", page    ").append(pageSizeInBytes);
    return sb;
  }

  @Override
  public String toString() {
    return toString(null).toString();
  }

}
