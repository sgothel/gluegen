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

package com.jogamp.common.util.locks;

import jogamp.common.Debug;

/**
 * Specifying a thread blocking lock implementation
 */
public interface Lock {

    /** Enable via the property <code>jogamp.debug.Lock</code> */
    public static final boolean DEBUG = Debug.debug("Lock");

    /** Enable via the property <code>jogamp.debug.Lock.TraceLock</code> */
    public static final boolean TRACE_LOCK = Debug.isPropertyDefined("jogamp.debug.Lock.TraceLock", true);

    /** The default {@link #TIMEOUT} value, of {@value} ms */
    public static final long DEFAULT_TIMEOUT = 5000; // 5s default timeout

    /**
     * The <code>TIMEOUT</code> for {@link #lock()} in ms,
     * defaults to {@link #DEFAULT_TIMEOUT}.
     * <p>
     * It can be overridden via the system property <code>jogamp.common.utils.locks.Lock.timeout</code>.
     * </p>
     */
    public static final long TIMEOUT = Debug.getLongProperty("jogamp.common.utils.locks.Lock.timeout", true, DEFAULT_TIMEOUT);

    /**
     * Blocking until the lock is acquired by this Thread or {@link #TIMEOUT} is reached.
     *
     * @throws RuntimeException in case of {@link #TIMEOUT}
     */
    void lock() throws RuntimeException;

    /**
     * Blocking until the lock is acquired by this Thread or <code>maxwait</code> in ms is reached.
     *
     * @param timeout Maximum time in ms to wait to acquire the lock. If this value is zero,
     *                the call returns immediately either without being able
     *                to acquire the lock, or with acquiring the lock directly while ignoring any scheduling order.
     * @return true if the lock has been acquired within <code>maxwait</code>, otherwise false
     *
     * @throws InterruptedException
     */
    boolean tryLock(long timeout) throws InterruptedException;

    /**
     * Release the lock.
     *
     * @throws RuntimeException in case the lock is not acquired by this thread.
     */
    void unlock() throws RuntimeException;

    /** Query if locked */
    boolean isLocked();
}
