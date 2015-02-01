
#include <gluegen_stddef.h>
#include <gluegen_stdint.h>

// #define EI_NIDENT 16

// #define ElfN_Addr uintptr_t
// #define ElfN_Off size_t
typedef uintptr_t ElfN_Addr;
typedef size_t ElfN_Off;
typedef size_t ElfN_size;
 
typedef struct {
    unsigned char   e_ident[16];
    uint16_t        e_type;
    uint16_t        e_machine;
    uint32_t        e_version;
} Ehdr_p1;

typedef struct {
    ElfN_Addr       e_entry;
    ElfN_Off        e_phoff;
    ElfN_Off        e_shoff;
    uint32_t        e_flags;
    uint16_t        e_ehsize;
    uint16_t        e_phentsize;
    uint16_t        e_phnum;
    uint16_t        e_shentsize;
    uint16_t        e_shnum;
    uint16_t        e_shstrndx;
} Ehdr_p2;

typedef struct {
    uint32_t        sh_name;     
    uint32_t        sh_type;     
    ElfN_size       sh_flags;    
    ElfN_Addr       sh_addr;     
    ElfN_Off        sh_offset;   
    ElfN_size       sh_size;     
    uint32_t        sh_link;     
    uint32_t        sh_info;     
    ElfN_size       sh_addralign;    
    ElfN_size       sh_entsize;      
} Shdr;

typedef struct {
    uint32_t        st_name;     
    ElfN_Addr       st_value;    
    ElfN_size       st_size;     
    uint8_t         st_info;     
    uint8_t         st_other;    
    uint16_t        st_shndx;    
} Sym32;

typedef struct {
    uint32_t        st_name;     
    uint8_t         st_info;     
    uint8_t         st_other;    
    uint16_t        st_shndx;    
    ElfN_Addr       st_value;    
    ElfN_size       st_size;     
} Sym64;

