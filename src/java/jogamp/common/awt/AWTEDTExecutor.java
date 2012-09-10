/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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
package jogamp.common.awt;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.common.util.RunnableExecutor;

/**
 * AWT EDT implementation of RunnableExecutor
 */
public class AWTEDTExecutor implements RunnableExecutor {
    /** {@link RunnableExecutor} implementation invoking {@link Runnable#run()}
     *  on the AWT EDT. 
     */
    public static final AWTEDTExecutor singleton = new AWTEDTExecutor();

    private AWTEDTExecutor() {}
    
    @Override
    public void invoke(boolean wait, Runnable r) {
        if(EventQueue.isDispatchThread()) {
            r.run();
        } else {
          try {
            if(wait) {
                EventQueue.invokeAndWait(r);
            } else {
                EventQueue.invokeLater(r);
            }
          } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getTargetException());
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
    }

    /**
     * Executes the given runnable on the AWT EDT if current thread is not the EDT and the given tree
     * lock is not hold, otherwise execute the runnable in current thread.
     * @param treeLock representing the AWT-tree-lock, i.e. {@link java.awt.Component#getTreeLock()}
     * @param wait if true method waits until {@link Runnable#run()} is completed, otherwise don't wait.  
     * @param r the {@link Runnable} to be executed.
     */
    public void invoke(Object treeLock, boolean wait, Runnable r) {
        if(EventQueue.isDispatchThread() || Thread.holdsLock(treeLock)) {
            r.run();
        } else {
          try {
            if(wait) {
                EventQueue.invokeAndWait(r);
            } else {
                EventQueue.invokeLater(r);
            }
          } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getTargetException());
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
    }
}
