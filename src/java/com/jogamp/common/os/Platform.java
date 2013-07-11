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

import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.TimeUnit;

import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.JarUtil;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.common.util.cache.TempJarCache;

import jogamp.common.Debug;
import jogamp.common.jvm.JVMUtil;
import jogamp.common.os.MachineDescriptionRuntime;
import jogamp.common.os.PlatformPropsImpl;

/**
 * Utility class for querying platform specific properties.
 * <p>
 * Some field declarations and it's static initialization has been delegated
 * to it's super class {@link PlatformPropsImpl} to solve 
 * static initialization interdependencies w/ the GlueGen native library loading
 * and it's derived information {@link #getMachineDescription()}, {@link #is32Bit()}, ..<br>
 * This mechanism is preferred in this case to avoid synchronization and locking
 * and allow better performance accessing the mentioned fields/methods.
 * </p>
 */
public class Platform extends PlatformPropsImpl {
    
    public enum OSType {
        LINUX(0), FREEBSD(1), ANDROID(2), MACOS(3), SUNOS(4), HPUX(5), WINDOWS(6), OPENKODE(7), RASPBERRYPI(8);
        
        public final int id;

        OSType(int id){
            this.id = id;
        }
    }
    
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
    
    public enum ABIType {
        GENERIC_ABI    ( 0x0000 ),        
        /** ARM GNU-EABI ARMEL -mfloat-abi=softfp */       
        EABI_GNU_ARMEL ( 0x0001 ),
        /** ARM GNU-EABI ARMHF -mfloat-abi=hard */       
        EABI_GNU_ARMHF ( 0x0002 );
        
        public final int id;
        
        ABIType(int id){
            this.id = id;
        }        
    }
    
    private static final String useTempJarCachePropName = "jogamp.gluegen.UseTempJarCache";
    
    /** fixed basename of JAR file and native library */
    private static final String libBaseName = "gluegen-rt";    
        
    //
    // static initialization order:
    //
    
    /**
     * System property: 'jogamp.gluegen.UseTempJarCache', 
     * defaults to true if {@link #OS_TYPE} is not {@link OSType#ANDROID}.
     */
    public static final boolean USE_TEMP_JAR_CACHE;
    
    //
    // post loading native lib:
    //
    
    private static final MachineDescription machineDescription;
    
    private static final boolean is32Bit;
    
    /** <code>true</code> if AWT is available and not in headless mode, otherwise <code>false</code>. */
    public static final boolean AWT_AVAILABLE;
        
    private static final boolean isRunningFromJarURL;
    
    static {
        final boolean[] _isRunningFromJarURL = new boolean[] { false };
        final boolean[] _USE_TEMP_JAR_CACHE = new boolean[] { false };
        final boolean[] _AWT_AVAILABLE = new boolean[] { false };
        
        AccessController.doPrivileged(new PrivilegedAction<Object>() { 
            public Object run() {
                
                PlatformPropsImpl.initSingleton(); // documenting the order of static initialization
                
                final ClassLoader cl = Platform.class.getClassLoader();
                
                final URI platformClassJarURI;
                {
                    URI _platformClassJarURI = null;
                    try {
                        _platformClassJarURI = JarUtil.getJarURI(Platform.class.getName(), cl);
                    } catch (Exception e) { }
                    platformClassJarURI = _platformClassJarURI;
                }
                _isRunningFromJarURL[0] = null != platformClassJarURI; 
                                
                _USE_TEMP_JAR_CACHE[0] = ( OS_TYPE != OSType.ANDROID ) && ( null != platformClassJarURI ) &&
                                         Debug.getBooleanProperty(useTempJarCachePropName, true, true);
            
                // load GluegenRT native library
                if(_USE_TEMP_JAR_CACHE[0] && TempJarCache.initSingleton()) {
                    String nativeJarName = null;
                    URI jarUriRoot = null;
                    URI nativeJarURI = null;
                    try {
                        final String jarName = JarUtil.getJarBasename( platformClassJarURI );
                        final String nativeJarBasename = jarName.substring(0, jarName.indexOf(".jar")); // ".jar" already validated w/ JarUtil.getJarBasename(..)
                        nativeJarName = nativeJarBasename+"-natives-"+PlatformPropsImpl.os_and_arch+".jar";                    
                        jarUriRoot = IOUtil.getDirname( JarUtil.getJarSubURI( platformClassJarURI ) );
                        nativeJarURI = JarUtil.getJarFileURI(jarUriRoot, nativeJarName);
                        TempJarCache.bootstrapNativeLib(Platform.class, libBaseName, nativeJarURI);
                    } catch (Exception e0) {
                        // IllegalArgumentException, IOException
                        System.err.println("Catched "+e0.getClass().getSimpleName()+": "+e0.getMessage()+", while TempJarCache.bootstrapNativeLib() of "+nativeJarURI+" ("+jarUriRoot+" + "+nativeJarName+")");
                    }
                }
                DynamicLibraryBundle.GlueJNILibLoader.loadLibrary(libBaseName, false, cl);
            
                // JVM bug workaround
                JVMUtil.initSingleton(); // requires gluegen-rt, one-time init.
            
                // AWT Headless determination
                if( !Debug.getBooleanProperty("java.awt.headless", true) &&
                    ReflectionUtil.isClassAvailable(ReflectionUtil.AWTNames.ComponentClass, cl) && 
                    ReflectionUtil.isClassAvailable(ReflectionUtil.AWTNames.GraphicsEnvironmentClass, cl) ) {
                    try {
                        _AWT_AVAILABLE[0] = false == ((Boolean)ReflectionUtil.callStaticMethod(ReflectionUtil.AWTNames.GraphicsEnvironmentClass, ReflectionUtil.AWTNames.isHeadlessMethod, null, null, cl)).booleanValue();
                    } catch (Throwable t) { }
                }
                return null;
            } } );
        isRunningFromJarURL = _isRunningFromJarURL[0];
        USE_TEMP_JAR_CACHE = _USE_TEMP_JAR_CACHE[0];
        AWT_AVAILABLE = _AWT_AVAILABLE[0];
                        
        MachineDescription md = MachineDescriptionRuntime.getRuntime();
        if(null == md) {
            MachineDescription.StaticConfig smd = MachineDescriptionRuntime.getStatic();
            md = smd.md;
            System.err.println("Warning: Using static MachineDescription: "+smd);
        } else {
            MachineDescription.StaticConfig smd = MachineDescriptionRuntime.getStatic();
            if(!md.compatible(smd.md)) {
                throw new RuntimeException("Incompatible MachineDescriptions:"+PlatformPropsImpl.NEWLINE+
                                           " Static "+smd+PlatformPropsImpl.NEWLINE+
                                           " Runtime "+md);
            }
        }
        machineDescription = md;
        is32Bit = machineDescription.is32Bit();        
    }

    private Platform() {}

    /**
     * @return true if we're running from a Jar URL, otherwise false
     */
    public static final boolean isRunningFromJarURL() {        
        return isRunningFromJarURL;
    }
    
    /**
     * kick off static initialization of <i>platform property information</i> and <i>native gluegen-rt lib loading</i>
     */
    public static void initSingleton() { } 
    
    /**
     * Returns true only if having {@link java.nio.LongBuffer} and {@link java.nio.DoubleBuffer} available.
     */
    public static boolean isJavaSE() {
        return JAVA_SE;
    }

    /**
     * Returns true only if being compatible w/ language level 6, e.g. JRE 1.6.
     * <p>
     * Implies {@link #isJavaSE()}.
     * </p>
     * <p>
     * <i>Note</i>: We claim Android is compatible.
     * </p> 
     */
    public static boolean isJava6() {
        return JAVA_6;
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
     * Returns the (guessed) ABI.
     */
    public static ABIType getABIType() {
        return ABI_TYPE;
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
     * Returns the platform's line separator.
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
    
    /** Returns <code>true</code> if AWT is available and not in headless mode, otherwise <code>false</code>. */
    public static boolean isAWTAvailable() {
        return AWT_AVAILABLE;
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

