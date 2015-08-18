package jogamp.common.os;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import jogamp.common.Debug;
import jogamp.common.os.elf.ElfHeaderPart1;
import jogamp.common.os.elf.ElfHeaderPart2;
import jogamp.common.os.elf.SectionArmAttributes;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.os.AndroidVersion;
import com.jogamp.common.os.NativeLibrary;
import com.jogamp.common.os.Platform;
import com.jogamp.common.os.Platform.ABIType;
import com.jogamp.common.os.Platform.CPUFamily;
import com.jogamp.common.os.Platform.CPUType;
import com.jogamp.common.os.Platform.OSType;
import com.jogamp.common.util.VersionNumber;

/**
 * Abstract parent class of {@link Platform} initializing and holding
 * platform information, which are initialized independent
 * of other classes.
 * <p>
 * This class is not intended to be exposed in the public namespace
 * and solely exist to solve initialization interdependencies.<br>
 * Please use {@link Platform} to access the public fields!
 * </p>
 */
public abstract class PlatformPropsImpl {
    static final boolean DEBUG = Debug.debug("Platform");

    /** Selected {@link Platform.OSType#MACOS} {@link VersionNumber}s. */
    public static class OSXVersion {
        /** OSX Tiger, i.e. 10.4.0 */
        public static final VersionNumber Tiger = new VersionNumber(10,4,0);
        /** OSX Lion, i.e. 10.7.0 */
        public static final VersionNumber Lion = new VersionNumber(10,7,0);
        /** OSX Mavericks, i.e. 10.9.0 */
        public static final VersionNumber Mavericks = new VersionNumber(10,9,0);
    }

    /**
     * Returns {@code true} if the given {@link CPUType}s and {@link ABIType}s are compatible.
     */
    public static final boolean isCompatible(final CPUType cpu1, final ABIType abi1, final CPUType cpu2, final ABIType abi2) {
        return cpu1.isCompatible(cpu2) && abi1.isCompatible(abi2);
    }

    //
    // static initialization order:
    //

    /** Version 1.6. As a JVM version, it enables certain JVM 1.6 features. */
    public static final VersionNumber Version16;
    /** Version 1.7. As a JVM version, it enables certain JVM 1.7 features. */
    public static final VersionNumber Version17;
    /** Version 1.8. As a JVM version, it enables certain JVM 1.8 features. */
    public static final VersionNumber Version18;
    /** Version 1.9. As a JVM version, it enables certain JVM 1.9 features. */
    public static final VersionNumber Version19;

    public static final String OS;
    public static final String OS_lower;
    public static final String OS_VERSION;
    public static final VersionNumber OS_VERSION_NUMBER;
    public static final String ARCH;
    public static final String ARCH_lower;
    public static final String JAVA_VENDOR;
    public static final String JAVA_VENDOR_URL;
    public static final String JAVA_VERSION;
    public static final VersionNumber JAVA_VERSION_NUMBER;
    public static final int JAVA_VERSION_UPDATE;
    public static final String JAVA_VM_NAME;
    public static final String JAVA_RUNTIME_NAME;
    /** True if having {@link java.nio.LongBuffer} and {@link java.nio.DoubleBuffer} available. */
    public static final boolean JAVA_SE;
    /**
     * True only if being compatible w/ language level 6, e.g. JRE 1.6.
     * <p>
     * Implies {@link #isJavaSE()}.
     * </p>
     * <p>
     * <i>Note</i>: We claim Android is compatible.
     * </p>
     */
    public static final boolean JAVA_6;

    public static final String NEWLINE;
    public static final boolean LITTLE_ENDIAN;

    public static final CPUType CPU_ARCH;
    public static final ABIType ABI_TYPE;
    public static final OSType OS_TYPE;
    public static final String os_and_arch;

    static {
        Version16 = new VersionNumber(1, 6, 0);
        Version17 = new VersionNumber(1, 7, 0);
        Version18 = new VersionNumber(1, 8, 0);
        Version19 = new VersionNumber(1, 9, 0);

        // We don't seem to need an AccessController.doPrivileged() block
        // here as these system properties are visible even to unsigned Applets.
        final boolean isAndroid = AndroidVersion.isAvailable; // also triggers it's static initialization
        JAVA_VENDOR = System.getProperty("java.vendor");
        JAVA_VENDOR_URL = System.getProperty("java.vendor.url");
        JAVA_VERSION = System.getProperty("java.version");
        JAVA_VERSION_NUMBER = new VersionNumber(JAVA_VERSION);
        {
           int usIdx = JAVA_VERSION.lastIndexOf("-u"); // OpenJDK update notation
           int usOff;
           if( 0 < usIdx ) {
               usOff = 2;
           } else {
               usIdx = JAVA_VERSION.lastIndexOf("_"); // Oracle update notation
               usOff = 1;
           }
           if( 0 < usIdx ) {
               final String buildS = PlatformPropsImpl.JAVA_VERSION.substring(usIdx+usOff);
               final VersionNumber update = new VersionNumber(buildS);
               JAVA_VERSION_UPDATE = update.getMajor();
           } else {
               JAVA_VERSION_UPDATE = 0;
           }
        }
        JAVA_VM_NAME = System.getProperty("java.vm.name");
        JAVA_RUNTIME_NAME = getJavaRuntimeNameImpl();
        JAVA_SE = initIsJavaSE();
        JAVA_6 = JAVA_SE && ( isAndroid || JAVA_VERSION_NUMBER.compareTo(Version16) >= 0 ) ;

        NEWLINE = System.getProperty("line.separator");

        OS =  System.getProperty("os.name");
        OS_lower = OS.toLowerCase();
        OS_VERSION =  System.getProperty("os.version");
        OS_VERSION_NUMBER = new VersionNumber(OS_VERSION);
        OS_TYPE = getOSTypeImpl(OS_lower, isAndroid);

        // Hard values, i.e. w/ probing binaries
        final String elfCpuName;
        final CPUType elfCpuType;
        final ABIType elfABIType;
        final int elfLittleEndian;
        final boolean elfValid;
        {
            final String[] _elfCpuName = { null };
            final CPUType[] _elfCpuType = { null };
            final ABIType[] _elfAbiType = { null };
            final int[] _elfLittleEndian = { 0 }; // 1 - little, 2 - big
            final boolean[] _elfValid = { false };
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    RandomAccessFile in = null;
                    try {
                        final File file = queryElfFile(OS_TYPE);
                        if(DEBUG) {
                            System.err.println("ELF-1: Using "+file);
                        }
                        in = new RandomAccessFile(file, "r");
                        final ElfHeaderPart1 eh1 = readElfHeaderPart1(OS_TYPE, in);
                        if(DEBUG) {
                            System.err.println("ELF-1: Got "+eh1);
                        }
                        if( null != eh1 ) {
                            final ElfHeaderPart2 eh2 = readElfHeaderPart2(eh1, in);
                            if(DEBUG) {
                                System.err.println("ELF-2: Got "+eh2);
                            }
                            if( null != eh2 ) {
                                _elfCpuName[0] = eh2.cpuName;
                                _elfCpuType[0] = eh2.cpuType;
                                _elfAbiType[0] = eh2.abiType;
                                if( eh1.isLittleEndian() ) {
                                    _elfLittleEndian[0] = 1;
                                } else if( eh1.isBigEndian() ) {
                                    _elfLittleEndian[0] = 2;
                                }
                                _elfValid[0] = true;
                            }
                        }
                    } catch (final Throwable t) {
                        if(DEBUG) {
                            t.printStackTrace();
                        }
                    } finally {
                        if(null != in) {
                            try {
                                in.close();
                            } catch (final IOException e) { }
                        }
                    }
                    return null;
                } });
            elfCpuName = _elfCpuName[0];
            elfCpuType = _elfCpuType[0];
            elfABIType = _elfAbiType[0];
            elfLittleEndian = _elfLittleEndian[0];
            elfValid = _elfValid[0];
            if( DEBUG ) {
                System.err.println("Platform.Elf: valid "+elfValid+", elfCpuName "+elfCpuName+", cpuType "+elfCpuType+", abiType "+elfABIType+", elfLittleEndian "+elfLittleEndian);
            }
        }

        // Determine endianess, favor ELF value
        final boolean littleEndian = queryIsLittleEndianImpl();
        if( elfValid ) {
            switch( elfLittleEndian ) {
                case 1:
                    LITTLE_ENDIAN = true;
                    break;
                case 2:
                    LITTLE_ENDIAN = false;
                    break;
                default:
                    LITTLE_ENDIAN = littleEndian;
                    break;
            }
        } else {
            LITTLE_ENDIAN = littleEndian;
        }
        if( DEBUG ) {
            System.err.println("Platform.Endian: test-little "+littleEndian+", elf[valid "+elfValid+", val "+elfLittleEndian+"] -> LITTLE_ENDIAN "+LITTLE_ENDIAN);
        }

        // Property values for comparison
        // We might take the property values even if ELF values are available,
        // since the latter only reflect the CPU/ABI version of the binary files!
        final String propARCH = System.getProperty("os.arch");
        final String propARCH_lower = propARCH.toLowerCase();
        final CPUType propCpuType = CPUType.query(propARCH_lower);
        final ABIType propABIType = ABIType.query(propCpuType, propARCH_lower);
        if( DEBUG ) {
            System.err.println("Platform.Property: ARCH "+propARCH+", CpuType "+propCpuType+", ABIType "+propABIType);
        }

        final int strategy;
        if( isAndroid ) {
            if( DEBUG ) {
                System.err.println("Android: CPU_ABI1 str "+AndroidVersion.CPU_ABI+", CPU_TYPE "+AndroidVersion.CPU_TYPE+", ABI_TYPE "+AndroidVersion.ABI_TYPE);
                System.err.println("Android: CPU_ABI2 str "+AndroidVersion.CPU_ABI2+", CPU_TYPE2 "+AndroidVersion.CPU_TYPE2+", ABI_TYPE2 "+AndroidVersion.ABI_TYPE2);
            }
            if( elfValid ) {
                if( null != AndroidVersion.CPU_TYPE &&
                    isCompatible(elfCpuType, elfABIType, AndroidVersion.CPU_TYPE, AndroidVersion.ABI_TYPE) )
                {
                    // ELF matches Android-1
                    ARCH = AndroidVersion.CPU_ABI;
                    ARCH_lower = ARCH;
                    CPU_ARCH = AndroidVersion.CPU_TYPE;
                    strategy = 110;
                } else if( null != AndroidVersion.CPU_TYPE2 &&
                           isCompatible(elfCpuType, elfABIType, AndroidVersion.CPU_TYPE2, AndroidVersion.ABI_TYPE2) )
                {
                    // ELF matches Android-2
                    ARCH = AndroidVersion.CPU_ABI2;
                    ARCH_lower = ARCH;
                    CPU_ARCH = AndroidVersion.CPU_TYPE2;
                    strategy = 111;
                } else {
                    // We assume our ELF data beats AndroidVersion info (correctness)
                    ARCH = elfCpuType.toString();
                    ARCH_lower = ARCH.toLowerCase();
                    CPU_ARCH = elfCpuType;
                    strategy = 112;
                }
                ABI_TYPE = elfABIType;
            } else {
                if( AndroidVersion.CPU_TYPE.family == CPUFamily.ARM ||
                    null == AndroidVersion.CPU_TYPE2 ) {
                    // Favor Android-1: Either b/c ARM Family, or no Android-2
                    ARCH = AndroidVersion.CPU_ABI;
                    ARCH_lower = ARCH;
                    CPU_ARCH = AndroidVersion.CPU_TYPE;
                    ABI_TYPE = AndroidVersion.ABI_TYPE;
                    strategy = 120;
                } else {
                    // Last resort Android-2
                    ARCH = AndroidVersion.CPU_ABI2;
                    ARCH_lower = ARCH;
                    CPU_ARCH = AndroidVersion.CPU_TYPE2;
                    ABI_TYPE = AndroidVersion.ABI_TYPE2;
                    strategy = 121;
                }
            }
        } else {
            if( elfValid ) {
                if( isCompatible(elfCpuType, elfABIType, propCpuType, propABIType) ) {
                    // Use property ARCH, compatible w/ ELF
                    ARCH = propARCH;
                    ARCH_lower = propARCH_lower;
                    CPU_ARCH = propCpuType;
                    ABI_TYPE = propABIType;
                    strategy = 210;
                } else {
                    // use ELF ARCH
                    ARCH = elfCpuName;
                    ARCH_lower = elfCpuName;
                    CPU_ARCH = elfCpuType;
                    ABI_TYPE = elfABIType;
                    strategy = 211;
                }
            } else {
                // Last resort: properties
                ARCH = propARCH;
                ARCH_lower = propARCH_lower;
                CPU_ARCH = propCpuType;
                ABI_TYPE = propABIType;
                strategy = 220;
            }
        }
        if( DEBUG ) {
            System.err.println("Platform.Hard: ARCH "+ARCH+", CPU_ARCH "+CPU_ARCH+", ABI_TYPE "+ABI_TYPE+" - strategy "+strategy+"(isAndroid "+isAndroid+", elfValid "+elfValid+")");
        }
        os_and_arch = getOSAndArch(OS_TYPE, CPU_ARCH, ABI_TYPE, LITTLE_ENDIAN);
    }

    protected PlatformPropsImpl() {}

    private static final String getJavaRuntimeNameImpl() {
        // the fast path, check property Java SE instead of traversing through the ClassLoader
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
              return System.getProperty("java.runtime.name");
            }
          });
    }

    private static final boolean initIsJavaSE() {
        if( null != JAVA_RUNTIME_NAME && JAVA_RUNTIME_NAME.indexOf("Java SE") != -1) {
            return true;
        }

        // probe for classes we need on a SE environment
        try {
            Class.forName("java.nio.LongBuffer");
            Class.forName("java.nio.DoubleBuffer");
            return true;
        } catch(final ClassNotFoundException ex) {
            // continue with Java SE check
        }

        return false;
    }

    private static final boolean queryIsLittleEndianImpl() {
        final ByteBuffer tst_b = Buffers.newDirectByteBuffer(Buffers.SIZEOF_INT); // 32bit in native order
        final IntBuffer tst_i = tst_b.asIntBuffer();
        final ShortBuffer tst_s = tst_b.asShortBuffer();
        tst_i.put(0, 0x0A0B0C0D);
        return 0x0C0D == tst_s.get(0);
    }

    @SuppressWarnings("unused")
    private static final boolean contains(final String data, final String[] search) {
        if(null != data && null != search) {
            for(int i=0; i<search.length; i++) {
                if(data.indexOf(search[i]) >= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the {@link ABIType} of the current platform using given {@link CPUType cpuType}
     * and {@link OSType osType} as a hint.
     * <p>
     * For Elf parsing one of the following binaries is used:
     * <ul>
     *  <li>Linux: Current executable</li>
     *  <li>Android: Found gluegen-rt library</li>
     *  <li>Other: A found java/jvm native library.</li>
     * </ul>
     * </p>
     * <p>
     * Elf ARM Tags are read using {@link ElfHeader}, .. and {@link SectionArmAttributes#abiVFPArgsAcceptsVFPVariant(byte)}.
     * </p>
     */
    private static final File queryElfFile(final OSType osType) {
        File file = null;
        try {
            if( OSType.ANDROID == osType ) {
                file = new File(NativeLibrary.findLibrary("gluegen-rt", PlatformPropsImpl.class.getClassLoader()));
            } else {
                if( OSType.LINUX == osType ) {
                    file = new File("/proc/self/exe");
                    if( !checkFileReadAccess(file) ) {
                        file = null;
                    }
                }
                if( null == file ) {
                    file = findSysLib("java");
                }
                if( null == file ) {
                    file = findSysLib("jvm");
                }
            }
        } catch(final Throwable t) {
            if(DEBUG) {
                t.printStackTrace();
            }
        }
        return file;
    }
    private static final ElfHeaderPart1 readElfHeaderPart1(final OSType osType, final RandomAccessFile in) {
        ElfHeaderPart1 res = null;
        try {
            res = ElfHeaderPart1.read(osType, in);
        } catch(final Throwable t) {
            if(DEBUG) {
                System.err.println("Caught: "+t.getMessage());
                t.printStackTrace();
            }
        }
        return res;
    }
    private static final ElfHeaderPart2 readElfHeaderPart2(final ElfHeaderPart1 eh1, final RandomAccessFile in) {
        ElfHeaderPart2 res = null;
        try {
            res = ElfHeaderPart2.read(eh1, in);
        } catch(final Throwable t) {
            if(DEBUG) {
                System.err.println("Caught: "+t.getMessage());
                t.printStackTrace();
            }
        }
        return res;
    }
    private static boolean checkFileReadAccess(final File file) {
        try {
            return file.isFile() && file.canRead();
        } catch (final Throwable t) { }
        return false;
    }
    private static File findSysLib(final String libName) {
        final ClassLoader cl = PlatformPropsImpl.class.getClassLoader();
        final List<String> possibleLibPaths = NativeLibrary.enumerateLibraryPaths(libName, libName, libName, true, cl);
        for(int i=0; i<possibleLibPaths.size(); i++) {
            final String libPath = possibleLibPaths.get(i);
            final File lib = new File(libPath);
            if(DEBUG) {
                System.err.println("findSysLib #"+i+": test "+lib);
            }
            if( checkFileReadAccess(lib) ) {
                return lib;
            }
            if(DEBUG) {
                System.err.println("findSysLib #"+i+": "+lib+" not readable");
            }
        }
        return null;
    }

    private static final OSType getOSTypeImpl(final String osLower, final boolean isAndroid) throws RuntimeException {
        if ( isAndroid ) {
            return OSType.ANDROID;
        }
        if ( osLower.startsWith("linux") ) {
            return OSType.LINUX;
        }
        if ( osLower.startsWith("freebsd") ) {
            return OSType.FREEBSD;
        }
        if ( osLower.startsWith("android") ) {
            return OSType.ANDROID;
        }
        if ( osLower.startsWith("mac os x") ||
             osLower.startsWith("darwin") ) {
            return OSType.MACOS;
        }
        if ( osLower.startsWith("sunos") ) {
            return OSType.SUNOS;
        }
        if ( osLower.startsWith("hp-ux") ) {
            return OSType.HPUX;
        }
        if ( osLower.startsWith("windows") ) {
            return OSType.WINDOWS;
        }
        if ( osLower.startsWith("kd") ) {
            return OSType.OPENKODE;
        }
        throw new RuntimeException("Please port OS detection to your platform (" + OS_lower + "/" + ARCH_lower + ")");
    }

    /**
     * kick off static initialization of <i>platform property information</i>
     */
    public static void initSingleton() { }

    /**
     * Returns the GlueGen common name for the given
     * {@link OSType}, {@link CPUType}, {@link ABIType} and {@code littleEndian}.
     * <p>
     * Consult 'gluegen/make/gluegen-cpptasks-base.xml' to complete/sync mapping!
     * </p>
     *
     * An excerpt of supported <code>os.and.arch</code> strings:
     * <ul>
     *   <li>android-armv6</li>
     *   <li>android-aarch64</li>
     *   <li>linux-armv6</li>
     *   <li>linux-armv6hf</li>
     *   <li>linux-i586</li>
     *   <li>linux-ppc</li>
     *   <li>linux-mips</li>
     *   <li>linux-mipsel</li>
     *   <li>linux-superh</li>
     *   <li>linux-sparc</li>
     *   <li>linux-aarch64</li>
     *   <li>linux-amd64</li>
     *   <li>linux-ppc64</li>
     *   <li>linux-mips64</li>
     *   <li>linux-ia64</li>
     *   <li>linux-sparcv9</li>
     *   <li>linux-risc2.0</li>
     *   <li>freebsd-i586</li>
     *   <li>freebsd-amd64</li>
     *   <li>hpux-hppa</li>
     *   <li>macosx-universal</li>
     *   <li>solaris-sparc</li>
     *   <li>solaris-sparcv9</li>
     *   <li>solaris-amd64</li>
     *   <li>solaris-i586</li>
     *   <li>windows-amd64</li>
     *   <li>windows-i586</li>
     * </ul>
     * @return The <i>os.and.arch</i> value.
     */
    public static final String getOSAndArch(final OSType osType, final CPUType cpuType, final ABIType abiType, final boolean littleEndian) {
        final String os_;
        final String _and_arch_tmp, _and_arch_final;

        switch( cpuType ) {
            case ARM:
            case ARMv5:
            case ARMv6:
            case ARMv7:
                if( ABIType.EABI_GNU_ARMHF == abiType ) {
                    _and_arch_tmp = "armv6hf";
                } else {
                    _and_arch_tmp = "armv6";
                }
                break;
            case X86_32:
                _and_arch_tmp = "i586";
                break;
            case PPC:
                _and_arch_tmp = "ppc";
                break;
            case MIPS_32:
                _and_arch_tmp = littleEndian ? "mipsel" : "mips";
                break;
            case SuperH:
                _and_arch_tmp = "superh";
                break;
            case SPARC_32:
                _and_arch_tmp = "sparc";
                break;

            case ARM64:
            case ARMv8_A:
                _and_arch_tmp = "aarch64";
                break;
            case X86_64:
                _and_arch_tmp = "amd64";
                break;
            case PPC64:
                _and_arch_tmp = "ppc64";
                break;
            case MIPS_64:
                _and_arch_tmp = "mips64";
                break;
            case IA64:
                _and_arch_tmp = "ia64";
                break;
            case SPARCV9_64:
                _and_arch_tmp = "sparcv9";
                break;
            case PA_RISC2_0:
                _and_arch_tmp = "risc2.0";
                break;
            default:
                throw new InternalError("Unhandled CPUType: "+cpuType);
        }

        switch( osType ) {
            case ANDROID:
              os_ = "android";
              _and_arch_final = _and_arch_tmp;
              break;
            case MACOS:
              os_ = "macosx";
              _and_arch_final = "universal";
              break;
            case WINDOWS:
              os_ = "windows";
              _and_arch_final = _and_arch_tmp;
              break;
            case OPENKODE:
              os_ = "openkode";             // TODO: think about that
              _and_arch_final = _and_arch_tmp;
              break;
            case LINUX:
              os_ = "linux";
              _and_arch_final = _and_arch_tmp;
              break;
            case FREEBSD:
              os_ = "freebsd";
              _and_arch_final = _and_arch_tmp;
              break;
            case SUNOS:
              os_ = "solaris";
              _and_arch_final = _and_arch_tmp;
              break;
            case HPUX:
              os_ = "hpux";
              _and_arch_final = "hppa";     // TODO: really only hppa ?
              break;
            default:
              throw new InternalError("Unhandled OSType: "+osType);
        }
        return os_ + "-" + _and_arch_final;
    }

}
