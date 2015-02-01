/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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
package com.jogamp.common.os;

import java.lang.reflect.Field;

import com.jogamp.common.os.Platform.ABIType;
import com.jogamp.common.os.Platform.CPUType;
import com.jogamp.common.util.IntObjectHashMap;
import com.jogamp.common.util.ReflectionUtil;

public class AndroidVersion {
    public static final boolean isAvailable;

    /** The name of the instruction set (CPU type + ABI convention) of native code. API-4. All lower case.*/
    public static final String CPU_ABI;
    public static final CPUType CPU_TYPE;
    public static final ABIType ABI_TYPE;

    /** The name of the second instruction set (CPU type + ABI convention) of native code. API-8. All lower case.*/
    public static final String CPU_ABI2;
    public static final CPUType CPU_TYPE2;
    public static final ABIType ABI_TYPE2;

    /** Development codename, or the string "REL" for official release */
    public static final String CODENAME;

    /** internal build value used by the underlying source control. */
    public static final String INCREMENTAL;

    /** official build version string */
    public static final String RELEASE;

    /** SDK Version number, key to VERSION_CODES */
    public static final int SDK_INT;

    /** SDK Version string */
    public static final String SDK_NAME;

    private static final String androidBuild = "android.os.Build";
    private static final String androidBuildVersion = "android.os.Build$VERSION";
    private static final String androidBuildVersionCodes = "android.os.Build$VERSION_CODES";

    static {
        final ClassLoader cl = AndroidVersion.class.getClassLoader();
        Class<?> abClass = null;
        Object abObject= null;
        Class<?> abvClass = null;
        Object abvObject= null;
        Class<?> abvcClass = null;
        Object abvcObject= null;
        try {
            abClass = ReflectionUtil.getClass(androidBuild, true, cl);
            abObject = abClass.newInstance();
            abvClass = ReflectionUtil.getClass(androidBuildVersion, true, cl);
            abvObject = abvClass.newInstance();
            abvcClass = ReflectionUtil.getClass(androidBuildVersionCodes, true, cl);
            abvcObject = abvcClass.newInstance();
        } catch (final Exception e) { /* n/a */ }
        isAvailable = null != abObject && null != abvObject;
        if(isAvailable) {
            CPU_ABI = getString(abClass, abObject, "CPU_ABI", true);
            CPU_ABI2 = getString(abClass, abObject, "CPU_ABI2", true);
            CODENAME = getString(abvClass, abvObject, "CODENAME", false);
            INCREMENTAL = getString(abvClass, abvObject, "INCREMENTAL", false);
            RELEASE = getString(abvClass, abvObject, "RELEASE", false);
            SDK_INT = getInt(abvClass, abvObject, "SDK_INT");
            final String sdk_name;
            if( null != abvcObject ) {
                final IntObjectHashMap version_codes = getVersionCodes(abvcClass, abvcObject);
                sdk_name = (String) version_codes.get(SDK_INT);
            } else {
                sdk_name = null;
            }
            SDK_NAME = ( null != sdk_name ) ? sdk_name : "SDK_"+SDK_INT ;

            /**
             * <p>
             * FIXME: Where is a comprehensive list of known 'android.os.Build.CPU_ABI' and 'android.os.Build.CPU_ABI2' strings ?<br/>
             * Fount this one: <code>http://www.kandroid.org/ndk/docs/CPU-ARCH-ABIS.html</code>
             * <pre>
             *  lib/armeabi/libfoo.so
             *  lib/armeabi-v7a/libfoo.so
             *  lib/x86/libfoo.so
             *  lib/mips/libfoo.so
             * </pre>
             * </p>
             */
            CPU_TYPE = Platform.CPUType.query(CPU_ABI);
            ABI_TYPE = Platform.ABIType.query(CPU_TYPE, CPU_ABI);
            if( null != CPU_ABI2 && CPU_ABI2.length() > 0 ) {
                CPU_TYPE2 = Platform.CPUType.query(CPU_ABI2);
                ABI_TYPE2 = Platform.ABIType.query(CPU_TYPE2, CPU_ABI2);
            } else {
                CPU_TYPE2 = null;
                ABI_TYPE2 = null;
            }
        } else {
            CPU_ABI = null;
            CPU_ABI2 = null;
            CODENAME = null;
            INCREMENTAL = null;
            RELEASE = null;
            SDK_INT = -1;
            SDK_NAME = null;
            CPU_TYPE = null;
            ABI_TYPE = null;
            CPU_TYPE2 = null;
            ABI_TYPE2 = null;
        }
    }

    private static final IntObjectHashMap getVersionCodes(final Class<?> cls, final Object obj) {
        final Field[] fields = cls.getFields();
        final IntObjectHashMap map = new IntObjectHashMap( 3 * fields.length / 2, 0.75f );
        for(int i=0; i<fields.length; i++) {
            try {
                final int version = fields[i].getInt(obj);
                final String version_name = fields[i].getName();
                // System.err.println(i+": "+version+": "+version_name);
                map.put(new Integer(version), version_name);
            } catch (final Exception e) { e.printStackTrace(); /* n/a */ }
        }
        return map;
    }

    private static final String getString(final Class<?> cls, final Object obj, final String name, final boolean lowerCase) {
        try {
            final Field f = cls.getField(name);
            final String s = (String) f.get(obj);
            if( lowerCase && null != s ) {
                return s.toLowerCase();
            } else {
                return s;
            }
        } catch (final Exception e) { e.printStackTrace(); /* n/a */ }
        return null;
    }

    private static final int getInt(final Class<?> cls, final Object obj, final String name) {
        try {
            final Field f = cls.getField(name);
            return f.getInt(obj);
        } catch (final Exception e) { e.printStackTrace(); /* n/a */ }
        return -1;
    }

    // android.os.Build.VERSION
}
