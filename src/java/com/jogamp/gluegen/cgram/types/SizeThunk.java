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

import com.jogamp.common.os.MachineDescription;

/** Provides a level of indirection between the definition of a type's
    size and the absolute value of this size. Necessary when
    generating glue code for two different CPU architectures (e.g.,
    32-bit and 64-bit) from the same internal representation of the
    various types involved. */
public abstract class SizeThunk implements Cloneable {
  private final boolean fixedNativeSize;

  // Private constructor because there are only a few of these
  private SizeThunk(final boolean fixedNativeSize) { this.fixedNativeSize = fixedNativeSize; }

  @Override
  public Object clone() {
    try {
        return super.clone();
    } catch (final CloneNotSupportedException ex) {
        throw new InternalError();
    }
  }

  public final boolean hasFixedNativeSize() { return fixedNativeSize; }

  public abstract long computeSize(MachineDescription machDesc);
  public abstract long computeAlignment(MachineDescription machDesc);

  public static final SizeThunk INT8 = new SizeThunk(true) {
      @Override
      public long computeSize(final MachineDescription machDesc) {
        return machDesc.int8SizeInBytes();
      }
      @Override
      public long computeAlignment(final MachineDescription machDesc) {
        return machDesc.int8AlignmentInBytes();
      }
    };

  public static final SizeThunk INT16 = new SizeThunk(true) {
      @Override
      public long computeSize(final MachineDescription machDesc) {
        return machDesc.int16SizeInBytes();
      }
      @Override
      public long computeAlignment(final MachineDescription machDesc) {
        return machDesc.int16AlignmentInBytes();
      }
    };

  public static final SizeThunk INT32 = new SizeThunk(true) {
      @Override
      public long computeSize(final MachineDescription machDesc) {
        return machDesc.int32SizeInBytes();
      }
      @Override
      public long computeAlignment(final MachineDescription machDesc) {
        return machDesc.int32AlignmentInBytes();
      }
    };

  public static final SizeThunk INTxx = new SizeThunk(false) {
      @Override
      public long computeSize(final MachineDescription machDesc) {
        return machDesc.intSizeInBytes();
      }
      @Override
      public long computeAlignment(final MachineDescription machDesc) {
        return machDesc.intAlignmentInBytes();
      }
    };

  public static final SizeThunk LONG = new SizeThunk(false) {
      @Override
      public long computeSize(final MachineDescription machDesc) {
        return machDesc.longSizeInBytes();
      }
      @Override
      public long computeAlignment(final MachineDescription machDesc) {
        return machDesc.longAlignmentInBytes();
      }
    };

  public static final SizeThunk INT64 = new SizeThunk(true) {
      @Override
      public long computeSize(final MachineDescription machDesc) {
        return machDesc.int64SizeInBytes();
      }
      @Override
      public long computeAlignment(final MachineDescription machDesc) {
        return machDesc.int64AlignmentInBytes();
      }
    };

  public static final SizeThunk FLOAT = new SizeThunk(true) {
      @Override
      public long computeSize(final MachineDescription machDesc) {
        return machDesc.floatSizeInBytes();
      }
      @Override
      public long computeAlignment(final MachineDescription machDesc) {
        return machDesc.floatAlignmentInBytes();
      }
    };

  public static final SizeThunk DOUBLE = new SizeThunk(true) {
      @Override
      public long computeSize(final MachineDescription machDesc) {
        return machDesc.doubleSizeInBytes();
      }
      @Override
      public long computeAlignment(final MachineDescription machDesc) {
        return machDesc.doubleAlignmentInBytes();
      }
    };

  public static final SizeThunk POINTER = new SizeThunk(false) {
      @Override
      public long computeSize(final MachineDescription machDesc) {
        return machDesc.pointerSizeInBytes();
      }
      @Override
      public long computeAlignment(final MachineDescription machDesc) {
        return machDesc.pointerAlignmentInBytes();
      }
    };

  // Factory methods for performing certain limited kinds of
  // arithmetic on these values
  public static SizeThunk add(final SizeThunk thunk1,
                              final SizeThunk thunk2) {
    return new SizeThunk(false) {
        @Override
        public long computeSize(final MachineDescription machDesc) {
          return thunk1.computeSize(machDesc) + thunk2.computeSize(machDesc);
        }
        @Override
        public long computeAlignment(final MachineDescription machDesc) {
          final long thunk1A = thunk1.computeAlignment(machDesc);
          final long thunk2A = thunk2.computeAlignment(machDesc);
          return ( thunk1A > thunk2A ) ? thunk1A : thunk2A ;
        }
      };
  }

  public static SizeThunk mul(final SizeThunk thunk1,
                              final SizeThunk thunk2) {
    return new SizeThunk(false) {
        @Override
        public long computeSize(final MachineDescription machDesc) {
          return thunk1.computeSize(machDesc) * thunk2.computeSize(machDesc);
        }
        @Override
        public long computeAlignment(final MachineDescription machDesc) {
          final long thunk1A = thunk1.computeAlignment(machDesc);
          final long thunk2A = thunk2.computeAlignment(machDesc);
          return ( thunk1A > thunk2A ) ? thunk1A : thunk2A ;
        }
      };
  }

  public static SizeThunk align(final SizeThunk offsetThunk,
                                final SizeThunk alignmentThunk) {
    return new SizeThunk(false) {
        @Override
        public long computeSize(final MachineDescription machDesc) {
          // x % 2n == x & (2n - 1)
          // remainder = net_size & ( alignment - 1 )
          // padding = alignment - remainder ;
          // aligned_size = net_size + padding ;

          final long size = offsetThunk.computeSize(machDesc);
          final long alignment = alignmentThunk.computeAlignment(machDesc);

          final long remainder = size & ( alignment - 1 ) ;
          final long padding = (remainder > 0) ? alignment - remainder : 0;
          return size + padding;
        }

        @Override
        public long computeAlignment(final MachineDescription machDesc) {
          final long thunk1A = offsetThunk.computeAlignment(machDesc);
          final long thunk2A = alignmentThunk.computeAlignment(machDesc);
          return ( thunk1A > thunk2A ) ? thunk1A : thunk2A ;
        }
      };
  }

  public static SizeThunk max(final SizeThunk thunk1,
                              final SizeThunk thunk2) {
    return new SizeThunk(false) {
        @Override
        public long computeSize(final MachineDescription machDesc) {
          return Math.max(thunk1.computeSize(machDesc), thunk2.computeSize(machDesc));
        }
        @Override
        public long computeAlignment(final MachineDescription machDesc) {
          final long thunk1A = thunk1.computeAlignment(machDesc);
          final long thunk2A = thunk2.computeAlignment(machDesc);
          return ( thunk1A > thunk2A ) ? thunk1A : thunk2A ;
        }
      };
  }

  public static SizeThunk constant(final int constant) {
    return new SizeThunk(false) {
        @Override
        public long computeSize(final MachineDescription machDesc) {
          return constant;
        }
        @Override
        public long computeAlignment(final MachineDescription machDesc) {
          return 1; // no alignment for constants
        }
      };
  }
}
