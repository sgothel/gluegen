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

    //
    // static initialization order:
    //

    /** Version 1.6. As a JVM version, it enables certain JVM 1. features. */
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

    public static final CPUType CPU_ARCH;
    public static final ABIType ABI_TYPE;
    public static final OSType OS_TYPE;
    public static final String os_and_arch;

    static {
        Version16 = new VersionNumber(1, 6, 0);
        Version17 = new VersionNumber(1, 7, 0);
        // We don't seem to need an AccessController.doPrivileged() block
        // here as these system properties are visible even to unsigned Applets.
        OS =  System.getProperty("os.name");
        OS_lower = OS.toLowerCase();
        OS_VERSION =  System.getProperty("os.version");
        OS_VERSION_NUMBER = new VersionNumber(OS_VERSION);
        ARCH = System.getProperty("os.arch");
        ARCH_lower = ARCH.toLowerCase();
        JAVA_VENDOR = System.getProperty("java.vendor");
        JAVA_VENDOR_URL = System.getProperty("java.vendor.url");
        JAVA_VERSION = System.getProperty("java.version");
        JAVA_VERSION_NUMBER = new VersionNumber(JAVA_VERSION);
        {
           final int usIdx = JAVA_VERSION.lastIndexOf("_");
           int jvmUpdate = 0;
           if( usIdx > 0 ) {
               final String buildS = Platform.JAVA_VERSION.substring(usIdx+1);
               try {
                   jvmUpdate = Integer.valueOf(buildS);
               } catch (NumberFormatException nfe) {}
           }
           JAVA_VERSION_UPDATE = jvmUpdate;
        }
        JAVA_VM_NAME = System.getProperty("java.vm.name");
        JAVA_RUNTIME_NAME = getJavaRuntimeNameImpl();
        JAVA_SE = initIsJavaSE();
        JAVA_6 = JAVA_SE && ( AndroidVersion.isAvailable || JAVA_VERSION_NUMBER.compareTo(Version16) >= 0 ) ;

        NEWLINE = System.getProperty("line.separator");
        LITTLE_ENDIAN = queryIsLittleEndianImpl();

        CPU_ARCH = getCPUTypeImpl(ARCH_lower);
        OS_TYPE = getOSTypeImpl();
        ABI_TYPE = queryABITypeImpl(OS_TYPE, CPU_ARCH);
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
        } catch(ClassNotFoundException ex) {
            // continue with Java SE check
        }

        return false;
    }

    private static final boolean queryIsLittleEndianImpl() {
        ByteBuffer tst_b = Buffers.newDirectByteBuffer(Buffers.SIZEOF_INT); // 32bit in native order
        IntBuffer tst_i = tst_b.asIntBuffer();
        ShortBuffer tst_s = tst_b.asShortBuffer();
        tst_i.put(0, 0x0A0B0C0D);
        return 0x0C0D == tst_s.get(0);
    }

    private static final CPUType getCPUTypeImpl(String archLower) {
        if(        archLower.equals("x86")  ||
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
        } else if( archLower.equals("armv7l") ) {
            return CPUType.ARMv7;
        } else if( archLower.equals("sparc") ) {
            return CPUType.SPARC_32;
        } else if( archLower.equals("sparcv9") ) {
            return CPUType.SPARCV9_64;
        } else if( archLower.equals("pa_risc2.0") ) {
            return CPUType.PA_RISC2_0;
        } else if( archLower.equals("ppc") ) {
            return CPUType.PPC;
        } else {
            throw new RuntimeException("Please port CPU detection to your platform (" + OS_lower + "/" + archLower + ")");
        }
    }

    @SuppressWarnings("unused")
    private static final boolean contains(String data, String[] search) {
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
     * Note the following queries are performed:
     * <ul>
     *   <li> not {@link CPUFamily#ARM} -> {@link ABIType#GENERIC_ABI} </li>
     *   <li> else
     *   <ul>
     *     <li> {@link OSType#ANDROID} -> {@link ABIType#EABI_GNU_ARMEL} (due to EACCES, Permission denied)</li>
     *     <li> else
     *     <ul>
     *       <li> Elf ARM Tags -> {@link ABIType#EABI_GNU_ARMEL}, {@link ABIType#EABI_GNU_ARMHF}</li>
     *       <li> On Error -> {@link ABIType#EABI_GNU_ARMEL}</li>
     *     </ul></li>
     *   </ul></li>
     * </ul>
     * </p>
     * <p>
     * For Elf parsing either the current executable is used (Linux) or a found java/jvm native library.
     * </p>
     * <p>
     * Elf ARM Tags are read using {@link ElfHeader}, .. and {@link SectionArmAttributes#abiVFPArgsAcceptsVFPVariant(byte)}.
     * </p>
     * @param osType
     * @param cpuType
     *
     * @return
     */
    private static final ABIType queryABITypeImpl(final OSType osType, final CPUType cpuType) {
        if( CPUFamily.ARM  != cpuType.family ) {
            return ABIType.GENERIC_ABI;
        }
        if( OSType.ANDROID == osType ) { // EACCES (Permission denied) - We assume a not rooted device!
            return ABIType.EABI_GNU_ARMEL;
        }
        return AccessController.doPrivileged(new PrivilegedAction<ABIType>() {
            private final String GNU_LINUX_SELF_EXE = "/proc/self/exe";
            @Override
            public ABIType run() {
                boolean abiARM = false;
                boolean abiVFPArgsAcceptsVFPVariant = false;
                RandomAccessFile in = null;
                try {
                    File file = null;
                    if( OSType.LINUX == osType ) {
                        file = new File(GNU_LINUX_SELF_EXE);
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
                    if( null != file ) {
                        in = new RandomAccessFile(file, "r");
                        final ElfHeader eh = ElfHeader.read(in);
                        if(DEBUG) {
                            System.err.println("ELF: Got HDR "+GNU_LINUX_SELF_EXE+": "+eh);
                        }
                        abiARM = eh.isArm();
                        if( abiARM ) {
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
                        }
                    }
                } catch(Throwable t) {
                    if(DEBUG) {
                        t.printStackTrace();
                    }
                } finally {
                    if(null != in) {
                        try {
                            in.close();
                        } catch (IOException e) { }
                    }
                }
                final ABIType res;
                if( abiARM ) {
                    res = abiVFPArgsAcceptsVFPVariant ? ABIType.EABI_GNU_ARMHF : ABIType.EABI_GNU_ARMEL;
                } else {
                    res = ABIType.GENERIC_ABI;
                }
                if(DEBUG) {
                    System.err.println("ELF: abiARM "+abiARM+", abiVFPArgsAcceptsVFPVariant "+abiVFPArgsAcceptsVFPVariant+" -> "+res);
                }
                return res;
            } } );
    }
    private static boolean checkFileReadAccess(File file) {
        try {
            return file.isFile() && file.canRead();
        } catch (Throwable t) { }
        return false;
    }
    private static File findSysLib(String libName) {
        ClassLoader cl = PlatformPropsImpl.class.getClassLoader();
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

    private static final OSType getOSTypeImpl() throws RuntimeException {
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
     * @return
     */
    public static final String getOSAndArch(OSType osType, CPUType cpuType, ABIType abiType) {
        String _os_and_arch;

        switch( cpuType ) {
            case X86_32:
                _os_and_arch = "i586";
                break;
            case ARM:
            case ARMv5:
            case ARMv6:
            case ARMv7:
                _os_and_arch = "armv6"; // TODO: sync with gluegen-cpptasks-base.xml
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
        if( ABIType.EABI_GNU_ARMHF == abiType ) {
            _os_and_arch = _os_and_arch + "hf" ;
        }
        switch( osType ) {
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

}
