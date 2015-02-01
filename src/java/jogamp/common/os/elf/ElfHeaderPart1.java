/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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
package jogamp.common.os.elf;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import jogamp.common.Debug;
import jogamp.common.os.MachineDataInfoRuntime;

import com.jogamp.common.os.MachineDataInfo;
import com.jogamp.common.os.Platform.ABIType;
import com.jogamp.common.os.Platform.CPUType;
import com.jogamp.common.os.Platform.OSType;

import static jogamp.common.os.elf.IOUtils.readBytes;

/**
 * ELF ABI Header Part-1
 * <p>
 * Part-1 can be read w/o knowledge of CPUType!
 * </p>
 * <p>
 * References:
 * <ul>
 *   <li>http://www.sco.com/developers/gabi/latest/contents.html</li>
 *   <li>https://en.wikipedia.org/wiki/Executable_and_Linkable_Format</li>
 *   <li>http://linux.die.net/man/5/elf</li>
 *   <li>http://infocenter.arm.com/
 *   <ul>
 *      <li>ARM IHI 0044E, current through ABI release 2.09</li>
 *      <li>ARM IHI 0056B: Elf for ARM 64-bit Architecture</li>
 *   </ul></li>
 * </ul>
 * </p>
 */
public class ElfHeaderPart1 {
    static final boolean DEBUG = Debug.debug("Platform");

    /** Size of e_ident array - {@value} */
    public static int EI_NIDENT = 16;

    /** ident byte #0 - {@value} */
    public static final byte ELFMAG0 = 0x7f;
    /** ident byte #1 - {@value} */
    public static final byte ELFMAG1 = 'E';
    /** ident byte #2 - {@value} */
    public static final byte ELFMAG2 = 'L';
    /** ident byte #3 - {@value} */
    public static final byte ELFMAG3 = 'F';

    /** ident byte #4 */
    public static final int EI_CLASS = 4;
    public static final byte ELFCLASSNONE = 0;
    public static final byte ELFCLASS32 = 1;
    public static final byte ELFCLASS64 = 2;

    /** ident byte #5 */
    public static final int EI_DATA = 5;
    public static final byte ELFDATANONE = 0;
    public static final byte ELFDATA2LSB = 1;
    public static final byte ELFDATA2MSB = 2;

    /** ident byte #6 */
    public static final int EI_VERSION = 6;
    public static final byte EV_NONE = 0;
    public static final byte EV_CURRENT = 1;

    /** ident byte #7 */
    public static final int EI_OSABI = 7;
    /** Unix System V ABI - {@value} */
    public static final byte ELFOSABI_SYSV = 0;
    public static final byte ELFOSABI_NONE = ELFOSABI_SYSV;
    /** HP-UX ABI - {@value} */
    public static final byte ELFOSABI_HPUX = 1;
    /** NetBSD ABI - {@value} **/
    public static final byte ELFOSABI_NETBSD = 2;
    /** Linux ABI - {@value} **/
    public static final byte ELFOSABI_LINUX = 3;
    /** Solaris ABI - {@value} **/
    public static final byte ELFOSABI_SOLARIS = 6;
    /** IRIX ABI - {@value} **/
    public static final byte ELFOSABI_IRIX = 7;
    /** FreeBSD ABI - {@value} **/
    public static final byte ELFOSABI_FREEBSD = 8;
    /** ARM architecture ABI - {@value} **/
    public static final byte ELFOSABI_ARM = 8; // FIXME
    /** Stand-alone (embedded) ABI - {@value} **/
    public static final byte ELFOSABI_STANDALONE = 9; // FIXME
    /** TRU64 UNIX ABI - {@value} **/
    public static final byte ELFOSABI_TRU64 = 10;
    /** Novell Modesto ABI - {@value} **/
    public static final byte ELFOSABI_MODESTO = 11;
    /** Open BSD ABI - {@value} **/
    public static final byte ELFOSABI_OPENBSD = 12;
    /** Open VMS ABI - {@value} **/
    public static final byte ELFOSABI_OPENVMS = 13;
    /** Hewlett-Packard Non-Stop Kernel ABI - {@value} **/
    public static final byte ELFOSABI_NSK     = 14;
    /** Amiga Research OS ABI - {@value} **/
    public static final byte ELFOSABI_AROS    = 15;
    /** The FenixOS highly scalable multi-core OS 64-255 Architecture-specific value range - {@value} */
    public static final byte ELFOSABI_FENIXOS = 16;

    /** ident byte #8
     * <p>
     * This byte identifies the version of the ABI to which the object is targeted.
     * This field is used to distinguish among incompatible versions of an ABI.
     * The interpretation of this version number is dependent on the ABI identified by the EI_OSABI field.
     * Applications conforming to this specification use the value 0.
     * </p>
     */
    public static final int EI_ABIVERSION = 8;

    /**
     * ident byte #9 .. ?
     * <p>
     * Start of padding.
     * These bytes are reserved and set to zero.
     * Programs which read them should ignore them.
     * The value for EI_PAD will change in the future if currently unused bytes are given meanings.
     * </p>
     */
    public static final int EI_PAD = 9;

    /** An unknown type - {@value} */
    public static final short ET_NONE   = 0;
    /** A relocatable file - {@value} */
    public static final short ET_REL    = 1;
    /** An executable file - {@value} */
    public static final short ET_EXEC   = 2;
    /** A shared object - {@value} */
    public static final short ET_DYN    = 3;
    /** A core file - {@value} */
    public static final short ET_CORE   = 4;

    public static final short EM_NONE = 0;
    public static final short EM_M32 = 1;
    public static final short EM_SPARC = 2;
    public static final short EM_386 = 3;
    public static final short EM_68K = 4;
    public static final short EM_88K = 5;
    public static final short EM_486 = 6;
    public static final short EM_860 = 7;
    public static final short EM_MIPS = 8;
    public static final short EM_S370 = 9;
    public static final short EM_MIPS_RS3_LE = 10;
    public static final short EM_PARISC = 15;
    public static final short EM_res016 = 16;
    public static final short EM_VPP550 = 17;
    public static final short EM_SPARC32PLUS = 18;
    public static final short EM_960 = 19;
    public static final short EM_PPC = 20;
    public static final short EM_PPC64 = 21;
    public static final short EM_S390 = 22;
    public static final short EM_SPU = 23;
    public static final short EM_V800 = 36;
    public static final short EM_FR20 = 37;
    public static final short EM_RH32 = 38;
    public static final short EM_MCORE = 39;
    public static final short EM_RCE = 39;
    public static final short EM_ARM = 40;
    public static final short EM_OLD_ALPHA = 41;
    public static final short EM_SH = 42;
    public static final short EM_SPARCV9 = 43;
    public static final short EM_TRICORE = 44;
    public static final short EM_ARC = 45;
    public static final short EM_H8_300 = 46;
    public static final short EM_H8_300H = 47;
    public static final short EM_H8S = 48;
    public static final short EM_H8_500 = 49;
    public static final short EM_IA_64 = 50;
    public static final short EM_MIPS_X = 51;
    public static final short EM_COLDFIRE = 52;
    public static final short EM_68HC12 = 53;
    public static final short EM_MMA = 54;
    public static final short EM_PCP = 55;
    public static final short EM_NCPU = 56;
    public static final short EM_NDR1 = 57;
    public static final short EM_STARCORE = 58;
    public static final short EM_ME16 = 59;
    public static final short EM_ST100 = 60;
    public static final short EM_TINYJ = 61;
    public static final short EM_X86_64 = 62;
    public static final short EM_PDSP = 63;
    public static final short EM_PDP10 = 64;
    public static final short EM_PDP11 = 65;
    public static final short EM_FX66 = 66;
    public static final short EM_ST9PLUS = 67;
    public static final short EM_ST7 = 68;
    public static final short EM_68HC16 = 69;
    public static final short EM_68HC11 = 70;
    public static final short EM_68HC08 = 71;
    public static final short EM_68HC05 = 72;
    public static final short EM_SVX = 73;
    public static final short EM_ST19 = 74;
    public static final short EM_VAX = 75;
    public static final short EM_CRIS = 76;
    public static final short EM_JAVELIN = 77;
    public static final short EM_FIREPATH = 78;
    public static final short EM_ZSP = 79;
    public static final short EM_MMIX = 80;
    public static final short EM_HUANY = 81;
    public static final short EM_PRISM = 82;
    public static final short EM_AVR = 83;
    public static final short EM_FR30 = 84;
    public static final short EM_D10V = 85;
    public static final short EM_D30V = 86;
    public static final short EM_V850 = 87;
    public static final short EM_M32R = 88;
    public static final short EM_MN10300 = 89;
    public static final short EM_MN10200 = 90;
    public static final short EM_PJ = 91;
    public static final short EM_OPENRISC = 92;
    public static final short EM_ARC_A5 = 93;
    public static final short EM_XTENSA = 94;
    public static final short EM_VIDEOCORE = 95;
    public static final short EM_TMM_GPP = 96;
    public static final short EM_NS32K = 97;
    public static final short EM_TPC = 98;
    public static final short EM_SNP1K = 99;
    public static final short EM_ST200 = 100;
    public static final short EM_IP2K = 101;
    public static final short EM_MAX = 102;
    public static final short EM_CR = 103;
    public static final short EM_F2MC16 = 104;
    public static final short EM_MSP430 = 105;
    public static final short EM_BLACKFIN = 106;
    public static final short EM_SE_C33 = 107;
    public static final short EM_SEP = 108;
    public static final short EM_ARCA = 109;
    public static final short EM_UNICORE = 110;
    public static final short EM_EXCESS = 111;
    public static final short EM_DXP = 112;
    public static final short EM_ALTERA_NIOS2 = 113;
    public static final short EM_CRX = 114;
    public static final short EM_XGATE = 115;
    public static final short EM_C166 = 116;
    public static final short EM_M16C = 117;
    public static final short EM_DSPIC30F = 118;
    public static final short EM_CE = 119;
    public static final short EM_M32C = 120;
    public static final short EM_TSK3000 = 131;
    public static final short EM_RS08 = 132;
    public static final short EM_res133 = 133;
    public static final short EM_ECOG2 = 134;
    public static final short EM_SCORE = 135;
    public static final short EM_SCORE7 = 135;
    public static final short EM_DSP24 = 136;
    public static final short EM_VIDEOCORE3 = 137;
    public static final short EM_LATTICEMICO32 = 138;
    public static final short EM_SE_C17 = 139;
    public static final short EM_TI_C6000 = 140;
    public static final short EM_TI_C2000 = 141;
    public static final short EM_TI_C5500 = 142;
    public static final short EM_MMDSP_PLUS = 160;
    public static final short EM_CYPRESS_M8C = 161;
    public static final short EM_R32C = 162;
    public static final short EM_TRIMEDIA = 163;
    public static final short EM_QDSP6 = 164;
    public static final short EM_8051 = 165;
    public static final short EM_STXP7X = 166;
    public static final short EM_NDS32 = 167;
    public static final short EM_ECOG1 = 168;
    public static final short EM_ECOG1X = 168;
    public static final short EM_MAXQ30 = 169;
    public static final short EM_XIMO16 = 170;
    public static final short EM_MANIK = 171;
    public static final short EM_CRAYNV2 = 172;
    public static final short EM_RX = 173;
    public static final short EM_METAG = 174;
    public static final short EM_MCST_ELBRUS = 175;
    public static final short EM_ECOG16 = 176;
    public static final short EM_CR16 = 177;
    public static final short EM_ETPU = 178;
    public static final short EM_SLE9X = 179;
    public static final short EM_L1OM = 180;
    public static final short EM_INTEL181 = 181;
    public static final short EM_INTEL182 = 182;
    public static final short EM_AARCH64 = 183;
    public static final short EM_ARM184 = 184;
    public static final short EM_AVR32 = 185;
    public static final short EM_STM8 = 186;
    public static final short EM_TILE64 = 187;
    public static final short EM_TILEPRO = 188;
    public static final short EM_MICROBLAZE = 189;
    public static final short EM_CUDA = 190;

    public static final boolean isIdentityValid(final byte[] ident) {
        return ELFMAG0 == ident[0] &&
               ELFMAG1 == ident[1] &&
               ELFMAG2 == ident[2] &&
               ELFMAG3 == ident[3] ;
    }

    /** Public access to the raw elf header part-1 (CPU/ABI independent read) */
    public final Ehdr_p1 raw;
    private final byte[] E_ident;

    /** Lower case CPUType name */
    public final String cpuName;
    public final CPUType cpuType;
    public final ABIType abiType;

    public final MachineDataInfo.StaticConfig machDesc;

    /**
     * Note: The input stream shall stay untouch to be able to read sections!
     * @param osType TODO
     * @param in input stream of a binary file at position zero
     *
     * @return
     * @throws IOException if reading from the given input stream fails or less then ELF Header size bytes
     * @throws IllegalArgumentException if the given input stream does not represent an ELF Header
     */
    public static ElfHeaderPart1 read(final OSType osType, final RandomAccessFile in) throws IOException, IllegalArgumentException {
        return new ElfHeaderPart1(osType, in);
    }

    /**
     * @param osType TODO
     * @param buf ELF Header bytes
     * @throws IllegalArgumentException if the given buffer does not represent an ELF Header
     * @throws IOException
     */
    ElfHeaderPart1(final OSType osType, final RandomAccessFile in) throws IllegalArgumentException, IOException {
        {
            final byte[] buf = new byte[Ehdr_p1.size()];
            readBytes (in, buf, 0, buf.length);
            final ByteBuffer eh1Bytes = ByteBuffer.wrap(buf, 0, buf.length);
            raw = Ehdr_p1.create(eh1Bytes);
        }
        E_ident = raw.getE_ident(0, new byte[Ehdr_p1.getE_identArrayLength()]);
        if( !isIdentityValid(E_ident) ) {
            throw new IllegalArgumentException("Buffer is not an ELF Header");
        }

        final short machine = getMachine();
        switch ( machine ) {
            case EM_ARM:
                cpuName = "arm"; // lowest 32bit denominator, ok for now
                abiType = ABIType.GENERIC_ABI;
                break;
            case EM_AARCH64:
                cpuName = "aarch64";
                abiType = ABIType.EABI_AARCH64;
                break;
            case EM_X86_64:
                cpuName = "x86_64";
                abiType = ABIType.GENERIC_ABI;
                break;
            case EM_386:
                cpuName = "i386";
                abiType = ABIType.GENERIC_ABI;
                break;
            case EM_486:
                cpuName = "i486";
                abiType = ABIType.GENERIC_ABI;
                break;
            case EM_IA_64:
                cpuName = "ia64";
                abiType = ABIType.GENERIC_ABI;
                break;
            case EM_MIPS:
                 // Can be little-endian or big-endian and 32 or 64 bits
                if( 64 == getArchClassBits() ) {
                    cpuName = isLittleEndian() ? "mips64le" : "mips64";
                } else {
                    cpuName = isLittleEndian() ? "mipsle" : "mips";
                }
                abiType = ABIType.GENERIC_ABI;
                break;
            case EM_MIPS_RS3_LE:
                cpuName = "mipsle-rs3";  // FIXME: Only little-endian?
                abiType = ABIType.GENERIC_ABI;
                break;
            case EM_MIPS_X:
                cpuName = isLittleEndian() ? "mipsle-x" : "mips-x"; // Can be little-endian
                abiType = ABIType.GENERIC_ABI;
                break;
            case EM_PPC:
                cpuName = "ppc";
                abiType = ABIType.GENERIC_ABI;
                break;
            case EM_PPC64:
                cpuName = "ppc64";
                abiType = ABIType.GENERIC_ABI;
                break;
            case EM_SH:
                cpuName = "superh";
                abiType = ABIType.GENERIC_ABI;
                break;
            default:
                throw new IllegalArgumentException("CPUType and ABIType could not be determined");
        }
        cpuType = CPUType.query(cpuName.toLowerCase());
        machDesc = MachineDataInfoRuntime.guessStaticMachineDataInfo(osType, cpuType);
        if(DEBUG) {
            System.err.println("ELF-1: cpuName "+cpuName+" -> "+cpuType+", "+abiType+", machDesc "+machDesc.toShortString());
        }
    }

    /**
     * Returns the architecture class in bits,
     * 32 for {@link #ELFCLASS32}, 64 for {@link #ELFCLASS64}
     * and 0 for {@link #ELFCLASSNONE}.
     */
    public final int getArchClassBits() {
        switch( E_ident[EI_CLASS] ) {
            case ELFCLASS32: return 32;
            case ELFCLASS64: return 64;
            default: return 0;
        }
    }

    /**
     * Returns the processor's data encoding, i.e.
     * {@link #ELFDATA2LSB}, {@link #ELFDATA2MSB} or {@link #ELFDATANONE};
     */
    public final byte getDataEncodingMode() {
        return E_ident[EI_DATA];
    }

    /**
     * Returns whether the processor's {@link #getDataEncodingMode() data encoding} is {@link #ELFDATA2LSB}.
     */
    public final boolean isLittleEndian() { return ELFDATA2LSB == E_ident[EI_DATA]; }
    /**
     * Returns whether the processor's {@link #getDataEncodingMode() data encoding} is {@link #ELFDATA2MSB}.
     */
    public final boolean isBigEndian() { return ELFDATA2MSB == E_ident[EI_DATA]; }
    /**
     * Returns whether the processor's {@link #getDataEncodingMode() data encoding} is {@link #ELFDATANONE}.
     */
    public final boolean isNoneEndian() { return ELFDATANONE == E_ident[EI_DATA]; }

    /** Returns the ELF file version, should be {@link #EV_CURRENT}. */
    public final byte getVersion() {
        return E_ident[EI_VERSION];
    }

    /** Returns the operating system and ABI for this file, 3 == Linux. Note: Often not used. */
    public final byte getOSABI() {
        return E_ident[EI_OSABI];
    }

    /** Returns the version of the {@link #getOSABI() OSABI} for this file. */
    public final byte getOSABIVersion() {
        return E_ident[EI_ABIVERSION];
    }

    /** Returns the object file type, e.g. {@link #ET_EXEC}, .. */
    public final short getType() {
        return raw.getE_type();
    }

    /** Returns the required architecture for the file, e.g. {@link #EM_386}, .. */
    public final short getMachine() {
        return raw.getE_machine();
    }

    @Override
    public final String toString() {
        final int enc = getDataEncodingMode();
        final String encS;
        switch(enc) {
            case ELFDATA2LSB: encS = "LSB"; break;
            case ELFDATA2MSB: encS = "MSB"; break;
            default:          encS = "NON"; break; /* ELFDATANONE */
        }
        final int type = getType();
        final String typeS;
        switch(type) {
            case ET_REL:  typeS = "reloc"; break;
            case ET_EXEC: typeS = "exec"; break;
            case ET_DYN:  typeS = "shared"; break;
            case ET_CORE: typeS = "core"; break;
            default:      typeS = "none"; break; /* ET_NONE */
        }
        return "ELF-1[vers "+getVersion()+", machine["+getMachine()+", "+cpuType+", "+abiType+", machDesc "+machDesc.toShortString()+"], bits "+getArchClassBits()+", enc "+encS+
               ", abi[os "+getOSABI()+", vers "+getOSABIVersion()+"], type "+typeS+"]";
    }
}
