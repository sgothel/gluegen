/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
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
import com.jogamp.gluegen.cgram.types.TypeComparator.SemanticEqualityOp;

/** Provides a level of indirection between the definition of a type's
    size and the absolute value of this size. Necessary when
    generating glue code for two different CPU architectures (e.g.,
    32-bit and 64-bit) from the same internal representation of the
    various types involved. */
public abstract class SizeThunk implements Cloneable, SemanticEqualityOp {
  /* pp */ static boolean relaxedEqSem = false;
  private final boolean fixedNativeSize;

  public static void setRelaxedEqualSemanticsTest(final boolean v) {
      relaxedEqSem = v;
  }

  // Private constructor because there are only a few of these
  private SizeThunk(final boolean fixedNativeSize) {
      this.fixedNativeSize = fixedNativeSize;
  }

  @Override
  public Object clone() {
    try {
        return super.clone();
    } catch (final CloneNotSupportedException ex) {
        throw new InternalError();
    }
  }

  public final boolean hasFixedNativeSize() { return fixedNativeSize; }

  public abstract long computeSize(MachineDataInfo machDesc);
  public abstract long computeAlignment(MachineDataInfo machDesc);

  @Override
  public final int hashCode() {
      final int hash = 0x02DEAD6F; // magic hash start
      return ((hash << 5) - hash) + hashCodeImpl();
  }
  /* pp */ abstract int hashCodeImpl();

  @Override
  public final boolean equals(final Object arg) {
    if (arg == this) {
        return true;
    } else  if ( !(arg instanceof SizeThunk) ) {
        return false;
    } else {
        final SizeThunk t = (SizeThunk) arg;
        return hashCodeImpl() == t.hashCodeImpl();
    }
  }

  @Override
  public final int hashCodeSemantics() {
      final int hash = 0x01DEAD5F; // magic hash start
      return ((hash << 5) - hash) + hashCodeSemanticsImpl();
  }
  /* pp */ abstract int hashCodeSemanticsImpl();

  @Override
  public final boolean equalSemantics(final SemanticEqualityOp arg) {
    if (arg == this) {
        return true;
    } else  if ( !(arg instanceof SizeThunk) ) {
        return false;
    } else {
        final SizeThunk t = (SizeThunk) arg;
        return hashCodeSemanticsImpl() == t.hashCodeSemanticsImpl();
    }
  }

  static final int magic_int08   = 0x00000010;
  static final int magic_int16   = 0x00000012;
  static final int magic_int32   = 0x00000014;
  static final int magic_intxx   = 0x00000016;
  static final int magic_long64  = 0x00000020;
  static final int magic_longxx  = 0x00000022;
  static final int magic_float32 = 0x00000030;
  static final int magic_float64 = 0x00000032;
  static final int magic_aptr64  = 0x00000040;
  static final int magic_ops     = 0x00010000;

  public static final SizeThunk INT8 = new SizeThunk(true) {
      @Override
      public long computeSize(final MachineDataInfo machDesc) {
        return machDesc.int8SizeInBytes();
      }
      @Override
      public long computeAlignment(final MachineDataInfo machDesc) {
        return machDesc.int8AlignmentInBytes();
      }
      @Override
      protected int hashCodeImpl() { return 1; }
      @Override
      protected int hashCodeSemanticsImpl() { return relaxedEqSem ? magic_int32 : magic_int08; }
    };

  public static final SizeThunk INT16 = new SizeThunk(true) {
      @Override
      public long computeSize(final MachineDataInfo machDesc) {
        return machDesc.int16SizeInBytes();
      }
      @Override
      public long computeAlignment(final MachineDataInfo machDesc) {
        return machDesc.int16AlignmentInBytes();
      }
      @Override
      protected int hashCodeImpl() { return 2; }
      @Override
      protected int hashCodeSemanticsImpl() { return relaxedEqSem ? magic_int32 : magic_int16; }
    };

  public static final SizeThunk INT32 = new SizeThunk(true) {
      @Override
      public long computeSize(final MachineDataInfo machDesc) {
        return machDesc.int32SizeInBytes();
      }
      @Override
      public long computeAlignment(final MachineDataInfo machDesc) {
        return machDesc.int32AlignmentInBytes();
      }
      @Override
      protected int hashCodeImpl() { return 3; }
      @Override
      protected int hashCodeSemanticsImpl() { return magic_int32; }
    };

  public static final SizeThunk INTxx = new SizeThunk(false) {
      @Override
      public long computeSize(final MachineDataInfo machDesc) {
        return machDesc.intSizeInBytes();
      }
      @Override
      public long computeAlignment(final MachineDataInfo machDesc) {
        return machDesc.intAlignmentInBytes();
      }
      @Override
      protected int hashCodeImpl() { return 4; }
      @Override
      protected int hashCodeSemanticsImpl() { return relaxedEqSem ? magic_int32 : magic_intxx; }
    };

  public static final SizeThunk LONG = new SizeThunk(false) {
      @Override
      public long computeSize(final MachineDataInfo machDesc) {
        return machDesc.longSizeInBytes();
      }
      @Override
      public long computeAlignment(final MachineDataInfo machDesc) {
        return machDesc.longAlignmentInBytes();
      }
      @Override
      protected int hashCodeImpl() { return 5; }
      @Override
      protected int hashCodeSemanticsImpl() { return relaxedEqSem ? magic_long64 : magic_longxx; }
    };

  public static final SizeThunk INT64 = new SizeThunk(true) {
      @Override
      public long computeSize(final MachineDataInfo machDesc) {
        return machDesc.int64SizeInBytes();
      }
      @Override
      public long computeAlignment(final MachineDataInfo machDesc) {
        return machDesc.int64AlignmentInBytes();
      }
      @Override
      protected int hashCodeImpl() { return 6; }
      @Override
      protected int hashCodeSemanticsImpl() { return magic_long64; }
    };

  public static final SizeThunk FLOAT = new SizeThunk(true) {
      @Override
      public long computeSize(final MachineDataInfo machDesc) {
        return machDesc.floatSizeInBytes();
      }
      @Override
      public long computeAlignment(final MachineDataInfo machDesc) {
        return machDesc.floatAlignmentInBytes();
      }
      @Override
      protected int hashCodeImpl() { return 7; }
      @Override
      protected int hashCodeSemanticsImpl() { return magic_float32; }
    };

  public static final SizeThunk DOUBLE = new SizeThunk(true) {
      @Override
      public long computeSize(final MachineDataInfo machDesc) {
        return machDesc.doubleSizeInBytes();
      }
      @Override
      public long computeAlignment(final MachineDataInfo machDesc) {
        return machDesc.doubleAlignmentInBytes();
      }
      @Override
      protected int hashCodeImpl() { return 8; }
      @Override
      protected int hashCodeSemanticsImpl() { return magic_float64; }
    };

  public static final SizeThunk POINTER = new SizeThunk(false) {
      @Override
      public long computeSize(final MachineDataInfo machDesc) {
        return machDesc.pointerSizeInBytes();
      }
      @Override
      public long computeAlignment(final MachineDataInfo machDesc) {
        return machDesc.pointerAlignmentInBytes();
      }
      @Override
      protected int hashCodeImpl() { return 9; }
      @Override
      protected int hashCodeSemanticsImpl() { return magic_aptr64; }
    };

  // Factory methods for performing certain limited kinds of
  // arithmetic on these values
  public static SizeThunk add(final SizeThunk thunk1,
                              final SizeThunk thunk2) {
    return new SizeThunk(false) {
        @Override
        public long computeSize(final MachineDataInfo machDesc) {
          return thunk1.computeSize(machDesc) + thunk2.computeSize(machDesc);
        }
        @Override
        public long computeAlignment(final MachineDataInfo machDesc) {
          final long thunk1A = thunk1.computeAlignment(machDesc);
          final long thunk2A = thunk2.computeAlignment(machDesc);
          return ( thunk1A > thunk2A ) ? thunk1A : thunk2A ;
        }
        @Override
        protected int hashCodeImpl() {
            // 31 * x == (x << 5) - x
            int hash = 31 + 10;
            hash = ((hash << 5) - hash) + ( null != thunk1 ? thunk1.hashCode() : 0 );
            return ((hash << 5) - hash) + ( null != thunk2 ? thunk2.hashCode() : 0 );
        }
        @Override
        protected int hashCodeSemanticsImpl() { return magic_ops + 1; }
      };
  }

  public static SizeThunk mul(final SizeThunk thunk1,
                              final SizeThunk thunk2) {
    return new SizeThunk(false) {
        @Override
        public long computeSize(final MachineDataInfo machDesc) {
          return thunk1.computeSize(machDesc) * thunk2.computeSize(machDesc);
        }
        @Override
        public long computeAlignment(final MachineDataInfo machDesc) {
          final long thunk1A = thunk1.computeAlignment(machDesc);
          final long thunk2A = thunk2.computeAlignment(machDesc);
          return ( thunk1A > thunk2A ) ? thunk1A : thunk2A ;
        }
        @Override
        protected int hashCodeImpl() {
            // 31 * x == (x << 5) - x
            int hash = 31 + 11;
            hash = ((hash << 5) - hash) + ( null != thunk1 ? thunk1.hashCode() : 0 );
            return ((hash << 5) - hash) + ( null != thunk2 ? thunk2.hashCode() : 0 );
        }
        @Override
        protected int hashCodeSemanticsImpl() { return magic_ops + 2; }
      };
  }

  public static SizeThunk align(final SizeThunk offsetThunk,
                                final SizeThunk alignmentThunk) {
    return new SizeThunk(false) {
        @Override
        public long computeSize(final MachineDataInfo machDesc) {
          /**
           * padding = ( alignment - ( net_size % alignment ) ) % alignment ;
           * aligned_size = net_size + padding ;
           *
           * With x % 2n == x & (2n - 1)
           *
           * Either:
           *   remainder = net_size & ( alignment - 1 )
           *   padding = ( remainder > 0 ) ? alignment - remainder ;
           *   aligned_size = net_size + padding ;
           *
           * Or:
           *   padding = ( alignment - ( net_size & ( alignment - 1 ) ) ) & ( alignment - 1 );
           *   aligned_size = net_size + padding ;
           *
           */

          final long net_size = offsetThunk.computeSize(machDesc);
          final long alignment = alignmentThunk.computeAlignment(machDesc);

          /**
          final long remainder = net_size & ( alignment - 1 ) ;
          final long padding = (remainder > 0) ? alignment - remainder : 0;
           */
          final long padding = ( alignment - ( net_size & ( alignment - 1 ) ) ) & ( alignment - 1 );
          return net_size + padding;
        }

        @Override
        public long computeAlignment(final MachineDataInfo machDesc) {
          final long thunk1A = offsetThunk.computeAlignment(machDesc);
          final long thunk2A = alignmentThunk.computeAlignment(machDesc);
          return ( thunk1A > thunk2A ) ? thunk1A : thunk2A ;
        }
        @Override
        protected int hashCodeImpl() {
            // 31 * x == (x << 5) - x
            int hash = 31 + 12;
            hash = ((hash << 5) - hash) + ( null != offsetThunk ? offsetThunk.hashCode() : 0 );
            return ((hash << 5) - hash) + ( null != alignmentThunk ? alignmentThunk.hashCode() : 0 );
        }
        @Override
        protected int hashCodeSemanticsImpl() { return magic_ops + 3; }
      };
  }

  public static SizeThunk max(final SizeThunk thunk1,
                              final SizeThunk thunk2) {
    return new SizeThunk(false) {
        @Override
        public long computeSize(final MachineDataInfo machDesc) {
          return Math.max(thunk1.computeSize(machDesc), thunk2.computeSize(machDesc));
        }
        @Override
        public long computeAlignment(final MachineDataInfo machDesc) {
          final long thunk1A = thunk1.computeAlignment(machDesc);
          final long thunk2A = thunk2.computeAlignment(machDesc);
          return ( thunk1A > thunk2A ) ? thunk1A : thunk2A ;
        }
        @Override
        protected int hashCodeImpl() {
            // 31 * x == (x << 5) - x
            int hash = 31 + 13;
            hash = ((hash << 5) - hash) + ( null != thunk1 ? thunk1.hashCode() : 0 );
            return ((hash << 5) - hash) + ( null != thunk2 ? thunk2.hashCode() : 0 );
        }
        @Override
        protected int hashCodeSemanticsImpl() { return magic_ops + 4; }
      };
  }

  public static SizeThunk constant(final int constant) {
    return new SizeThunk(false) {
        @Override
        public long computeSize(final MachineDataInfo machDesc) {
          return constant;
        }
        @Override
        public long computeAlignment(final MachineDataInfo machDesc) {
          return 1; // no alignment for constants
        }
        @Override
        protected int hashCodeImpl() {
            // 31 * x == (x << 5) - x
            final int hash = 31 + 14;
            return ((hash << 5) - hash) + constant;
        }
        @Override
        protected int hashCodeSemanticsImpl() { return magic_ops + 5; }
      };
  }
}
