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

package com.jogamp.common.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;

import jogamp.common.Debug;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.os.MachineDescription;
import com.jogamp.common.os.Platform;

public class IOUtil {
    private static final boolean DEBUG = Debug.isPropertyDefined("jogamp.debug.IOUtil", true, AccessController.getContext());
    
    private IOUtil() {}
    
    /***
     * 
     * STREAM COPY STUFF
     *  
     */
    
    /**
     * Copy the specified input stream to the specified output file. The total
     * number of bytes written is returned. Both streams are closed upon completion.
     */
    public static int copyURL2File(URL url, File outFile) throws IOException {
        URLConnection conn = url.openConnection();
        conn.connect();        

        int totalNumBytes = 0;
        InputStream in = new BufferedInputStream(conn.getInputStream());
        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
            try {
                totalNumBytes = copyStream2Stream(in, out, conn.getContentLength());
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
        return totalNumBytes;
    }

    /**
     * Copy the specified input stream to the specified output stream. The total
     * number of bytes written is returned.
     */
    public static int copyStream2Stream(InputStream in, OutputStream out, int totalNumBytes) throws IOException {
        final byte[] buf = new byte[Platform.getMachineDescription().pageSizeInBytes()];
        int numBytes = 0;
        while (true) {
            int count;
            if ((count = in.read(buf)) == -1) {
                break;
            }
            out.write(buf, 0, count);
            numBytes += count;
        }
        return numBytes;
    }

    /**
     * Copy the specified input stream to a byte array, which is being returned.
     */
    public static byte[] copyStream2ByteArray(InputStream stream) throws IOException {
        // FIXME: Shall enforce a BufferedInputStream ?
        if( !(stream instanceof BufferedInputStream) ) {
            stream = new BufferedInputStream(stream);
        }
        int totalRead = 0;
        int avail = stream.available();
        byte[] data = new byte[avail];
        int numRead = 0;
        do {
            if (totalRead + avail > data.length) {
                final byte[] newData = new byte[totalRead + avail];
                System.arraycopy(data, 0, newData, 0, totalRead);
                data = newData;
            }
            numRead = stream.read(data, totalRead, avail);
            if (numRead >= 0) {
                totalRead += numRead;
            }
            avail = stream.available();
        } while (avail > 0 && numRead >= 0);

        // just in case the announced avail > totalRead
        if (totalRead != data.length) {
            final byte[] newData = new byte[totalRead];
            System.arraycopy(data, 0, newData, 0, totalRead);
            data = newData;
        }
        return data;
    }

    /**
     * Copy the specified input stream to a NIO ByteBuffer w/ native byte order, which is being returned.
     * <p>The implementation creates the ByteBuffer w/ {@link #copyStream2ByteArray(InputStream)}'s returned byte array.</p>
     */
    public static ByteBuffer copyStream2ByteBuffer(InputStream stream) throws IOException {
        // FIXME: Shall enforce a BufferedInputStream ?
        if( !(stream instanceof BufferedInputStream) ) {
            stream = new BufferedInputStream(stream);
        }
        int avail = stream.available();
        final MachineDescription machine = Platform.getMachineDescription(); 
        ByteBuffer data = Buffers.newDirectByteBuffer( machine.pageAlignedSize(avail) );
        byte[] chunk = new byte[machine.pageSizeInBytes()];
        int chunk2Read = Math.min(machine.pageSizeInBytes(), avail);
        int numRead = 0;
        do {
            if (avail > data.remaining()) {
                final ByteBuffer newData = Buffers.newDirectByteBuffer(
                                               machine.pageAlignedSize(data.position() + avail) );
                newData.put(data);
                data = newData;
            }
            
            numRead = stream.read(chunk, 0, chunk2Read);
            if (numRead >= 0) {
                data.put(chunk, 0, numRead);
            }
            avail = stream.available();
            chunk2Read = Math.min(machine.pageSizeInBytes(), avail);
        } while (avail > 0 && numRead >= 0);

        data.flip();
        return data;
    }

    /***
     * 
     * RESOURCE / FILE NAME STUFF
     *  
     */
    
    /**
     * Returns the lowercase suffix of the given file name (the text
     * after the last '.' in the file name). Returns null if the file
     * name has no suffix. Only operates on the given file name;
     * performs no I/O operations.
     *
     * @param file name of the file
     * @return lowercase suffix of the file name
     * @throws NullPointerException if file is null
     */

    public static String getFileSuffix(File file) {
        return getFileSuffix(file.getName());
    }

    /**
     * Returns the lowercase suffix of the given file name (the text
     * after the last '.' in the file name). Returns null if the file
     * name has no suffix. Only operates on the given file name;
     * performs no I/O operations.
     *
     * @param filename name of the file
     * @return lowercase suffix of the file name
     * @throws NullPointerException if filename is null
     */
    public static String getFileSuffix(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0) {
            return null;
        }
        return toLowerCase(filename.substring(lastDot + 1));
    }

    private static String toLowerCase(String arg) {
        if (arg == null) {
            return null;
        }

        return arg.toLowerCase();
    }
    
    /***
     * 
     * RESOURCE LOCATION STUFF
     *  
     */
    
    /**
     * Locating a resource using 'getResource(String path, ClassLoader cl)',
     * with the 
     * <ul>
     *   <li>context's package name-path plus the resourcePath (incl. JAR/Applets)</li>
     *   <li>context's ClassLoader and the resourcePath as is (filesystem)</li>
     * </ul>
     *
     * @see #getResource(String, ClassLoader)
     */
    public static URL getResource(Class<?> context, String resourcePath) {
        if(null == resourcePath) {
            return null;
        }
        ClassLoader contextCL = (null!=context)?context.getClassLoader():null;
        URL url = null;
        if(null != context) {
            // scoping the path within the class's package
            String className = context.getName().replace('.', '/');
            int lastSlash = className.lastIndexOf('/');
            if (lastSlash >= 0) {
                String tmpPath = className.substring(0, lastSlash + 1) + resourcePath;
                url = getResource(tmpPath, contextCL);
            }
            if(DEBUG) {
                System.err.println("IOUtil: found <"+resourcePath+"> within class package: "+(null!=url));
            }
        } else if(DEBUG) {
            System.err.println("IOUtil: null context");
        }
        if(null == url) {        
            url = getResource(resourcePath, contextCL);
            if(DEBUG) {
                System.err.println("IOUtil: found <"+resourcePath+"> by classloader: "+(null!=url));
            }
        }
        return url;
    }

    /**
     * Locating a resource using the ClassLoader's facility if not null,
     * the absolute URL and absolute file.
     *
     * @see ClassLoader#getResource(String)
     * @see ClassLoader#getSystemResource(String)
     * @see URL#URL(String)
     * @see File#File(String)
     */
    @SuppressWarnings("deprecation")
    public static URL getResource(String resourcePath, ClassLoader cl) {
        if(null == resourcePath) {
            return null;
        }
        if(DEBUG) {
            System.err.println("IOUtil: locating <"+resourcePath+">, has cl: "+(null!=cl));
        }
        URL url = null;
        if (cl != null) {
            url = cl.getResource(resourcePath);
            if(!urlExists(url, "cl.getResource()")) {
                url = null;
            }
        }
        if(null == url) {
            url = ClassLoader.getSystemResource(resourcePath);
            if(!urlExists(url, "cl.getSystemResource()")) {
                url = null;
            }
        }
        if(null == url) {
            try {
                url = new URL(resourcePath);
                if(!urlExists(url, "new URL()")) {
                    url = null;
                }
            } catch (Throwable e) { 
                if(DEBUG) {
                    System.err.println("IOUtil: Catched Exception:");
                    e.printStackTrace();
                }                
            }
        }
        if(null == url) {
            try {
                File file = new File(resourcePath);
                if(file.exists()) {
                    url = file.toURL();
                }
            } catch (Throwable e) {
                if(DEBUG) {
                    System.err.println("IOUtil: Catched Exception:");
                    e.printStackTrace();
                }
            }
            if(DEBUG) {
                System.err.println("IOUtil: file.exists("+resourcePath+") - "+(null!=url));
            }
        }
        return url;
    }

    /**
     * Generates a path for the 'relativeFile' relative to the 'baseLocation'.
     * 
     * @param baseLocation denotes a directory
     * @param relativeFile denotes a relative file to the baseLocation
     */
    public static String getRelativeOf(File baseLocation, String relativeFile) {
        if(null == relativeFile) {
            return null;
        }
        
        while (baseLocation != null && relativeFile.startsWith("../")) {
            baseLocation = baseLocation.getParentFile();
            relativeFile = relativeFile.substring(3);
        }
        if (baseLocation != null) {
            final File file = new File(baseLocation, relativeFile);
            // Handle things on Windows
            return file.getPath().replace('\\', '/');
        }
        return null;
    }

    /**
     * Generates a path for the 'relativeFile' relative to the 'baseLocation'.
     * 
     * @param baseLocation denotes a URL to a file
     * @param relativeFile denotes a relative file to the baseLocation's parent directory
     */
    public static String getRelativeOf(URL baseLocation, String relativeFile) {    
        String urlPath = baseLocation.getPath();
        
        if ( baseLocation.toString().startsWith("jar") ) {
            JarURLConnection jarConnection;
            try {
                jarConnection = (JarURLConnection) baseLocation.openConnection();
                urlPath = jarConnection.getEntryName();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        
        // Try relative path first
        return getRelativeOf(new File(urlPath).getParentFile(), relativeFile);       
    }
    
    /**
     * Returns true, if the URL exists and a connection could be opened.
     */
    public static boolean urlExists(URL url) {
        return urlExists(url, ".");
    }
    
    public static boolean urlExists(URL url, String dbgmsg) {
        boolean v = false;
        if(null!=url) {
            try {
                url.openConnection();
                v = true;
                if(DEBUG) {
                    System.err.println("IOUtil: urlExists("+url+") ["+dbgmsg+"] - true");
                }
            } catch (IOException ioe) { 
                if(DEBUG) {
                    System.err.println("IOUtil: urlExists("+url+") ["+dbgmsg+"] - false: "+ioe.getMessage());
                }                
            }
        } else if(DEBUG) {
            System.err.println("IOUtil: no url - urlExists(null) ["+dbgmsg+"]");
        }                
        
        return v;
    }    
}
