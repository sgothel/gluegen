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
package com.jogamp.common.nio;

import java.nio.*;

/**
 * @author Kenneth Russel, et al.
 */
public class StructAccessor {

    private final ByteBuffer bb;

    public StructAccessor(ByteBuffer bb) {
        // Setting of byte order is concession to native code which needs
        // to instantiate these
        this.bb = bb.order(ByteOrder.nativeOrder());
    }

    public final ByteBuffer getBuffer() {
        return bb;
    }

    /**
     * Returns a slice of the current ByteBuffer starting at the
     * specified byte offset and extending the specified number of
     * bytes. Note that this method is not thread-safe with respect to
     * the other methods in this class.
     */
    public final ByteBuffer slice(int byteOffset, int byteLength) {
        bb.position(byteOffset);
        bb.limit(byteOffset + byteLength);
        final ByteBuffer newBuf = bb.slice().order(bb.order()); // slice and duplicate may change byte order
        bb.position(0);
        bb.limit(bb.capacity());
        return newBuf;
    }

    /** Retrieves the byte at the specified byteOffset. */
    public final byte getByteAt(int byteOffset) {
        return bb.get(byteOffset);
    }

    /** Puts a byte at the specified byteOffset. */
    public final void setByteAt(int byteOffset, byte v) {
        bb.put(byteOffset, v);
    }

    /** Retrieves the char at the specified byteOffset. */
    public final char getCharAt(int byteOffset) {
        return bb.getChar(byteOffset);
    }

    /** Puts a char at the specified byteOffset. */
    public final void setCharAt(int byteOffset, char v) {
        bb.putChar(byteOffset, v);
    }

    /** Retrieves the short at the specified byteOffset. */
    public final short getShortAt(int byteOffset) {
        return bb.getShort(byteOffset);
    }

    /** Puts a short at the specified byteOffset. */
    public final void setShortAt(int byteOffset, short v) {
        bb.putShort(byteOffset, v);
    }

    /** Retrieves the int at the specified byteOffset. */
    public final int getIntAt(int byteOffset) {
        return bb.getInt(byteOffset);
    }

    /** Puts a int at the specified byteOffset. */
    public final void setIntAt(int byteOffset, int v) {
        bb.putInt(byteOffset, v);
    }

    /** Retrieves the int at the specified byteOffset. */
    public final int getIntAt(int byteOffset, int nativeSizeInBytes) {
        switch(nativeSizeInBytes) {
            case 2:
                return (int) bb.getShort(byteOffset) & 0x0000FFFF ;
            case 4:
                return bb.getInt(byteOffset);
            case 8:
                return (int) ( bb.getLong(byteOffset) & 0x00000000FFFFFFFFL ) ;
            default:
                throw new InternalError("invalid nativeSizeInBytes "+nativeSizeInBytes);
        }
    }

    /** Puts a int at the specified byteOffset. */
    public final void setIntAt(int byteOffset, int v, int nativeSizeInBytes) {
        switch(nativeSizeInBytes) {
            case 2:
                bb.putShort(byteOffset, (short) ( v & 0x0000FFFF ) );
                break;
            case 4:
                bb.putInt(byteOffset, v);
                break;
            case 8:
                bb.putLong(byteOffset, (long)v & 0x00000000FFFFFFFFL );
                break;
            default:
                throw new InternalError("invalid nativeSizeInBytes "+nativeSizeInBytes);
        }
    }

    /** Retrieves the float at the specified byteOffset. */
    public final float getFloatAt(int byteOffset) {
        return bb.getFloat(byteOffset);
    }

    /** Puts a float at the specified byteOffset. */
    public final void setFloatAt(int byteOffset, float v) {
        bb.putFloat(byteOffset, v);
    }

    /** Retrieves the double at the specified byteOffset. */
    public final double getDoubleAt(int byteOffset) {
        return bb.getDouble(byteOffset);
    }

    /** Puts a double at the specified byteOffset. */
    public final void setDoubleAt(int byteOffset, double v) {
        bb.putDouble(byteOffset, v);
    }

    /** Retrieves the long at the specified byteOffset. */
    public final long getLongAt(int byteOffset) {
        return bb.getLong(byteOffset);
    }

    /** Puts a long at the specified byteOffset. */
    public final void setLongAt(int byteOffset, long v) {
        bb.putLong(byteOffset, v);
    }

    /** Retrieves the long at the specified byteOffset. */
    public final long getLongAt(int byteOffset, int nativeSizeInBytes) {
        switch(nativeSizeInBytes) {
            case 4:
                return (long) bb.getInt(byteOffset) & 0x00000000FFFFFFFFL;
            case 8:
                return bb.getLong(byteOffset);
            default:
                throw new InternalError("invalid nativeSizeInBytes "+nativeSizeInBytes);
        }
    }

    /** Puts a long at the specified byteOffset. */
    public final void setLongAt(int byteOffset, long v, int nativeSizeInBytes) {
        switch(nativeSizeInBytes) {
            case 4:
                bb.putInt(byteOffset, (int) ( v & 0x00000000FFFFFFFFL ) );
                break;
            case 8:
                bb.putLong(byteOffset, v);
                break;
            default:
                throw new InternalError("invalid nativeSizeInBytes "+nativeSizeInBytes);
        }
    }

    public final void setBytesAt(int byteOffset, byte[] v) {
        for (int i = 0; i < v.length; i++) {
            bb.put(byteOffset++, v[i]);
        }
    }

    public final byte[] getBytesAt(int byteOffset, byte[] v) {
        for (int i = 0; i < v.length; i++) {
            v[i] = bb.get(byteOffset++);
        }
        return v;
    }

    public final void setCharsAt(int byteOffset, char[] v) {
        for (int i = 0; i < v.length; i++, byteOffset+=2) {
            bb.putChar(byteOffset, v[i]);
        }
    }

    public final char[] getCharsAt(int byteOffset, char[] v) {
        for (int i = 0; i < v.length; i++, byteOffset+=2) {
            v[i] = bb.getChar(byteOffset);
        }
        return v;
    }

    public final void setShortsAt(int byteOffset, short[] v) {
        for (int i = 0; i < v.length; i++, byteOffset+=2) {
            bb.putShort(byteOffset, v[i]);
        }
    }

    public final short[] getShortsAt(int byteOffset, short[] v) {
        for (int i = 0; i < v.length; i++, byteOffset+=2) {
            v[i] = bb.getShort(byteOffset);
        }
        return v;
    }

    public final void setIntsAt(int byteOffset, int[] v) {
        for (int i = 0; i < v.length; i++, byteOffset+=4) {
            bb.putInt(byteOffset, v[i]);
        }
    }

    public final int[] getIntsAt(int byteOffset, int[] v) {
        for (int i = 0; i < v.length; i++, byteOffset+=4) {
            v[i] = bb.getInt(byteOffset);
        }
        return v;
    }

    public final void setFloatsAt(int byteOffset, float[] v) {
        for (int i = 0; i < v.length; i++, byteOffset+=4) {
            bb.putFloat(byteOffset, v[i]);
        }
    }

    public final float[] getFloatsAt(int byteOffset, float[] v) {
        for (int i = 0; i < v.length; i++, byteOffset+=4) {
            v[i] = bb.getFloat(byteOffset);
        }
        return v;
    }

    public final void setDoublesAt(int byteOffset, double[] v) {
        for (int i = 0; i < v.length; i++, byteOffset+=8) {
            bb.putDouble(byteOffset, v[i]);
        }
    }

    public final double[] getDoublesAt(int byteOffset, double[] v) {
        for (int i = 0; i < v.length; i++, byteOffset+=8) {
            v[i] = bb.getDouble(byteOffset);
        }
        return v;
    }

    public final void setLongsAt(int byteOffset, long[] v) {
        for (int i = 0; i < v.length; i++, byteOffset+=8) {
            bb.putLong(byteOffset, v[i]);
        }
    }

    public final long[] getLongsAt(int byteOffset, long[] v) {
        for (int i = 0; i < v.length; i++, byteOffset+=8) {
            v[i] = bb.getLong(byteOffset);
        }
        return v;
    }
}
