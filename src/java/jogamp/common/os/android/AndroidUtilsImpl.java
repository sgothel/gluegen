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
package jogamp.common.os.android;

import java.io.File;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class AndroidUtilsImpl {
   private static boolean DEBUG = false;

   public static final PackageInfo getPackageInfo(final String packageName) {
       return getPackageInfo(StaticContext.getContext(), packageName);
   }

   public static final PackageInfo getPackageInfo(final Context ctx, final String packageName) {
       if(null != ctx) {
           try {
               final PackageInfo pi = ctx.getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
               if(DEBUG) Log.d(MD.TAG, "getPackageInfo("+packageName+"): "+pi);
               return pi;
           } catch (final Exception e) { if(DEBUG) { Log.d(MD.TAG, "getPackageInfo("+packageName+")", e); } }
       }
       if(DEBUG) Log.d(MD.TAG, "getPackageInfo("+packageName+"): NULL");
       return null;
   }

   public static final int getPackageInfoVersionCode(final String packageName) {
       final PackageInfo pInfo = getPackageInfo(packageName);
       return ( null != pInfo ) ? pInfo.versionCode : -1 ;
   }

   public static final String getPackageInfoVersionName(final String packageName) {
       final PackageInfo pInfo = getPackageInfo(packageName);
       final String s = ( null != pInfo ) ? pInfo.versionName : null ;
       if(DEBUG) Log.d(MD.TAG, "getPackageInfoVersionName("+packageName+"): "+s);
       return s;
   }

   /**
    * @return null if no Android Context is registered
    *         via {@link jogamp.common.os.android.StaticContext#init(android.content.Context) StaticContext.init(..)},
    *         otherwise the context relative world readable <code>temp</code> directory returned.
    */
   public static File getTempRoot()
        throws SecurityException, RuntimeException
   {
       final Context ctx = StaticContext.getContext();
       if(null != ctx) {
           final File tmpRoot = ctx.getDir("temp", Context.MODE_WORLD_READABLE);
           if(null==tmpRoot|| !tmpRoot.isDirectory() || !tmpRoot.canWrite()) {
               throw new RuntimeException("Not a writable directory: '"+tmpRoot+"', retrieved Android static context");
           }
           if(DEBUG) {
               System.err.println("IOUtil.getTempRoot(Android): temp dir: "+tmpRoot.getAbsolutePath());
           }
           return tmpRoot;
       }
       return null;
   }

}
