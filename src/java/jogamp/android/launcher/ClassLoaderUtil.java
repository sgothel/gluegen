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
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import dalvik.system.DexClassLoader;

public class ClassLoaderUtil {
   private static final String TAG = "JogampClassLoader";
   
   public static final String packageGlueGen = "com.jogamp.common";       
   public static final String packageJogl = "javax.media.opengl";   // FIXME: a 'performance' hack
   
   public static final String dexPathName= "jogampDex";
   
   private static LauncherTempFileCache tmpFileCache = null;
   private static ClassLoader jogAmpClassLoader = null;
   
   /**
    * 
    * @param ctx
    * @param userPackageNames list of user package names, the last entry shall reflect the Activity
    * @return
    */
   public static synchronized ClassLoader createJogampClassLoaderSingleton(Context ctx, List<String> userPackageNames) {
       if(null==jogAmpClassLoader) {
           if(null!=tmpFileCache) {
               throw new InternalError("XXX0");
           }
           if(!LauncherTempFileCache.initSingleton(ctx)) {
               throw new InternalError("TempFileCache initialization error");
           }
           tmpFileCache = new LauncherTempFileCache();
           if(!tmpFileCache.isValid()) {
               throw new InternalError("TempFileCache instantiation error");                
           }
           final ApplicationInfo ai = ctx.getApplicationInfo();
           Log.d(TAG, "S: userPackageName: "+userPackageNames+", dataDir: "+ai.dataDir+", nativeLibraryDir: "+ai.nativeLibraryDir);
    
           final String appDir = new File(ai.dataDir).getParent();
           final String libSub = ai.nativeLibraryDir.substring(ai.nativeLibraryDir.lastIndexOf('/')+1);
           Log.d(TAG, "S: appDir: "+appDir+", libSub: "+libSub);
           
           final String libPathName = appDir + "/" + packageGlueGen + "/" + libSub + "/:" +
                                      appDir + "/" + packageJogl + "/" + libSub + "/" ;
           Log.d(TAG, "S: libPath: "+libPathName);
                   
           String apkGlueGen = null;
           String apkJogl = null;
       
           try {
               apkGlueGen = ctx.getPackageManager().getApplicationInfo(packageGlueGen,0).sourceDir;
               apkJogl = ctx.getPackageManager().getApplicationInfo(packageJogl,0).sourceDir;
           } catch (PackageManager.NameNotFoundException e) {
               Log.d(TAG, "error: "+e, e);
           }
           if(null == apkGlueGen || null == apkJogl) {
               Log.d(TAG, "not found: gluegen <"+apkGlueGen+">, jogl <"+apkJogl+">");
               return null;
           }
           
           final String cp = apkGlueGen + ":" + apkJogl ;
           Log.d(TAG, "jogamp cp: " + cp);
       
           final File dexPath = new File(tmpFileCache.getTempDir(), dexPathName);
           Log.d(TAG, "jogamp dexPath: " + dexPath.getAbsolutePath());
           dexPath.mkdir();
           jogAmpClassLoader = new DexClassLoader(cp, dexPath.getAbsolutePath(), libPathName, ctx.getClassLoader());
       } else {
           if(null==tmpFileCache) {
               throw new InternalError("XXX1");
           }           
       }
       
       StringBuilder userAPKs = new StringBuilder();
       int numUserAPKs = 0;
       String lastUserPackageName = null; // the very last one reflects the Activity
       for(Iterator<String> i=userPackageNames.iterator(); i.hasNext(); ) {
           lastUserPackageName = i.next();
           String userAPK = null;
           try {
               userAPK = ctx.getPackageManager().getApplicationInfo(lastUserPackageName,0).sourceDir;
           } catch (PackageManager.NameNotFoundException e) {
               Log.d(TAG, "error: "+e, e);
           }
           if(null != userAPK) {
               numUserAPKs++;
               if(numUserAPKs>0) {
                   userAPKs.append(":");
               }
               userAPKs.append(userAPK);
               Log.d(TAG, "APK found: <"+lastUserPackageName+"> -> <"+userAPK+">");
           } else {
               Log.d(TAG, "APK not found: <"+lastUserPackageName+">");
           }
       }
       if( userPackageNames.size()!=numUserAPKs ) {
           Log.d(TAG, "APKs incomplete, abort");
           return null;
       }
       
       final String userAPKs_s = userAPKs.toString();
       Log.d(TAG, "user cp: " + userAPKs_s);
       final File dexPath = new File(tmpFileCache.getTempDir(), lastUserPackageName);
       Log.d(TAG, "user dexPath: " + dexPath.getAbsolutePath());
       dexPath.mkdir();
       ClassLoader cl = new DexClassLoader(userAPKs_s, dexPath.getAbsolutePath(), null, jogAmpClassLoader);
       Log.d(TAG, "cl: " + cl);
       
       return cl;
   }
   
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

}
