/////////////////////
// Sections constants

// Section indexes
#define SHN_UNDEF          0
#define SHN_LORESERVE 0xFF00
#define SHN_LOPROC    0xFF00
#define SHN_HIPROC    0xFF1F
#define SHN_LOOS      0xFF20
#define SHN_HIOS      0xFF3F
#define SHN_ABS       0xFFF1
#define SHN_COMMON    0xFFF2
#define SHN_XINDEX    0xFFFF
#define SHN_HIRESERVE 0xFFFF

// Section types
#define SHT_NULL                   0
#define SHT_PROGBITS               1
#define SHT_SYMTAB                 2
#define SHT_STRTAB                 3
#define SHT_RELA                   4
#define SHT_HASH                   5
#define SHT_DYNAMIC                6
#define SHT_NOTE                   7
#define SHT_NOBITS                 8
#define SHT_REL                    9
#define SHT_SHLIB                 10
#define SHT_DYNSYM                11
#define SHT_INIT_ARRAY            14
#define SHT_FINI_ARRAY            15
#define SHT_PREINIT_ARRAY         16
#define SHT_GROUP                 17
#define SHT_SYMTAB_SHNDX          18
#define SHT_LOOS          0x60000000
#define SHT_HIOS          0x6fffffff
#define SHT_LOPROC        0x70000000
#define SHT_HIPROC        0x7FFFFFFF
#define SHT_LOUSER        0x80000000
#define SHT_HIUSER        0xFFFFFFFF

// Section attribute flags
#define SHF_WRITE                   0x1
#define SHF_ALLOC                   0x2
#define SHF_EXECINSTR               0x4
#define SHF_MERGE                  0x10
#define SHF_STRINGS                0x20
#define SHF_INFO_LINK              0x40
#define SHF_LINK_ORDER             0x80
#define SHF_OS_NONCONFORMING      0x100
#define SHF_GROUP                 0x200
#define SHF_TLS                   0x400
#define SHF_MASKOS           0x0ff00000
#define SHF_MASKPROC         0xF0000000

// Section group flags
#define GRP_COMDAT          0x1
#define GRP_MASKOS   0x0ff00000
#define GRP_MASKPROC 0xf0000000

// Symbol binding
#define STB_LOCAL     0
#define STB_GLOBAL    1
#define STB_WEAK      2
#define STB_LOOS     10
#define STB_HIOS     12
#define STB_MULTIDEF 13
#define STB_LOPROC   13
#define STB_HIPROC   15

// Symbol types
#define STT_NOTYPE   0
#define STT_OBJECT   1
#define STT_FUNC     2
#define STT_SECTION  3
#define STT_FILE     4
#define STT_COMMON   5
#define STT_TLS      6
#define STT_LOOS    10
#define STT_HIOS    12
#define STT_LOPROC  13
#define STT_HIPROC  15

// Symbol visibility
#define STV_DEFAULT   0
#define STV_INTERNAL  1
#define STV_HIDDEN    2
#define STV_PROTECTED 3

// Undefined name
#define STN_UNDEF 0

// Relocation types
#define R_386_NONE         0
#define R_X86_64_NONE      0
#define R_386_32           1
#define R_X86_64_64        1
#define R_386_PC32         2
#define R_X86_64_PC32      2
#define R_386_GOT32        3
#define R_X86_64_GOT32     3
#define R_386_PLT32        4
#define R_X86_64_PLT32     4
#define R_386_COPY         5
#define R_X86_64_COPY      5
#define R_386_GLOB_DAT     6
#define R_X86_64_GLOB_DAT  6
#define R_386_JMP_SLOT     7
#define R_X86_64_JUMP_SLOT 7
#define R_386_RELATIVE     8
#define R_X86_64_RELATIVE  8
#define R_386_GOTOFF       9
#define R_X86_64_GOTPCREL  9
#define R_386_GOTPC       10
#define R_X86_64_32       10
#define R_X86_64_32S      11
#define R_X86_64_16       12
#define R_X86_64_PC16     13
#define R_X86_64_8        14
#define R_X86_64_PC8      15
#define R_X86_64_DTPMOD64 16
#define R_X86_64_DTPOFF64 17
#define R_X86_64_TPOFF64  18
#define R_X86_64_TLSGD    19
#define R_X86_64_TLSLD    20
#define R_X86_64_DTPOFF32 21
#define R_X86_64_GOTTPOFF 22
#define R_X86_64_TPOFF32  23
#define R_X86_64_PC64     24
#define R_X86_64_GOTOFF64 25
#define R_X86_64_GOTPC32  26
#define R_X86_64_GOT64    27
#define R_X86_64_GOTPCREL64      28
#define R_X86_64_GOTPC64  29
#define R_X86_64_GOTPLT64 30
#define R_X86_64_PLTOFF64 31
#define R_X86_64_GOTPC32_TLSDESC 34
#define R_X86_64_TLSDESC_CALL    35
#define R_X86_64_TLSDESC         36
#define R_X86_64_IRELATIVE       37
#define R_X86_64_GNU_VTINHERIT  250
#define R_X86_64_GNU_VTENTRY    251

// Segment types
#define PT_NULL             0
#define PT_LOAD             1
#define PT_DYNAMIC          2
#define PT_INTERP           3
#define PT_NOTE             4
#define PT_SHLIB            5
#define PT_PHDR             6
#define PT_TLS              7
#define PT_LOOS    0x60000000
#define PT_HIOS    0x6fffffff
#define PT_LOPROC  0x70000000
#define PT_HIPROC  0x7FFFFFFF

// Segment flags
#define PF_X                 1 // Execute
#define PF_W                 2 // Write
#define PF_R                 4 // Read
#define PF_MASKOS   0x0ff00000 // Unspecified
#define PF_MASKPROC 0xf0000000 // Unspecified

// Dynamic Array Tags
#define DT_NULL              0
#define DT_NEEDED            1
#define DT_PLTRELSZ          2
#define DT_PLTGOT            3
#define DT_HASH              4
#define DT_STRTAB            5
#define DT_SYMTAB            6
#define DT_RELA              7
#define DT_RELASZ            8
#define DT_RELAENT           9
#define DT_STRSZ            10
#define DT_SYMENT           11
#define DT_INIT             12
#define DT_FINI             13
#define DT_SONAME           14
#define DT_RPATH            15
#define DT_SYMBOLIC         16
#define DT_REL              17
#define DT_RELSZ            18
#define DT_RELENT           19
#define DT_PLTREL           20
#define DT_DEBUG            21
#define DT_TEXTREL          22
#define DT_JMPREL           23
#define DT_BIND_NOW         24
#define DT_INIT_ARRAY       25
#define DT_FINI_ARRAY       26
#define DT_INIT_ARRAYSZ     27
#define DT_FINI_ARRAYSZ     28
#define DT_RUNPATH          29
#define DT_FLAGS            30
#define DT_ENCODING         32
#define DT_PREINIT_ARRAY    32
#define DT_PREINIT_ARRAYSZ  33
#define DT_LOOS     0x6000000D
#define DT_HIOS     0x6ffff000
#define DT_LOPROC   0x70000000
#define DT_HIPROC   0x7FFFFFFF

// DT_FLAGS values
#define DF_ORIGIN     0x1
#define DF_SYMBOLIC   0x2
#define DF_TEXTREL    0x4
#define DF_BIND_NOW   0x8
#define DF_STATIC_TLS 0x10


