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
package com.jogamp.common.util.locks;

/**
 * Reentrance capable locking toolkit, supporting multiple threads as owner.
 * <p>
 * See use case description at {@link #addOwner(Thread)}.
 * </p>
 */
public interface RecursiveThreadGroupLock extends RecursiveLock {
    /**
     * Returns true if the current thread is the original lock owner, ie.
     * successfully claimed this lock the first time, ie. {@link #getHoldCount()} == 1.
     */
    boolean isOriginalOwner();

    /**
     * Returns true if the passed thread is the original lock owner, ie.
     * successfully claimed this lock the first time, ie. {@link #getHoldCount()} == 1.
     */
    boolean isOriginalOwner(Thread thread);

    /**
     * Add a thread to the list of additional lock owners, which enables them to recursively claim this lock.
     * <p>
     * The caller must hold this lock and be the original lock owner, see {@link #isOriginalOwner()}.
     * </p>
     * <p>
     * If the original owner releases this lock via {@link #unlock()}
     * all additional lock owners are released as well.
     * This ensures consistency of spawn off additional lock owner threads and it's release.
     * </p>
     * Use case:
     * <pre>
     * Thread2 thread2 = new Thread2();
     *
     * Thread1 {
     *
     *   // Claim this lock and become the original lock owner.
     *   lock.lock();
     *
     *   try {
     *
     *     // Allow Thread2 to claim the lock, ie. make thread2 an additional lock owner
     *     addOwner(thread2);
     *
     *     // Start thread2
     *     thread2.start();
     *
     *     // Wait until thread2 has finished requiring this lock, but keep thread2 running
     *     while(!thread2.waitForResult()) sleep();
     *
     *     // Optional: Only if sure that this thread doesn't hold the lock anymore,
     *     // otherwise just release the lock via unlock().
     *     removeOwner(thread2);
     *
     *   } finally {
     *
     *     // Release this lock and remove all additional lock owners.
     *     // Implicit wait until thread2 gets off the lock.
     *     lock.unlock();
     *
     *   }
     *
     * }.start();
     * </pre>
     *
     * @param t the thread to be added to the list of additional owning threads
     * @throws RuntimeException if the current thread does not hold the lock.
     * @throws IllegalArgumentException if the passed thread is the lock owner or already added.
     *
     * @see #removeOwner(Thread)
     * @see #unlock()
     * @see #lock()
     */
    void addOwner(Thread t) throws RuntimeException, IllegalArgumentException;

    /**
     * Remove a thread from the list of additional lock owner threads.
     * <p>
     * The caller must hold this lock and be the original lock owner, see {@link #isOriginalOwner()}.
     * </p>
     * <p>
     * Only use this method if sure that the thread doesn't hold the lock anymore.
     * </p>
     *
     * @param t the thread to be removed from the list of additional owning threads
     * @throws RuntimeException if the current thread does not hold the lock.
     * @throws IllegalArgumentException if the passed thread is not added by {@link #addOwner(Thread)}
     */
    void removeOwner(Thread t) throws RuntimeException, IllegalArgumentException;

    /**
     * <p>
     * Wait's until all additional owners released this lock before releasing it.
     * </p>
     *
     * {@inheritDoc}
     */
    @Override
    void unlock() throws RuntimeException;

    /**
     * <p>
     * Wait's until all additional owners released this lock before releasing it.
     * </p>
     *
     * {@inheritDoc}
     */
    @Override
    void unlock(Runnable taskAfterUnlockBeforeNotify);

}
