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
 */
package com.jogamp.common.nio;

import com.jogamp.common.os.Platform;
import java.nio.*;

/**
 * @author Sven Gothel
 * @author Michael Bien
 */
final class PointerBufferSE extends PointerBuffer {

    private Buffer pb;

    PointerBufferSE(ByteBuffer bb) {
        super(bb);

        if (Platform.is32Bit()) {
            this.pb = bb.asIntBuffer();
        } else {
            this.pb = bb.asLongBuffer();
        }
    }

    public final long get(int idx) {
        if (0 > idx || idx >= capacity) {
            throw new IndexOutOfBoundsException();
        }
        if (Platform.is32Bit()) {
            return (long) ((IntBuffer) pb).get(idx)  & 0x00000000FFFFFFFFL;
        } else {
            return ((LongBuffer) pb).get(idx);
        }
    }

    public final AbstractLongBuffer put(int idx, long v) {
        if (0 > idx || idx >= capacity) {
            throw new IndexOutOfBoundsException();
        }
        backup[idx] = v;
        if (Platform.is32Bit()) {
            ((IntBuffer) pb).put(idx, (int) v);
        } else {
            ((LongBuffer) pb).put(idx, v);
        }
        return this;
    }

}
