/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

import java.util.*;

import com.jogamp.common.jvm.JNILibLoaderBase;

/**
 * Provides bundling of:<br>
 * <ul>
 * <li>The to-be-glued native library, eg OpenGL32.dll. From hereon this is referred as the Tool.</li>
 * <li>The JNI glue-code native library, eg jogl_desktop.dll. From heron this is referred as the Glue</li>
 * </ul><br>
 * An instance provides a complete {@link com.jogamp.common.os.DynamicLookupHelper}
 * to {@link com.jogamp.gluegen.runtime.ProcAddressTable#reset(com.jogamp.common.os.DynamicLookupHelper) reset}
 * the {@link com.jogamp.gluegen.runtime.ProcAddressTable}.<br>
 * At construction, it:
 * <ul>
 *  <li> loads the Tool native library via
 *       {@link com.jogamp.common.os.NativeLibrary#open(java.lang.String, java.lang.ClassLoader, boolean) NativeLibrary's open method}</li>
 *  <li> loads the {@link com.jogamp.common.jvm.JNILibLoaderBase#loadLibrary(java.lang.String, java.lang.String[], boolean)  Glue native library}</li>
 *  <li> resolves the Tool's {@link com.jogamp.common.os.DynamicLibraryBundleInfo#getToolGetProcAddressFuncNameList() GetProcAddress}. (optional)</li>
 * </ul>
 */
public class DynamicLibraryBundle implements DynamicLookupHelper {
    protected static final boolean DEBUG = NativeLibrary.DEBUG;
    protected static final boolean DEBUG_LOOKUP = NativeLibrary.DEBUG_LOOKUP;

    private DynamicLibraryBundleInfo info;

    private List/*<List<String>>*/ toolLibNames;
    private boolean[] toolLibLoaded;
    private int toolLibLoadedNumber;
    protected List/*<NativeLibrary>*/ nativeLibraries;

    private List/*<String>*/ glueLibNames;
    private boolean[] glueLibLoaded;
    private int glueLibLoadedNumber;

    private long toolGetProcAddressHandle;
    private HashSet toolGetProcAddressFuncNameSet;
    private List toolGetProcAddressFuncNameList;

    public DynamicLibraryBundle(DynamicLibraryBundleInfo info) {
        if(null==info) {
            throw new RuntimeException("Null DynamicLibraryBundleInfo");
        }
        this.info = info;
        if(DEBUG) {
            System.out.println("DynamicLibraryBundle.init start with: "+info.getClass().getName());
        }
        nativeLibraries = new ArrayList();
        toolLibNames = info.getToolLibNames();
        glueLibNames = info.getGlueLibNames();
        loadLibraries();
        toolGetProcAddressFuncNameList = info.getToolGetProcAddressFuncNameList();
        if(null!=toolGetProcAddressFuncNameList) {
            toolGetProcAddressFuncNameSet = new HashSet(toolGetProcAddressFuncNameList);
            toolGetProcAddressHandle = getToolGetProcAddressHandle();
        } else {
            toolGetProcAddressFuncNameSet = new HashSet();
            toolGetProcAddressHandle = 0;
        }
        if(DEBUG) {
            System.out.println("DynamicLibraryBundle.init Summary: "+info.getClass().getName());
            System.out.println("     toolGetProcAddressFuncNameList: "+toolGetProcAddressFuncNameList);
            System.out.println("     Tool Lib Names : "+toolLibNames);
            System.out.println("     Tool Lib Loaded: "+getToolLibLoadedNumber()+"/"+getToolLibNumber()+", complete "+isToolLibComplete());
            System.out.println("     Glue Lib Names : "+glueLibNames);
            System.out.println("     Glue Lib Loaded: "+getGlueLibLoadedNumber()+"/"+getGlueLibNumber()+", complete "+isGlueLibComplete());
            System.out.println("     All Complete: "+isLibComplete());
        }
    }

    public final boolean isLibComplete() {
        return isToolLibComplete() && isGlueLibComplete() ;
    }

    public final int getToolLibNumber() {
        return toolLibNames.size();
    }

    public final int getToolLibLoadedNumber() {
        return toolLibLoadedNumber;
    }

    public final boolean isToolLibComplete() {
        return getToolLibNumber() == getToolLibLoadedNumber();
    }

    public final boolean isToolLibLoaded() {
        return 0 < toolLibLoadedNumber;
    }

    public final boolean isToolLibLoaded(int i) {
        if(0 <= i && i < toolLibLoaded.length) {
            return toolLibLoaded[i];
        }
        return false;
    }

    public final int getGlueLibNumber() {
        return glueLibNames.size();
    }

    public final int getGlueLibLoadedNumber() {
        return glueLibLoadedNumber;
    }

    public final boolean isGlueLibComplete() {
        return getGlueLibNumber() == getGlueLibLoadedNumber();
    }

    public final boolean isGlueLibLoaded(int i) {
        if(0 <= i && i < glueLibLoaded.length) {
            return glueLibLoaded[i];
        }
        return false;
    }

    public final DynamicLibraryBundleInfo getBundleInfo() { return info; }

    protected long getToolGetProcAddressHandle() {
        if(!isToolLibLoaded()) {
            return 0;
        }
        long aptr = 0;
        for(Iterator iter=toolGetProcAddressFuncNameList.iterator(); 0==aptr && iter.hasNext(); ) {
            String name = (String) iter.next();
            aptr = dynamicLookupFunctionOnLibs(name);
            if(DEBUG) {
                System.out.println("getToolGetProcAddressHandle: "+name+" -> 0x"+Long.toHexString(aptr));
            }
        }
        return aptr;
    }

    protected NativeLibrary loadFirstAvailable(List/*<String>*/ libNames, ClassLoader loader, boolean global) {
        for (Iterator iter = libNames.iterator(); iter.hasNext(); ) {
            NativeLibrary lib = NativeLibrary.open((String) iter.next(), loader, global);
            if (lib != null) {
                return lib;
            }
        }
        return null;
    }

    private void loadLibraries() {
        if( null == toolLibNames || toolLibNames.size() == 0) {
            if(DEBUG) {
                System.out.println("No Tool native library names given");
            }
            return;
        }

        if( null == glueLibNames || glueLibNames.size() == 0 ) {
            if(DEBUG) {
                System.out.println("No Glue native library names given");
            }
            return;
        }

        toolLibLoadedNumber = 0;
        int i;
        toolLibLoaded = new boolean[toolLibNames.size()];
        for(i=0; i<toolLibNames.size(); i++) {
            toolLibLoaded[i] = false;
        }

        glueLibLoaded = new boolean[glueLibNames.size()];
        for(i=0; i<glueLibNames.size(); i++) {
            glueLibLoaded[i] = false;
        }

        ClassLoader loader = getClass().getClassLoader();
        NativeLibrary lib = null;

        i=0;
        for(Iterator iter = toolLibNames.iterator(); iter.hasNext(); i++) {
            Object listObj = iter.next();
            List/*<String>*/ libNames = null;
            if(listObj instanceof List) {
                libNames = (List) listObj;
            } else if(listObj instanceof String) {
                libNames = new ArrayList();
                libNames.add((String)listObj);
            } else {
                throw new RuntimeException("List element "+i+" must be either a List or String: "+toolLibNames);
            }
            if( null != libNames && libNames.size() > 0 ) {
                lib = loadFirstAvailable(libNames, loader, info.shallLinkGlobal());
                if ( null == lib ) {
                    if(DEBUG) {
                        System.out.println("Unable to load any Tool library of: "+libNames);
                    }
                } else {
                    nativeLibraries.add(lib);
                    toolLibLoaded[i]=true;
                    toolLibLoadedNumber++;
                    if(DEBUG) {
                        System.out.println("Loaded Tool library: "+lib);
                    }
                }
            }
        }
        if( !isToolLibLoaded() ) {
            if(DEBUG) {
                System.out.println("No Tool libraries loaded");
            }
            return;
        }

        glueLibLoadedNumber = 0;
        i=0;
        for(Iterator iter = glueLibNames.iterator(); iter.hasNext(); i++) {
            String libName = (String) iter.next();
            boolean ignoreError = true;
            boolean res;
            try {
                res = GlueJNILibLoaderBase.loadLibrary(libName, ignoreError);
                if(DEBUG && !res) {
                    System.out.println("Info: Could not load JNI/Glue library: "+libName);
                }
            } catch (UnsatisfiedLinkError e) {
                res = false;
                if(DEBUG) {
                    System.out.println("Unable to load JNI/Glue library: "+libName);
                    e.printStackTrace();
                }
            }
            glueLibLoaded[i] = res;
            if(res) {
                glueLibLoadedNumber++;
            }
        }
    }

    private long dynamicLookupFunctionOnLibs(String funcName) {
        if(!isToolLibLoaded() || null==funcName) {
            if(DEBUG_LOOKUP && !isToolLibLoaded()) {
                System.err.println("Lookup-Native: <" + funcName + "> ** FAILED ** Tool native library not loaded");
            }
            return 0;
        }
        long addr = 0;
        NativeLibrary lib = null;

        if(info.shallLookupGlobal()) {
            // Try a global symbol lookup first ..
            addr = NativeLibrary.dynamicLookupFunctionGlobal(funcName);
        }
        // Look up this function name in all known libraries
        for (Iterator iter = nativeLibraries.iterator(); 0==addr && iter.hasNext(); ) {
            lib = (NativeLibrary) iter.next();
            addr = lib.dynamicLookupFunction(funcName);
        }
        if(DEBUG_LOOKUP) {
            String libName = ( null == lib ) ? "GLOBAL" : lib.toString();
            if(0!=addr) {
                System.err.println("Lookup-Native: <" + funcName + "> 0x" + Long.toHexString(addr) + " in lib " + libName );
            } else {
                System.err.println("Lookup-Native: <" + funcName + "> ** FAILED ** in libs " + nativeLibraries);
            }
        }
        return addr;
    }

    public long dynamicLookupFunction(String funcName) {
        if(!isToolLibLoaded() || null==funcName) {
            if(DEBUG_LOOKUP && !isToolLibLoaded()) {
                System.err.println("Lookup: <" + funcName + "> ** FAILED ** Tool native library not loaded");
            }
            return 0;
        }

        if(toolGetProcAddressFuncNameSet.contains(funcName)) {
            return toolGetProcAddressHandle;
        }

        long addr = 0;

        if(0 != toolGetProcAddressHandle) {
            addr = info.toolDynamicLookupFunction(toolGetProcAddressHandle, funcName);
            if(DEBUG_LOOKUP) {
                if(0!=addr) {
                    System.err.println("Lookup-Tool: <"+funcName+"> 0x"+Long.toHexString(addr));
                }
            }
        }
        if(0==addr) {
            addr = dynamicLookupFunctionOnLibs(funcName);
        }
        return addr;
    }

    /** Inherit access */
    static class GlueJNILibLoaderBase extends JNILibLoaderBase {
      protected static synchronized boolean loadLibrary(String libname, boolean ignoreError) {
        return JNILibLoaderBase.loadLibrary(libname, ignoreError);
      }
    }
}

