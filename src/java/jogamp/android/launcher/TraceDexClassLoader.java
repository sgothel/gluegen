package jogamp.android.launcher;

import java.net.URL;

import android.util.Log;

import dalvik.system.DexClassLoader;

public class TraceDexClassLoader extends DexClassLoader {
    private static final boolean DEBUG = false;

    public TraceDexClassLoader(final String dexPath, final String dexOutputDir, final String libPath, final ClassLoader parent) {
        super(dexPath, dexOutputDir, libPath, parent);
        if(DEBUG) {
            Log.d(TraceDexClassLoader.class.getSimpleName(), "ctor: dexPath " + dexPath + ", dexOutputDir " + dexOutputDir + ", libPath " + libPath + ", parent " + parent);
        }
    }

    @Override
    public URL findResource(final String name) {
        final URL url = super.findResource(name);
        if(DEBUG) {
            Log.d(TraceDexClassLoader.class.getSimpleName(), "findResource: " + name + " -> " + url);
        }
        return url;
    }
}
