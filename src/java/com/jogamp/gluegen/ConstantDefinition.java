/**
 * Copyright 2015 JogAmp Community. All rights reserved.
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
package com.jogamp.gluegen;

import java.math.BigInteger;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jogamp.gluegen.ASTLocusTag.ASTLocusTagProvider;
import com.jogamp.gluegen.cgram.types.AliasedSymbol.AliasedSymbolImpl;
import com.jogamp.gluegen.cgram.types.TypeComparator.AliasedSemanticSymbol;
import com.jogamp.gluegen.cgram.types.TypeComparator.SemanticEqualityOp;

/**
 * Represents a [native] constant expression,
 * comprises the [native] expression, see {@link #getNativeExpr()}
 * and the optional {@link CNumber} representation, see {@link #getNumber()}.
 * <p>
 * The representation of the equivalent java expression including
 * the result type is covered by {@link JavaExpr},
 * which can be computed via {@link #computeJavaExpr(Map)}.
 * </p>
 * <p>
 * This class and its sub-classes define and convert all native expressions
 * to Java space.
 * </p>
 */
public class ConstantDefinition extends AliasedSymbolImpl implements AliasedSemanticSymbol, ASTLocusTagProvider {
    public static final long UNSIGNED_INT_MAX_VALUE = 0xffffffffL;
    public static final BigInteger UNSIGNED_LONG_MAX_VALUE = new BigInteger("ffffffffffffffff", 16);

    /**
     * A Number, either integer, optionally  [long, unsigned],
     * or floating point, optionally [double].
     */
    public static class CNumber {
        /**
         * {@code true} if number is integer and value stored in {@link #i},
         * otherwise {@code false} for floating point and value stored in {@link #f}.
         */
        public final boolean isInteger;
        /** {@code true} if number is a {@code long} {@link #isInteger}. */
        public final boolean isLong;
        /** {@code true} if number is an {@code unsigned} {@link #isInteger}. */
        public final boolean isUnsigned;
        /** The value if {@link #isInteger} */
        public final long i;

        /** {@code true} if number is a {@code double precision} {@code floating point}, i.e. !{@link #isInteger}. */
        public final boolean isDouble;
        /** The value if !{@link #isInteger} */
        public final double f;

        /** ctor for integer number */
        public CNumber(final boolean isLong, final boolean isUnsigned, final long value) {
            this.isInteger = true;
            this.isLong = isLong;
            this.isUnsigned = isUnsigned;
            this.i = value;
            this.isDouble = false;
            this.f = 0.0;
        }
        /** ctor for floating point number */
        public CNumber(final boolean isDouble, final double value) {
            this.isInteger = false;
            this.isLong = false;
            this.isUnsigned = false;
            this.i = 0;
            this.isDouble = isDouble;
            this.f = value;
        }
        @Override
        public int hashCode() {
            return isInteger ? Long.valueOf(i).hashCode() : Double.valueOf(f).hashCode();
        }
        @Override
        public boolean equals(final Object arg) {
            if (arg == this) {
                return true;
            } else if ( !(arg instanceof CNumber) ) {
                return false;
            }
            final CNumber t = (CNumber) arg;
            return isInteger == t.isInteger &&
                   ( isInteger ? i == t.i : f == t.f );
        }
        public final String toJavaString() {
            if( isInteger ) {
                if( i >= 0 || isUnsigned ) {
                    if( isLong ) {
                        return "0x"+Long.toHexString(i)+"L";
                    } else {
                        return "0x"+Integer.toHexString((int)i);
                    }
                } else {
                    if( isLong ) {
                        return String.valueOf(i)+"L";
                    } else {
                        return String.valueOf((int)i);
                    }
                }
            } else {
               return String.valueOf(f) + ( !isDouble ? "f" : "");
            }
        }
        public final String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("[");
            if( isInteger ) {
                if( isUnsigned ) {
                    sb.append("unsigned ");
                }
                if( isLong) {
                    sb.append("long: ");
                } else {
                    sb.append("int: ");
                }
                sb.append(i);
            } else {
                if( isDouble ) {
                    sb.append("double: ");
                } else {
                    sb.append("float: ");
                }
                sb.append(f);
            }
            sb.append("]");
            return sb.toString();
        }
    }

    /**
     * A valid java expression, including its result type,
     * usually generated from a native [C] expression,
     * see {@link JavaExpr#create(ConstantDefinition)}.
     */
    public static class JavaExpr {
          public final String javaExpression;
          public final CNumber resultType;
          public final Number resultJavaType;
          public final String resultJavaTypeName;
          public JavaExpr(final String javaExpression, final CNumber resultType) {
              this.javaExpression = javaExpression;
              this.resultType = resultType;
              if( resultType.isDouble ) {
                  resultJavaTypeName = "double";
                  resultJavaType = Double.valueOf(resultType.f);
              } else if( !resultType.isInteger ) {
                  resultJavaTypeName = "float";
                  resultJavaType = Double.valueOf(resultType.f).floatValue();
              } else if( resultType.isLong ) {
                  resultJavaTypeName = "long";
                  resultJavaType = Long.valueOf(resultType.i);
              } else /* if( resultType.isInteger ) */ {
                  resultJavaTypeName = "int";
                  resultJavaType = Long.valueOf(resultType.i).intValue();
              }
          }
          /**
           * Computes a valid {@link JavaExpr java expression} based on the given {@link ConstantDefinition},
           * which may either be a single {@link CNumber}, see {@link ConstantDefinition#getNumber()},
           * or represents a native expression, see {@link ConstantDefinition#getExpr()}.
           */
          public static JavaExpr compute(final ConstantDefinition constDef,
                                               final Map<String, ConstantDefinition.JavaExpr> constMap) {
              final boolean debug = GlueGen.debug();
              if( debug ) {
                  System.err.println("ConstJavaExpr.create: "+constDef);
              }
              if( constDef.hasNumber() ) {
                  // Already parsed as CNumber completely!
                  if( debug ) {
                      System.err.printf("V %s (isCNumber)%n", constDef);
                  }
                  return new JavaExpr(constDef.getNumber().toJavaString(), constDef.getNumber());
              }
              final StringBuilder javaExpr = new StringBuilder();
              final String nativeExpr = constDef.getNativeExpr();

              // "calculates" the result type of a simple expression
              // example: (2+3)-(2.0f-3.0) -> Double
              // example: (1 << 2) -> Integer
              CNumber resultType = null;
              final Matcher matcher = patternCPPOperand.matcher(nativeExpr);
              int preStartIdx = 0;
              int opEndIdx = 0;
              while ( matcher.find() ) {
                  final int opStartIdx = matcher.start();
                  if( opStartIdx > preStartIdx ) {
                      final String sValue = nativeExpr.substring(preStartIdx, opStartIdx).trim();
                      if( sValue.length() > 0 ) {
                          if( debug ) {
                              System.err.printf("V %03d-%03d: %s%n", preStartIdx, opStartIdx, sValue);
                          }
                          resultType = processValue(constDef, sValue, constMap, resultType, javaExpr);
                          javaExpr.append(" ");
                      }
                  }
                  opEndIdx = matcher.end();
                  final String op  =  nativeExpr.substring(opStartIdx, opEndIdx);
                  if( debug ) {
                      System.err.printf("O %03d-%03d: %s%n", opStartIdx, opEndIdx, op);
                  }
                  javaExpr.append(op).append(" ");
                  preStartIdx = opEndIdx;
              }
              if( opEndIdx < nativeExpr.length() ) {
                  // tail ..
                  final String sValue = nativeExpr.substring(opEndIdx).trim();
                  if( sValue.length() > 0 ) {
                      if( debug ) {
                          System.err.printf("V %03d %03d-%03d: %s (tail)%n", preStartIdx, opEndIdx, nativeExpr.length(), sValue);
                      }
                      resultType = processValue(constDef, sValue, constMap, resultType, javaExpr);
                  }
              }
              final String javaExprS = javaExpr.toString().trim();
              if( null == resultType ) {
                  throw new GlueGenException("Cannot emit const \""+constDef.getName()+"\": value \""+nativeExpr+
                      "\", parsed \""+javaExprS+"\" does not contain a constant number", constDef.getASTLocusTag());
              }
              return new JavaExpr(javaExprS, resultType);
          }
          private static CNumber processValue(final ConstantDefinition constDef,
                                              final String sValue,
                                              final Map<String, ConstantDefinition.JavaExpr> constMap,
                                              CNumber resultType,
                                              final StringBuilder javaExpr) {
              final CNumber nValue = getANumber(constDef, sValue);
              if( null != nValue ) {
                  resultType = evalType(resultType , nValue);
                  javaExpr.append(nValue.toJavaString());
              } else {
                  // Lookup CNumber type in const-map, to evaluate this result type
                  final JavaExpr cje = constMap.get(sValue);
                  if( null != cje ) {
                      resultType = evalType(resultType , cje.resultType);
                  }
                  javaExpr.append(sValue);
              }
              return resultType;
          }
          private static CNumber getANumber(final ConstantDefinition constDef, final String value) {
              try {
                  final CNumber number = decodeANumber(value);
                  if( null != number ) {
                      return number;
                  }
              } catch( final Throwable _t ) {
                  final String msg = "Cannot emit const \""+constDef.getName()+"\": value \""+value+
                          "\" cannot be assigned to a int, long, float, or double";
                  throw new GlueGenException(msg, constDef.getASTLocusTag(), _t);
              }
              return null;
          }
          private static CNumber evalType(final CNumber resultType, final CNumber type) {
              //fast path
              if( type.isDouble ) {
                  return type;
              }
              if( null != resultType ) {
                  if( resultType.isInteger ) {
                      if( resultType.isLong ) {
                          /* resultType is Long */
                          if( !type.isInteger ) {
                              /* resultType: Long -> [ Float || Double ] */
                              return type;
                          }
                      } else if( type.isLong || !type.isInteger ) {
                          /* resultType: Integer -> [ Long || Float || Double ] */
                          return type;
                      }
                  } else if( !resultType.isInteger && !resultType.isDouble ) {
                      if( type.isDouble ) {
                          /* resultType: Float -> Double */
                          return type;
                      }
                  }
              } else {
                  return type;
              }
              return resultType;
          }
      }

    private final boolean relaxedEqSem;
    private final String nativeExpr;
    private final CNumber number;
    private final boolean isEnum;
    private final String enumName;
    private final ASTLocusTag astLocus;

    /**
     * Constructor for plain const-values, non-enumerates.
     * @param name unique name of this constant expression
     * @param nativeExpr original [native] expression
     * @param number optional {@link CNumber} representing this constant.
     *               If {@code null}, implementation attempts to derive a {@link CNumber}
     *               of the given {@code nativeExpr}.
     * @param astLocus AST location of the represented constant.
     */
    public ConstantDefinition(final String name,
                              final String nativeExpr,
                              final CNumber number,
                              final ASTLocusTag astLocus) {
        this(name, nativeExpr, number, false, null, astLocus);
    }
    /**
     * Constructor for enumerates
     * @param name unique name of this constant expression
     * @param nativeExpr original [native] expression
     * @param number optional {@link CNumber} representing this constant.
     *               If {@code null}, implementation attempts to derive a {@link CNumber}
     *               of the given {@code nativeExpr}.
     * @param enumName optional name of the represented enumeration
     * @param astLocus AST location of the represented constant.
     */
    public ConstantDefinition(final String name,
                              final String nativeExpr,
                              final CNumber number,
                              final String enumName, final ASTLocusTag astLocus) {
        this(name, nativeExpr, number, true, enumName, astLocus);
    }
    /**
     * @param name unique name of this constant expression
     * @param nativeExpr original [native] expression
     * @param number optional {@link CNumber} representing this constant.
     *               If {@code null}, implementation attempts to derive a {@link CNumber}
     *               of the given {@code nativeExpr}.
     * @param isEnum {@code true} if this constant is an enumerate, otherwise {@code false}.
     * @param enumName optional name of the represented enumeration
     * @param astLocus AST location of the represented constant.
     */
    private ConstantDefinition(final String name,
                               final String nativeExpr,
                               final CNumber number,
                               final boolean isEnum, final String enumName, final ASTLocusTag astLocus) {
        super(name);
        this.nativeExpr = nativeExpr;
        this.relaxedEqSem = TypeConfig.relaxedEqualSemanticsTest();
        if( null != number ) {
            this.number = number;
        } else {
            // Attempt to parse define string as number
            final CNumber iNum = decodeIntegerNumber(nativeExpr);
            if( null != iNum ) {
                this.number = iNum;
            } else {
                final CNumber fNum = decodeDecimalNumber(nativeExpr);
                if( null != fNum ) {
                    this.number = fNum;
                } else {
                    this.number = null;
                }
            }
        }
        this.isEnum = isEnum;
        this.enumName = enumName;
        this.astLocus = astLocus;
    }

    @Override
    public ASTLocusTag getASTLocusTag() { return astLocus; }

    /**
     * Hash by its given {@link #getName() name}.
     */
    @Override
    public final int hashCode() {
        return getName().hashCode();
    }

    /**
     * Equality test by its given {@link #getName() name}.
     */
    @Override
    public final boolean equals(final Object arg) {
        if (arg == this) {
            return true;
        } else  if ( !(arg instanceof ConstantDefinition) ) {
            return false;
        } else {
            final ConstantDefinition t = (ConstantDefinition)arg;
            return equals(getName(), t.getName());
        }
    }

    @Override
    public final int hashCodeSemantics() {
        // 31 * x == (x << 5) - x
        int hash = 31 + ( null != getName() ? getName().hashCode() : 0 );
        hash = ((hash << 5) - hash) + ( isEnum ? 1 : 0 );
        hash = ((hash << 5) - hash) + ( null != enumName ? enumName.hashCode() : 0 );
        hash = ((hash << 5) - hash) + ( null != number ? number.hashCode() : 0 );
        return ((hash << 5) - hash) + ( !relaxedEqSem && null != nativeExpr ? nativeExpr.hashCode() : 0 );
    }

    @Override
    public final boolean equalSemantics(final SemanticEqualityOp arg) {
        if (arg == this) {
            return true;
        } else  if ( !(arg instanceof ConstantDefinition) ) {
            return false;
        } else {
            final ConstantDefinition t = (ConstantDefinition) arg;
            if( !equals(getName(), t.getName()) ||
                isEnum != t.isEnum ||
                !equals(enumName, t.enumName) ) {
                return false;
            }
            if( null != number ) {
                if( number.isInteger ) {
                    return number.i == t.number.i;
                } else {
                    return number.f == t.number.f;
                }
            } else {
                // define's string value may be semantical equal .. but formatted differently!
                return relaxedEqSem || equals(nativeExpr, t.nativeExpr);
            }
        }
    }

    /** Returns the original [native] expression. */
    public String getNativeExpr() { return nativeExpr; }
    /**
     * Returns the parsed {@link CNumber} of the {@link #getNativeExpr() native expression},
     * or {@code null} if the latter does not comprise a single number,
     * i.e. is a complex expression.
     */
    public CNumber getNumber()  { return number; }
    /**
     * Returns {@code true} if this instance represents has a {@link #getNumber() number},
     * otherwise {@code false}.
     */
    public boolean hasNumber()  { return null != number; }

    /** Returns {@code null} if this definition was not part of an
        enumeration, or if the enumeration is anonymous. */
    public String getEnumName() { return enumName; }

    public boolean isEnum() { return isEnum; }

    @Override
    public String toString() {
        return "ConstantDefinition [name \"" + getName()
                + "\", expression \"" + nativeExpr
                + "\", number "+number
                + "], enum[is " + isEnum + ", name \"" + enumName + "\"]]";
    }

    private static boolean equals(final String s1, final String s2) {
        if (s1 == null || s2 == null) {
            if (s1 == null && s2 == null) {
                return true;
            }
            return false;
        }

        return s1.equals(s2);
    }

    /**
     * Computes the {@link JavaExpr java expression} based on this instance,
     * see {@link JavaExpr#create(ConstantDefinition)}.
     */
    public final JavaExpr computeJavaExpr(final Map<String, ConstantDefinition.JavaExpr> constMap) {
        return JavaExpr.compute(this, constMap);
    }

    //
    // Static utility functions for type detection
    //

    public static boolean isConstantExpression(final String value) {
        if( null != value && value.length() > 0 ) {
            // Single numeric value
            if ( isNumber(value) ) {
                return true;
            }
            // Find constant expressions like (1 << 3)
            // if found just pass them through, they will most likely work in java too
            // expressions containing identifiers are currently ignored (casts too)
            final String[] values = value.split("[\\s\\(\\)]"); // [ whitespace '(' ')' ]
            int numberCount = 0;
            for (final String s : values) {
                if( s.length() > 0 ) {
                    if( isCPPOperand(s) ) {
                        // OK
                    } else if ( isNumber(s) ) {
                        // OK
                        numberCount++;
                    } else {
                        return false;
                    }
                }
            }
            final boolean res = numberCount > 0;
            return res;
        }
        return false;
    }

    public static boolean isIdentifier(final String value) {
        boolean identifier = false;

        final char[] chars = value.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            final char c = chars[i];
            if (i == 0) {
                if (Character.isJavaIdentifierStart(c)) {
                    identifier = true;
                }
            } else {
                if (!Character.isJavaIdentifierPart(c)) {
                    identifier = false;
                    break;
                }
            }
        }
        return identifier;
    }

    /**
     * Returns either {@link #decodeIntegerNumber(String)},
     * {@link #decodeDecimalNumber(String)} or {@code null}.
     * @param v
     */
    public static CNumber decodeANumber(final String v) {
        final CNumber iNumber = ConstantDefinition.decodeIntegerNumber(v);
        if( null != iNumber ) {
            return iNumber;
        }
        return ConstantDefinition.decodeDecimalNumber(v);
    }

    /**
     * If the given string {@link #isIntegerNumber(String)},
     * return the decoded integer value, represented as a {@code ANumber},
     * otherwise returns {@code null}.
     * <p>
     * Method strips off sign prefix {@code +}
     * and integer modifier suffixes {@code [uUlL]}
     * before utilizing {@link Long#decode(String)}.
     * </p>
     * @param v
     */
    public static CNumber decodeIntegerNumber(final String v) {
        if( null == v || !isIntegerNumber(v) ) {
            return null;
        }
        String s0 = v.trim();
        if( 0 == s0.length() ) {
            return null;
        }
        if (s0.startsWith("+")) {
            s0 = s0.substring(1, s0.length()).trim();
            if( 0 == s0.length() ) {
                return null;
            }
        }
        final boolean neg;
        if (s0.startsWith("-")) {
            s0 = s0.substring(1, s0.length()).trim();
            if( 0 == s0.length() ) {
                return null;
            }
            neg = true;
        } else {
            neg = false;
        }

        // Test last two chars for [lL] and [uU] modifiers!
        boolean isUnsigned = false;
        boolean isLong = false;
        final int j = s0.length() - 2;
        for(int i = s0.length() - 1; i >= 0 && i >= j; i--) {
            final char lastChar = s0.charAt(s0.length()-1);
            if( lastChar == 'u' || lastChar == 'U' ) {
                s0 = s0.substring(0, s0.length()-1);
                isUnsigned = true;
            } else if( lastChar == 'l' || lastChar == 'L' ) {
                s0 = s0.substring(0, s0.length()-1);
                isLong = true;
            } else {
                // early out, no modifier match!
                break;
            }
        }
        if( 0 == s0.length() ) {
            return null;
        }
        final long res;
        if( isLong && isUnsigned ) {
            res = decodeULong(s0, neg);
        } else {
            if( neg ) {
                s0 = "-" + s0;
            }
            res = Long.decode(s0).longValue();
        }
        final boolean isLong2 = isLong ||
                                ( !isUnsigned && ( Integer.MIN_VALUE > res || res > Integer.MAX_VALUE ) ) ||
                                ( isUnsigned && res > UNSIGNED_INT_MAX_VALUE );
        return new CNumber(isLong2, isUnsigned, res);
    }
    private static long decodeULong(final String v, final boolean neg) throws NumberFormatException {
        final int radix;
        final int idx;
        if (v.startsWith("0x") || v.startsWith("0X")) {
            idx = 2;
            radix = 16;
        } else if (v.startsWith("#")) {
            idx = 1;
            radix = 16;
        } else if (v.startsWith("0") && v.length() > 1) {
            idx = 1;
            radix = 8;
        } else {
            idx = 0;
            radix = 10;
        }
        final String s0 = ( neg ? "-" : "" ) + v.substring(idx);
        final BigInteger res = new BigInteger(s0, radix);
        if( res.compareTo(UNSIGNED_LONG_MAX_VALUE) > 0 ) {
            throw new NumberFormatException("Value \""+v+"\" is > UNSIGNED_LONG_MAX");
        }
        return res.longValue();
    }

    /**
     * If the given string {@link #isDecimalNumber(String)},
     * return the decoded floating-point value, represented as a {@code ANumber} object,
     * otherwise returns {@code null}.
     * <p>
     * Method utilizes {@link Double#valueOf(String)}.
     * </p>
     * @param v
     * @param isDouble return value for {@code double} flag
     */
    public static CNumber decodeDecimalNumber(final String v) {
        if( null == v || !isDecimalNumber(v) ) {
            return null;
        }
        final String s0 = v.trim();
        if( 0 == s0.length() ) {
            return null;
        }
        boolean _isDouble = false;
        final char lastChar = s0.charAt(s0.length()-1);
        if( lastChar == 'd' || lastChar == 'D' ) {
            _isDouble = true;
        }
        final double res = Double.valueOf(s0).doubleValue();
        final double ares = Math.abs(res);
        return new CNumber(_isDouble || Float.MIN_VALUE > ares || ares > Float.MAX_VALUE, res);
    }

    /**
     * Matches {@link #isHexNumber(String)} or {@link #isDecimalOrIntNumber(String)}.
     */
    public static boolean isNumber(final String s) {
        if( isHexNumber(s) ) {
            return true;
        } else {
            return isDecimalOrIntNumber(s);
        }
    }

    /**
     * Matches {@link #isHexNumber(String)} or {@link #patternIntegerNumber}.
     */
    public static boolean isIntegerNumber(final String s) {
        if( isHexNumber(s) ) {
            return true;
        } else {
            return patternIntegerNumber.matcher(s).matches();
        }
    }

    /**
     * Matches {@link #patternHexNumber}.
     */
    public static boolean isHexNumber(final String s) {
        return patternHexNumber.matcher(s).matches();
    }

    /**
     * Matches pattern for <code>floating point</code> number,
     * compatible and described in {@link Double#valueOf(String)}.
     */
    public static boolean isDecimalNumber(final String s) {
        return patternDecimalNumber.matcher(s).matches();
    }

    /**
     * Complete pattern for <code>floating point</code> <i>and</i> <code>integer</code> number,
     * covering {@link #patternDecimalNumber} <i>and</i> {@link #patternIntegerNumber}.
     */
    public static boolean isDecimalOrIntNumber(final String s) {
        return patternDecimalOrIntNumber.matcher(s).matches();
    }

    /**
     * Matches pattern for valid CPP operands, see {@link #patternCPPOperand}.
     */
    public static boolean isCPPOperand(final String s) {
        return patternCPPOperand.matcher(s).matches();
    }

    /**
     * Complete pattern for <code>hexadecimal</code> number,
     * including an optional sign {@code [+-]} and optional suffixes {@code [uUlL]}.
     */
    public static Pattern patternHexNumber;

    /**
     * Complete pattern for <code>floating point</code> number,
     * compatible and described in {@link Double#valueOf(String)}.
     */
    public final static Pattern patternDecimalNumber;

    /**
     * Complete pattern for <code>floating point</code> <i>and</i> <code>integer</code> number,
     * covering {@link #patternDecimalNumber} <i>and</i> {@link #patternIntegerNumber}.
     */
    public final static Pattern patternDecimalOrIntNumber;

    /**
     * Complete pattern for <code>integer</code> number,
     * including an optional sign {@code [+-]} and optional suffixes {@code [uUlL]}.
     */
    public final static Pattern patternIntegerNumber;

    /**
     * One of: {@code +} {@code -} {@code *} {@code /} {@code |} {@code &} {@code (} {@code )} {@code <<} {@code >>}
     * <p>
     * Expression excludes {@link #patternDecimalOrIntNumber}.
     * </p>
     */
    public static Pattern patternCPPOperand;

    static {
        final String WhiteSpace = "[\\x00-\\x20]*";
        final String Digits = "(\\p{Digit}+)";
        final String HexDigits = "(\\p{XDigit}+)";
        final String IntTypeSuffix =
             "(" +
                 "[uU]|" +
                 "([uU][lL])|" +
                 "[lL]|" +
                 "([lL][uU])" +
             ")";

        final String hexRegex =
             WhiteSpace +  // Optional leading "whitespace"
             "[+-]?" + // Optional sign character
             // HexDigits IntTypeSuffix_opt
             "0[xX]" + HexDigits + IntTypeSuffix + "?" +
             WhiteSpace // Optional trailing "whitespace"
             ;
        patternHexNumber = Pattern.compile(hexRegex);

        final String intRegex =
             WhiteSpace +  // Optional leading "whitespace"
             "[+-]?" + // Optional sign character
             // Digits IntTypeSuffix_opt
             Digits + IntTypeSuffix + "?" +
             WhiteSpace // Optional trailing "whitespace"
             ;
        patternIntegerNumber = Pattern.compile(intRegex);

        // an exponent is 'e' or 'E' followed by an optionally
        // signed decimal integer.
        final String Exp = "[eE][+-]?"+Digits;
        final String fpRegex =
             WhiteSpace +  // Optional leading "whitespace"
             "[+-]?" + // Optional sign character
             "("+
                 "NaN|" +       // "NaN" string
                 "Infinity|" +  // "Infinity" string

                 // A decimal floating-point string representing a finite positive
                 // number without a leading sign has at most five basic pieces:
                 // Digits . Digits ExponentPart FloatTypeSuffix
                 //
                 // Since this method allows integer-only strings as input
                 // in addition to strings of floating-point literals, the
                 // two sub-patterns below are simplifications of the grammar
                 // productions from the Java Language Specification, 2nd
                 // edition, section 3.10.2.

                 "("+
                     "("+
                         // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
                         "("+Digits+"(\\.)?("+Digits+"?)("+Exp+")?)|"+

                         // . Digits ExponentPart_opt FloatTypeSuffix_opt
                         "(\\.("+Digits+")("+Exp+")?)|"+

                         // Hexadecimal w/ binary exponent
                         "(" +
                             "(" +
                                 // Hexadecimal strings
                                 // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
                                 "(0[xX]" + HexDigits + "(\\.)?)|" +

                                 // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
                                 "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +
                             ")" +

                           // binary exponent
                           "[pP][+-]?" + Digits +
                         ")" +
                     ")" +
                     "[fFdD]?"+
                 ")"+
             ")" +
             WhiteSpace // Optional trailing "whitespace"
             ;
        patternDecimalNumber = Pattern.compile(fpRegex);

        final String fpOrIntRegex =
             WhiteSpace +  // Optional leading "whitespace"
             "[+-]?" + // Optional sign character
             "("+
                 "NaN|" +       // "NaN" string
                 "Infinity|" +  // "Infinity" string

                 // Matching integers w/ IntTypeSuffix,
                 // which are otherwise not matched by the below floating point matcher!
                 // Digits IntTypeSuffix
                 "(" + Digits + IntTypeSuffix +")|" +

                 // A decimal floating-point string representing a finite positive
                 // number without a leading sign has at most five basic pieces:
                 // Digits . Digits ExponentPart FloatTypeSuffix
                 //
                 // Since this method allows integer-only strings as input
                 // in addition to strings of floating-point literals, the
                 // two sub-patterns below are simplifications of the grammar
                 // productions from the Java Language Specification, 2nd
                 // edition, section 3.10.2.

                 "("+
                     "("+
                         // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
                         "(" + Digits + "(\\.)?(" + Digits + "?)(" + Exp + ")?)|" +

                         // . Digits ExponentPart_opt FloatTypeSuffix_opt
                         "(\\.(" + Digits + ")(" + Exp + ")?)|" +

                         // Hexadecimal w/ binary exponent
                         "(" +
                             "(" +
                                 // Hexadecimal strings
                                 // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
                                 "(0[xX]" + HexDigits + "(\\.)?)|" +

                                 // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
                                 "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +
                             ")" +

                           // binary exponent
                           "[pP][+-]?" + Digits +
                         ")" +
                     ")" +
                     "[fFdD]?"+
                 ")"+
             ")" +
             WhiteSpace // Optional trailing "whitespace"
             ;
        patternDecimalOrIntNumber = Pattern.compile(fpOrIntRegex);

        final String fpOrIntRegex2 =
             WhiteSpace +  // Optional leading "whitespace"
             // "[+-]?" + // Optional sign character
             "("+
                 "NaN|" +       // "NaN" string
                 "Infinity|" +  // "Infinity" string

                 // Matching integers w/ IntTypeSuffix,
                 // which are otherwise not matched by the below floating point matcher!
                 // Digits IntTypeSuffix
                 "(" + Digits + IntTypeSuffix +")|" +

                 // A decimal floating-point string representing a finite positive
                 // number without a leading sign has at most five basic pieces:
                 // Digits . Digits ExponentPart FloatTypeSuffix
                 //
                 // Since this method allows integer-only strings as input
                 // in addition to strings of floating-point literals, the
                 // two sub-patterns below are simplifications of the grammar
                 // productions from the Java Language Specification, 2nd
                 // edition, section 3.10.2.

                 "("+
                     "("+
                         // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
                         "(" + Digits + "(\\.)?(" + Digits + "?)(" + Exp + ")?)|" +

                         // . Digits ExponentPart_opt FloatTypeSuffix_opt
                         "(\\.(" + Digits + ")(" + Exp + ")?)|" +

                         // Hexadecimal w/ binary exponent
                         "(" +
                             "(" +
                                 // Hexadecimal strings
                                 // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
                                 "(0[xX]" + HexDigits + "(\\.)?)|" +

                                 // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
                                 "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +
                             ")" +

                           // binary exponent
                           "[pP][+-]?" + Digits +
                         ")" +
                     ")" +
                     "[fFdD]?"+
                 ")"+
             ")" +
             WhiteSpace // Optional trailing "whitespace"
             ;
        patternCPPOperand = Pattern.compile("(?!"+fpOrIntRegex2+")[\\+\\-\\*\\/\\|\\&\\(\\)]|(\\<\\<)|(\\>\\>)");
    }
}
