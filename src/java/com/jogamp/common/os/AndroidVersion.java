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

import com.jogamp.common.util.IntObjectHashMap;
import com.jogamp.common.util.ReflectionUtil;

public class AndroidVersion {
    public static final boolean isAvailable;

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

    private static final String androidBuildVersion = "android.os.Build$VERSION";
    private static final String androidBuildVersionCodes = "android.os.Build$VERSION_CODES";

    static {
        final ClassLoader cl = AndroidVersion.class.getClassLoader();
        Class<?> abvClass = null;
        Object abvObject= null;
        Class<?> abvcClass = null;
        Object abvcObject= null;
        try {
            abvClass = ReflectionUtil.getClass(androidBuildVersion, true, cl);
            abvObject = abvClass.newInstance();
            abvcClass = ReflectionUtil.getClass(androidBuildVersionCodes, true, cl);
            abvcObject = abvcClass.newInstance();
        } catch (Exception e) { /* n/a */ }
        isAvailable = null != abvObject;
        if(isAvailable) {
            CODENAME = getString(abvClass, abvObject, "CODENAME");
            INCREMENTAL = getString(abvClass, abvObject, "INCREMENTAL");
            RELEASE = getString(abvClass, abvObject, "RELEASE");
            SDK_INT = getInt(abvClass, abvObject, "SDK_INT");
            final IntObjectHashMap version_codes = getVersionCodes(abvcClass, abvcObject);
            final String sdk_name = (String) version_codes.get(SDK_INT);
            SDK_NAME = ( null != sdk_name ) ? sdk_name : "SDK_"+SDK_INT ;
        } else {
            CODENAME = null;
            INCREMENTAL = null;
            RELEASE = null;
            SDK_INT = -1;
            SDK_NAME = null;
        }
    }

    private static final IntObjectHashMap getVersionCodes(Class<?> cls, Object obj) {
        final Field[] fields = cls.getFields();
        IntObjectHashMap map = new IntObjectHashMap( 3 * fields.length / 2, 0.75f );
        for(int i=0; i<fields.length; i++) {
            try {
                final int version = fields[i].getInt(obj);
                final String version_name = fields[i].getName();
                // System.err.println(i+": "+version+": "+version_name);
                map.put(new Integer(version), version_name);
            } catch (Exception e) { e.printStackTrace(); /* n/a */ }
        }
        return map;
    }

    private static final String getString(Class<?> cls, Object obj, String name) {
        try {
            Field f = cls.getField(name);
            return (String) f.get(obj);
        } catch (Exception e) { e.printStackTrace(); /* n/a */ }
        return null;
    }

    private static final int getInt(Class<?> cls, Object obj, String name) {
        try {
            Field f = cls.getField(name);
            return f.getInt(obj);
        } catch (Exception e) { e.printStackTrace(); /* n/a */ }
        return -1;
    }

    // android.os.Build.VERSION
}
