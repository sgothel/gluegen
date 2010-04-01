
package com.jogamp.gluegen.test.junit.runtime;

import com.jogamp.common.nio.*;
import com.jogamp.common.os.*;

import java.nio.*;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.lang.System.*;

public class TestPointerBufferEndian {

    protected void testImpl (boolean direct) {
        int bitsPtr = Platform.getPointerSizeInBits();
        String bitsProp = System.getProperty("sun.arch.data.model");
        String os = System.getProperty("os.name");
        String cpu = System.getProperty("os.arch");
        System.out.println("OS: <"+os+"> CPU: <"+cpu+"> Bits: <"+bitsPtr+"/"+bitsProp+">");
        System.out.println("CPU is: "+ (Platform.is32Bit()?"32":"64") + " bit");
        System.out.println("Buffer is in: "+ (Platform.isLittleEndian()?"little":"big") + " endian");

        long[] valuesSource = { 0x0123456789ABCDEFL, 0x8877665544332211L, 0xAFFEDEADBEEFAFFEL };
        long[] values32Bit  = { 0x0000000089ABCDEFL, 0x0000000044332211L, 0x00000000BEEFAFFEL };

        PointerBuffer ptr = direct ? PointerBuffer.allocateDirect(3) : PointerBuffer.allocate(valuesSource.length);
        ptr.put(valuesSource, 0, valuesSource.length);
        ptr.rewind();

        int i=0;
        while(ptr.hasRemaining()) {
            long v = ptr.get() ;
            long t = Platform.is32Bit() ? values32Bit[i] : valuesSource[i];
            Assert.assertTrue("Value["+i+"] shall be 0x"+Long.toHexString(t)+", is: 0x"+Long.toHexString(v), t == v);
            i++;
        }
        Assert.assertTrue("iterator "+i+" != "+valuesSource.length, i==valuesSource.length);
    }

    @Test
    public void testDirect () {
        testImpl (true);
    }

    @Test
    public void testIndirect () {
        testImpl (false);
    }
}
