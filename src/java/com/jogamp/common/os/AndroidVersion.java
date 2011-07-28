package com.jogamp.common.os;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.jogamp.common.util.ReflectionUtil;

public class AndroidVersion {
    public static final boolean isAvailable;
    
    /** All SDK version map, where SDK_INT is the key to the current running version */
    public static final Map<Integer, String> VERSION_CODES; 
    
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
    
    static final String androidBuildVersion = "android.os.Build$VERSION";
    static final String androidBuildVersionCodes = "android.os.Build$VERSION_CODES";
    
    static {
        final ClassLoader cl = AndroidVersion.class.getClassLoader();
        Class abvClass = null;
        Object abvObject= null;
        Class abvcClass = null;
        Object abvcObject= null;
        try {
            abvClass = ReflectionUtil.getClass(androidBuildVersion, true, cl);
            abvObject = abvClass.newInstance();
            abvcClass = ReflectionUtil.getClass(androidBuildVersionCodes, true, cl);
            abvcObject = abvcClass.newInstance();
        } catch (Exception e) { /* n/a */ } 
        isAvailable = null != abvObject ;
        if(isAvailable) {
            CODENAME = getString(abvClass, abvObject, "CODENAME");
            INCREMENTAL = getString(abvClass, abvObject, "INCREMENTAL");
            RELEASE = getString(abvClass, abvObject, "RELEASE");
            SDK_INT = getInt(abvClass, abvObject, "SDK_INT");
            VERSION_CODES = getVersionCodes(abvcClass, abvcObject);
            String sdk_name = VERSION_CODES.get(new Integer(SDK_INT));
            SDK_NAME = ( null != sdk_name ) ? sdk_name : "SDK_"+SDK_INT ; 
        } else {
            CODENAME = null;
            INCREMENTAL = null;
            RELEASE = null;
            SDK_INT = -1;  
            VERSION_CODES = new HashMap<Integer, String>();
            SDK_NAME = null;
        }
    }
    
    private static final Map<Integer, String> getVersionCodes(Class cls, Object obj) {
        HashMap<Integer, String> map = new HashMap<Integer, String>();
        Field[] fields = cls.getFields();
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
    
    private static final String getString(Class cls, Object obj, String name) {
        try {
            Field f = cls.getField(name);
            return (String) f.get(obj);
        } catch (Exception e) { e.printStackTrace(); /* n/a */ }
        return null;
    }

    private static final int getInt(Class cls, Object obj, String name) {
        try {
            Field f = cls.getField(name);
            return f.getInt(obj);
        } catch (Exception e) { e.printStackTrace(); /* n/a */ }
        return -1;
    }
    
    // android.os.Build.VERSION
}
