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

import static jogamp.common.os.elf.IOUtils.toHexString;
import static jogamp.common.os.elf.IOUtils.readUInt32;
import static jogamp.common.os.elf.IOUtils.getString;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.common.util.Bitstream;

/**
 * ARM EABI attributes within section header {@link SectionHeader#SHT_ARM_ATTRIBUTES}.
 * <p>
 * References:
 * <ul>
 *   <li>http://infocenter.arm.com/
 *   <ul>
 *      <li>ARM IHI 0044E, current through ABI release 2.09</li>
 *      <li>ARM IHI 0045D, current through ABI release 2.09</li>
 *   </ul></li>
 * </ul>
 * </p>
 */
public class SectionArmAttributes extends Section {
    public static final byte FORMAT_VERSION_A = 0x41; // 'A';

    public static enum Type {
        /** No Value */
        None,
        /** A Sub-Section - following the 4 byte sub section total size (tag + size + content) - byte order of the ELF file */
        SubSection,
        /** Null Terminated Byte-String */
        NTBS,
        ULEB128,
    }

    /** ULEB128 Value for {@link Tag#ABI_VFP_args}: FP parameter/result passing conforms to AAPCS, BASE variant. */
    public static final byte ABI_VFP_ARGS_IS_BASE_VARIANT = 0;
    /** ULEB128 Value for {@link Tag#ABI_VFP_args}: FP parameter/result passing conforms to AAPCS, VFP variant. */
    public static final byte ABI_VFP_ARGS_IS_VFP_VARIANT = 1;
    /** ULEB128 Value for {@link Tag#ABI_VFP_args}: FP parameter/result passing conforms to custom toolchain. */
    public static final byte ABI_VFP_ARGS_IS_CUSTOM_VARIANT = 2;
    /** ULEB128 Value for {@link Tag#ABI_VFP_args}: FP parameter/result passing conforms to both , BASE and VFP variant. */
    public static final byte ABI_VFP_ARGS_IS_BOTH_BASE_AND_VFP_VARIANT = 3;

    /**
     * Returns true if value is either {@link #ABI_VFP_ARGS_IS_VFP_VARIANT} or {@link #ABI_VFP_ARGS_IS_BOTH_BASE_AND_VFP_VARIANT}
     * @param v ULEB128 Value from {@link Tag#ABI_VFP_args} attribute
     */
    public static final boolean abiVFPArgsAcceptsVFPVariant(final byte v) {
        return ABI_VFP_ARGS_IS_VFP_VARIANT == v || ABI_VFP_ARGS_IS_BOTH_BASE_AND_VFP_VARIANT == v;
    }

    public static enum Tag {
        None(0, Type.None),
        File(1, Type.SubSection), Section(2, Type.SubSection), Symbol(3, Type.SubSection),
        CPU_raw_name( 4, Type.NTBS ),
        CPU_name( 5, Type.NTBS ),
        CPU_arch( 6, Type.ULEB128 ),
        CPU_arch_profile( 7, Type.ULEB128 ),
        ARM_ISA_use( 8, Type.ULEB128 ),
        THUMB_ISA_use( 9, Type.ULEB128 ),
        FP_arch( 10, Type.ULEB128 ),
        WMMX_arch( 11, Type.ULEB128 ),
        Advanced_SIMD_arch( 12, Type.ULEB128 ),
        PCS_config( 13, Type.ULEB128 ),
        ABI_PCS_R9_use ( 14, Type.ULEB128 ),
        ABI_PCS_RW_data( 15, Type.ULEB128 ),
        ABI_PCS_RO_data( 16, Type.ULEB128 ),
        ABI_PCS_GOT_use( 17, Type.ULEB128 ),
        ABI_PCS_wchar_t( 18, Type.ULEB128 ),
        ABI_FP_rounding( 19, Type.ULEB128 ),
        ABI_FP_denormal( 20, Type.ULEB128 ),
        ABI_FP_exceptions( 21, Type.ULEB128 ),
        ABI_FP_user_exceptions( 22, Type.ULEB128 ),
        ABI_FP_number_model( 23, Type.ULEB128 ),
        ABI_align_needed( 24, Type.ULEB128 ),
        ABI_align_preserved( 25, Type.ULEB128 ),
        ABI_enum_size( 26, Type.ULEB128 ),
        ABI_HardFP_use( 27, Type.ULEB128 ),
        ABI_VFP_args( 28, Type.ULEB128 ),
        ABI_WMMX_args( 29, Type.ULEB128 ),
        ABI_optimization_goals( 30, Type.ULEB128 ),
        ABI_FP_optimization_goals( 31, Type.ULEB128 ),
        compatibility ( 32, Type.NTBS ), /** with each byte interpreted as an ULEB128 with closing EOS */
        CPU_unaligned_access( 34, Type.ULEB128 ),
        FP_HP_extension( 36, Type.ULEB128 ),
        ABI_FP_16bit_format( 38, Type.ULEB128 ),
        MPextension_use( 42, Type.ULEB128 ),
        DIV_use( 44, Type.ULEB128 ),
        nodefaults( 64, Type.ULEB128 ), /* value ignored */
        also_compatible_with( 65, Type.ULEB128 ),
        T2EE_use( 66, Type.ULEB128 ),
        conformance( 67, Type.NTBS ),
        Virtualization_use( 68, Type.ULEB128 ),
        undefined69( 69, Type.None ),
        MPextension_use_legacy( 70, Type.ULEB128 )
        ;

        public final int id;
        public final Type type;

        /** Slow O(n) transition of a native tag value to a Tag. */
        public static Tag get(final int id) {
            final Tag[] tags = Tag.values();
            final int tag_count = tags.length;
            for(int i=0; i < tag_count; i++) {
                if( tags[i].id == id ) {
                    return tags[i];
                }
            }
            return null;
        }

        Tag(final int id, final Type type){
            this.id = id;
            this.type = type;
        }
    }

    public static class Attribute {
        public final Tag tag;
        private final Object value;

        Attribute(final Tag tag, final Object value) {
            this.tag = tag;
            this.value = value;
        }

        public final boolean isNTBS() {
            return Type.NTBS == tag.type;
        }
        public final String getNTBS() {
            if( Type.NTBS == tag.type ) {
                return (String) value;
            }
            throw new IllegalArgumentException("Not NTBS but "+tag.type);
        }

        public final boolean isULEB128() {
            return Type.ULEB128 == tag.type;
        }
        public final byte getULEB128() {
            if( Type.ULEB128== tag.type ) {
                return ((Byte) value).byteValue();
            }
            throw new IllegalArgumentException("Not ULEB128 but "+tag.type);
        }

        @Override
        public String toString() {
            return tag+" = "+value;
        }
    }

    public static class VendorAttributes {
        public final String vendor;
        public final List<Attribute> attributes;

        VendorAttributes(final String vendor, final List<Attribute> attributes) {
            this.vendor = vendor;
            this.attributes = attributes;
        }

        @Override
        public String toString() {
            return vendor + attributes.toString();
        }
    }
    public final List<VendorAttributes> vendorAttributesList;

    SectionArmAttributes(final SectionHeader sh, final byte[] data, final int offset, final int length) throws IndexOutOfBoundsException, IllegalArgumentException {
        super(sh, data, offset, length);
        this.vendorAttributesList = parse(sh, data, offset, length);
    }

    @Override
    public String toString() {
        return "SectionArmAttributes["+super.toSubString()+", "+vendorAttributesList.toString()+"]";
    }

    public final Attribute get(final Tag tag) {
        for(int i=0; i<vendorAttributesList.size(); i++) {
            final List<Attribute> attributes = vendorAttributesList.get(i).attributes;
            for(int j=0; j<attributes.size(); j++) {
                final Attribute a = attributes.get(j);
                if( a.tag == tag ) {
                    return a;
                }
            }
        }
        return null;
    }

    public final List<Attribute> get(final String vendor) {
        return get(vendorAttributesList, vendor);
    }

    static final List<Attribute> get(final List<VendorAttributes> vendorAttributesList, final String vendor) {
        for(int i=0; i<vendorAttributesList.size(); i++) {
            final VendorAttributes vas = vendorAttributesList.get(i);
            if( vas.vendor.equals(vendor) ) {
                return vas.attributes;
            }
        }
        return null;
    }

    /**
     * @param sh TODO
     * @param in byte source buffer to parse
     * @param offset offset within byte source buffer to start parsing
     * @param remaining remaining numbers of bytes to parse beginning w/ <code>sb_off</code>,
     *                  which shall not exceed <code>sb.length - offset</code>.
     * @throws IndexOutOfBoundsException if <code>offset + remaining > sb.length</code>.
     * @throws IllegalArgumentException if section parsing failed, i.e. incompatible version or data.
     */
    static List<VendorAttributes> parse(final SectionHeader sh, final byte[] in, final int offset, final int remaining) throws IndexOutOfBoundsException, IllegalArgumentException {
        Bitstream.checkBounds(in, offset, remaining);
        int i = offset;
        if( FORMAT_VERSION_A != in[ i ] ) {
            throw new IllegalArgumentException("ShArmAttr: Not version A, but: "+toHexString(in[i]));
        }
        i++;

        final List<VendorAttributes> vendorAttributesList = new ArrayList<VendorAttributes>();
        final boolean isBigEndian = sh.eh2.eh1.isBigEndian();

        while(i < remaining) {
            final int i_pre = i;
            final int secLen = readUInt32(isBigEndian, in, i); /* total section size: 4 + string + content, i.e. offset to next section */
            i+=4;

            final String vendor;
            {
                final int[] i_post = new int[] { 0 };
                vendor = getString(in, i, secLen - 4, i_post);
                i = i_post[0];
            }

            final List<Attribute> attributes = new ArrayList<Attribute>();

            while(i < secLen) {
                final int[] i_post = new int[] { 0 };
                parseSub(isBigEndian, in, i, secLen - i, i_post, attributes);
                i = i_post[0];
            }

            if( i_pre + secLen != i ) {
                throw new IllegalArgumentException("ShArmAttr: Section length count mismatch, expected "+(i_pre + secLen)+", has "+i);
            }

            final List<Attribute> mergeAttribs = get(vendorAttributesList, vendor);
            if( null != mergeAttribs ) {
                mergeAttribs.addAll(attributes);
            } else {
                vendorAttributesList.add(new VendorAttributes(vendor, attributes));
            }
        }

        return vendorAttributesList;
    }

    /**
     * @param isBigEndian TODO
     * @param in byte source buffer to parse
     * @param offset offset within byte source buffer to start parsing
     * @param remaining remaining numbers of bytes to parse beginning w/ <code>sb_off</code>,
     *                  which shall not exceed <code>sb.length - offset</code>.
     * @throws IndexOutOfBoundsException if <code>offset + remaining > sb.length</code>.
     * @throws IllegalArgumentException if section parsing failed, i.e. incompatible version or data.
     */
    private static void parseSub(final boolean isBigEndian, final byte[] in, final int offset, final int remaining,
                                 final int[] offset_post, final List<Attribute> attributes)
            throws IndexOutOfBoundsException, IllegalArgumentException
    {
        Bitstream.checkBounds(in, offset, remaining);

        // Starts w/ sub-section Tag
        int i = offset;
        final int i_sTag = in[i++];
        final Tag sTag = Tag.get(i_sTag);
        if( null == sTag ) {
            throw new IllegalArgumentException("ShArmAttr: Invalid Sub-Section tag (NaT): "+i_sTag);
        }
        final int subSecLen; // sub section total size (tag + size + content)
        switch(sTag) {
            case File:
            case Section:
            case Symbol:
                subSecLen = readUInt32(isBigEndian, in, i);
                i+=4;
                break;
            default:
                throw new IllegalArgumentException("ShArmAttr: Invalid Sub-Section tag: "+sTag);
        }
        if( Tag.File == sTag ) {
            while( i < offset + subSecLen ) {
                final int i_tag = in[i++];
                final Tag tag = Tag.get(i_tag);
                if( null == tag ) {
                    throw new IllegalArgumentException("ShArmAttr: Invalid Attribute tag (NaT): "+i_tag);
                }
                switch(tag.type) {
                    case NTBS:
                        {
                            final int[] i_post = new int[] { 0 };
                            final String value = getString(in, i, subSecLen + offset - i, i_post);
                            attributes.add(new Attribute(tag, value));
                            i = i_post[0];
                        }
                        break;
                    case ULEB128:
                        {
                            final byte value = in[i++];
                            attributes.add(new Attribute(tag, new Byte(value)));
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("ShArmAttr: Invalid Attribute tag: "+tag);
                }
            }
        }
        offset_post[0] = offset + subSecLen;
    }
}
