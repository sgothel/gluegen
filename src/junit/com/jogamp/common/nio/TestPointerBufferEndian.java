
package com.jogamp.common.nio;

import java.io.IOException;

import com.jogamp.common.os.*;
import com.jogamp.junit.util.JunitTracer;

import org.junit.Assert;
import org.junit.Test;

import static java.lang.System.*;

public class TestPointerBufferEndian extends JunitTracer {

    protected void testImpl (boolean direct) {
        final MachineDescription machine = Platform.getMachineDescription();
        int bitsPtr = machine.pointerSizeInBytes() * 8;
        String bitsProp = System.getProperty("sun.arch.data.model");
        out.println("OS: <"+Platform.OS+"> CPU: <"+Platform.ARCH+"> Bits: <"+bitsPtr+"/"+bitsProp+">");
        out.println(machine.toString());

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
    
    public static void main(String args[]) throws IOException {
        String tstname = TestPointerBufferEndian.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }    
}
