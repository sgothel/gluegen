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

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class ClassLoaderUtil {
   private static final String TAG = "JogampClassLoader";
   
   // FIXME: Need to generalize this .. (Note: native lib resources must be cached!)
   private static final String[] packagesJogAmp = { "com.jogamp.common", "javax.media.opengl" };   
   private static ClassLoader jogAmpClassLoader = null;
   
   // location where optimized dex files will be written
   private static final String dexPathName= "jogampDex";
   private static File dexPath = null; 
   
   private static LauncherTempFileCache tmpFileCache = null;
   
   private static final String PATH_SEP = "/";
   private static final String ELEM_SEP = ":";
   
   private static synchronized void init(Context ctx) {
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
       }       
   }
   
   public static synchronized ClassLoader createClassLoader(Context ctx, List<String> userPackageNames, boolean addUserLibPath, 
                                                            List<String> apkNames) {       
       return createClassLoader(ctx, userPackageNames, addUserLibPath, apkNames, null); 
   }
   
   public static synchronized ClassLoader createClassLoader(Context ctx, List<String> userPackageNames, boolean addUserLibPath, 
                                                            List<String> apkNames, ClassLoader parent) {
       init(ctx);
       
       if(null==jogAmpClassLoader) {           
           jogAmpClassLoader = createClassLoaderImpl(ctx, Arrays.asList(packagesJogAmp), true, null,
                                                     (null != parent ) ? parent : ctx.getClassLoader());
       }
       parent =  jogAmpClassLoader;
       
       return createClassLoaderImpl(ctx, userPackageNames, addUserLibPath, apkNames, jogAmpClassLoader); 
   }
   
   /**
    * 
    * @param ctx
    * @param userPackageNames list of user package names, the last entry shall reflect the Activity
    * @return
    */
   private static synchronized ClassLoader createClassLoaderImpl(Context ctx, List<String> userPackageNames, boolean addUserLibPath, 
                                                                 List<String> apkNames, ClassLoader parent) {
       
       
       final ApplicationInfo appInfo = ctx.getApplicationInfo();
       final String appDir = new File(appInfo.dataDir).getParent();
       final String libSub = appInfo.nativeLibraryDir.substring(appInfo.nativeLibraryDir.lastIndexOf('/')+1);
       Log.d(TAG, "S: userPackageName: "+userPackageNames+"; appName "+appInfo.name+", appDir "+appDir+", nativeLibraryDir: "+appInfo.nativeLibraryDir+"; dataDir: "+appInfo.dataDir+", libSub "+libSub);
       
       StringBuilder apks = new StringBuilder();
       StringBuilder libs = new StringBuilder();
       int apkCount = 0;
       String lastUserPackageName = null; // the very last one reflects the Activity
       
       if( null != userPackageNames ) {
           for(Iterator<String> i=userPackageNames.iterator(); i.hasNext(); ) {
               lastUserPackageName = i.next();
               String userAPK = null;
               try {
                   userAPK = ctx.getPackageManager().getApplicationInfo(lastUserPackageName,0).sourceDir;
               } catch (PackageManager.NameNotFoundException e) {
                   Log.d(TAG, "error: "+e, e);
               }
               if(null != userAPK) {
                   if(apkCount>0) {
                       apks.append(ELEM_SEP);
                       if(addUserLibPath) {
                           libs.append(ELEM_SEP);
                       }
                   }
                   apks.append(userAPK);
                   Log.d(TAG, "APK["+apkCount+"] found: <"+lastUserPackageName+"> -> <"+userAPK+">");
                   Log.d(TAG, "APK["+apkCount+"] apks: <"+apks.toString()+">");
                   if(addUserLibPath) {
                       libs.append(appDir).append(PATH_SEP).append(lastUserPackageName).append(PATH_SEP).append(libSub).append(PATH_SEP);
                       Log.d(TAG, "APK["+apkCount+"] libs: <"+libs.toString()+">");
                   }
                   apkCount++;
               } else {
                   Log.d(TAG, "APK not found: <"+lastUserPackageName+">");
               }
           }
           if( userPackageNames.size() != apkCount ) {
               Log.d(TAG, "User APKs incomplete, abort (1)");
               return null;
           }
       }
       final int userAPKCount = apkCount;
       
       if( null != apkNames ) {
           for(Iterator<String> i=apkNames.iterator(); i.hasNext(); ) {
               String userAPK = i.next();
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
