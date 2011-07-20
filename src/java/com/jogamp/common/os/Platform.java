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
        LINUX(0), FREEBSD(1), ANDROID(2), MACOS(3), SUNOS(4), HPUX(5), WINDOWS(6), OPENKODE(7); 
        
        public final int id;

        OSType(int id){
            this.id = id;
        }
    }    
    public static final OSType OS_TYPE;
    
    public enum CPUFamily {
        /** AMD/Intel */
        X86(    0x00000000), 
        /** ARM */
        ARM(    0x00010000),
        /** Power PC */
        PPC(    0x00020000),
        /** SPARC */
        SPARC(  0x00030000),
        /** PA RISC */
        PA_RISC(0xFFFF0000),
        /** Itanium */
        IA64(   0xFFFF1000); 
        
        public final int id;

        CPUFamily(int id){
            this.id = id;
        }
    }    
    public enum CPUType {
        /** X86 32bit */       
        X86_32(    CPUFamily.X86,     0x0001),
        /** X86 64bit */
        X86_64(    CPUFamily.X86,     0x0002),
        /** ARM default */
        ARM(       CPUFamily.ARM,     0x0000),
        /** ARM7EJ, ARM9E, ARM10E, XScale */
        ARMv5(     CPUFamily.ARM,     0x0001),
        /** ARM11 */
        ARMv6(     CPUFamily.ARM,     0x0002),
        /** ARM Cortex */
        ARMv7(     CPUFamily.ARM,     0x0004),
        /** PPC default */
        PPC(       CPUFamily.PPC,     0x0000),
        /** SPARC 32bit */
        SPARC_32(  CPUFamily.SPARC,   0x0001),
        /** SPARC 64bit */
        SPARCV9_64(CPUFamily.SPARC,   0x0002),
        /** Itanium default */
        IA64(      CPUFamily.IA64,    0x0000),
        /** PA_RISC2_0 */
        PA_RISC2_0(CPUFamily.PA_RISC, 0x0001);
        
        public final int id;
        public final CPUFamily family;
        
        CPUType(CPUFamily type, int id){
            this.family = type;
            this.id = id;
        }
        
        public CPUFamily getFamily() { return family; }
    }      
    public static final CPUType CPU_ARCH;
    
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
            CPU_ARCH = CPUType.X86_32;
        } else if( ARCH_lower.equals("x86_64") ||
                   ARCH_lower.equals("amd64")  ) {
            CPU_ARCH = CPUType.X86_64;
        } else if( ARCH_lower.equals("ia64") ) {
            CPU_ARCH = CPUType.IA64;
        } else if( ARCH_lower.equals("arm") ) {
            CPU_ARCH = CPUType.ARM;
        } else if( ARCH_lower.equals("armv5l") ) {
            CPU_ARCH = CPUType.ARMv5;
        } else if( ARCH_lower.equals("armv6l") ) {
            CPU_ARCH = CPUType.ARMv6;
        } else if( ARCH_lower.equals("armv7l") ) {
            CPU_ARCH = CPUType.ARMv7;
        } else if( ARCH_lower.equals("sparc") ) {
            CPU_ARCH = CPUType.SPARC_32;
        } else if( ARCH_lower.equals("sparcv9") ) {
            CPU_ARCH = CPUType.SPARCV9_64;
        } else if( ARCH_lower.equals("pa_risc2.0") ) {
            CPU_ARCH = CPUType.PA_RISC2_0;
        } else if( ARCH_lower.equals("ppc") ) {
            CPU_ARCH = CPUType.PPC;
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
            case ARM:
            case ARMv5:
            case ARMv6:
            case ARMv7:
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
        if ( OS_lower.startsWith("android") ) {
            return OSType.ANDROID;            
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
    public static CPUFamily getCPUFamily() {
        return CPU_ARCH.getFamily();
    }
    
    /**
     * Returns the CPU architecture.
     */
    public static CPUType getCPUType() {
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

