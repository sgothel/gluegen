/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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
package jogamp.common.os;

import java.io.File;
import java.lang.reflect.Method;

import com.jogamp.common.os.AndroidVersion;
import com.jogamp.common.util.ReflectionUtil;

public class AndroidUtils {

    private static final Method androidGetPackageInfoVersionNameMethod;
    private static final Method androidGetPackageInfoVersionCodeMethod;
    private static final Method androidGetTempRootMethod;

    static {
        if(AndroidVersion.isAvailable) {
            final ClassLoader cl = AndroidUtils.class.getClassLoader();
            final Class<?> androidAndroidUtilsImplClz = ReflectionUtil.getClass("jogamp.common.os.android.AndroidUtilsImpl", true, cl);
            androidGetPackageInfoVersionCodeMethod = ReflectionUtil.getMethod(androidAndroidUtilsImplClz, "getPackageInfoVersionCode", String.class);
            androidGetPackageInfoVersionNameMethod = ReflectionUtil.getMethod(androidAndroidUtilsImplClz, "getPackageInfoVersionName", String.class);
            androidGetTempRootMethod = ReflectionUtil.getMethod(androidAndroidUtilsImplClz, "getTempRoot");
        } else {
            androidGetPackageInfoVersionCodeMethod = null;
            androidGetPackageInfoVersionNameMethod = null;
            androidGetTempRootMethod = null;
        }
    }

    /**
     * @return null if platform is not Android or no Android Context is registered
     *         via {@link jogamp.common.os.android.StaticContext#init(android.content.Context) StaticContext.init(..)},
     *         otherwise the found package version code of <code>packageName</code> is returned.
     */
    public static final int getPackageInfoVersionCode(final String packageName) {
        if(null != androidGetPackageInfoVersionCodeMethod) {
            return ((Integer) ReflectionUtil.callMethod(null, androidGetPackageInfoVersionCodeMethod, packageName)).intValue();
        }
        return -1;
    }

    /**
     * @return null if platform is not Android or no Android Context is registered
     *         via {@link jogamp.common.os.android.StaticContext#init(android.content.Context) StaticContext.init(..)},
     *         otherwise the found package version name of <code>packageName</code> is returned.
     */
    public static final String getPackageInfoVersionName(final String packageName) {
        if(null != androidGetPackageInfoVersionNameMethod) {
            return (String) ReflectionUtil.callMethod(null, androidGetPackageInfoVersionNameMethod, packageName);
        }
        return null;
    }

    /**
     * @return null if platform is not Android or no Android Context is registered
     *         via {@link jogamp.common.os.android.StaticContext#init(android.content.Context) StaticContext.init(..)},
     *         otherwise the context relative world readable <code>temp</code> directory returned.
     */
    public static File getTempRoot()
        throws RuntimeException {
        if(null != androidGetTempRootMethod) {
            return (File) ReflectionUtil.callMethod(null, androidGetTempRootMethod);
        }
        return null;
    }

}
