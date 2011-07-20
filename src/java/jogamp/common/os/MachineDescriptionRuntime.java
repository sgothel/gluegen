/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
 
package jogamp.common.os;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.os.MachineDescription;
import com.jogamp.common.os.NativeLibrary;
import com.jogamp.common.os.Platform;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Runtime MachineDescription
 */
public class MachineDescriptionRuntime {

  public static MachineDescription getMachineDescription(boolean is32BitByCPUArch) {
        boolean libsLoaded = true;
        try {
            NativeLibrary.ensureNativeLibLoaded();
        } catch (UnsatisfiedLinkError err) {
            libsLoaded = false;
        }
        
        if(libsLoaded) {
            int pointerSizeInBytes = getPointerSizeInBytesImpl();
            switch(pointerSizeInBytes) {
                case 4:
                case 8:
                    break;
                default:
                    throw new RuntimeException("Unsupported pointer size "+pointerSizeInBytes+"bytes, please implement.");
            }

            final long pageSizeL =  getPageSizeInBytesImpl();
            if(Integer.MAX_VALUE < pageSizeL) {
                throw new InternalError("PageSize exceeds integer value: " + pageSizeL);
            }
            
            return getMachineDescriptionImpl(pointerSizeInBytes, (int) pageSizeL);
        } else {
            return MachineDescription.createStatic(is32BitByCPUArch);
        }
    }

    private static MachineDescription getMachineDescriptionImpl(int pointerSize, int pageSize) {
        // size:      int, long, float, double, pointer, pageSize
        // alignment: int8, int16, int32, int64, int, long, float, double, pointer
        return new MachineDescription( 
            true /* runtime validated */, MachineDescription.queryIsLittleEndian(),
            
            getSizeOfIntImpl(), getSizeOfLongImpl(),
            getSizeOfFloatImpl(), getSizeOfDoubleImpl(), getSizeOfLongDoubleImpl(), pointerSize, pageSize,
            
            getAlignmentInt8Impl(), getAlignmentInt16Impl(), getAlignmentInt32Impl(), getAlignmentInt64Impl(),
            getAlignmentIntImpl(), getAlignmentLongImpl(), 
            getAlignmentFloatImpl(), getAlignmentDoubleImpl(), getAlignmentLongDoubleImpl(), 
            getAlignmentPointerImpl());        
    }
    
    private static native int getPointerSizeInBytesImpl();
    private static native long getPageSizeInBytesImpl();
    
    private static native int getAlignmentInt8Impl();
    private static native int getAlignmentInt16Impl();
    private static native int getAlignmentInt32Impl();
    private static native int getAlignmentInt64Impl();
    private static native int getAlignmentIntImpl();
    private static native int getAlignmentLongImpl();
    private static native int getAlignmentPointerImpl();
    private static native int getAlignmentFloatImpl();
    private static native int getAlignmentDoubleImpl();
    private static native int getAlignmentLongDoubleImpl();
    private static native int getSizeOfIntImpl();
    private static native int getSizeOfLongImpl();
    private static native int getSizeOfPointerImpl();
    private static native int getSizeOfFloatImpl();
    private static native int getSizeOfDoubleImpl();    
    private static native int getSizeOfLongDoubleImpl();    
}

