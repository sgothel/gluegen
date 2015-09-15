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

package jogamp.common.util.locks;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

import com.jogamp.common.util.InterruptSource;
import com.jogamp.common.util.locks.SingletonInstance;

public class SingletonInstanceFileLock extends SingletonInstance {

    static final String temp_file_path;

    static {
        String s = null;
        try {
            final File tmpFile = File.createTempFile("TEST", "tst");
            final String absTmpFile = tmpFile.getCanonicalPath();
            tmpFile.delete();
            s = absTmpFile.substring(0, absTmpFile.lastIndexOf(File.separator));
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
        temp_file_path = s;
    }

    public static String getCanonicalTempPath() {
        return temp_file_path;
    }

    public static String getCanonicalTempLockFilePath(final String basename) {
        return getCanonicalTempPath() + File.separator + basename;
    }

    public SingletonInstanceFileLock(final long poll_ms, final String lockFileBasename) {
        super(poll_ms);
        file = new File ( getCanonicalTempLockFilePath ( lockFileBasename ) );
        setupFileCleanup();
    }

    public SingletonInstanceFileLock(final long poll_ms, final File lockFile) {
        super(poll_ms);
        file = lockFile ;
        setupFileCleanup();
    }

    @Override
    public final String getName() { return file.getPath(); }

    private void setupFileCleanup() {
        file.deleteOnExit();
        Runtime.getRuntime().addShutdownHook(new InterruptSource.Thread() {
            @Override
            public void run() {
                if(isLocked()) {
                    System.err.println(infoPrefix()+" XXX "+SingletonInstanceFileLock.this.getName()+" - Unlock @ JVM Shutdown");
                }
                unlock();
            }
        });
    }

    @Override
    protected boolean tryLockImpl() {
        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            fileLock = randomAccessFile.getChannel().tryLock();

            if (fileLock != null) {
                return true;
            }
        } catch (final Exception e) {
            System.err.println(infoPrefix()+" III "+getName()+" - Unable to create and/or lock file");
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected boolean unlockImpl() {
        try {
            if(null != fileLock) {
                fileLock.release();
                fileLock = null;
            }
            if(null != randomAccessFile) {
                randomAccessFile.close();
                randomAccessFile = null;
            }
            if(null != file) {
                file.delete();
            }
            return true;
        } catch (final Exception e) {
            System.err.println(infoPrefix()+" EEE "+getName()+" - Unable to remove lock file");
            e.printStackTrace();
        } finally {
            fileLock = null;
            randomAccessFile = null;
        }
        return false;
    }

    private final File file;
    private RandomAccessFile randomAccessFile = null;
    private FileLock fileLock = null;
}
