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

import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.TimeUnit;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.JarUtil;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.common.util.cache.TempJarCache;

import jogamp.common.Debug;
import jogamp.common.jvm.JVMUtil;
import jogamp.common.os.MachineDescriptionRuntime;

/**
 * Utility class for querying platform specific properties.
 * @author Michael Bien, Sven Gothel, et. al.
 */
public class Platform {

    /** fixed basename of JAR file and native library */
    private static final String libBaseName = "gluegen-rt";
    
    /**
     * System property: 'jogamp.gluegen.UseTempJarCache', 
     * defaults to true if {@link #OS_TYPE} is not {@link OSType#ANDROID}.
     */
    public static final boolean USE_TEMP_JAR_CACHE;
    private static final String useTempJarCachePropName = "jogamp.gluegen.UseTempJarCache";
    
    public static final boolean JAVA_SE;
    public static final boolean LITTLE_ENDIAN;
    public static final String OS;
    public static final String OS_lower;
    public static final String OS_VERSION;
    public static final VersionNumber OS_VERSION_NUMBER;
    public static final String ARCH;
    public static final String ARCH_lower;
    public static final String JAVA_VENDOR;
    public static final String JAVA_VENDOR_URL;
    public static final String JAVA_VM_NAME;
    public static final String JAVA_RUNTIME_NAME;
    public static final String JAVA_VERSION;
    public static final VersionNumber JAVA_VERSION_NUMBER;
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
    
    private static final String os_and_arch;
    
    static {
        // We don't seem to need an AccessController.doPrivileged() block
        // here as these system properties are visible even to unsigned Applets.
        OS =  System.getProperty("os.name");
        OS_lower = OS.toLowerCase();
        OS_VERSION =  System.getProperty("os.version");
        OS_VERSION_NUMBER = new VersionNumber(OS_VERSION, ".");
        ARCH = System.getProperty("os.arch");
        ARCH_lower = ARCH.toLowerCase();
        JAVA_VENDOR = System.getProperty("java.vendor");
        JAVA_VENDOR_URL = System.getProperty("java.vendor.url");
        JAVA_VERSION = System.getProperty("java.version");
        JAVA_VERSION_NUMBER = new VersionNumber(JAVA_VERSION, ".");
        NEWLINE = System.getProperty("line.separator");
        JAVA_VM_NAME = System.getProperty("java.vm.name");
        JAVA_RUNTIME_NAME = getJavaRuntimeNameImpl();
        JAVA_SE = initIsJavaSE();

        LITTLE_ENDIAN = queryIsLittleEndianImpl();
        
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
        
        os_and_arch = getOSAndArch(OS_TYPE, CPU_ARCH);
        
        USE_TEMP_JAR_CACHE = (OS_TYPE != OSType.ANDROID) && isRunningFromJarURL() &&
            AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                public Boolean run() {
                    return Boolean.valueOf(Debug.getBooleanProperty(true, useTempJarCachePropName, true, AccessController.getContext()));
                }
            }).booleanValue();
        
        loadGlueGenRTImpl();
        JVMUtil.initSingleton(); // requires gluegen-rt, one-time init.
        
        MachineDescription md = MachineDescriptionRuntime.getRuntime();
        if(null == md) {
            MachineDescription.StaticConfig smd = MachineDescriptionRuntime.getStatic();
            md = smd.md;
            System.err.println("Warning: Using static MachineDescription: "+smd);
        } else {
            MachineDescription.StaticConfig smd = MachineDescriptionRuntime.getStatic();
            if(!md.compatible(smd.md)) {
                throw new RuntimeException("Incompatible MachineDescriptions:"+Platform.NEWLINE+
                                           " Static "+smd+Platform.NEWLINE+
                                           " Runtime "+md);
            }
        }
        machineDescription = md;
        is32Bit = machineDescription.is32Bit();
    }

    private Platform() {}

    /**
     * Preemptively avoids initializing and using {@link TempJarCache} in case we are <b>not</b> running 
     * from a Jar URL, ie. plain class files. Used to set {@link USE_TEMP_JAR_CACHE}.
     * <p> 
     * Impact: Less overhead and more robustness.
     * </p> 
     *
     * @return true if we're running from a Jar URL, otherwise false
     */
    private static boolean isRunningFromJarURL() {        
        return JarUtil.hasJarURL(Platform.class.getName(), Platform.class.getClassLoader());
    }
    
    private static boolean queryIsLittleEndianImpl() {
        ByteBuffer tst_b = Buffers.newDirectByteBuffer(Buffers.SIZEOF_INT); // 32bit in native order
        IntBuffer tst_i = tst_b.asIntBuffer();
        ShortBuffer tst_s = tst_b.asShortBuffer();
        tst_i.put(0, 0x0A0B0C0D);
        return 0x0C0D == tst_s.get(0);
    }
  
    private static OSType getOSTypeImpl() throws RuntimeException {
        if ( AndroidVersion.isAvailable ) {
            return OSType.ANDROID;
        }
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

    private static String getJavaRuntimeNameImpl() {
        // the fast path, check property Java SE instead of traversing through the ClassLoader
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
              return System.getProperty("java.runtime.name");
            }
          });
    }
    
    private static boolean initIsJavaSE() {
        if(JAVA_RUNTIME_NAME.indexOf("Java SE") != -1) {
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

    private static void loadGlueGenRTImpl() {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
              if(USE_TEMP_JAR_CACHE && TempJarCache.initSingleton()) {
                  final String nativeJarName = libBaseName+"-natives-"+os_and_arch+".jar";
                  final ClassLoader cl = Platform.class.getClassLoader();                
                  try {
                    final URL jarUrlRoot = JarUtil.getURLDirname(
                                        JarUtil.getJarSubURL(Platform.class.getName(), cl) );
                    final URL nativeJarURL = JarUtil.getJarFileURL(jarUrlRoot, nativeJarName);
                    TempJarCache.bootstrapNativeLib(Platform.class, libBaseName, nativeJarURL, cl);
                  } catch (Exception e0) {
                    // IllegalArgumentException, IOException
                    System.err.println("Catched: "+e0.getMessage());
                  }
              }
              DynamicLibraryBundle.GlueJNILibLoader.loadLibrary(libBaseName, false);
              return null;
            }
        });
    }
    
    /**
     * kick off static initialization incl native gluegen-rt lib loading
     */
    public static void initSingleton() { } 
    
    /**
     * Returns true only if this program is running on the Java Standard Edition.
     */
    public static boolean isJavaSE() {
        return JAVA_SE;
    }

    /**
     * Returns true if this machine is little endian, otherwise false.
     */
    public static boolean isLittleEndian() {
        return LITTLE_ENDIAN;
    }

    /**
     * Returns the OS name.
     * <p>In case of {@link OSType#ANDROID}, see {@link #getOSType()}, the OS name is Linux</p>
     */
    public static String getOSName() {
        return OS;
    }
    
    /**
     * Returns the OS version.
     */
    public static String getOSVersion() {
        return OS_VERSION;
    }

    /**
     * Returns the OS version number.
     */
    public static VersionNumber getOSVersionNumber() {
        return OS_VERSION_NUMBER;
    }

    /**
     * Returns the CPU architecture String.
     */
    public static String getArchName() {
        return ARCH;
    }

    /**
     * Returns the OS type.
     * <p>In case of {@link OSType#ANDROID} the OS name, see {@link #getOSName()}, is Linux</p>
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
     * Returns the GlueGen common name for the currently running OSType and CPUType
     * as implemented in the build system in 'gluegen-cpptasks-base.xml'.<br>
     * 
     * @see #getOSAndArch(OSType, CPUType)
     */
    public static String getOSAndArch() {
        return os_and_arch;
    }
    
    /**
     * Returns the GlueGen common name for the given OSType and CPUType
     * as implemented in the build system in 'gluegen-cpptasks-base.xml'.<br>
     * 
     * A list of currently supported <code>os.and.arch</code> strings:
     * <ul>
     *   <li>freebsd-i586</li>
     *   <li>freebsd-amd64</li>
     *   <li>hpux-hppa</li>
     *   <li>linux-amd64</li>
     *   <li>linux-ia64</li>
     *   <li>linux-i586</li>
     *   <li>linux-armv7</li>
     *   <li>android-armv7</li>
     *   <li>macosx-universal</li>
     *   <li>solaris-sparc</li>
     *   <li>solaris-sparcv9</li>
     *   <li>solaris-amd64</li>
     *   <li>solaris-i586</li>
     *   <li>windows-amd64</li>
     *   <li>windows-i586</li>
     * </ul>
     * @return
     */
    public static String getOSAndArch(OSType osType, CPUType cpuType) {
        String _os_and_arch;
        
        switch( CPU_ARCH ) {
            case X86_32:
                _os_and_arch = "i586";
                break;
            case ARM:
                _os_and_arch = "armv7"; // TODO: sync with gluegen-cpptasks-base.xml
                break;
            case ARMv5:
                _os_and_arch = "armv5";
                break;
            case ARMv6:
                _os_and_arch = "armv5";
                break;
            case ARMv7:
                _os_and_arch = "armv7";
                break;
            case SPARC_32:
                _os_and_arch = "sparc"; 
                break;
            case PPC:
                _os_and_arch = "ppc"; // TODO: sync with gluegen-cpptasks-base.xml
                break;
            case X86_64:
                _os_and_arch = "amd64";
                break;
            case IA64:
                _os_and_arch = "ia64";
                break;
            case SPARCV9_64:
                _os_and_arch = "sparcv9"; 
                break;
            case PA_RISC2_0:
                _os_and_arch = "risc2.0"; // TODO: sync with gluegen-cpptasks-base.xml 
                break;
            default:
                throw new InternalError("Complete case block");
        }
        switch(OS_TYPE) {
            case ANDROID:
              _os_and_arch = "android-" + _os_and_arch;  
              break;
            case MACOS:
              _os_and_arch = "macosx-universal";  
              break;
            case WINDOWS:
              _os_and_arch = "windows-" + _os_and_arch;  
              break;
            case OPENKODE:
              _os_and_arch = "openkode-" + _os_and_arch; // TODO: think about that   
              break;                
            case LINUX:
              _os_and_arch = "linux-" + _os_and_arch;  
              break;
            case FREEBSD:
              _os_and_arch = "freebsd-" + _os_and_arch;  
              break;
            case SUNOS:
              _os_and_arch = "solaris-" + _os_and_arch;  
              break;
            case HPUX:
              _os_and_arch = "hpux-hppa";  // TODO: really only hppa ?
              break;              
            default:
              throw new InternalError("Complete case block");
        }
        return _os_and_arch;        
    }
    
    /**
     * Returns the JAVA vendor.
     */
    public static String getJavaVendor() {
        return JAVA_VENDOR;
    }

    /**
     * Returns the JAVA VM name.
     */
    public static String getJavaVMName() {
        return JAVA_VM_NAME;
    }
    
    /**
     * Returns the JAVA runtime name.
     */
    public static String getJavaRuntimeName() {
        return JAVA_RUNTIME_NAME;
    }
    
    /**
     * Returns the JAVA vendor url.
     */
    public static String getJavaVendorURL() {
        return JAVA_VENDOR_URL;
    }

    /**
     * Returns the JAVA version.
     */
    public static String getJavaVersion() {
        return JAVA_VERSION;
    }

    /**
     * Returns the JAVA version number.
     */
    public static VersionNumber getJavaVersionNumber() {
        return JAVA_VERSION_NUMBER;
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
    
    //
    // time / jitter
    //

    /**
     * Returns the estimated sleep jitter value in nanoseconds.
     * <p>
     * Includes a warm-up path, allowing hotspot to optimize the code.
     * </p>
     */
    public static synchronized long getCurrentSleepJitter() {
        getCurrentSleepJitterImpl(TimeUnit.MILLISECONDS.toNanos(10), 10); // warm-up
        return getCurrentSleepJitterImpl(TimeUnit.MILLISECONDS.toNanos(10), 10);
    }  
    private static long getCurrentSleepJitterImpl(final long nsDuration, final int splitInLoops) {
        final long nsPeriod = nsDuration / splitInLoops;
        final long t0_ns = System.nanoTime();
        for(int i=splitInLoops; i>0; i--) {
            try { TimeUnit.NANOSECONDS.sleep(nsPeriod); } catch (InterruptedException e) { }
        }
        return  ( ( System.nanoTime() - t0_ns ) - nsDuration ) / splitInLoops;
    }

}

