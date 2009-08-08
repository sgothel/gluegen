
package com.sun.gluegen.test;

import com.sun.gluegen.runtime.*;
import java.nio.*;

public class TestPointerBufferEndian {
    public static void main (String[] args) {
        boolean direct = args.length>0 && args[0].equals("-direct");
        boolean ok = true;
        int bitsPtr = CPU.getPointerSizeInBits();
        String bitsProp = System.getProperty("sun.arch.data.model");
        String os = System.getProperty("os.name");
        String cpu = System.getProperty("os.arch");
        System.out.println("OS: <"+os+"> CPU: <"+cpu+"> Bits: <"+bitsPtr+"/"+bitsProp+">");
        System.out.println("CPU is: "+ (CPU.is32Bit()?"32":"64") + " bit");
        System.out.println("Buffer is in: "+ (BufferFactory.isLittleEndian()?"little":"big") + " endian");
        PointerBuffer ptr = direct ? PointerBuffer.allocateDirect(3) : PointerBuffer.allocate(3);
        ptr.put(0, 0x0123456789ABCDEFL);
        ptr.put(1, 0x8877665544332211L);
        ptr.put(2, 0xAFFEDEADBEEFAFFEL);
        long v = ptr.get(0);
        if( 0x0123456789ABCDEFL != v ) {
            System.out.println("Err[0] shall 0x0123456789ABCDEF, is: "+Long.toHexString(v));
            ok=false;
        }
        v = ptr.get(1);
        if( 0x8877665544332211L != v ) {
            System.out.println("Err[1] shall 0x8877665544332211, is: "+Long.toHexString(v));
            ok=false;
        }
        v = ptr.get(2);
        if( 0xAFFEDEADBEEFAFFEL != v ) {
            System.out.println("Err[2] shall 0xAFFEDEADBEEFAFFE, is: "+Long.toHexString(v));
            ok=false;
        }
        if(!ok) {
            throw new RuntimeException("Long conversion failure");
        }
    }
}
