/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

header {
        package com.jogamp.gluegen.cgram;

        import java.io.*;
        import java.util.*;

        import antlr.CommonAST;
        import com.jogamp.gluegen.ASTLocusTag;
        import com.jogamp.gluegen.ConstantDefinition;
        import com.jogamp.gluegen.ConstantDefinition.CNumber;
        import com.jogamp.gluegen.GlueGenException;
        import com.jogamp.gluegen.JavaConfiguration;
        import com.jogamp.gluegen.cgram.types.*;
        import com.jogamp.gluegen.cgram.types.EnumType;
        import com.jogamp.gluegen.cgram.types.EnumType.Enumerator;
}

class HeaderParser extends GnuCTreeParser;
options {
        k = 1;
}

{
    /** Name assigned to a anonymous EnumType (e.g., "enum { ... }"). */
    public static final String ANONYMOUS_ENUM_NAME = "<anonymous>";

    boolean debug = false;

    public boolean getDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /** Set the configuration for this
        HeaderParser. Must be done before parsing. */
    public void setJavaConfiguration(JavaConfiguration cfg) {
        this.cfg = cfg;
    }

    /** Set the dictionary mapping typedef names to types for this
        HeaderParser. Must be done before parsing. */
    public void setTypedefDictionary(TypeDictionary dict) {
        this.typedefDictionary = dict;
    }

    /** Returns the typedef dictionary this HeaderParser uses. */
    public TypeDictionary getTypedefDictionary() {
        return typedefDictionary;
    }    
    
    /** Set the dictionary mapping struct names (i.e., the "foo" in
        "struct foo { ... };") to types for this HeaderParser. Must be done
        before parsing. */
    public void setStructDictionary(TypeDictionary dict) {
        this.structDictionary = dict;
    }

    /** Returns the struct name dictionary this HeaderParser uses. */
    public TypeDictionary getStructDictionary() {
        return structDictionary;
    }    
    
    /** Get the canonicalization map, which is a regular HashMap
        mapping Type to Type and which is used for looking up the unique
        instances of e.g. pointer-to-structure types that have been typedefed
        and therefore have names. */
    public Map getCanonMap() {
        return canonMap;
    }

    /** Pre-define the list of EnumTypes for this HeaderParser. Must be
                done before parsing. */
    public void setEnums(List<EnumType> enumTypes) {
        // FIXME: Need to take the input set of EnumTypes, extract all
        // the enumerates from each EnumType, and fill in the enumHash
        // so that each enumerate maps to the enumType to which it
        // belongs.
        throw new RuntimeException("setEnums is Unimplemented!");
    }

    /** Returns the EnumTypes this HeaderParser processed. */
    public List<EnumType> getEnums() {
        return new ArrayList<EnumType>(enumHash.values());
    }    
    
    /** Clears the list of functions this HeaderParser has parsed.
        Useful when reusing the same HeaderParser for more than one
        header file. */
    public void clearParsedFunctions() {
        functions.clear();
    }

    /** Returns the list of FunctionSymbols this HeaderParser has parsed. */
    public List<FunctionSymbol> getParsedFunctions() {
        return functions;
    }

    private CompoundType lookupInStructDictionary(String structName,
                                                  CompoundTypeKind kind,
                                                  int cvAttrs, final ASTLocusTag locusTag) 
    {
        CompoundType t = (CompoundType) structDictionary.get(structName);
        if (t == null) {
            t = CompoundType.create(structName, null, kind, cvAttrs, locusTag);
            structDictionary.put(structName, t);
            debugPrintln("Adding compound mapping: [" + structName + "] -> "+getDebugTypeString(t)+" @ "+locusTag);
            debugPrintln(t.getStructString());
        }
        return t;
    }

    private Type lookupInTypedefDictionary(final AST _t, String typeName) {
        Type t = typedefDictionary.get(typeName);
        if (t == null) {
            throwGlueGenException(_t,
                 "Undefined reference to typedef name " + typeName);
        }
        return t;
    }

    static class ParameterDeclaration {
        private String id;
        private Type   type;

        ParameterDeclaration(String id, Type type) {
            this.id = id;
            this.type = type;
        }
        String id()              { return id; }
        Type   type()            { return type; }
        void setType(final Type t) { type = t; }
        public String toString() { return "ParamDecl["+id+": "+type.getDebugString()+"]"; }
    }

    // A box for a Type. Allows type to be passed down to be modified by recursive rules.
    static class TypeBox {
        private Type origType;
        private Type type;
        private boolean isTypedef;

        TypeBox(Type type) {
            this(type, false);
        }

        TypeBox(Type type, boolean isTypedef) {
            this.origType = type;
            this.isTypedef = isTypedef;
        }

        Type type() {
            if (type == null) {
                return origType;
            }
            return type;
        }
        void setType(Type type) {
            this.type = type;
        }
        void reset() {
            type = null;
        }

        boolean isTypedef()     { return isTypedef; }

            // for easier debugging
            public String toString() { 
               String tStr = "Type=NULL_REF";
               if (type == origType) {
                         tStr = "Type=ORIG_TYPE";
                     } else if (type != null) {
                     tStr = "Type: name=\"" + type.getCVAttributesString() + " " + 
                    type.getName() + "\"; signature=\"" + type + "\"; class " + 
                                        type.getClass().getName();
               }
               String oStr = "OrigType=NULL_REF";
               if (origType != null) {
                     oStr = "OrigType: name=\"" + origType.getCVAttributesString() + " " + 
             origType.getName() + "\"; signature=\"" + origType + "\"; class " + 
                        origType.getClass().getName();
               }
               return "<["+tStr + "] [" + oStr + "] " + " isTypedef=" + isTypedef+">"; 
            }
    }

    private String getDebugTypeString(Type t) {
      if(debug) {
        return getTypeString(t);
      } else {
        return null;
      }
    }
    private String getTypeString(Type t) {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      if(null!=t) {
        sb.append(t.getDebugString());
        sb.append(", opaque ").append(isOpaque(t)).append("]");
      } else {
        sb.append("nil]");
      }
      return sb.toString();
    }
    private boolean isOpaque(final Type type) {
      return (cfg.typeInfo(type) != null);
    }

    private void debugPrintln(String msg) {
        if(debug) {
            System.err.println(msg);
        }
    }

    private void debugPrint(String msg) {
        if(debug) {
            System.err.print(msg);
        }
    }

    private JavaConfiguration cfg;
    private TypeDictionary typedefDictionary;
    private TypeDictionary structDictionary;
    private List<FunctionSymbol> functions = new ArrayList<FunctionSymbol>();
    // hash from name of an enumerated value to the EnumType to which it belongs
    private HashMap<String, EnumType> enumHash = new HashMap<String, EnumType>();
    private HashMap<String, EnumType> enumMap = new HashMap<String, EnumType>();

    // Storage class specifiers
    private static final int AUTO     = 1 << 0;
    private static final int REGISTER = 1 << 1;
    private static final int TYPEDEF  = 1 << 2;
    // Function storage class specifiers
    private static final int EXTERN   = 1 << 3;
    private static final int STATIC   = 1 << 4;
    private static final int INLINE   = 1 << 5;
    // Type qualifiers
    private static final int CONST    = 1 << 6;
    private static final int VOLATILE = 1 << 7;
    private static final int SIGNED   = 1 << 8;
    private static final int UNSIGNED = 1 << 9;

    private boolean isFuncDeclaration;   // Used to only process function typedefs
    private String  funcDeclName;
    private List<ParameterDeclaration> funcDeclParams;
    private ASTLocusTag funcLocusTag;

    private void resetFuncDeclaration() {
        isFuncDeclaration = false;
        funcDeclName = null;
        funcDeclParams = null;
        funcLocusTag = null;
    }
    private void setFuncDeclaration(final String name, final List<ParameterDeclaration> p, final ASTLocusTag locusTag) {
        isFuncDeclaration = true;
        funcDeclName = name;
        funcDeclParams = p;
        funcLocusTag = locusTag;
    }

    private void processDeclaration(Type returnType) {
        if (isFuncDeclaration) {
            final FunctionSymbol sym = new FunctionSymbol(funcDeclName, 
                                                          new FunctionType(null, null, returnType, 0, funcLocusTag),
                                                          funcLocusTag);
            debugPrintln("Function ... "+sym.toString()+" @ "+funcLocusTag);
            if (funcDeclParams != null) { // handle funcs w/ empty parameter lists (e.g., "foo()")
                for (Iterator<ParameterDeclaration> iter = funcDeclParams.iterator(); iter.hasNext(); ) {
                    ParameterDeclaration pd = iter.next();
                    pd.setType(pd.type());
                    debugPrintln(" add "+pd.toString());
                    sym.addArgument(pd.type(), pd.id());
                }
            }
            debugPrintln("Function Added "+sym.toString());
            functions.add(sym);
            resetFuncDeclaration();
        }
    }

    private int attrs2CVAttrs(int attrs) {
        int cvAttrs = 0;
        if ((attrs & CONST) != 0) {
            cvAttrs |= CVAttributes.CONST;
        }
        if ((attrs & VOLATILE) != 0) {
            cvAttrs |= CVAttributes.VOLATILE;
        }
        return cvAttrs;
    }

    /** Helper routine which handles creating a pointer or array type
        for [] expressions */
    private void handleArrayExpr(TypeBox tb, AST t, ASTLocusTag locusTag) {
        if (t != null) {
            try {
                final int len = parseIntConstExpr(t);
                tb.setType(canonicalize(new ArrayType(tb.type(), SizeThunk.mul(SizeThunk.constant(len), tb.type().getSize()), len, 0, locusTag)));
                return;
            } catch (RecognitionException e) {
                // Fall through
            }
        }
        tb.setType(canonicalize(new PointerType(SizeThunk.POINTER,
                                                tb.type(), 0, locusTag)));
    }

    private int parseIntConstExpr(AST t) throws RecognitionException {
        return intConstExpr(t);
    }

  /** Utility function: creates a new EnumType with the given name, or
          returns an existing one if it has already been created. */
    private EnumType getEnumType(String enumTypeName, ASTLocusTag locusTag) {
        EnumType enumType = null;
        Iterator<EnumType> it = enumHash.values().iterator(); 
        while (it.hasNext()) {
          EnumType potentialMatch = it.next();
          if (potentialMatch.getName().equals(enumTypeName)) {
              enumType = potentialMatch;
              break;        
          }
        }
        
        if (enumType == null) {
          // This isn't quite correct. In theory the enum should expand to
          // the size of the largest element, so if there were a long long
          // entry the enum should expand to e.g. int64. However, using
          // "long" here (which is what used to be the case) was 
          // definitely incorrect and caused problems.
          enumType = new EnumType(enumTypeName, SizeThunk.INT32, locusTag);
        }  
        
        return enumType;
    }        
  
  // Map used to canonicalize types. For example, we may typedef
  // struct foo { ... } *pfoo; subsequent references to struct foo* should
  // point to the same PointerType object that had its name set to "pfoo".
  // Opaque canonical types are excluded.
  private Map<Type, Type> canonMap = new HashMap<Type, Type>();
  private Type canonicalize(Type t) {
    Type res = (Type) canonMap.get(t);
    if (res != null) {
      return res;
    } else {
      canonMap.put(t, t);
      return t;
    }
  }

  private void throwGlueGenException(final AST t, final String message) throws GlueGenException {
    // dumpASTTree("XXX", t);
    throw new GlueGenException(message, findASTLocusTag(t));
  }
  private void throwGlueGenException(final ASTLocusTag locusTag, final String message) throws GlueGenException {
    // dumpASTTree("XXX", t);
    throw new GlueGenException(message, locusTag);
  }

  /**
   * Return ASTLocusTag in tree, or null if not found.
   */
  private ASTLocusTag findASTLocusTag(final AST astIn) {
    AST ast = astIn;
    while(null != ast) {
        if( ast instanceof TNode ) {
            final TNode tn = (TNode) ast;
            final ASTLocusTag tag = tn.getASTLocusTag();
            if( null != tag ) {
                return tag;
            }
        }
        ast = ast.getFirstChild();
    }
    return null;
  }
  private void dumpASTTree(final String pre, final AST t) {
    int i=0;
    AST it = t;
    while( null != it ) {
        it = dumpAST(pre+"."+i, it);
        i++;
    }
  }
  private AST dumpAST(final String pre, final AST ast) {
    if( null == ast ) {
        System.err.println(pre+".0: AST NULL");
        return null;
    } else {
        System.err.println(pre+".0: AST Type: "+ast.getClass().getName());
        System.err.println(pre+".1: line:col "+ast.getLine()+":"+ast.getColumn()+" -> "+ast.getText());
        if( ast instanceof TNode ) {
            final TNode tn = (TNode) ast;
            final ASTLocusTag tag = tn.getASTLocusTag();
            System.err.println(pre+".TN.1: "+tag);
            final Hashtable<String, Object> attributes = tn.getAttributesTable();
            System.err.println(pre+".TN.2: "+attributes);
        }
        return ast.getFirstChild();
    }
  }
}

declarator[TypeBox tb] returns [String s] {
    resetFuncDeclaration();
    s = null;
    List<ParameterDeclaration> params = null;
    String funcPointerName = null;
    TypeBox dummyTypeBox = null;
    final ASTLocusTag locusTag = findASTLocusTag(declarator_AST_in);
}
        :   #( NDeclarator
                ( pointerGroup[tb] )?

                ( id:ID  { s = id.getText(); }
                | LPAREN funcPointerName = declarator[dummyTypeBox] RPAREN
                )

                (   #( NParameterTypeList
                      (
                        params = parameterTypeList
                        | (idList)?
                      ) 
                      RPAREN
                    )  {
                           if (id != null) {
                               setFuncDeclaration(id.getText(), params, locusTag);
                           } else if ( funcPointerName != null ) {
                               /* TypeBox becomes function pointer in this case */
                               final FunctionType ft = new FunctionType(null, null, tb.type(), 0, locusTag);
                               if (params == null) {
                                   // If the function pointer has no declared parameters, it's a 
                                   // void function. I'm not sure if the parameter name is 
                                   // ever referenced anywhere when the type is VoidType, so
                                   // just in case I'll set it to a comment string so it will
                                   // still compile if written out to code anywhere.
                                   ft.addArgument(new VoidType(0, locusTag), "/*unnamed-void*/");
                               } else {
                                   for (Iterator iter = params.iterator(); iter.hasNext(); ) {
                                       ParameterDeclaration pd = (ParameterDeclaration) iter.next();
                                       ft.addArgument(pd.type(), pd.id());
                                   }
                               }
                               tb.setType(canonicalize(new PointerType(SizeThunk.POINTER,
                                                                       ft, 0, locusTag)));
                               s = funcPointerName;
                           }
                       }
                 | LBRACKET ( e:expr )? RBRACKET { handleArrayExpr(tb, e, locusTag); }
                )*
             )
        ;

typelessDeclaration {
    TypeBox tb = null;
}
        :       #(NTypeMissing initDeclList[tb] SEMI)
        ;

declaration {
    TypeBox tb = null;
}
        :       #( NDeclaration
                    tb = declSpecifiers 
                    (
                        initDeclList[tb] 
                    )?
                    ( SEMI )+
                ) { processDeclaration(tb.type()); }
        ;

parameterTypeList returns [List<ParameterDeclaration> l] { l = new ArrayList<ParameterDeclaration>(); ParameterDeclaration decl = null; }
        :       ( decl = parameterDeclaration { if (decl != null) { l.add(decl); } } ( COMMA | SEMI )? )+ ( VARARGS )?
        ;

parameterDeclaration returns [ParameterDeclaration pd] {
    Type t = null;
    String decl = null;
    pd = null;
    TypeBox tb = null;
}
        :       #( NParameterDeclaration
                tb    = declSpecifiers
                (decl = declarator[tb] | nonemptyAbstractDeclarator[tb])?
                ) { 
                  if( null == tb ) {
                    throwGlueGenException(parameterDeclaration_AST_in,
                        String.format("Undefined type for declaration '%s'", decl));
                  }
                  pd = new ParameterDeclaration(decl, tb.type()); 
                }
        ;

functionDef {
    TypeBox tb = null;
}
        :   #( NFunctionDef
                ( functionDeclSpecifiers)? 
                declarator[tb]
                (declaration | VARARGS)*
                compoundStatement
            )
        ;

declSpecifiers returns [TypeBox tb] {
    tb = null;
    Type t = null;
    int x = 0;
    int y = 0; 
}
        :       ( y = storageClassSpecifier { x |= y; } 
                | y = typeQualifier         { x |= y; }
                | t = typeSpecifier[x]
                )+
{
            if (t == null &&
                (x & (SIGNED | UNSIGNED)) != 0) {
                t = new IntType("int", SizeThunk.INTxx, 
                                ((x & UNSIGNED) != 0), 
                                attrs2CVAttrs(x),
                                findASTLocusTag(declSpecifiers_AST_in));
            }
            tb = new TypeBox(t, ((x & TYPEDEF) != 0));
}
        ;

storageClassSpecifier returns [int x] { x = 0; }
        :       "auto"     { x |= AUTO;     }
        |       "register" { x |= REGISTER; }
        |       "typedef"  { x |= TYPEDEF;  }
        |       x = functionStorageClassSpecifier
        ;


functionStorageClassSpecifier returns [int x] { x = 0; }
        :       "extern" { x |= EXTERN; }
        |       "static" { x |= STATIC; }
        |       "inline" { x |= INLINE; }
        ;


typeQualifier returns [int x] { x = 0; }
        :       "const"    { x |= CONST; }
        |       "volatile" { x |= VOLATILE; }
        |       "signed"   { x |= SIGNED; }
        |       "unsigned" { x |= UNSIGNED; }
        ;

typeSpecifier[int attributes] returns [Type t] {
    t = null;
    int cvAttrs = attrs2CVAttrs(attributes);
    boolean unsig = ((attributes & UNSIGNED) != 0);
    final ASTLocusTag locusTag = findASTLocusTag(typeSpecifier_AST_in);
}
        //                                                                                    TYPEDEF
        //                                                                                    |      TYPEDEF-UNSIGNED
        //                                                                    UNSIGNED        |      |
        //      TOKEN                 TYPE    NAME         SIZE               |      ATTRIBS  |      |      LOCUS
        :       "void"      { t = new VoidType(                                      cvAttrs,               locusTag); }
        |       "char"      { t = new IntType("char" ,     SizeThunk.INT8,    unsig, cvAttrs, false, false, locusTag); }
        |       "short"     { t = new IntType("short",     SizeThunk.INT16,   unsig, cvAttrs, false, false, locusTag); }
        |       "int"       { t = new IntType("int"  ,     SizeThunk.INTxx,   unsig, cvAttrs, false, false, locusTag); }
        |       "long"      { t = new IntType("long" ,     SizeThunk.LONG,    unsig, cvAttrs, false, false, locusTag); }
        |       "float"     { t = new FloatType("float",   SizeThunk.FLOAT,          cvAttrs,               locusTag); }
        |       "double"    { t = new DoubleType("double", SizeThunk.DOUBLE,         cvAttrs,               locusTag); }
        |       "__int32"   { t = new IntType("__int32",   SizeThunk.INT32,   unsig, cvAttrs, true,  false, locusTag); }  /* TD: signed   */
        |       "__int64"   { t = new IntType("__int64",   SizeThunk.INT64,   unsig, cvAttrs, true,  false, locusTag); }  /* TD: signed   */
        |       "int8_t"    { t = new IntType("int8_t",    SizeThunk.INT8,    unsig, cvAttrs, true,  false, locusTag); }  /* TD: signed   */
        |       "uint8_t"   { t = new IntType("uint8_t",   SizeThunk.INT8,    unsig, cvAttrs, true,  true,  locusTag); }  /* TD: unsigned */
        |       "int16_t"   { t = new IntType("int16_t",   SizeThunk.INT16,   unsig, cvAttrs, true,  false, locusTag); }  /* TD: signed   */
        |       "uint16_t"  { t = new IntType("uint16_t",  SizeThunk.INT16,   unsig, cvAttrs, true,  true,  locusTag); }  /* TD: unsigned */
        |       "int32_t"   { t = new IntType("int32_t",   SizeThunk.INT32,   unsig, cvAttrs, true,  false, locusTag); }  /* TD: signed   */
        |       "wchar_t"   { t = new IntType("wchar_t",   SizeThunk.INT32,   unsig, cvAttrs, true,  false, locusTag); }  /* TD: signed   */
        |       "uint32_t"  { t = new IntType("uint32_t",  SizeThunk.INT32,   unsig, cvAttrs, true,  true,  locusTag); }  /* TS: unsigned */
        |       "int64_t"   { t = new IntType("int64_t",   SizeThunk.INT64,   unsig, cvAttrs, true,  false, locusTag); }  /* TD: signed   */
        |       "uint64_t"  { t = new IntType("uint64_t",  SizeThunk.INT64,   unsig, cvAttrs, true,  true,  locusTag); }  /* TD: unsigned */
        |       "ptrdiff_t" { t = new IntType("ptrdiff_t", SizeThunk.POINTER, unsig, cvAttrs, true,  false, locusTag); }  /* TD: signed   */
        |       "intptr_t"  { t = new IntType("intptr_t",  SizeThunk.POINTER, unsig, cvAttrs, true,  false, locusTag); }  /* TD: signed   */
        |       "size_t"    { t = new IntType("size_t",    SizeThunk.POINTER, unsig, cvAttrs, true,  true,  locusTag); }  /* TD: unsigned */
        |       "uintptr_t" { t = new IntType("uintptr_t", SizeThunk.POINTER, unsig, cvAttrs, true,  true,  locusTag); }  /* TD: unsigned */
        |       t = structSpecifier[cvAttrs] ( attributeDecl )*
        |       t = unionSpecifier [cvAttrs] ( attributeDecl )*
        |       t = enumSpecifier  [cvAttrs] 
        |       t = typedefName    [cvAttrs] 
        |       #("typeof" LPAREN
                    ( (typeName )=> typeName 
                    | expr
                    )
                    RPAREN
                )
        |       "__complex"
        ;

typedefName[int cvAttrs] returns [Type t] { t = null; }
        :       #(NTypedefName id : ID)
            {
              final Type t0 = lookupInTypedefDictionary(typedefName_AST_in, id.getText());
              debugPrint("Adding typedef lookup: [" + id.getText() + "] -> "+getDebugTypeString(t0));
              final Type t1 = t0.newCVVariant(cvAttrs);
              debugPrintln(" - cvvar -> "+getDebugTypeString(t1));
              t = canonicalize(t1);
              debugPrintln(" - canon -> "+getDebugTypeString(t));
            }
        ;

structSpecifier[int cvAttrs] returns [Type t] { t = null; }
        :   #( "struct" t = structOrUnionBody[CompoundTypeKind.STRUCT, cvAttrs] )
        ;

unionSpecifier[int cvAttrs] returns [Type t] { t = null; }
        :   #( "union" t = structOrUnionBody[CompoundTypeKind.UNION, cvAttrs] )
        ;
   
structOrUnionBody[CompoundTypeKind kind, int cvAttrs] returns [CompoundType t] {
    t = null;
    boolean addedAny = false;
    final ASTLocusTag locusTag = findASTLocusTag(structOrUnionBody_AST_in);
}
        :       ( (ID LCURLY) => id:ID LCURLY {
                    // fully declared struct, i.e. not anonymous
                    t = (CompoundType) canonicalize(lookupInStructDictionary(id.getText(), kind, cvAttrs, locusTag));
                  } ( addedAny = structDeclarationList[t] )?
                    RCURLY { t.setBodyParsed(); }
                |   LCURLY { 
                      // anonymous declared struct
                      t = CompoundType.create(null, null, kind, cvAttrs, locusTag); 
                    } ( structDeclarationList[t] )?
                    RCURLY { t.setBodyParsed(); }
                | id2:ID { 
                      // anonymous struct
                      t = (CompoundType) canonicalize(lookupInStructDictionary(id2.getText(), kind, cvAttrs, locusTag)); 
                    }
                ) {
                    debugPrintln("Adding compound body: [" + t.getName() + "] -> "+getDebugTypeString(t)+" @ "+locusTag);
                    debugPrintln(t.getStructString());
                }
        ;

structDeclarationList[CompoundType t] returns [boolean addedAny] {
    addedAny = false;
    boolean addedOne = false;
}
        :       ( addedOne = structDeclaration[t] { addedAny |= addedOne; } )+
        ;

structDeclaration[CompoundType containingType] returns [boolean addedAny] {
    addedAny = false;
    Type t = null;
}
        :       t = specifierQualifierList addedAny = structDeclaratorList[containingType, t] {
                    if (!addedAny) {
                        if (t != null) {
                            CompoundType ct = t.asCompound();
                            if( null == ct ) {
                                throwGlueGenException(structDeclaration_AST_in,
                                    String.format("Anonymous compound, w/ NULL type:%n  containing '%s'",
                                        getTypeString(containingType)));
                            }
                            if ( ct.isUnion() ) {
                                // Anonymous union
                                containingType.addField(new Field(null, t, null));
                            }
                        }
                    }
                }
        ;

specifierQualifierList returns [Type t] {
    t = null; int x = 0; int y = 0;
}
        :       (
                t = typeSpecifier[x]
                | y = typeQualifier { x |= y; }
                )+ {
            if (t == null &&
                (x & (SIGNED | UNSIGNED)) != 0) {
                t = new IntType("int", SizeThunk.INTxx, ((x & UNSIGNED) != 0), attrs2CVAttrs(x), 
                                findASTLocusTag(specifierQualifierList_AST_in));
            }
}
        ;

structDeclaratorList[CompoundType containingType, Type t] returns [boolean addedAny] {
    addedAny = false;
    boolean y = false;
}
        :       ( y = structDeclarator[containingType, t] { addedAny = y; })+
        ;

structDeclarator[CompoundType containingType, Type t] returns [boolean addedAny] {
    addedAny = false;
    String s = null;
    TypeBox tb = new TypeBox(t);
}
        :
        #( NStructDeclarator      
            ( s = declarator[tb] { containingType.addField(new Field(s, tb.type(), null)); addedAny = true; } )?
            ( COLON expr     { /* FIXME: bit types not handled yet */ }        ) ?
            ( attributeDecl )*
        )
        ;

// This will not correctly set the name of the enumeration when
// encountering a declaration like this:
//
//     typedef enum {  } enumName;
//                
// In this case calling getName() on the EnumType return value will
// incorrectly return HeaderParser.ANONYMOUS_ENUM_NAME instead of
// "enumName"
//
// The followup typedef, see 'initDecl', will alias this name, 
// hence correct the issue!
enumSpecifier [int cvAttrs] returns [Type t] { 
        t = null; 
        EnumType e = null;
        ASTLocusTag locusTag = findASTLocusTag(enumSpecifier_AST_in);
}
        :       #( "enum"
                   ( ( ID LCURLY )=> i:ID LCURLY enumList[(EnumType)(e = getEnumType(i.getText(), locusTag))] RCURLY 
                     | LCURLY enumList[(EnumType)(e = getEnumType(ANONYMOUS_ENUM_NAME, locusTag))] RCURLY 
                     | ID { e = getEnumType(i.getText(), locusTag); }
                   ) {
                     debugPrintln("Adding enum mapping: "+getDebugTypeString(e));
                     if( null != e ) {
                        final String eName = e.getName();
                        if( null != eName && !eName.equals(ANONYMOUS_ENUM_NAME) ) { // validate only non-anonymous enum
                            final EnumType dupE = enumMap.get(eName);
                            if( null != dupE && !dupE.equalSemantics(e) ) {
                                throwGlueGenException(enumSpecifier_AST_in,
                                    String.format("Duplicate enum w/ incompatible type:%n  this '%s',%n  have '%s',%n  %s: previous definition is here",
                                        getTypeString(e), getTypeString(dupE), dupE.getASTLocusTag().toString(new StringBuilder(), "note", true)));
                            }
                            enumMap.put(eName, (EnumType)e.clone(locusTag));
                        }
                     }
                     t = e; // return val
                   }
                 )
        ;

enumList[EnumType enumeration] {
        ConstantDefinition defEnumerant = new ConstantDefinition("def", "0", new CNumber(true, false, 0), findASTLocusTag(enumList_AST_in));
}
      :       ( defEnumerant = enumerator[enumeration, defEnumerant] )+
      ;

enumerator[EnumType enumeration, ConstantDefinition defaultValue] returns [ConstantDefinition newDefaultValue] {
        newDefaultValue = defaultValue;
}
        :       eName:ID ( ASSIGN eVal:expr )? {
                    final String eTxt = eName.getText();
                    final Enumerator newEnum;
                    if (eVal != null) {
                      String vTxt = eVal.getAllChildrenText(eTxt);
                      if (enumHash.containsKey(vTxt)) {
                        EnumType oldEnumType = enumHash.get(vTxt);
                        Enumerator oldEnum = oldEnumType.getEnum(vTxt);
                        newEnum = oldEnum;
                      } else {
                        newEnum = new Enumerator(eTxt, vTxt);
                      }
                    } else if( defaultValue.hasNumber() ) {
                      newEnum = new Enumerator(eTxt, defaultValue.getNumber());
                    } else {
                      newEnum = new Enumerator(eTxt, defaultValue.getNativeExpr());
                    }
                    final ASTLocusTag locus = findASTLocusTag(enumerator_AST_in);
                    final CNumber newEnumNum = newEnum.getNumber();
                    if( null != newEnumNum && newEnumNum.isInteger ) {
                        final long n = newEnumNum.i+1;
                        newDefaultValue = new ConstantDefinition("def", String.valueOf(n), new CNumber(newEnumNum.isLong, newEnumNum.isUnsigned, n), locus);
                    } else {
                        newDefaultValue = new ConstantDefinition("def", "("+newEnum.getExpr()+")+1", null, locus);
                    }
                    if (enumHash.containsKey(eTxt)) {
                        EnumType oldEnumType = enumHash.get(eTxt);
                        final Enumerator oldEnum = oldEnumType.getEnum(eTxt);
                        final String oldExpr = oldEnum.getExpr();
                        if( !oldExpr.equals(newEnum.getExpr()) ) {
                            throwGlueGenException(enumerator_AST_in,
                                 String.format("Duplicate enum value '%s.%s' w/ diff value:%n  this %s,%n  have %s",
                                     oldEnumType.getName(), eTxt, newEnum, oldEnum));
                        }
                        // remove old definition
                        oldEnumType.removeEnumerate(eTxt);
                    }
                    // insert new definition
                    enumeration.addEnum(eTxt, newEnum);
                    enumHash.put(eTxt, enumeration);
                    debugPrintln("ENUM [" + enumeration.getName() + "]: " + eTxt + " = " + newEnum +
                                 " (new default = " + newDefaultValue + ")");
                }
            ;

initDeclList[TypeBox tb]
        :       ( initDecl[tb] )+
        ;

initDecl[TypeBox tb] {
    String declName = null;
    final ASTLocusTag locusTag = findASTLocusTag(initDecl_AST_in);
}
        :       #( NInitDecl
                declName = declarator[tb] { 
                    debugPrintln("GOT declName: " + declName + " TB=" + tb);
                  }
                ( attributeDecl )*
                ( ASSIGN initializer
                | COLON expr
                )?
                )
{
    if ((declName != null) && (tb != null) && tb.isTypedef()) {
        Type t = tb.type();
        debugPrintln("Adding typedef mapping: [" + declName + "] -> "+getDebugTypeString(t));
        final Type tg;
        if( t.isPointer() ) {
            tg = t.getTargetType();
            debugPrintln("  - has target: "+getDebugTypeString(tg));
        } else {
            tg = null;
        }
        // NOTE: Struct Name Resolution (JavaEmitter, HeaderParser)
        // Also see NOTE below.
        if (!t.isTypedef()) {
            if( t.isCompound() || t.isEnum() ) {
                // This aliases '_a' -> 'A' for 'typedef struct _a { } A;' in-place
                // This aliases '_a' -> 'A' for 'typedef enum _a { } A;' in-place
                t.setTypedefName(declName);
                debugPrintln(" - alias.11 -> "+getDebugTypeString(t));
            } else {
                // Use new typedef, using a copy to preserve canonicalized base type
                t = t.clone(locusTag);
                t.setTypedefName(declName);
                debugPrintln(" - newdefine.12 -> "+getDebugTypeString(t));
            }
        } else {
            // Adds typeInfo alias w/ t's typeInfo, if exists
            cfg.addTypeInfo(declName, t);
            final Type alias;
            if( t.isCompound() ) {
                // This aliases 'D' -> 'A' for 'typedef struct _a { } A, D;' in-place
                debugPrintln(" - alias.21 -> "+getDebugTypeString(t));
            } else {
                // copy to preserve canonicalized base type
                t = t.clone(locusTag);
                t.setTypedefName(declName);
                debugPrintln(" - copy.22 -> "+getDebugTypeString(t));
            }
        }
        final Type dupT = typedefDictionary.get(declName);
        if( null != dupT && !dupT.equalSemantics(t) ) {
            throwGlueGenException(locusTag,
                  String.format("Duplicate typedef w/ incompatible type:%n  this '%s',%n  have '%s',%n  %s: previous definition is here",
                     getTypeString(t), getTypeString(dupT), dupT.getASTLocusTag().toString(new StringBuilder(), "note", true)));
        }
        t = canonicalize(t);
        debugPrintln(" - canon -> "+getDebugTypeString(t));
        typedefDictionary.put(declName, t);
        // Clear out PointerGroup effects in case another typedef variant follows
        tb.reset();
    }
}
    /*
        // Below just shows a different handling using copying
        // and enforcing aliased names, which is not desired.
        // Keeping it in here for documentation.
        // NOTE: Struct Name Resolution (JavaEmitter, HeaderParser)
        if ( !t.isTypedef() ) {
            if( t.isCompound() ) {
                // This aliases '_a' -> 'A' for 'typedef struct _a { } A;'
                t.setTypedefName(declName);
                debugPrintln(" - alias.10 -> "+getDebugTypeString(t));
            } else if( null != tg && tg.isCompound() ) {
                if( !tg.isTypedef() ) {
                    // This aliases '_a *' -> 'A*' for 'typedef struct _a { } *A;'
                    t.setTypedefName(declName);
                    debugPrintln(" - alias.11 -> "+getDebugTypeString(t));
                } else {
                    // This aliases 'B' -> 'A*' for 'typedef struct _a { } A, *B;' and 'typedef A * B;'
                    t = new PointerType(SizeThunk.POINTER, tg, 0, locusTag); // name: 'A*'
                    t.setTypedefName(t.getName()); // make typedef
                    debugPrintln(" - alias.12 -> "+getDebugTypeString(t));
                }
            } else {
                // Use new typedef, using a copy to preserve canonicalized base type
                t = t.clone(locusTag);
                t.setTypedefName(declName);
                debugPrintln(" - newdefine.13 -> "+getDebugTypeString(t));
            }
        } else {
            // Adds typeInfo alias w/ t's typeInfo, if exists
            cfg.addTypeInfo(declName, t);
            if( t.isCompound() ) {
                // This aliases 'D' -> 'A' for 'typedef struct _a { } A, D;'
                debugPrintln(" - alias.20 -> "+getDebugTypeString(t));
            } else if( null != tg && tg.isCompound() ) {
                // This aliases 'B' -> 'A' for 'typedef A B;', where A is pointer to compound
                debugPrintln(" - alias.21 -> "+getDebugTypeString(t));
            } else {
                // copy to preserve canonicalized base type
                t = t.clone(locusTag);
                t.setTypedefName(declName);
                debugPrintln(" - copy.22 -> "+getDebugTypeString(t));
            }
        }
*/
        ;

pointerGroup[TypeBox tb] { int x = 0; int y = 0; }
        :       #( NPointerGroup ( STAR { x = 0; y = 0; } ( y = typeQualifier { x |= y; } )*
                                    {
                                        debugPrintln("IN PTR GROUP: TB=" + tb);
                                        if (tb != null) {
                                            tb.setType(canonicalize(new PointerType(SizeThunk.POINTER,
                                                                                    tb.type(),
                                                                                    attrs2CVAttrs(x), 
                                                                                    findASTLocusTag(pointerGroup_AST_in))));
                                        }
                                    }
                                 )+ )
  ;
                                    

functionDeclSpecifiers
        :       
                ( functionStorageClassSpecifier
                | typeQualifier
                | typeSpecifier[0]
                )+
        ;

typeName {
    TypeBox tb = null;
}
        :       specifierQualifierList (nonemptyAbstractDeclarator[tb])?
        ;


/* FIXME: the handling of types in this rule has not been well thought
   out and is known to be incomplete. Currently it is only used to handle
   pointerGroups for unnamed parameters. */
nonemptyAbstractDeclarator[TypeBox tb] {
    final ASTLocusTag locusTag = findASTLocusTag(nonemptyAbstractDeclarator_AST_in);
}
        :   #( NNonemptyAbstractDeclarator
            (   pointerGroup[tb]
                (   (LPAREN  
                    (   nonemptyAbstractDeclarator[tb]
                        | parameterTypeList
                    )?
                    RPAREN)
                | (LBRACKET (e1:expr)? RBRACKET) { handleArrayExpr(tb, e1, locusTag); }
                )*

            |  (   (LPAREN  
                    (   nonemptyAbstractDeclarator[tb]
                        | parameterTypeList
                    )?
                    RPAREN)
                | (LBRACKET (e2:expr)? RBRACKET) { handleArrayExpr(tb, e2, locusTag); }
                )+
            )
            )
        ;

/* Helper routine for parsing expressions which evaluate to integer
   constants. Can be made more complicated as necessary. */
intConstExpr returns [int i] { i = -1; }
        : n:Number   { return Integer.parseInt(n.getText()); }
        | e:ID {
            final String enumName = e.getText();
            final EnumType enumType = enumHash.get(enumName);
            if( null == enumType ) {
               throwGlueGenException(intConstExpr_AST_in,
                     "Error: intConstExpr ID "+enumName+" recognized, but no containing enum-type found");
            }
            final Enumerator enumerator = enumType.getEnum(enumName);
            final CNumber number = enumerator.getNumber();
            if( null != number && number.isInteger && !number.isLong ) {
                debugPrintln("INFO: intConstExpr: enum[Type "+enumType.getName()+", "+enumerator+"]");
            } else {
                throwGlueGenException(intConstExpr_AST_in,
                     "Error: intConstExpr ID "+enumName+" enum "+enumerator+" not an int32_t");
            }
            return (int)number.i;
          }
        ;
