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
 
package com.jogamp.common.os;

import java.security.AccessController;
import java.security.PrivilegedAction;

import jogamp.common.os.MachineDescriptionRuntime;

/**
 * Utility class for querying platform specific properties.
 * @author Michael Bien
 * @author Sven Gothel
 */
public class Platform {

    public static final boolean JAVA_SE;
    public static final String OS;
    public static final String OS_lower;
    public static final String OS_VERSION;
    public static final String ARCH;
    public static final String ARCH_lower;
    public static final String JAVA_VENDOR;
    public static final String JAVA_VENDOR_URL;
    public static final String JAVA_VERSION;
    public static final String NEWLINE;

    public enum OSType {
        LINUX(0), FREEBSD(1), DALVIK(2), MACOS(3), SUNOS(4), HPUX(5), WINDOWS(6), OPENKODE(7); 
        
        public final int id;

        OSType(int id){
            this.id = id;
        }
    }    
    public static final OSType OS_TYPE;
    
    public enum CPUType {
        X86(0), IA(1), ARM(2), SPARC(3), PA_RISC(4), PPC(5);
        
        public final int id;

        CPUType(int id){
            this.id = id;
        }
    }    
    public static final CPUType CPU_TYPE;

    public enum CPUArch {
        X86_32(0), X86_64(1), IA64(2), ARM_32(3), SPARC_32(4), SPARCV9_64(5), PA_RISC2_0(6), PPC(7);
        
        public final int id;

        CPUArch(int id){
            this.id = id;
        }
    }      
    public static final CPUArch CPU_ARCH;
    
    private static final boolean is32Bit;

    private static final MachineDescription machineDescription;
    
    static {
        
        // We don't seem to need an AccessController.doPrivileged() block
        // here as these system properties are visible even to unsigned
        // applets
        OS =  System.getProperty("os.name");
        OS_lower = OS.toLowerCase();
        OS_VERSION =  System.getProperty("os.version");
        ARCH = System.getProperty("os.arch");
        ARCH_lower = ARCH.toLowerCase();
        JAVA_VENDOR = System.getProperty("java.vendor");
        JAVA_VENDOR_URL = System.getProperty("java.vendor.url");
        JAVA_VERSION = System.getProperty("java.version");
        NEWLINE = System.getProperty("line.separator");

        JAVA_SE = initIsJavaSE();

        if(        ARCH_lower.equals("x86")  ||
                   ARCH_lower.equals("i386") ||
                   ARCH_lower.equals("i486") ||
                   ARCH_lower.equals("i586") ||
                   ARCH_lower.equals("i686") ) {
            CPU_ARCH = CPUArch.X86_32;
            CPU_TYPE = CPUType.X86;
        } else if( ARCH_lower.equals("x86_64") ||
                   ARCH_lower.equals("amd64")  ) {
            CPU_ARCH = CPUArch.X86_64;
            CPU_TYPE = CPUType.X86;
        } else if( ARCH_lower.equals("ia64") ) {
            CPU_ARCH = CPUArch.IA64;
            CPU_TYPE = CPUType.IA;
        } else if( ARCH_lower.equals("arm") ) {
            CPU_ARCH = CPUArch.ARM_32;
            CPU_TYPE = CPUType.ARM;
        } else if( ARCH_lower.equals("sparc") ) {
            CPU_ARCH = CPUArch.SPARC_32;
            CPU_TYPE = CPUType.SPARC;
        } else if( ARCH_lower.equals("sparcv9") ) {
            CPU_ARCH = CPUArch.SPARCV9_64;
            CPU_TYPE = CPUType.SPARC;
        } else if( ARCH_lower.equals("pa_risc2.0") ) {
            CPU_ARCH = CPUArch.PA_RISC2_0;
            CPU_TYPE = CPUType.PA_RISC;
        } else if( ARCH_lower.equals("ppc") ) {
            CPU_ARCH = CPUArch.PPC;
            CPU_TYPE = CPUType.PPC;
        } else {
            throw new RuntimeException("Please port CPU detection to your platform (" + OS_lower + "/" + ARCH_lower + ")");
        }               
        OS_TYPE = getOSTypeImpl();
        
        machineDescription = MachineDescriptionRuntime.getMachineDescription(getIs32BitByCPUArchImpl());
        is32Bit = machineDescription.is32Bit();
    }

    private Platform() {}

    private static boolean getIs32BitByCPUArchImpl() throws RuntimeException {
        switch( CPU_ARCH ) {
            case X86_32:
            case ARM_32:
            case SPARC_32:
            case PPC:
                return true;
            case X86_64:
            case IA64:
            case SPARCV9_64:
            case PA_RISC2_0:
                return false;
            default:
                throw new RuntimeException("Please port CPU detection (32/64 bit) to your platform (" + Platform.OS_lower + "/" + Platform.ARCH_lower + "("+Platform.CPU_ARCH+"))");
        }
    }
        
    private static OSType getOSTypeImpl() throws RuntimeException {
        if ( OS_lower.startsWith("linux") ) {
            return OSType.LINUX;            
        }
        if ( OS_lower.startsWith("freebsd") ) {
            return OSType.FREEBSD;            
        }
        if ( OS_lower.startsWith("dalvik") ) {
            return OSType.DALVIK;            
        }
        if ( OS_lower.startsWith("mac os x") ||
             OS_lower.startsWith("darwin") ) {
            return OSType.MACOS;            
        }
        if ( OS_lower.startsWith("sunos") ) {
            return OSType.SUNOS;            
        }
        if ( OS_lower.startsWith("hp-ux") ) {
            return OSType.HPUX;            
        }
        if ( OS_lower.startsWith("windows") ) {
            return OSType.WINDOWS;
        }
        if ( OS_lower.startsWith("kd") ) {
            return OSType.OPENKODE;
        }
        throw new RuntimeException("Please port OS detection to your platform (" + OS_lower + "/" + ARCH_lower + ")");        
    }
    
    private static boolean initIsJavaSE() {
        // the fast path, check property Java SE instead of traversing through the ClassLoader
        String java_runtime_name = (String) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
              return System.getProperty("java.runtime.name");
            }
          });
        if(java_runtime_name.indexOf("Java SE") != -1) {
            return true;
        }

        // probe for classes we need on a SE environment
        try {
            Class.forName("java.nio.LongBuffer");
            Class.forName("java.nio.DoubleBuffer");
            return true;
        } catch(ClassNotFoundException ex) {
            // continue with Java SE check
        }

        return false;
    }

    /**
     * Returns true only if this program is running on the Java Standard Edition.
     */
    public static boolean isJavaSE() {
        return JAVA_SE;
    }

    /**
     * Returns the OS name.
     */
    public static String getOS() {
        return OS;
    }

    /**
     * Returns the OS version.
     */
    public static String getOSVersion() {
        return OS_VERSION;
    }


    /**
     * Returns the CPU architecture String.
     */
    public static String getArch() {
        return ARCH;
    }

    /**
     * Returns the OS type.
     */
    public static OSType getOSType() {
        return OS_TYPE;
    }
    
    /**
     * Returns the CPU type.
     */
    public static CPUType getCPUType() {
        return CPU_TYPE;
    }
    
    /**
     * Returns the CPU architecture.
     */
    public static CPUArch getCPUArch() {
        return CPU_ARCH;
    }
    
    /**
     * Returns the JAVA.
     */
    public static String getJavaVendor() {
        return JAVA_VENDOR;
    }

    /**
     * Returns the JAVA vendor url.
     */
    public static String getJavaVendorURL() {
        return JAVA_VENDOR_URL;
    }

    /**
     * Returns the JAVA vendor.
     */
    public static String getJavaVersion() {
        return JAVA_VERSION;
    }

    /**
     * Returns the JAVA vendor.
     */
    public static String getNewline() {
        return NEWLINE;
    }

    /**
     * Returns true if this JVM/ARCH is 32bit.
     * <p>Shortcut to {@link #getMachineDescription()}.{@link MachineDescription#is32Bit() is32Bit()}</p>
     */
    public static boolean is32Bit() {
        // return Platform.machineDescription.is32Bit();
        return Platform.is32Bit; // used very often
    }

    /**
     * Returns true if this JVM/ARCH is 64bit.
     * <p>Shortcut to {@link #getMachineDescription()}.{@link MachineDescription#is32Bit() is64Bit()}</p>
     */
    public static boolean is64Bit() {
        // return Platform.machineDescription.is64Bit();
        return !Platform.is32Bit; // used very often
    }

    /**
     * Returns the MachineDescription of the running machine.
     */
    public static MachineDescription getMachineDescription() {
        return machineDescription;
    }
}

