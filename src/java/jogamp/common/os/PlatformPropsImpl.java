package jogamp.common.os;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.os.AndroidVersion;
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
    //
    // static initialization order:
    //
    
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
    public static final String JAVA_VM_NAME;
    public static final String JAVA_RUNTIME_NAME;
    public static final boolean JAVA_SE;
    
    public static final String NEWLINE;
    public static final boolean LITTLE_ENDIAN;
    
    public static final CPUType CPU_ARCH;
    public static final ABIType ABI_TYPE;
    public static final OSType OS_TYPE;
    public static final String os_and_arch;
    
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
        JAVA_VM_NAME = System.getProperty("java.vm.name");
        JAVA_RUNTIME_NAME = getJavaRuntimeNameImpl();
        JAVA_SE = initIsJavaSE();
        
        NEWLINE = System.getProperty("line.separator");
        LITTLE_ENDIAN = queryIsLittleEndianImpl();
        
        CPU_ARCH = getCPUTypeImpl(ARCH_lower);
        ABI_TYPE = guessABITypeImpl(CPU_ARCH);
        OS_TYPE = getOSTypeImpl();
        os_and_arch = getOSAndArch(OS_TYPE, CPU_ARCH, ABI_TYPE);        
    }

    protected PlatformPropsImpl() {}
    
    private static final String getJavaRuntimeNameImpl() {
        // the fast path, check property Java SE instead of traversing through the ClassLoader
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
              return System.getProperty("java.runtime.name");
            }
          });
    }
    
    private static final boolean initIsJavaSE() {
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
    
    private static final ABIType guessABITypeImpl(CPUType cpuType) {
        if(CPUFamily.ARM != cpuType.family) {
            return ABIType.GENERIC_ABI;
        }
        return AccessController.doPrivileged(new PrivilegedAction<ABIType>() {
            private final String[] gnueabihf = new String[] { "gnueabihf", "armhf" };
            public ABIType run() {                    
                if ( contains(System.getProperty("sun.boot.library.path"), gnueabihf) ||
                     contains(System.getProperty("java.library.path"), gnueabihf) ||
                     contains(System.getProperty("java.home"), gnueabihf) ) {
                    return ABIType.EABI_GNU_ARMHF;
                }
                return ABIType.EABI_GNU_ARMEL;
            } } );
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
                _os_and_arch = "armv6"; // TODO: sync with gluegen-cpptasks-base.xml
                break;
            case ARMv5:
                _os_and_arch = "armv6";
                break;
            case ARMv6:
                _os_and_arch = "armv6";
                break;
            case ARMv7:
                _os_and_arch = "armv6";
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
