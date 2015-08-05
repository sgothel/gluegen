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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.jogamp.common.jvm.JNILibLoaderBase;
import com.jogamp.common.util.RunnableExecutor;

/**
 * Provides bundling of:<br>
 * <ul>
 * <li>The to-be-glued native library, eg OpenGL32.dll. From here on this is referred as the Tool.</li>
 * <li>The JNI glue-code native library, eg jogl_desktop.dll. From here on this is referred as the Glue</li>
 * </ul>
 * <p>
 * An {@link DynamicLibraryBundleInfo} instance is being passed in the constructor,
 * providing the required information about the tool and glue libraries.
 * The ClassLoader of it's implementation is also being used to help locating the native libraries.
 * </p>
 * An instance provides a complete {@link com.jogamp.common.os.DynamicLookupHelper}
 * to {@link com.jogamp.gluegen.runtime.ProcAddressTable#reset(com.jogamp.common.os.DynamicLookupHelper) reset}
 * the {@link com.jogamp.gluegen.runtime.ProcAddressTable}.<br>
 * At construction, it:
 * <ul>
 *  <li> loads the Tool native library via
 *       {@link com.jogamp.common.os.NativeLibrary#open(java.lang.String, java.lang.ClassLoader, boolean) NativeLibrary's open method}</li>
 *  <li> loads the {@link com.jogamp.common.jvm.JNILibLoaderBase#loadLibrary(java.lang.String, java.lang.String[], boolean, ClassLoader)  Glue native library}</li>
 *  <li> resolves the Tool's {@link com.jogamp.common.os.DynamicLibraryBundleInfo#getToolGetProcAddressFuncNameList() GetProcAddress}. (optional)</li>
 * </ul>
 */
public class DynamicLibraryBundle implements DynamicLookupHelper {
    private final DynamicLibraryBundleInfo info;

    protected final List<NativeLibrary> nativeLibraries;
    private final DynamicLinker dynLinkGlobal;
    private final List<List<String>> toolLibNames;
    private final List<String> glueLibNames;
    private final boolean[] toolLibLoaded;

    private int toolLibLoadedNumber;

    private final boolean[] glueLibLoaded;
    private int glueLibLoadedNumber;

    private long toolGetProcAddressHandle;
    private boolean toolGetProcAddressComplete;
    private HashSet<String> toolGetProcAddressFuncNameSet;
    private final List<String> toolGetProcAddressFuncNameList;

    /** Returns an AWT-EDT {@link RunnableExecutor} implementation if AWT is available, otherwise {@link RunnableExecutor#currentThreadExecutor}. */
    public static RunnableExecutor getDefaultRunnableExecutor() {
        return RunnableExecutor.currentThreadExecutor;
    }

    /**
     * Instantiates and loads all {@link NativeLibrary}s incl. JNI libraries.
     * <p>
     * The ClassLoader of the {@link DynamicLibraryBundleInfo} implementation class
     * is being used to help locating the native libraries.
     * </p>
     */
    public DynamicLibraryBundle(final DynamicLibraryBundleInfo info) {
        if(null==info) {
            throw new RuntimeException("Null DynamicLibraryBundleInfo");
        }
        this.info = info;
        if(DEBUG) {
            System.err.println(Thread.currentThread().getName()+" - DynamicLibraryBundle.init start with: "+info.getClass().getName());
        }
        nativeLibraries = new ArrayList<NativeLibrary>();
        toolLibNames = info.getToolLibNames();
        glueLibNames = info.getGlueLibNames();
        toolLibLoaded = new boolean[toolLibNames.size()];
        if(DEBUG) {
            if( toolLibNames.size() == 0 ) {
                System.err.println("No Tool native library names given");
            }

            if( glueLibNames.size() == 0 ) {
                System.err.println("No Glue native library names given");
            }
        }

        for(int i=toolLibNames.size()-1; i>=0; i--) {
            toolLibLoaded[i] = false;
        }
        glueLibLoaded = new boolean[glueLibNames.size()];
        for(int i=glueLibNames.size()-1; i>=0; i--) {
            glueLibLoaded[i] = false;
        }

        {
            final DynamicLinker[] _dynLinkGlobal = { null };
            info.getLibLoaderExecutor().invoke(true, new Runnable() {
                    @Override
                    public void run() {
                        _dynLinkGlobal[0] = loadLibraries();
                    } } ) ;
            dynLinkGlobal = _dynLinkGlobal[0];
        }

        toolGetProcAddressFuncNameList = info.getToolGetProcAddressFuncNameList();
        if( null != toolGetProcAddressFuncNameList ) {
            toolGetProcAddressFuncNameSet = new HashSet<String>(toolGetProcAddressFuncNameList);
            toolGetProcAddressHandle = getToolGetProcAddressHandle();
            toolGetProcAddressComplete = 0 != toolGetProcAddressHandle;
        } else {
            toolGetProcAddressFuncNameSet = new HashSet<String>();
            toolGetProcAddressHandle = 0;
            toolGetProcAddressComplete = true;
        }
        if(DEBUG) {
            System.err.println("DynamicLibraryBundle.init Summary: "+info.getClass().getName());
            System.err.println("     toolGetProcAddressFuncNameList: "+toolGetProcAddressFuncNameList+", complete: "+toolGetProcAddressComplete+", 0x"+Long.toHexString(toolGetProcAddressHandle));
            System.err.println("     Tool Lib Names : "+toolLibNames);
            System.err.println("     Tool Lib Loaded: "+getToolLibLoadedNumber()+"/"+getToolLibNumber()+" "+Arrays.toString(toolLibLoaded)+", complete "+isToolLibComplete());
            System.err.println("     Glue Lib Names : "+glueLibNames);
            System.err.println("     Glue Lib Loaded: "+getGlueLibLoadedNumber()+"/"+getGlueLibNumber()+" "+Arrays.toString(glueLibLoaded)+", complete "+isGlueLibComplete());
            System.err.println("     All Complete: "+isLibComplete());
            System.err.println("     LibLoaderExecutor: "+info.getLibLoaderExecutor().getClass().getName());
        }
    }

    /** Unload all {@link NativeLibrary}s, and remove all references. */
    public final void destroy() {
        if(DEBUG) {
            System.err.println(Thread.currentThread().getName()+" - DynamicLibraryBundle.destroy() START: "+info.getClass().getName());
        }
        toolGetProcAddressFuncNameSet = null;
        toolGetProcAddressHandle = 0;
        toolGetProcAddressComplete = false;
        for(int i = 0; i<nativeLibraries.size(); i++) {
            nativeLibraries.get(i).close();
        }
        nativeLibraries.clear();
        toolLibNames.clear();
        glueLibNames.clear();
        if(DEBUG) {
            System.err.println(Thread.currentThread().getName()+" - DynamicLibraryBundle.destroy() END: "+info.getClass().getName());
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

    /**
     * @return true if all tool libraries are loaded,
     *         otherwise false.
     *
     * @see DynamicLibraryBundleInfo#getToolLibNames()
     */
    public final boolean isToolLibComplete() {
        final int toolLibNumber = getToolLibNumber();
        return toolGetProcAddressComplete &&
               ( 0 == toolLibNumber || null != dynLinkGlobal ) &&
               toolLibNumber == getToolLibLoadedNumber();
    }

    public final boolean isToolLibLoaded() {
        return 0 < toolLibLoadedNumber;
    }

    public final boolean isToolLibLoaded(final int i) {
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

    /**
     * @return true if the last entry has been loaded,
     *         while ignoring the preload dependencies.
     *         Otherwise false.
     *
     * @see DynamicLibraryBundleInfo#getGlueLibNames()
     */
    public final boolean isGlueLibComplete() {
        return 0 == getGlueLibNumber() || isGlueLibLoaded(getGlueLibNumber() - 1);
    }

    public final boolean isGlueLibLoaded(final int i) {
        if(0 <= i && i < glueLibLoaded.length) {
            return glueLibLoaded[i];
        }
        return false;
    }

    public final DynamicLibraryBundleInfo getBundleInfo() { return info; }

    protected final long getToolGetProcAddressHandle() throws SecurityException {
        if(!isToolLibLoaded()) {
            return 0;
        }
        long aptr = 0;
        for (int i=0; i < toolGetProcAddressFuncNameList.size(); i++) {
            final String name = toolGetProcAddressFuncNameList.get(i);
            aptr = dynamicLookupFunctionOnLibs(name);
            if(DEBUG) {
                System.err.println("getToolGetProcAddressHandle: "+name+" -> 0x"+Long.toHexString(aptr));
            }
        }
        return aptr;
    }

    protected static final NativeLibrary loadFirstAvailable(final List<String> libNames, final ClassLoader loader, final boolean global) throws SecurityException {
        for (int i=0; i < libNames.size(); i++) {
            final NativeLibrary lib = NativeLibrary.open(libNames.get(i), loader, global);
            if (lib != null) {
                return lib;
            }
        }
        return null;
    }

    final DynamicLinker loadLibraries() throws SecurityException {
        int i;
        toolLibLoadedNumber = 0;
        final ClassLoader cl = info.getClass().getClassLoader();
        NativeLibrary lib = null;
        DynamicLinker dynLinkGlobal = null;

        for (i=0; i < toolLibNames.size(); i++) {
            final List<String> libNames = toolLibNames.get(i);
            if( null != libNames && libNames.size() > 0 ) {
                lib = loadFirstAvailable(libNames, cl, info.shallLinkGlobal());
                if ( null == lib ) {
                    if(DEBUG) {
                        System.err.println("Unable to load any Tool library of: "+libNames);
                    }
                } else {
                    if( null == dynLinkGlobal ) {
                        dynLinkGlobal = lib.getDynamicLinker();
                    }
                    nativeLibraries.add(lib);
                    toolLibLoaded[i]=true;
                    toolLibLoadedNumber++;
                    if(DEBUG) {
                        System.err.println("Loaded Tool library: "+lib);
                    }
                }
            }
        }
        if( toolLibNames.size() > 0 && !isToolLibLoaded() ) {
            if(DEBUG) {
                System.err.println("No Tool libraries loaded");
            }
            return dynLinkGlobal;
        }

        glueLibLoadedNumber = 0;
        for (i=0; i < glueLibNames.size(); i++) {
            final String libName = glueLibNames.get(i);
            final boolean ignoreError = true;
            boolean res;
            try {
                res = GlueJNILibLoader.loadLibrary(libName, ignoreError, cl);
                if(DEBUG && !res) {
                    System.err.println("Info: Could not load JNI/Glue library: "+libName);
                }
            } catch (final UnsatisfiedLinkError e) {
                res = false;
                if(DEBUG) {
                    System.err.println("Unable to load JNI/Glue library: "+libName);
                    e.printStackTrace();
                }
            }
            glueLibLoaded[i] = res;
            if(res) {
                glueLibLoadedNumber++;
            }
        }

        return dynLinkGlobal;
    }

    /**
     * @param funcName
     * @return
     * @throws SecurityException if user is not granted access for the library set.
     */
    private final long dynamicLookupFunctionOnLibs(final String funcName) throws SecurityException {
        if(!isToolLibLoaded() || null==funcName) {
            if(DEBUG_LOOKUP && !isToolLibLoaded()) {
                System.err.println("Lookup-Native: <" + funcName + "> ** FAILED ** Tool native library not loaded");
            }
            return 0;
        }
        long addr = 0;
        NativeLibrary lib = null;

        if( info.shallLookupGlobal() ) {
            // Try a global symbol lookup first ..
            // addr = NativeLibrary.dynamicLookupFunctionGlobal(funcName);
            addr = dynLinkGlobal.lookupSymbolGlobal(funcName);
        }
        // Look up this function name in all known libraries
        for (int i=0; 0==addr && i < nativeLibraries.size(); i++) {
            lib = nativeLibraries.get(i);
            addr = lib.dynamicLookupFunction(funcName);
        }
        if(DEBUG_LOOKUP) {
            final String libName = ( null == lib ) ? "GLOBAL" : lib.toString();
            if(0!=addr) {
                System.err.println("Lookup-Native: <" + funcName + "> 0x" + Long.toHexString(addr) + " in lib " + libName );
            } else {
                System.err.println("Lookup-Native: <" + funcName + "> ** FAILED ** in libs " + nativeLibraries);
            }
        }
        return addr;
    }

    private final long toolDynamicLookupFunction(final String funcName) {
        if(0 != toolGetProcAddressHandle) {
            final long addr = info.toolGetProcAddress(toolGetProcAddressHandle, funcName);
            if(DEBUG_LOOKUP) {
                if(0!=addr) {
                    System.err.println("Lookup-Tool: <"+funcName+"> 0x"+Long.toHexString(addr)+", via tool 0x"+Long.toHexString(toolGetProcAddressHandle));
                }
            }
            return addr;
        }
        return 0;
    }

    @Override
    public final void claimAllLinkPermission() throws SecurityException {
        for (int i=0; i < nativeLibraries.size(); i++) {
            nativeLibraries.get(i).claimAllLinkPermission();
        }
    }
    @Override
    public final void releaseAllLinkPermission() throws SecurityException {
        for (int i=0; i < nativeLibraries.size(); i++) {
            nativeLibraries.get(i).releaseAllLinkPermission();
        }
    }

    @Override
    public final long dynamicLookupFunction(final String funcName) throws SecurityException {
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
        final boolean useToolGetProcAdressFirst = info.useToolGetProcAdressFirst(funcName);

        if(useToolGetProcAdressFirst) {
            addr = toolDynamicLookupFunction(funcName);
        }
        if(0==addr) {
            addr = dynamicLookupFunctionOnLibs(funcName);
        }
        if(0==addr && !useToolGetProcAdressFirst) {
            addr = toolDynamicLookupFunction(funcName);
        }
        return addr;
    }

    @Override
    public final boolean isFunctionAvailable(final String funcName) throws SecurityException {
        return 0 != dynamicLookupFunction(funcName);
    }

    /** Inherit access */
    static final class GlueJNILibLoader extends JNILibLoaderBase {
      protected static synchronized boolean loadLibrary(final String libname, final boolean ignoreError, final ClassLoader cl) {
        return JNILibLoaderBase.loadLibrary(libname, ignoreError, cl);
      }
    }
}

