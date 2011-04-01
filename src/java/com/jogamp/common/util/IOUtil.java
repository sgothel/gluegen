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
import java.net.URL;
import java.net.URLConnection;

public class IOUtil {
    
    // Copy the specified URL to the specified File
    public static int copyURLToFile(URL url, File outFile) throws IOException {
        URLConnection conn = url.openConnection();
        conn.connect();        

        int totalNumBytes = 0;
        InputStream in = new BufferedInputStream(conn.getInputStream());
        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
            try {
                totalNumBytes = copyStream(in, out, conn.getContentLength());
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
     * number of bytes written is returned. If the close flag is set, both
     * streams are closed upon completion.
     */
    public static int copyStream(InputStream in, OutputStream out, int totalNumBytes) throws IOException {
        int numBytes = 0;
        final int BUFFER_SIZE = 1000;
        byte[] buf = new byte[BUFFER_SIZE];
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


}
