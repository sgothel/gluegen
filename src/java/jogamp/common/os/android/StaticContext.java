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

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

public class StaticContext {
   private static Context appContext = null;
   private static ViewGroup contentViewGroup = null;

   private static boolean DEBUG = false;

   /**
    * Register Android application context for static usage.
    *
    * @param appContext mandatory application Context
    * @throws RuntimeException if the context is already registered.
    */
   public static final synchronized void init(final Context appContext) {
       init(appContext, null);
   }

   /**
    * Register Android application context w/ a ViewGroup for static usage.
    *
    * @param appContext mandatory application Context
    * @param contentViewGroup optional ViewGroup acting as the Window's ContentView, usually a FrameLayout instance.
    * @throws RuntimeException if the context is already registered.
    */
   public static final synchronized void init(final Context appContext, final ViewGroup contentViewGroup) {
       if(null != StaticContext.appContext) {
           throw new RuntimeException("Context already set");
       }
       if(DEBUG) { Log.d(MD.TAG, "init(appCtx "+appContext+", viewGroup "+contentViewGroup+")"); }
       StaticContext.appContext = appContext;
       StaticContext.contentViewGroup = contentViewGroup;
   }

   /**
    * Unregister the Android application Context and ViewGroup
    */
   public static final synchronized void clear() {
       if(DEBUG) { Log.d(MD.TAG, "clear()"); }
       appContext = null;
       contentViewGroup = null;
   }

   /**
    * Return the registered Android application Context
    * @return
    */
   public static final synchronized Context getContext() {
       return appContext;
   }

   /**
    * Return the registered Android ViewGroup acting as the Window's ContentView
    * @return
    */
   public static final synchronized ViewGroup getContentViewGroup() {
       return contentViewGroup;
   }
}
