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
import jogamp.common.os.elf.ElfHeader;
import jogamp.common.os.elf.SectionArmAttributes;
import jogamp.common.os.elf.SectionHeader;

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

    //
    // static initialization order:
    //

    /** Version 1.6. As a JVM version, it enables certain JVM 1.6 features. */
    public static final VersionNumber Version16;
    /** Version 1.7. As a JVM version, it enables certain JVM 1.7 features. */
    public static final VersionNumber Version17;

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
    /** True if being compatible w/ language level 6, e.g. JRE 1.6. Implies {@link #JAVA_SE}. <i>Note</i>: We claim Android is compatible. */
    public static final boolean JAVA_6;

    public static final String NEWLINE;
    public static final boolean LITTLE_ENDIAN;

    /* pp */ static final CPUType sCpuType;

    public static final CPUType CPU_ARCH;
    public static final ABIType ABI_TYPE;
    public static final OSType OS_TYPE;
    public static final String os_and_arch;

    static {
        Version16 = new VersionNumber(1, 6, 0);
        Version17 = new VersionNumber(1, 7, 0);

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

        LITTLE_ENDIAN = queryIsLittleEndianImpl();

        // Soft values, i.e. w/o probing binaries
        final String sARCH = System.getProperty("os.arch");
        final String sARCH_lower = sARCH.toLowerCase();
        sCpuType = getCPUTypeImpl(sARCH_lower);
        if( DEBUG ) {
            System.err.println("Platform.Soft: str "+sARCH+", cpuType "+sCpuType);
        }

        // Hard values, i.e. w/ probing binaries
        //
        // FIXME / HACK:
        //   We use sCPUType for MachineDescriptionRuntime.getStatic()
        //   until we have determined the final CPU_TYPE, etc.
        //   MachineDescriptionRuntime gets notified via MachineDescriptionRuntime.notifyPropsInitialized() below.
        //
        //   We could use Elf Ehdr's machine value to determine the bit-size
        //   used for it's offset table!
        //   However, 'os.arch' should be a good guess for this task.
        final CPUType ehCpuType;
        final ABIType ehAbiType;
        final boolean ehValid;
        {
            final CPUType[] _ehCpuType = { null };
            final ABIType[] _ehAbiType = { null };
            final ElfHeader eh = queryABITypeImpl(OS_TYPE, _ehCpuType, _ehAbiType);
            if( null != eh && null != _ehCpuType[0] && null != _ehAbiType[0] ) {
                ehCpuType = _ehCpuType[0];
                ehAbiType = _ehAbiType[0];
                if( isAndroid ) {
                    if( DEBUG ) {
                        System.err.println("Android: CPU_ABI1 str "+AndroidVersion.CPU_ABI+", cpu "+AndroidVersion.CPU_TYPE+", abi "+AndroidVersion.ABI_TYPE);
                        System.err.println("Android: CPU_ABI2 str "+AndroidVersion.CPU_ABI2+", cpu "+AndroidVersion.CPU_TYPE2+", abi "+AndroidVersion.ABI_TYPE2);
                    }
                    final CPUFamily aCpuFamily1 = null != AndroidVersion.CPU_TYPE ? AndroidVersion.CPU_TYPE.family : null;
                    final CPUFamily aCpuFamily2 = null != AndroidVersion.CPU_TYPE2 ? AndroidVersion.CPU_TYPE2.family : null;
                    if( ehCpuType.family != aCpuFamily1 && ehCpuType.family != aCpuFamily2 ) {
                        // Ooops !
                        ehValid = false;
                    } else {
                        ehValid = true;
                    }
                } else {
                    if( ehCpuType.family != sCpuType.family ) {
                        // Ooops !
                        ehValid = false;
                    } else {
                        ehValid = true;
                    }
                }
                if( DEBUG ) {
                    System.err.println("Platform.Elf: cpuType "+ehCpuType+", abiType "+ehAbiType+", valid "+ehValid);
                }
            } else {
                ehCpuType = null;
                ehAbiType = null;
                ehValid = false;
                if( DEBUG ) {
                    System.err.println("Platform.Elf: n/a");
                }
            }
        }
        if( isAndroid ) {
            if( ehValid ) {
                if( ehCpuType.family == AndroidVersion.CPU_TYPE.family ) {
                    ARCH = AndroidVersion.CPU_ABI;
                    CPU_ARCH = AndroidVersion.CPU_TYPE;
                } else {
                    ARCH = AndroidVersion.CPU_ABI2;
                    CPU_ARCH = AndroidVersion.CPU_TYPE2;
                }
                ABI_TYPE = ehAbiType;
            } else {
                // default
                if( AndroidVersion.CPU_TYPE.family == CPUFamily.ARM || null == AndroidVersion.CPU_TYPE2 ) {
                    ARCH = AndroidVersion.CPU_ABI;
                    CPU_ARCH = AndroidVersion.CPU_TYPE;
                    ABI_TYPE = AndroidVersion.ABI_TYPE;
                } else {
                    ARCH = AndroidVersion.CPU_ABI2;
                    CPU_ARCH = AndroidVersion.CPU_TYPE2;
                    ABI_TYPE = AndroidVersion.ABI_TYPE2;
                }
            }
            ARCH_lower  = ARCH;
        } else {
            ARCH = sARCH;
            ARCH_lower = sARCH_lower;
            if( ehValid && CPUFamily.ARM == ehCpuType.family ) {
                // Use Elf for ARM
                CPU_ARCH = ehCpuType;
                ABI_TYPE = ehAbiType;
            } else {
                // Otherwise trust detailed os.arch (?)
                CPU_ARCH = sCpuType;
                ABI_TYPE = ABIType.GENERIC_ABI;
            }
        }
        MachineDescriptionRuntime.notifyPropsInitialized();
        os_and_arch = getOSAndArch(OS_TYPE, CPU_ARCH, ABI_TYPE);
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

    private static final CPUType getCPUTypeImpl(final String archLower) {
        if(        archLower.equals("x86")  ||         // jvm + android
                   archLower.equals("i386") ||
                   archLower.equals("i486") ||
                   archLower.equals("i586") ||
                   archLower.equals("i686") ) {
            return CPUType.X86_32;
        } else if( archLower.equals("x86_64") ||
                   archLower.equals("amd64")  ) {
            return CPUType.X86_64;
        } else if( archLower.equals("ia64") ) {
            return CPUType.IA64;
        } else if( archLower.equals("arm") ) {
            return CPUType.ARM;
        } else if( archLower.equals("armv5l") ) {
            return CPUType.ARMv5;
        } else if( archLower.equals("armv6l") ) {
            return CPUType.ARMv6;
        } else if( archLower.equals("armv7l") ||
                   archLower.equals("armeabi") ||      // android
                   archLower.equals("armeabi-v7a") ) { // android
            return CPUType.ARMv7;
        } else if( archLower.equals("sparc") ) {
            return CPUType.SPARC_32;
        } else if( archLower.equals("sparcv9") ) {
            return CPUType.SPARCV9_64;
        } else if( archLower.equals("pa_risc2.0") ) {
            return CPUType.PA_RISC2_0;
        } else if( archLower.equals("ppc") ) {
            return CPUType.PPC;
        } else if( archLower.equals("mips") ) {        // android
            return CPUType.MIPS_32;
        } else {
            throw new RuntimeException("Please port CPU detection to your platform (" + OS_lower + "/" + archLower + ")");
        }
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
    private static final ElfHeader queryABITypeImpl(final OSType osType, final CPUType[] cpuType, final ABIType[] abiType) {
        return AccessController.doPrivileged(new PrivilegedAction<ElfHeader>() {
            @Override
            public ElfHeader run() {
                ElfHeader res = null;
                try {
                    File file = null;
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
                    if( null != file ) {
                        res = queryABITypeImpl(file, cpuType, abiType);
                    }
                } catch(final Throwable t) {
                    if(DEBUG) {
                        t.printStackTrace();
                    }
                }
                return res;
            } } );
    }
    private static final ElfHeader queryABITypeImpl(final File file, final CPUType[] cpuType, final ABIType[] abiType) {
        ElfHeader res = null;
        RandomAccessFile in = null;
        try {
            in = new RandomAccessFile(file, "r");
            final ElfHeader eh = ElfHeader.read(in);
            if(DEBUG) {
                System.err.println("ELF: Got HDR "+file+": "+eh);
            }
            if( eh.isArm() ) {
                boolean abiVFPArgsAcceptsVFPVariant = false;
                final SectionHeader sh = eh.getSectionHeader(SectionHeader.SHT_ARM_ATTRIBUTES);
                if( null != sh ) {
                    if(DEBUG) {
                        System.err.println("ELF: Got ARM Attribs Section Header: "+sh);
                    }
                    final SectionArmAttributes sArmAttrs = (SectionArmAttributes) sh.readSection(in);
                    if(DEBUG) {
                        System.err.println("ELF: Got ARM Attribs Section Block : "+sArmAttrs);
                    }
                    final SectionArmAttributes.Attribute abiVFPArgsAttr = sArmAttrs.get(SectionArmAttributes.Tag.ABI_VFP_args);
                    if( null != abiVFPArgsAttr ) {
                        abiVFPArgsAcceptsVFPVariant = SectionArmAttributes.abiVFPArgsAcceptsVFPVariant(abiVFPArgsAttr.getULEB128());
                    }
                }
                cpuType[0] = CPUType.ARM; // lowest denominator, ok for us
                abiType[0] = abiVFPArgsAcceptsVFPVariant ? ABIType.EABI_GNU_ARMHF : ABIType.EABI_GNU_ARMEL;
                if(DEBUG) {
                    System.err.println("ELF: abiARM, abiVFPArgsAcceptsVFPVariant "+abiVFPArgsAcceptsVFPVariant);
                }
            } else if ( eh.isX86_64() ) {
                cpuType[0] = CPUType.X86_64;
                abiType[0] = ABIType.GENERIC_ABI;
            } else if ( eh.isX86_32() ) {
                cpuType[0] = CPUType.X86_32;
                abiType[0] = ABIType.GENERIC_ABI;
            } else if ( eh.isIA64() ) {
                cpuType[0] = CPUType.IA64;
                abiType[0] = ABIType.GENERIC_ABI;
            } else if ( eh.isMips() ) {
                cpuType[0] = CPUType.MIPS_32; // FIXME
                abiType[0] = ABIType.GENERIC_ABI;
            }
            res = eh;
        } catch(final Throwable t) {
            if(DEBUG) {
                System.err.println("Caught: "+t.getMessage());
                t.printStackTrace();
            }
        } finally {
            if(null != in) {
                try {
                    in.close();
                } catch (final IOException e) { }
            }
        }
        if(DEBUG) {
            System.err.println("ELF: res "+res+", cpuType "+cpuType[0]+", abiType "+abiType[0]);
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
     *   <li>linux-armv6</li>
     *   <li>linux-armv6hf</li>
     *   <li>android-armv6</li>
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
    public static final String getOSAndArch(final OSType osType, final CPUType cpuType, final ABIType abiType) {
        final String os_;
        final String _and_arch_tmp, _and_arch_final;

        switch( cpuType ) {
            case X86_32:
                _and_arch_tmp = "i586";
                break;
            case ARM:
            case ARMv5:
            case ARMv6:
            case ARMv7:
                if( ABIType.EABI_GNU_ARMHF == abiType ) {
                    _and_arch_tmp = "armv6hf" ; // TODO: sync with gluegen-cpptasks-base.xml
                } else {
                    _and_arch_tmp = "armv6";    // TODO: sync with gluegen-cpptasks-base.xml
                }
                break;
            case SPARC_32:
                _and_arch_tmp = "sparc";
                break;
            case PPC:
                _and_arch_tmp = "ppc";          // TODO: sync with gluegen-cpptasks-base.xml
                break;
            case X86_64:
                _and_arch_tmp = "amd64";
                break;
            case IA64:
                _and_arch_tmp = "ia64";
                break;
            case SPARCV9_64:
                _and_arch_tmp = "sparcv9";
                break;
            case PA_RISC2_0:
                _and_arch_tmp = "risc2.0";      // TODO: sync with gluegen-cpptasks-base.xml
                break;
            default:
                throw new InternalError("Complete case block");
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
              throw new InternalError("Complete case block");
        }
        return os_ + "-" + _and_arch_final;
    }

}
