
package com.jogamp.common.nio;

import java.io.IOException;

import jogamp.common.os.PlatformPropsImpl;

import com.jogamp.common.os.*;
import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.Assert;
import org.junit.Test;

import static java.lang.System.*;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPointerBufferEndian extends SingletonJunitCase {

    protected void testImpl (final boolean direct) {
        final MachineDataInfo machine = Platform.getMachineDataInfo();
        final int bitsPtr = machine.pointerSizeInBytes() * 8;
        final String bitsProp = System.getProperty("sun.arch.data.model");
        out.println("OS: <"+PlatformPropsImpl.OS+"> CPU: <"+PlatformPropsImpl.ARCH+"> Bits: <"+bitsPtr+"/"+bitsProp+">");
        out.println(machine.toString());

        final long[] valuesSource = { 0x0123456789ABCDEFL, 0x8877665544332211L, 0xAFFEDEADBEEFAFFEL };
        final long[] values32Bit  = { 0x0000000089ABCDEFL, 0x0000000044332211L, 0x00000000BEEFAFFEL };

        final PointerBuffer ptr = direct ? PointerBuffer.allocateDirect(3) : PointerBuffer.allocate(valuesSource.length);
        ptr.put(valuesSource, 0, valuesSource.length);
        ptr.rewind();

        int i=0;
        while(ptr.hasRemaining()) {
            final long v = ptr.get() ;
            final long t = Platform.is32Bit() ? values32Bit[i] : valuesSource[i];
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

    public static void main(final String args[]) throws IOException {
        final String tstname = TestPointerBufferEndian.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
