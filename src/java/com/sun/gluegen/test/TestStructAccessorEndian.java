
package com.sun.gluegen.test;

import com.sun.gluegen.runtime.*;
import java.nio.*;

public class TestStructAccessorEndian {
    public static void main (String args[]) {
        boolean ok = true;
        System.out.println("CPU is : "+ (BufferFactory.isLittleEndian()?"little":"big") + " endian");
        ByteBuffer tst = BufferFactory.newDirectByteBuffer(BufferFactory.SIZEOF_LONG * 3);
        StructAccessor acc = new StructAccessor(tst);
        acc.setLongAt(0, 0x0123456789ABCDEFL);
        acc.setLongAt(1, 0x8877665544332211L);
        acc.setLongAt(2, 0xAFFEDEADBEEFAFFEL);
        long v = acc.getLongAt(0);
        if( 0x0123456789ABCDEFL != v ) {
            System.out.println("Err[0] shall 0x0123456789ABCDEF, is: "+Long.toHexString(v));
            ok=false;
        }
        v = acc.getLongAt(1);
        if( 0x8877665544332211L != v ) {
            System.out.println("Err[1] shall 0x8877665544332211, is: "+Long.toHexString(v));
            ok=false;
        }
        v = acc.getLongAt(2);
        if( 0xAFFEDEADBEEFAFFEL != v ) {
            System.out.println("Err[2] shall 0xAFFEDEADBEEFAFFE, is: "+Long.toHexString(v));
            ok=false;
        }
        if(!ok) {
            throw new RuntimeException("Long conversion failure");
        }
    }
}
