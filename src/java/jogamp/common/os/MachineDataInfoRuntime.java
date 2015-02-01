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

import com.jogamp.common.os.MachineDataInfo;
import com.jogamp.common.os.Platform;
import com.jogamp.common.os.MachineDataInfo.StaticConfig;

/**
 * Runtime operations of {@link MachineDataInfo}.
 */
public class MachineDataInfoRuntime {

  static volatile boolean initialized = false;
  static volatile MachineDataInfo runtimeMD = null;
  static volatile MachineDataInfo.StaticConfig staticMD = null;

  public static void initialize() {
      if( !initialized ) {
          synchronized(MachineDataInfo.class) { // volatile dbl-checked-locking OK
              if( !initialized ) {
                  MachineDataInfo.StaticConfig.validateUniqueMachineDataInfo();

                  final MachineDataInfo runtimeMD = getRuntimeImpl();
                  final MachineDataInfo.StaticConfig staticMD = MachineDataInfo.StaticConfig.findCompatible(runtimeMD);
                  if( null == staticMD ) {
                      throw new RuntimeException("No compatible MachineDataInfo.StaticConfig for runtime:"+PlatformPropsImpl.NEWLINE+runtimeMD);
                  }
                  if( !staticMD.md.compatible(runtimeMD) ) {
                      throw new RuntimeException("Incompatible MachineDataInfo:"+PlatformPropsImpl.NEWLINE+
                                                 " Static "+staticMD+PlatformPropsImpl.NEWLINE+
                                                 " Runtime "+runtimeMD);
                  }
                  MachineDataInfoRuntime.runtimeMD = runtimeMD;
                  MachineDataInfoRuntime.staticMD = staticMD;
                  initialized=true;
                  if( PlatformPropsImpl.DEBUG ) {
                      System.err.println("MachineDataInfoRuntime.initialize():"+PlatformPropsImpl.NEWLINE+
                                         " Static "+staticMD+PlatformPropsImpl.NEWLINE+
                                         " Runtime "+runtimeMD);
                  }
                  return;
              }
          }
      }
      throw new InternalError("Already initialized");
  }
  public static MachineDataInfo.StaticConfig getStatic() {
      if(!initialized) {
          synchronized(MachineDataInfo.class) { // volatile dbl-checked-locking OK
              if(!initialized) {
                  throw new InternalError("Not set");
              }
          }
      }
      return staticMD;
  }
  public static MachineDataInfo getRuntime() {
      if(!initialized) {
          synchronized(MachineDataInfo.class) { // volatile dbl-checked-locking OK
              if(!initialized) {
                  throw new InternalError("Not set");
              }
          }
      }
      return runtimeMD;
  }

  public static MachineDataInfo.StaticConfig guessStaticMachineDataInfo(final Platform.OSType osType, final Platform.CPUType cpuType) {
      if( cpuType.is32Bit ) {
          if( Platform.CPUFamily.ARM == cpuType.family ||
              Platform.CPUType.MIPS_32 == cpuType ) {
              return StaticConfig.ARM_MIPS_32;
          } else if( Platform.OSType.WINDOWS == osType ) {
              return StaticConfig.X86_32_WINDOWS;
          } else if( Platform.OSType.MACOS == osType ) {
              return StaticConfig.X86_32_MACOS;
          } else if ( Platform.OSType.SUNOS == osType &&
                      Platform.CPUType.SPARC_32 == cpuType ) {
              return StaticConfig.SPARC_32_SUNOS;
          } else if ( Platform.CPUType.PPC == cpuType ) {
              return StaticConfig.PPC_32_UNIX;
          } else {
              return StaticConfig.X86_32_UNIX;
          }
      } else {
          if( osType == Platform.OSType.WINDOWS ) {
              return StaticConfig.X86_64_WINDOWS;
          } else {
              // for all 64bit unix types (x86_64, aarch64, sparcv9, ..)
              return StaticConfig.LP64_UNIX;
          }
      }
  }

  private static MachineDataInfo getRuntimeImpl() {
        try {
            Platform.initSingleton(); // loads native gluegen-rt library
        } catch (final UnsatisfiedLinkError err) {
            return null;
        }

        final int pointerSizeInBytes = getPointerSizeInBytesImpl();
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

        // size:      int, long, float, double, pointer, pageSize
        // alignment: int8, int16, int32, int64, int, long, float, double, pointer
        return new MachineDataInfo(
            true /* runtime validated */,

            getSizeOfIntImpl(), getSizeOfLongImpl(),
            getSizeOfFloatImpl(), getSizeOfDoubleImpl(), getSizeOfLongDoubleImpl(),
            pointerSizeInBytes, (int)pageSizeL,

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

