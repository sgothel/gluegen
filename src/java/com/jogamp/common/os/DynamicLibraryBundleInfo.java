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

import java.util.List;

import com.jogamp.common.util.RunnableExecutor;


public interface DynamicLibraryBundleInfo {
    public static final boolean DEBUG = DynamicLibraryBundle.DEBUG;

    /**
     * If a {@link SecurityManager} is installed, user needs link permissions
     * for the named libraries.
     *
     * @return a list of Tool library names or alternative library name lists.<br>
     * <ul>
     * <li>GL/GLU example Unix:   [ [ "libGL.so.1", "libGL.so", "GL" ], [ "libGLU.so", "GLU" ] ] </li>
     * <li>GL/GLU example Windows: [ "OpenGL32", "GLU32" ] </li>
     * <li>Cg/CgGL example: [ [ "libCg.so", "Cg" ], [ "libCgGL.so", "CgGL" ] ] </li>
     * </pre>
     */
    public List<List<String>> getToolLibNames();

    /**
     * If a {@link SecurityManager} is installed, user needs link permissions
     * for the named libraries.
     *
     * @return a list of Glue library names.<br>
     * <ul>
     * <li>GL:   [ "nativewindow_x11", "jogl_gl2es12", "jogl_desktop" ] </li>
     * <li>NEWT: [ "nativewindow_x11", "newt" ] </li>
     * <li>Cg:   [ "nativewindow_x11", "jogl_cg" ] </li>
     * </ul><br>
     * Only the last entry is crucial, ie all other are optional preload dependencies and may generate errors,
     * which are ignored.
     */
    public List<String> getGlueLibNames();

    /**
     * May return the native libraries <pre>GetProcAddressFunc</pre> names, the first found function is being used.<br>
     * This could be eg: <pre> glXGetProcAddressARB, glXGetProcAddressARB </pre>.<br>
     * If your Tool does not has this facility, just return null.
     * @see #toolGetProcAddress(long, String)
     */
    public List<String> getToolGetProcAddressFuncNameList() ;

    /**
     * May implement the lookup function using the Tools facility.<br>
     * The actual function pointer is provided to allow proper bootstrapping of the ProcAddressTable,
     * using one of the provided function names by {@link #getToolGetProcAddressFuncNameList()}.<br>
     */
    public long toolGetProcAddress(long toolGetProcAddressHandle, String funcName);

    /**
     * @param funcName
     * @return true if {@link #toolGetProcAddress(long, String)} shall be tried before
     *         the system loader for the given function lookup. Otherwise false.
     *         Default is <b>true</b>.
     */
    public boolean useToolGetProcAdressFirst(String funcName);

    /** @return true if the native library symbols shall be made available for symbol resolution of subsequently loaded libraries. */
    public boolean shallLinkGlobal();

    /**
     * If method returns <code>true</code> <i>and</i> if a {@link SecurityManager} is installed, user needs link permissions
     * for <b>all</b> libraries, i.e. for <code>new RuntimePermission("loadLibrary.*");</code>!
     *
     * @return true if the dynamic symbol lookup shall happen system wide, over all loaded libraries.
     * Otherwise only the loaded native libraries are used for lookup, which shall be the default.
     */
    public boolean shallLookupGlobal();

    /**
     * Returns a suitable {@link RunnableExecutor} implementation, which is being used
     * to load the <code>tool</code> and <code>glue</code> native libraries.
     * <p>
     * This allows the generic {@link DynamicLibraryBundle} implementation to
     * load the native libraries on a designated thread.
     * </p>
     * <p>
     * An implementation may return {@link DynamicLibraryBundle#getDefaultRunnableExecutor()}.
     * </p>
     */
    public RunnableExecutor getLibLoaderExecutor();
}


