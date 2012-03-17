package jogamp.android.launcher;

import java.net.URL;

import android.util.Log;

import dalvik.system.DexClassLoader;

public class AssetDexClassLoader extends DexClassLoader {
    private static final boolean DEBUG = false;
    private static final String assets_folder = "assets/";
    
    public AssetDexClassLoader(String dexPath, String dexOutputDir, String libPath, ClassLoader parent) {
        super(dexPath, dexOutputDir, libPath, parent);
        if(DEBUG) {
            Log.d(AssetDexClassLoader.class.getSimpleName(), "ctor: dexPath " + dexPath + ", dexOutputDir " + dexOutputDir + ", libPath " + libPath + ", parent " + parent);
        }
    }
    
    @Override
    public URL findResource(String name) {
        final String assetName = name.startsWith(assets_folder) ? name : assets_folder + name ;
        URL url = super.findResource(assetName);
        if(DEBUG) {
            Log.d(AssetDexClassLoader.class.getSimpleName(), "findResource: " + name + " -> " + assetName + " -> " + url);
        }
        return url;
    }
}
