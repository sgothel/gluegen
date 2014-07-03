package jogamp.android.launcher;

import java.net.URL;

import android.util.Log;

import dalvik.system.DexClassLoader;

public class AssetDexClassLoader extends DexClassLoader {
    private static final boolean DEBUG = false;
    private static final String assets_folder = "assets/";

    private static int next_id = 1;
    private final int id;

    public AssetDexClassLoader(final String dexPath, final String dexOutputDir, final String libPath, final ClassLoader parent) {
        super(dexPath, dexOutputDir, libPath, parent);
        synchronized(AssetDexClassLoader.class) {
            id = next_id++;
        }
        if(DEBUG) {
            Log.d(getSimpleName(), getIdName()+".ctor: dexPath " + dexPath + ", dexOutputDir " + dexOutputDir + ", libPath " + libPath + ", parent " + parent);
        }
    }

    protected final String getSimpleName() {
        return AssetDexClassLoader.class.getSimpleName();
    }
    protected final String getIdName() {
        return "ADCL["+id+"]";
    }

    @Override
    public String findLibrary(final String libName) {
        final String res = super.findLibrary(libName);
        if(DEBUG) {
            Log.d(getSimpleName(), getIdName()+".findLibrary: " + libName + " -> " + res);
        }
        return res;
    }

    @Override
    public URL findResource(final String name) {
        final String assetName = name.startsWith(assets_folder) ? name : assets_folder + name ;
        final URL url = super.findResource(assetName);
        if(DEBUG) {
            Log.d(getSimpleName(), getIdName()+".findResource: " + name + " -> " + assetName + " -> " + url);
        }
        return url;
    }
}
