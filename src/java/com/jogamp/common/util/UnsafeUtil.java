/**
 * Copyright 2019-2025 JogAmp Community. All rights reserved.
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
package com.jogamp.common.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.PrivilegedAction;

import com.jogamp.common.ExceptionUtils;

import jogamp.common.Debug;
import jogamp.common.os.PlatformPropsImpl;

/**
 * Utility methods allowing easy access to certain {@link sun.misc.Unsafe} functionality.
 */
public class UnsafeUtil {

    static final boolean DEBUG;
    static {
        DEBUG = Debug.debug("UnsafeUtil");
    }

    protected UnsafeUtil() {}

    private static final Object theUnsafe;
    private static final Method unsafeCleanBB;
    private static volatile boolean hasUnsafeCleanBBError; /** OK to be lazy on thread synchronization, just for early out **/

    private static final Method m_getObject;
    private static final Method m_putObject;
    private static final Method m_getObjectVolatile;
    private static final Method m_putObjectVolatile;

    private static final Method m_getLong; // long getLong(Object o, long offset)
    private static final Method m_putLong; // putLong(Object o, long offset, long x)

    private static final Method m_staticFieldOffset;

    private static final Class<?> c_illegalAccessLoggerClass;
    private static final Long o_illegalAccessLoggerOffset;
    private static final Object o_illegalAccessLoggerSync = new Object();
    private static volatile boolean hasIllegalAccessError;

    static {
        final Object[] _theUnsafe = { null };
        final Method[] _cleanBB = { null };
        final Method[] _staticFieldOffset = { null };
        final Method[] _getPutObject = { null, null }; // getObject, putObject
        final Method[] _getPutObjectVolatile = { null, null }; // getObjectVolatile, putObjectVolatile
        final Method[] _getPutLong = { null, null }; // long getLong(Object o, long offset), putLong(Object o, long offset, long x)
        final Class<?>[] _illegalAccessLoggerClass = { null };
        final Long[] _loggerOffset = { null };

        SecurityUtil.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                Class<?> unsafeClass = null;
                try {
                    // Using: sun.misc.Unsafe { public void invokeCleaner(java.nio.ByteBuffer directBuffer); }
                    unsafeClass = Class.forName("sun.misc.Unsafe");
                    {
                        final Field f = unsafeClass.getDeclaredField("theUnsafe");
                        f.setAccessible(true);
                        _theUnsafe[0] = f.get(null);
                    }
                    _cleanBB[0] = unsafeClass.getMethod("invokeCleaner", java.nio.ByteBuffer.class);
                    _cleanBB[0].setAccessible(true);
                } catch(final Throwable t) {
                    if( DEBUG ) {
                        ExceptionUtils.dumpThrowable("UnsafeUtil", t);
                    }
                }
                if( null != _theUnsafe[0] ) {
                    try {
                        _getPutObject[0] = unsafeClass.getDeclaredMethod("getObject", Object.class, long.class);
                        _getPutObject[1] = unsafeClass.getDeclaredMethod("putObject", Object.class, long.class, Object.class);
                    } catch(final Throwable t) {
                        if( DEBUG ) {
                            ExceptionUtils.dumpThrowable("UnsafeUtil", t);
                        }
                    }
                    try {
                        _getPutObjectVolatile[0] = unsafeClass.getDeclaredMethod("getObjectVolatile", Object.class, long.class);
                        _getPutObjectVolatile[1] = unsafeClass.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);
                    } catch(final Throwable t) {
                        if( DEBUG ) {
                            ExceptionUtils.dumpThrowable("UnsafeUtil", t);
                        }
                    }
                    try {
                        _getPutLong[0] = unsafeClass.getDeclaredMethod("getLong", Object.class, long.class);
                        _getPutLong[1] = unsafeClass.getDeclaredMethod("putLong", Object.class, long.class, long.class);
                    } catch(final Throwable t) {
                        if( DEBUG ) {
                            ExceptionUtils.dumpThrowable("UnsafeUtil", t);
                        }
                    }
                    try {
                        _staticFieldOffset[0] = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class);
                    } catch(final Throwable t) {
                        if( DEBUG ) {
                            ExceptionUtils.dumpThrowable("UnsafeUtil", t);
                        }
                    }
                    if( PlatformPropsImpl.JAVA_9 && null != _staticFieldOffset[0] ) {
                        try {
                            _illegalAccessLoggerClass[0] = Class.forName("jdk.internal.module.IllegalAccessLogger");
                            final Field loggerField = _illegalAccessLoggerClass[0].getDeclaredField("logger");
                            _loggerOffset[0] = (Long) _staticFieldOffset[0].invoke(_theUnsafe[0], loggerField);
                        } catch(final Throwable t) {
                            if( DEBUG ) {
                                ExceptionUtils.dumpThrowable("UnsafeUtil", t);
                            }
                        }
                    }
                }
                return null;
            } } );
        theUnsafe = _theUnsafe[0];
        unsafeCleanBB = _cleanBB[0];
        hasUnsafeCleanBBError = null == theUnsafe || null == unsafeCleanBB;
        if( DEBUG ) {
            System.err.println("UnsafeUtil.init: hasTheUnsafe: "+(null!=theUnsafe)+", hasInvokeCleaner: "+!hasUnsafeCleanBBError);
        }
        m_staticFieldOffset = _staticFieldOffset[0];
        m_getObject = _getPutObject[0];
        m_putObject = _getPutObject[1];
        m_getObjectVolatile = _getPutObjectVolatile[0];
        m_putObjectVolatile = _getPutObjectVolatile[1];
        m_getLong = _getPutLong[0];
        m_putLong = _getPutLong[1];
        c_illegalAccessLoggerClass = _illegalAccessLoggerClass[0];
        o_illegalAccessLoggerOffset = _loggerOffset[0];
        hasIllegalAccessError = null == m_getObjectVolatile || null == m_putObjectVolatile ||
                                null == c_illegalAccessLoggerClass || null == o_illegalAccessLoggerOffset;
        if( DEBUG ) {
            System.err.println("UnsafeUtil.init: hasUnsafeIllegalAccessLogger: "+!hasIllegalAccessError);
        }
    }

    /**
     * Returns {@code true} if {@code sun.misc.Unsafe.invokeCleaner(java.nio.ByteBuffer)}
     * is available and has not caused an exception.
     * @see #invokeCleaner(ByteBuffer)
     */
    public static boolean hasInvokeCleaner() { return !hasUnsafeCleanBBError; }

    /**
     * Access to {@code sun.misc.Unsafe.invokeCleaner(java.nio.ByteBuffer)}.
     * <p>
     * If {@code b} is an direct NIO buffer, i.e {@link sun.nio.ch.DirectBuffer},
     * calls it's {@link sun.misc.Cleaner} instance {@code clean()} method once.
     * </p>
     * @return {@code true} if successful, otherwise {@code false}.
     * @see #hasInvokeCleaner()
     */
    public static boolean invokeCleaner(final ByteBuffer bb) {
        if( hasUnsafeCleanBBError || !bb.isDirect() ) {
            return false;
        }
        try {
            unsafeCleanBB.invoke(theUnsafe, bb);
            return true;
        } catch(final Throwable t) {
            hasUnsafeCleanBBError = true;
            if( DEBUG ) {
                ExceptionUtils.dumpThrowable("UnsafeUtil", t);
            }
            return false;
        }
    }

    public static long staticFieldOffset(final Field f) {
        if( null != m_staticFieldOffset) {
            throw new UnsupportedOperationException("staticFieldOffset");
        }
        try {
            final Long res = (Long)m_staticFieldOffset.invoke(theUnsafe, f);
            if( null != res ) {
                return res.longValue();
            }
            throw new RuntimeException("staticFieldOffset: f "+f);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("UnsafeUtil");
        }
    }

    public static Object getObject(final Object o, final long offset) {
        if( null != m_getObject) {
            throw new UnsupportedOperationException("getObject");
        }
        try {
            return m_getObject.invoke(theUnsafe, o, offset);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("UnsafeUtil: o "+o+", offset "+offset, e);
        }
    }
    public static void putObject(final Object o, final long offset, final Object x) {
        if( null != m_putObject) {
            throw new UnsupportedOperationException("putObject");
        }
        try {
            m_putObject.invoke(theUnsafe, o, offset, x);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("UnsafeUtil");
        }
    }

    public static Object getObjectVolatile(final Object o, final long offset) {
        if( null != m_getObjectVolatile) {
            throw new UnsupportedOperationException("getObjectVolatile");
        }
        try {
            return m_getObjectVolatile.invoke(theUnsafe, o, offset);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("UnsafeUtil");
        }
    }
    public static void putObjectVolatile(final Object o, final long offset, final Object x) {
        if( null != m_putObjectVolatile) {
            throw new UnsupportedOperationException("putObjectVolatile");
        }
        try {
            m_putObjectVolatile.invoke(theUnsafe, o, offset, x);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("UnsafeUtil");
        }
    }

    public static long getLong(final Object o, final long offset) {
        if(null != m_getLong) {
            throw new UnsupportedOperationException("getLong");
        }
        try {
            final Long res = (Long)m_getLong.invoke(theUnsafe, o, offset);
            if( null != res ) {
                return res.longValue();
            }
            throw new RuntimeException("getLong: o "+o+", offset "+offset);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("UnsafeUtil");
        }
    }
    public static void putLong(final Object o, final long offset, final long x) {
        if(null != m_putLong) {
            throw new UnsupportedOperationException("putLong");
        }
        try {
            m_putLong.invoke(theUnsafe, o, offset);
            throw new RuntimeException("putLong: o "+o+", offset "+offset+", x "+x);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("UnsafeUtil");
        }
    }

    public static long getLong(final long address) {
        return getLong(null, address);
    }
    public static void putLong(final long address, final long x) {
        putLong(null, address, x);
    }

    /**
     * Returns {@code true} if access to {@code jdk.internal.module.IllegalAcessLogger}'s {@code logger} field
     * is available and has not caused an exception.
     * @see #doWithoutIllegalAccessLogger(PrivilegedAction)
     */
    public static boolean hasIllegalAccessLoggerAccess() { return !hasIllegalAccessError; }

    /**
     * Issue the given user {@code action} while {@code jdk.internal.module.IllegalAcessLogger}'s {@code logger} has been temporarily disabled.
     * <p>
     * The caller shall place this call into their own {@link SecurityUtil#doPrivileged(PrivilegedAction)} block.
     * </p>
     * <p>
     * In case the runtime is not {@link PlatformPropsImpl#JAVA_9} or the logger is not accessible or disabling caused an exception,
     * the user {@code action} is just executed w/o temporary logger modifications.
     * </p>
     * @param action the user action task
     * @throws RuntimeException is thrown for a caught {@link Throwable} while executing the user {@code action}
     * @see #hasIllegalAccessLoggerAccess()
     */
    public static <T> T doWithoutIllegalAccessLogger(final PrivilegedAction<T> action) throws RuntimeException {
        if( !hasIllegalAccessError ) {
            synchronized(o_illegalAccessLoggerSync) {
                final Object newLogger = null;
                Object oldLogger = null;
                try {
                    oldLogger = m_getObjectVolatile.invoke(theUnsafe, c_illegalAccessLoggerClass, o_illegalAccessLoggerOffset);
                    m_putObjectVolatile.invoke(theUnsafe, c_illegalAccessLoggerClass, o_illegalAccessLoggerOffset, newLogger);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    // unaccessible ..
                    hasIllegalAccessError = true;
                    if( DEBUG ) {
                        ExceptionUtils.dumpThrowable("UnsafeUtil", e);
                    }
                    return action.run();
                }
                try {
                    return action.run();
                } catch (final Throwable t) {
                    if( DEBUG ) {
                        t.printStackTrace();
                    }
                    throw new RuntimeException(t);
                } finally {
                    try {
                        m_putObjectVolatile.invoke(theUnsafe, c_illegalAccessLoggerClass, o_illegalAccessLoggerOffset, oldLogger);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        // should not happen, worked above @ logger setup
                        hasIllegalAccessError = true;
                        throw new InternalError(e);
                    }
                }
            }
        } else {
            return action.run();
        }
    }
}
