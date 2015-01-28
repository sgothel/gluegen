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

package jogamp.android.launcher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class ClassLoaderUtil {
   private static final String TAG = "JogampClassLoader";

   private static HashMap<String, ClassLoader> cachedClassLoader = new HashMap<String, ClassLoader>();

   // location where optimized dex files will be written
   private static final String dexPathName= "jogampDex";
   private static File dexPath = null;
   private static boolean needsAPKCopy;

   private static LauncherTempFileCache tmpFileCache = null;

   private static final String PATH_SEP = "/";
   private static final String ELEM_SEP = ":";

   private static synchronized void init(final Context ctx) {
       if(null == tmpFileCache) {
           if(!LauncherTempFileCache.initSingleton(ctx)) {
               throw new InternalError("TempFileCache initialization error");
           }
           tmpFileCache = new LauncherTempFileCache();
           if(!tmpFileCache.isValid()) {
               throw new InternalError("TempFileCache instantiation error");
           }
           dexPath = new File(tmpFileCache.getTempDir(), dexPathName);
           Log.d(TAG, "jogamp dexPath: " + dexPath.getAbsolutePath());
           dexPath.mkdir();

           needsAPKCopy = android.os.Build.VERSION.SDK_INT >= 21; // >= LOLLIPOP
           Log.d(TAG, "jogamp Android SDK "+android.os.Build.VERSION.SDK_INT+", needsAPKCopy "+needsAPKCopy);
       }
   }

   /**
    * @param ctx
    * @param cachedPackageName list of package names w/ native libraries for which a ClassLoader shall be cached, i.e. persistence.
    *                          This is usually required for native libraries.
    * @param userPackageNames list of user package names w/o native libraries, the last entry shall reflect the Activity.
    * @param userAPKNames optional non-cached user APKs
    * @return
    */
   public static synchronized ClassLoader createClassLoader(final Context ctx, final List<String> cachedPackageName,
                                                            final List<String> userPackageNames, final List<String> userAPKNames) {
       return createClassLoader(ctx, cachedPackageName, userPackageNames, userAPKNames, null);
   }

   /**
    * @param ctx
    * @param cachedPackageNames list of package names w/ native libraries for which a ClassLoader shall be cached, i.e. persistence.
    *                           This is usually required for native libraries.
    * @param userPackageNames list of user package names w/o native libraries, the last entry shall reflect the Activity.
    * @param userAPKNames optional non-cached user APKs
    * @param parent
    * @return
    */
   public static synchronized ClassLoader createClassLoader(final Context ctx, final List<String> cachedPackageNames,
                                                            final List<String> userPackageNames, final List<String> userAPKNames,
                                                            final ClassLoader parent) {
       init(ctx);

       final String cacheKey = cachedPackageNames.toString();
       ClassLoader clCached = cachedClassLoader.get(cacheKey);
       if( null == clCached ) {
           clCached = createClassLoaderImpl(ctx, cachedPackageNames, true, null, (null != parent ) ? parent : ctx.getClassLoader());
           cachedClassLoader.put(cacheKey, clCached);
           Log.d(TAG, "NEW cached-CL: cachedPackageNames: "+cacheKey);
       } else {
           Log.d(TAG, "REUSE cached-CL: cachedPackageNames: "+cacheKey);
       }

       return createClassLoaderImpl(ctx, userPackageNames, false, userAPKNames, clCached);
   }

   /**
    * @param ctx
    * @param packageNames list of package names
    * @param addLibPath
    * @param apkNames
    * @param parent
    * @return
    */
   private static synchronized ClassLoader createClassLoaderImpl(final Context ctx, final List<String> packageNames, final boolean addLibPath,
                                                                 final List<String> apkNames, final ClassLoader parent) {

       final ApplicationInfo appInfoLauncher= ctx.getApplicationInfo();
       final String appDirLauncher = new File(appInfoLauncher.dataDir).getParent();
       final String libSubDef = "lib";
       Log.d(TAG, "S: userPackageNames: "+packageNames+"; Launcher: appDir "+appDirLauncher+", dataDir: "+appInfoLauncher.dataDir+", nativeLibraryDir "+appInfoLauncher.nativeLibraryDir);

       final StringBuilder apks = new StringBuilder();
       final StringBuilder libs = new StringBuilder();
       int apkCount = 0;
       String lastUserPackageName = null; // the very last one reflects the Activity

       if( null != packageNames ) {
           for(final Iterator<String> i=packageNames.iterator(); i.hasNext(); ) {
               lastUserPackageName = i.next();

               String userAPK = null;
               String nativeLibraryDir=null;
               try {
                   final PackageManager pm = ctx.getPackageManager();
                   final ApplicationInfo appInfo = pm.getApplicationInfo(lastUserPackageName, 0);
                   final String appDir = new File(appInfoLauncher.dataDir).getParent();
                   userAPK = appInfo.sourceDir;
                   nativeLibraryDir = appInfo.nativeLibraryDir;
                   Log.d(TAG, "S: userPackage: "+lastUserPackageName+", apk "+userAPK+", appDir "+appDir+", dataDir: "+appInfo.dataDir+", nativeLibraryDir "+nativeLibraryDir);
               } catch (final PackageManager.NameNotFoundException e) {
                   Log.d(TAG, "error: "+e, e);
               }
               if(null != userAPK) {
                   if(apkCount>0) {
                       apks.append(ELEM_SEP);
                       if(addLibPath) {
                           libs.append(ELEM_SEP);
                       }
                   }
                   if( needsAPKCopy ) {
                       final File src = new File(userAPK);
                       userAPK = dexPath + "/" + lastUserPackageName + "-1.apk";
                       final File dst = new File(userAPK);
                       try {
                           copyFile(src, dst);
                       } catch (final IOException e) {
                           Log.d(TAG, "error copying <"+src+"> -> <"+dst+">: "+e, e);
                           return null;
                       }
                       Log.d(TAG, "APK["+apkCount+"] copied: <"+src+"> -> <"+dst+">");
                   }
                   apks.append(userAPK);
                   Log.d(TAG, "APK["+apkCount+"] found: <"+lastUserPackageName+"> -> <"+userAPK+">");
                   Log.d(TAG, "APK["+apkCount+"] apks: <"+apks.toString()+">");
                   if(addLibPath) {
                       if(null != nativeLibraryDir && nativeLibraryDir.length()>0 ) {
                           libs.append(nativeLibraryDir).append(PATH_SEP);
                       } else {
                           libs.append(appDirLauncher).append(PATH_SEP).append(lastUserPackageName).append(PATH_SEP).append(libSubDef).append(PATH_SEP);
                       }
                       Log.d(TAG, "APK["+apkCount+"] libs: <"+libs.toString()+">");
                   }
                   apkCount++;
               } else {
                   Log.d(TAG, "APK not found: <"+lastUserPackageName+">");
               }
           }
           if( packageNames.size() != apkCount ) {
               Log.d(TAG, "User APKs incomplete, abort (1)");
               return null;
           }
       }
       final int userAPKCount = apkCount;

       if( null != apkNames ) {
           for(final Iterator<String> i=apkNames.iterator(); i.hasNext(); ) {
               final String userAPK = i.next();
               if(apkCount>0) {
                   apks.append(ELEM_SEP);
               }
               apks.append(userAPK);
               Log.d(TAG, "APK added: <"+userAPK+">");
               apkCount++;
           }
           if( apkNames.size() != apkCount - userAPKCount ) {
               Log.d(TAG, "Framework APKs incomplete, abort (2)");
               return null;
           }
       }

       // return new TraceDexClassLoader(apks.toString(), dexPath.getAbsolutePath(), libs.toString(), parent);
       return new AssetDexClassLoader(apks.toString(), dexPath.getAbsolutePath(), libs.toString(), parent);
   }

   private static int copyFile(final File src, final File dst) throws IOException {
       int totalBytes = 0;
       final InputStream in = new BufferedInputStream(new FileInputStream(src));
       try {
           final OutputStream out = new BufferedOutputStream(new FileOutputStream(dst));
           try {
               final byte[] buf = new byte[bufferSize];
               while (true) {
                   int count;
                   if ((count = in.read(buf)) == -1) {
                       break;
                   }
                   out.write(buf, 0, count);
                   totalBytes += count;
               }
           } finally {
               out.close();
           }
       } finally {
           in.close();
       }
       return totalBytes;
   }
   private static final int bufferSize = 4096;

   /***
    *
   public boolean setAPKClassLoader(String activityPackageName, ClassLoader classLoader)
   {
       try {
           Field mMainThread = getField(Activity.class, "mMainThread");
           Object mainThread = mMainThread.get(this);
           Class<?> threadClass = mainThread.getClass();
           Field mPackages = getField(threadClass, "mPackages");

           @SuppressWarnings("unchecked")
           HashMap<String,?> map = (HashMap<String,?>) mPackages.get(mainThread);
           WeakReference<?> ref = (WeakReference<?>) map.get(activityPackageName);
           Object apk = ref.get();
           Class<?> apkClass = apk.getClass();
           Field mClassLoader = getField(apkClass, "mClassLoader");

           mClassLoader.set(apk, classLoader);

           Log.d(TAG, "setAPKClassLoader: OK");

           return true;
       } catch (IllegalArgumentException e) {
           e.printStackTrace();
       } catch (IllegalAccessException e) {
           e.printStackTrace();
       }
       Log.d(TAG, "setAPKClassLoader: FAILED");
       return false;
   }

   private Field getField(Class<?> cls, String name)
   {
       for (Field field: cls.getDeclaredFields())
       {
           if (!field.isAccessible()) {
               field.setAccessible(true);
           }
           if (field.getName().equals(name)) {
               return field;
           }
       }
       return null;
   }
   */

}
