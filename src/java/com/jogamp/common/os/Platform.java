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
import java.util.concurrent.TimeUnit;

import com.jogamp.common.jvm.JNILibLoaderBase;
import com.jogamp.common.net.Uri;
import com.jogamp.common.util.JarUtil;
import com.jogamp.common.util.PropertyAccess;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.common.util.cache.TempJarCache;

import jogamp.common.jvm.JVMUtil;
import jogamp.common.os.MachineDataInfoRuntime;
import jogamp.common.os.PlatformPropsImpl;

/**
 * Utility class for querying platform specific properties.
 * <p>
 * Some field declarations and it's static initialization has been delegated
 * to it's super class {@link PlatformPropsImpl} to solve
 * static initialization interdependencies w/ the GlueGen native library loading
 * and it's derived information {@link #getMachineDataInfo()}, {@link #is32Bit()}, ..<br>
 * This mechanism is preferred in this case to avoid synchronization and locking
 * and allow better performance accessing the mentioned fields/methods.
 * </p>
 */
public class Platform extends PlatformPropsImpl {

    public enum OSType {
        LINUX, FREEBSD, ANDROID, MACOS, SUNOS, HPUX, WINDOWS, OPENKODE;
    }

    public enum CPUFamily {
        /** AMD/Intel */
        X86,
        /** ARM */
        ARM,
        /** Power PC */
        PPC,
        /** SPARC */
        SPARC,
        /** Mips */
        MIPS,
        /** PA RISC */
        PA_RISC,
        /** Itanium */
        IA64,
        /** Hitachi SuperH */
        SuperH;
    }

    public enum CPUType {
        /** ARM 32bit default, usually little endian */
        ARM(       CPUFamily.ARM,     true),
        /** ARM7EJ, ARM9E, ARM10E, XScale, usually little endian */
        ARMv5(     CPUFamily.ARM,     true),
        /** ARM11, usually little endian */
        ARMv6(     CPUFamily.ARM,     true),
        /** ARM Cortex, usually little endian */
        ARMv7(     CPUFamily.ARM,     true),
        // 4

        /** X86 32bit, little endian */
        X86_32(    CPUFamily.X86,     true),
        /** PPC 32bit default, usually big endian */
        PPC(       CPUFamily.PPC,     true),
        /** MIPS 32bit, big endian (mips) or little endian (mipsel) */
        MIPS_32(   CPUFamily.MIPS,    true),
        /** Hitachi SuperH 32bit default, ??? endian */
        SuperH(    CPUFamily.SuperH,  true),
        /** SPARC 32bit, big endian */
        SPARC_32(  CPUFamily.SPARC,   true),
        // 9

        /** ARM64 default (64bit), usually little endian */
        ARM64(     CPUFamily.ARM,     false),
        /** ARM AArch64 (64bit), usually little endian */
        ARMv8_A(   CPUFamily.ARM,     false),
        /** X86 64bit, little endian */
        X86_64(    CPUFamily.X86,     false),
        /** PPC 64bit default, usually big endian */
        PPC64(     CPUFamily.PPC,     false),
        /** MIPS 64bit, big endian (mips64) or little endian (mipsel64) ? */
        MIPS_64(   CPUFamily.MIPS,    false),
        /** Itanium 64bit default, little endian */
        IA64(      CPUFamily.IA64,    false),
        /** SPARC 64bit, big endian */
        SPARCV9_64(CPUFamily.SPARC,   false),
        /** PA_RISC2_0 64bit, ??? endian */
        PA_RISC2_0(CPUFamily.PA_RISC, false);
        // 17

        public final CPUFamily family;
        public final boolean is32Bit;

        CPUType(final CPUFamily type, final boolean is32Bit){
            this.family = type;
            this.is32Bit = is32Bit;
        }

        /**
         * Returns {@code true} if the given {@link CPUType} is compatible
         * w/ this one, i.e. at least {@link #family} and {@link #is32Bit} is equal.
         */
        public final boolean isCompatible(final CPUType other) {
            if( null == other ) {
                return false;
            } else if( other == this ) {
                return true;
            } else {
                return this.family == other.family &&
                       this.is32Bit == other.is32Bit;
            }
        }

        public static final CPUType query(final String cpuABILower) {
            if( null == cpuABILower ) {
                throw new IllegalArgumentException("Null cpuABILower arg");
            }
            if(        cpuABILower.equals("x86")  ||
                       cpuABILower.equals("i386") ||
                       cpuABILower.equals("i486") ||
                       cpuABILower.equals("i586") ||
                       cpuABILower.equals("i686") ) {
                return X86_32;
            } else if( cpuABILower.equals("x86_64") ||
                       cpuABILower.equals("amd64")  ) {
                return X86_64;
            } else if( cpuABILower.equals("ia64") ) {
                return IA64;
            } else if( cpuABILower.equals("aarch64") ) {
                return ARM64;
            } else if( cpuABILower.startsWith("arm") ) {
                if(        cpuABILower.equals("armv8-a")   ||
                           cpuABILower.equals("arm-v8-a") ||
                           cpuABILower.equals("arm-8-a") ||
                           cpuABILower.equals("arm64-v8a") ) {
                    return ARMv8_A;
                } else if( cpuABILower.startsWith("arm64") ) {
                    return ARM64;
                } else if( cpuABILower.startsWith("armv7") ||
                           cpuABILower.startsWith("arm-v7") ||
                           cpuABILower.startsWith("arm-7") ||
                           cpuABILower.startsWith("armeabi-v7") ) {
                    return ARMv7;
                } else if( cpuABILower.startsWith("armv5") ||
                           cpuABILower.startsWith("arm-v5") ||
                           cpuABILower.startsWith("arm-5") ) {
                    return ARMv5;
                } else if( cpuABILower.startsWith("armv6") ||
                           cpuABILower.startsWith("arm-v6") ||
                           cpuABILower.startsWith("arm-6") ) {
                    return ARMv6;
                } else {
                    return ARM;
                }
            } else if( cpuABILower.equals("sparcv9") ) {
                return SPARCV9_64;
            } else if( cpuABILower.equals("sparc") ) {
                return SPARC_32;
            } else if( cpuABILower.equals("pa_risc2.0") ) {
                return PA_RISC2_0;
            } else if( cpuABILower.startsWith("ppc64") ) {
                return PPC64;
            } else if( cpuABILower.startsWith("ppc") ) {
                return PPC;
            } else if( cpuABILower.startsWith("mips64") ) {
                return MIPS_64;
            } else if( cpuABILower.startsWith("mips") ) {
                return MIPS_32;
            } else if( cpuABILower.startsWith("superh") ) {
                return SuperH;
            } else {
                throw new RuntimeException("Please port CPUType detection to your platform (CPU_ABI string '" + cpuABILower + "')");
            }
        }
    }

    public enum ABIType {
        GENERIC_ABI       ( 0x00 ),
        /** ARM GNU-EABI ARMEL -mfloat-abi=softfp */
        EABI_GNU_ARMEL    ( 0x01 ),
        /** ARM GNU-EABI ARMHF -mfloat-abi=hard */
        EABI_GNU_ARMHF    ( 0x02 ),
        /** ARM EABI AARCH64 (64bit) */
        EABI_AARCH64      ( 0x03 );

        public final int id;

        ABIType(final int id){
            this.id = id;
        }

        /**
         * Returns {@code true} if the given {@link ABIType} is compatible
         * w/ this one, i.e. they are equal.
         */
        public final boolean isCompatible(final ABIType other) {
            if( null == other ) {
                return false;
            } else {
                return other == this;
            }
        }

        public static final ABIType query(final CPUType cpuType, final String cpuABILower) {
            if( null == cpuType ) {
                throw new IllegalArgumentException("Null cpuType");
            } else if( null == cpuABILower ) {
                throw new IllegalArgumentException("Null cpuABILower");
            } else if( CPUFamily.ARM == cpuType.family ) {
                if( !cpuType.is32Bit ) {
                    return EABI_AARCH64;
                } else if( cpuABILower.equals("armeabi-v7a-hard") ) {
                    return EABI_GNU_ARMHF;
                } else {
                    return EABI_GNU_ARMEL;
                }
            } else {
                return GENERIC_ABI;
            }
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

    private static final MachineDataInfo machineDescription;

    /** <code>true</code> if AWT is available and not in headless mode, otherwise <code>false</code>. */
    public static final boolean AWT_AVAILABLE;

    private static final boolean isRunningFromJarURL;

    static {
        final boolean[] _isRunningFromJarURL = new boolean[] { false };
        final boolean[] _USE_TEMP_JAR_CACHE = new boolean[] { false };
        final boolean[] _AWT_AVAILABLE = new boolean[] { false };

        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {

                PlatformPropsImpl.initSingleton(); // documenting the order of static initialization

                final ClassLoader cl = Platform.class.getClassLoader();

                final Uri platformClassJarURI;
                {
                    Uri _platformClassJarURI = null;
                    try {
                        _platformClassJarURI = JarUtil.getJarUri(Platform.class.getName(), cl);
                    } catch (final Exception e) { }
                    platformClassJarURI = _platformClassJarURI;
                }
                _isRunningFromJarURL[0] = null != platformClassJarURI;

                _USE_TEMP_JAR_CACHE[0] = ( OS_TYPE != OSType.ANDROID ) && ( null != platformClassJarURI ) &&
                                         PropertyAccess.getBooleanProperty(useTempJarCachePropName, true, true);

                // load GluegenRT native library
                if(_USE_TEMP_JAR_CACHE[0] && TempJarCache.initSingleton()) {
                    try {
                        JNILibLoaderBase.addNativeJarLibs(new Class<?>[] { jogamp.common.Debug.class }, null);
                    } catch (final Exception e0) {
                        // IllegalArgumentException, IOException
                        System.err.println("Caught "+e0.getClass().getSimpleName()+": "+e0.getMessage()+", while JNILibLoaderBase.addNativeJarLibs(..)");
                    }
                }
                DynamicLibraryBundle.GlueJNILibLoader.loadLibrary(libBaseName, false, cl);

                // JVM bug workaround
                JVMUtil.initSingleton(); // requires gluegen-rt, one-time init.

                // AWT Headless determination
                if( !PropertyAccess.getBooleanProperty("java.awt.headless", true) &&
                    ReflectionUtil.isClassAvailable(ReflectionUtil.AWTNames.ComponentClass, cl) &&
                    ReflectionUtil.isClassAvailable(ReflectionUtil.AWTNames.GraphicsEnvironmentClass, cl) ) {
                    try {
                        _AWT_AVAILABLE[0] = false == ((Boolean)ReflectionUtil.callStaticMethod(ReflectionUtil.AWTNames.GraphicsEnvironmentClass, ReflectionUtil.AWTNames.isHeadlessMethod, null, null, cl)).booleanValue();
                    } catch (final Throwable t) { }
                }
                return null;
            } } );
        isRunningFromJarURL = _isRunningFromJarURL[0];
        USE_TEMP_JAR_CACHE = _USE_TEMP_JAR_CACHE[0];
        AWT_AVAILABLE = _AWT_AVAILABLE[0];

        //
        // Validate and setup MachineDataInfo.StaticConfig
        //
        MachineDataInfoRuntime.initialize();
        machineDescription = MachineDataInfoRuntime.getRuntime();
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
     * <p>In case of {@link OSType#ANDROID} the {@link #getOSName() OS name}, is Linux</p>
     */
    public static OSType getOSType() {
        return OS_TYPE;
    }

    /**
     * Returns the CPU family.
     */
    public static CPUFamily getCPUFamily() {
        return CPU_ARCH.family;
    }

    /**
     * Returns the CPU architecture type.
     */
    public static CPUType getCPUType() {
        return CPU_ARCH;
    }

    /**
     * Returns true if this JVM/ARCH is 32bit.
     * <p>Shortcut to {@link #getCPUType()}.{@link CPUType#is32Bit is32Bit}</p>
     */
    public static boolean is32Bit() {
        return CPU_ARCH.is32Bit; // used very often
    }

    /**
     * Returns true if this JVM/ARCH is 64bit.
     * <p>Shortcut to !{@link #getCPUType()}.{@link CPUType#is32Bit is32Bit}</p>
     */
    public static boolean is64Bit() {
        return !CPU_ARCH.is32Bit; // used very often
    }

    /**
     * Returns the ABI type.
     * <p>
     * In case of {@link CPUFamily#ARM}, the value is determined by parsing the <i>Elf Headers</i> of the running VM.
     * </p>
     * <p>
     * Otherwise the value is {@link ABIType#GENERIC_ABI}.
     * </p>
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
     * Returns the MachineDataInfo of the running machine.
     */
    public static MachineDataInfo getMachineDataInfo() {
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
     * Returns the unix based current time in milliseconds, based on <code>gettimeofday(..)</code>.
     * <p>
     * This is an alternative to {@link System#currentTimeMillis()} and {@link System#nanoTime()}.
     * While the named {@link System} methods do provide the required precision,
     * <code>gettimeofday()</code> <i>also</i> guarantees time accuracy, i.e. update interval.
     * </p>
     * @see #currentTimeMicros()
     */
    public static native long currentTimeMillis();

    /**
     * Returns the unix based current time in microseconds, based on <code>gettimeofday(..)</code>.
     * <p>
     * This is an alternative to {@link System#currentTimeMillis()} and {@link System#nanoTime()}.
     * While the named {@link System} methods do provide the required precision,
     * <code>gettimeofday()</code> <i>also</i> guarantees time accuracy, i.e. update interval.
     * </p>
     * @see #currentTimeMillis()
     */
    public static native long currentTimeMicros();

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
            try { TimeUnit.NANOSECONDS.sleep(nsPeriod); } catch (final InterruptedException e) { }
        }
        return  ( ( System.nanoTime() - t0_ns ) - nsDuration ) / splitInLoops;
    }

}

