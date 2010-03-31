package com.jogamp.gluegen.test.junit.runtime;

import com.jogamp.common.nio.*;
import com.jogamp.common.os.*;

import java.nio.*;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.lang.System.*;

public class TestStructAccessorEndian {

    @Test
    public void testStructAccessorEndian1 () {
        int bitsPtr = Platform.getPointerSizeInBits();
        String bitsProp = System.getProperty("sun.arch.data.model");
        String os = System.getProperty("os.name");
        String cpu = System.getProperty("os.arch");
        System.out.println("OS: <"+os+"> CPU: <"+cpu+"> Bits: <"+bitsPtr+"/"+bitsProp+">");
        System.out.println("CPU is: "+ (Platform.is32Bit()?"32":"64") + " bit");
        System.out.println("Buffer is in: "+ (Platform.isLittleEndian()?"little":"big") + " endian");

        long[] valuesSource = { 0x0123456789ABCDEFL, 0x8877665544332211L, 0xAFFEDEADBEEFAFFEL };
        ByteBuffer tst = Buffers.newDirectByteBuffer(Buffers.SIZEOF_LONG * valuesSource.length);
        StructAccessor acc = new StructAccessor(tst);

        int i;

        for(i=0; i<valuesSource.length; i++) {
            acc.setLongAt(i, valuesSource[i]);
        }

        for(i=0; i<valuesSource.length; i++) {
            long v = acc.getLongAt(i);
            long t = valuesSource[i];
            Assert.assertTrue("Value["+i+"] shall be 0x"+Long.toHexString(t)+", is: 0x"+Long.toHexString(v), t == v);
        }
    }
}
