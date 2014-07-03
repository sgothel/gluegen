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

import static jogamp.common.os.elf.IOUtils.readBytes;
import static jogamp.common.os.elf.IOUtils.seek;
import static jogamp.common.os.elf.IOUtils.shortToInt;
import static jogamp.common.os.elf.IOUtils.toHexString;

/**
 * ELF ABI Header
 * <p>
 * References:
 * <ul>
 *   <li>http://linux.die.net/man/5/elf</li>
 *   <li>http://www.sco.com/developers/gabi/latest/contents.html</li>
 *   <li>http://infocenter.arm.com/
 *   <ul>
 *      <li>ARM IHI 0044E, current through ABI release 2.09</li>
 *   </ul></li>
 * </ul>
 * </p>
 */
public class ElfHeader {
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

    /**
     * This masks an 8-bit version number, the version of the ABI to which this
     * ELF file conforms. This ABI is version 5. A value of 0 denotes unknown conformance.
     * {@value}
     */
    public static final int EF_ARM_ABIMASK  = 0xFF000000;
    public static final int EF_ARM_ABISHIFT  = 24;

    /**
     * ARM ABI version 5.
     * {@value}
     */
    public static final int EF_ARM_ABI5  = 0x05000000;

    /**
     * The ELF file contains BE-8 code, suitable for execution on an ARM
     * Architecture v6 processor. This flag must only be set on an executable file.
     * {@value}
     */
    public static final int EF_ARM_BE8      = 0x00800000;

    /**
     * Legacy code (ABI version 4 and earlier) generated by gcc-arm-xxx might
     * use these bits.
     * {@value}
     */
    public static final int EF_ARM_GCCMASK  = 0x00400FFF;

    /**
     * Set in executable file headers (e_type = ET_EXEC or ET_DYN) to note that
     * the executable file was built to conform to the hardware floating-point
     * procedure-call standard.
     * <p>
     * Compatible with legacy (pre version 5) gcc use as EF_ARM_VFP_FLOAT.
     * </p>
     * <p>
     * Note: This is not used (anymore)
     * </p>
     * {@value}
     */
    public static final int EF_ARM_ABI_FLOAT_HARD  = 0x00000400;

    /**
     * Set in executable file headers (e_type = ET_EXEC or ET_DYN) to note
     * explicitly that the executable file was built to conform to the software
     * floating-point procedure-call standard (the base standard). If both
     * {@link #EF_ARM_ABI_FLOAT_HARD} and {@link #EF_ARM_ABI_FLOAT_SOFT} are clear,
     * conformance to the base procedure-call standard is implied.
     * <p>
     * Compatible with legacy (pre version 5) gcc use as EF_ARM_SOFT_FLOAT.
     * </p>
     * <p>
     * Note: This is not used (anymore)
     * </p>
     * {@value}
     */
    public static final int EF_ARM_ABI_FLOAT_SOFT  = 0x00000200;

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
    public static final short EM_res183 = 183;
    public static final short EM_res184 = 184;
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

    /** Public access to the raw elf header */
    public final Ehdr d;

    /** Public access to the {@link SectionHeader} */
    public final SectionHeader[] sht;

    private final String string;

    /**
     * Note: The input stream shall stay untouch to be able to read sections!
     *
     * @param in input stream of a binary file at position zero
     * @return
     * @throws IOException if reading from the given input stream fails or less then ELF Header size bytes
     * @throws IllegalArgumentException if the given input stream does not represent an ELF Header
     */
    public static ElfHeader read(final RandomAccessFile in) throws IOException, IllegalArgumentException {
        final int eh_sz = Ehdr.size();
        final byte[] buf = new byte[eh_sz];
        readBytes (in, buf, 0, eh_sz);
        final ElfHeader eh = new ElfHeader(ByteBuffer.wrap(buf, 0, buf.length), in);
        return eh;
    }

    /**
     * @param buf ELF Header bytes
     * @throws IllegalArgumentException if the given buffer does not represent an ELF Header
     * @throws IOException
     */
    ElfHeader(final java.nio.ByteBuffer buf, final RandomAccessFile in) throws IllegalArgumentException, IOException {
        d = Ehdr.create(buf);
        if( !isIdentityValid(d.getE_ident()) ) {
            throw new IllegalArgumentException("Buffer is not an ELF Header");
        }
        sht = readSectionHeaderTable(in);
        string = toStringImpl();
    }

    public final short getSize() { return d.getE_ehsize(); }

    /**
     * Returns the architecture class in bits,
     * 32 for {@link #ELFCLASS32}, 64 for {@link #ELFCLASS64}
     * and 0 for {@link #ELFCLASSNONE}.
     */
    public final int getArchClassBits() {
        switch( d.getE_ident()[EI_CLASS] ) {
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
        return d.getE_ident()[EI_DATA];
    }

    /** Returns the ELF file version, should be {@link #EV_CURRENT}. */
    public final byte getVersion() {
        return d.getE_ident()[EI_VERSION];
    }

    /** Returns the operating system and ABI for this file, 3 == Linux. Note: Often not used. */
    public final byte getOSABI() {
        return d.getE_ident()[EI_OSABI];
    }

    /** Returns the version of the {@link #getOSABI() OSABI} for this file. */
    public final byte getOSABIVersion() {
        return d.getE_ident()[EI_ABIVERSION];
    }

    /** Returns the object file type, e.g. {@link #ET_EXEC}, .. */
    public final short getType() {
        return d.getE_type();
    }

    /** Returns the required architecture for the file, e.g. {@link #EM_386}, .. */
    public final short getMachine() {
        return d.getE_machine();
    }

    /**
     * Returns true if {@link #getMachine() machine} is a 32 or 64 bit ARM CPU
     * of type {@link #EM_ARM}. */
    public final boolean isArm() {
        return getMachine() == EM_ARM;
    }

    /**
     * Returns true if {@link #getMachine() machine} is a 32 or 64 bit Intel CPU
     * of type {@link #EM_386}, {@link #EM_486} or {@link #EM_X86_64}. */
    public final boolean isX86_32() {
        final short m = getMachine();
        return EM_386 == m ||
               EM_486 == m ||
               EM_X86_64 == m;
    }

    /**
     * Returns true if {@link #getMachine() machine} is a 64 bit AMD/Intel x86_64 CPU
     * of type {@link #EM_X86_64}. */
    public final boolean isX86_64() {
        return getMachine() == EM_X86_64;
    }

    /**
     * Returns true if {@link #getMachine() machine} is a 64 bit Intel Itanium CPU
     * of type {@link #EM_IA_64}. */
    public final boolean isIA64() {
        return getMachine() == EM_IA_64;
    }

    /**
     * Returns true if {@link #getMachine() machine} is a 32 or 64 bit MIPS CPU
     * of type {@link #EM_MIPS}, {@link #EM_MIPS_X} or {@link #EM_MIPS_RS3_LE}. */
    public final boolean isMips() {
        final short m = getMachine();
        return EM_MIPS == m ||
               EM_MIPS_X == m ||
               EM_MIPS_RS3_LE == m;
    }

    /** Returns the processor-specific flags associated with the file. */
    public final int getFlags() {
        return d.getE_flags();
    }

    /** Returns the ARM EABI version from {@link #getFlags() flags}, maybe 0 if not an ARM EABI. */
    public byte getArmABI() {
        return (byte) ( ( ( EF_ARM_ABIMASK & d.getE_flags() ) >> EF_ARM_ABISHIFT ) & 0xff );
    }

    /** Returns the ARM EABI legacy GCC {@link #getFlags() flags}, maybe 0 if not an ARM EABI or not having legacy GCC flags. */
    public int getArmLegacyGCCFlags() {
        final int f = d.getE_flags();
        return 0 != ( EF_ARM_ABIMASK & f ) ? ( EF_ARM_GCCMASK & f ) : 0;
    }

    /**
     * Returns the ARM EABI float mode from {@link #getFlags() flags},
     * i.e. 1 for {@link #EF_ARM_ABI_FLOAT_SOFT}, 2 for {@link #EF_ARM_ABI_FLOAT_HARD}
     * or 0 for none.
     * <p>
     * Note: This is not used (anymore)
     * </p>
     */
    public byte getArmFloatMode() {
        final int f = d.getE_flags();
        if( 0 != ( EF_ARM_ABIMASK & f ) ) {
            if( ( EF_ARM_ABI_FLOAT_HARD & f ) != 0 ) {
                return 2;
            }
            if( ( EF_ARM_ABI_FLOAT_SOFT & f ) != 0 ) {
                return 1;
            }
        }
        return 0;
    }

    /** Returns the 1st occurence of matching SectionHeader {@link SectionHeader#getType() type}, or null if not exists. */
    public final SectionHeader getSectionHeader(final int type) {
        for(int i=0; i<sht.length; i++) {
            final SectionHeader sh = sht[i];
            if( sh.getType() == type ) {
                return sh;
            }
        }
        return null;
    }

    /** Returns the 1st occurence of matching SectionHeader {@link SectionHeader#getName() name}, or null if not exists. */
    public final SectionHeader getSectionHeader(final String name) {
        for(int i=0; i<sht.length; i++) {
            final SectionHeader sh = sht[i];
            if( sh.getName().equals(name) ) {
                return sh;
            }
        }
        return null;
    }

    @Override
    public final String toString() {
        return string;
    }

    private final String toStringImpl() {
        final String machineS;
        if( isArm() ) {
            machineS=", arm";
        } else if( isX86_64() ) {
            machineS=", x86_64";
        } else if( isX86_32() ) {
            machineS=", x86_32";
        } else if( isIA64() ) {
            machineS=", itanium";
        } else if( isMips() ) {
            machineS=", mips";
        } else {
            machineS="";
        }
        final int enc = getDataEncodingMode();
        final String encS;
        switch(enc) {
            case 1:  encS = "LSB"; break;
            case 2:  encS = "MSB"; break;
            default: encS = "NON"; break;
        }
        final int armABI = getArmABI();
        final String armFlagsS;
        if( 0 != armABI ) {
            armFlagsS=", arm[abi "+armABI+", lGCC "+getArmLegacyGCCFlags()+", float "+getArmFloatMode()+"]";
        } else {
            armFlagsS="";
        }
        return "ElfHeader[vers "+getVersion()+", machine["+getMachine()+machineS+"], bits "+getArchClassBits()+", enc "+encS+
               ", abi[os "+getOSABI()+", vers "+getOSABIVersion()+"], flags["+toHexString(getFlags())+armFlagsS+"], type "+getType()+", sh-num "+sht.length+"]";
    }

    final SectionHeader[] readSectionHeaderTable(final RandomAccessFile in) throws IOException, IllegalArgumentException {
        // positioning
        {
            final long off = d.getE_shoff(); // absolute offset
            if( 0 == off ) {
                return new SectionHeader[0];
            }
            seek(in, off);
        }
        final SectionHeader[] sht;
        final int strndx = d.getE_shstrndx();
        final int size = d.getE_shentsize();
        final int num;
        int i;
        if( 0 == d.getE_shnum() ) {
            // Read 1st table 1st and use it's sh_size
            final byte[] buf0 = new byte[size];
            readBytes(in, buf0, 0, size);
            final SectionHeader sh0 = new SectionHeader(buf0, 0, size, 0);
            num = (int) sh0.d.getSh_size();
            if( 0 >= num ) {
                throw new IllegalArgumentException("EHdr sh_num == 0 and 1st SHdr size == 0");
            }
            sht = new SectionHeader[num];
            sht[0] = sh0;
            i=1;
        } else {
            num = d.getE_shnum();
            sht = new SectionHeader[num];
            i=0;
        }
        for(; i<num; i++) {
            final byte[] buf = new byte[size];
            readBytes(in, buf, 0, size);
            sht[i] = new SectionHeader(buf, 0, size, i);
        }
        if( SectionHeader.SHN_UNDEF != strndx ) {
            // has section name string table
            if( shortToInt(SectionHeader.SHN_LORESERVE) <= strndx ) {
                throw new InternalError("TODO strndx: "+SectionHeader.SHN_LORESERVE+" < "+strndx);
            }
            final SectionHeader strShdr = sht[strndx];
            if( SectionHeader.SHT_STRTAB != strShdr.getType() ) {
                throw new IllegalArgumentException("Ref. string Shdr["+strndx+"] is of type "+strShdr.d.getSh_type());
            }
            final Section strS = strShdr.readSection(in);
            for(i=0; i<num; i++) {
                sht[i].initName(strS, sht[i].d.getSh_name());
            }
        }

        return sht;
    }
}
