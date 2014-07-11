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

import com.jogamp.common.jvm.JNILibLoaderBase;
import com.jogamp.common.util.JarUtil;
import com.jogamp.common.util.PropertyAccess;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.common.util.cache.TempJarCache;

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
        LINUX(0), FREEBSD(1), ANDROID(2), MACOS(3), SUNOS(4), HPUX(5), WINDOWS(6), OPENKODE(7);

        public final int id;

        OSType(final int id){
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
        /** Mips */
        MIPS(   0x00040000),
        /** PA RISC */
        PA_RISC(0xFFFF0000),
        /** Itanium */
        IA64(   0xFFFF1000);

        public final int id;

        CPUFamily(final int id){
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
        /** MIPS 32bit */
        MIPS_32(  CPUFamily.MIPS,     0x0001),
        /** MIPS 64bit */
        MIPS_64(  CPUFamily.MIPS,     0x0002),
        /** Itanium default */
        IA64(      CPUFamily.IA64,    0x0000),
        /** PA_RISC2_0 */
        PA_RISC2_0(CPUFamily.PA_RISC, 0x0001);

        public final int id;
        public final CPUFamily family;

        CPUType(final CPUFamily type, final int id){
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

        ABIType(final int id){
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
            @Override
            public Object run() {

                PlatformPropsImpl.initSingleton(); // documenting the order of static initialization

                final ClassLoader cl = Platform.class.getClassLoader();

                final URI platformClassJarURI;
                {
                    URI _platformClassJarURI = null;
                    try {
                        _platformClassJarURI = JarUtil.getJarURI(Platform.class.getName(), cl);
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

        MachineDescription md = MachineDescriptionRuntime.getRuntime();
        if(null == md) {
            final MachineDescription.StaticConfig smd = MachineDescriptionRuntime.getStatic();
            md = smd.md;
            System.err.println("Warning: Using static MachineDescription: "+smd);
        } else {
            final MachineDescription.StaticConfig smd = MachineDescriptionRuntime.getStatic();
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
     * <p>In case of {@link OSType#ANDROID} the {@link #getOSName() OS name}, is Linux</p>
     */
    public static OSType getOSType() {
        return OS_TYPE;
    }

    /**
     * Returns the CPU family.
     */
    public static CPUFamily getCPUFamily() {
        return CPU_ARCH.getFamily();
    }

    /**
     * Returns the CPU architecture type.
     */
    public static CPUType getCPUType() {
        return CPU_ARCH;
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

