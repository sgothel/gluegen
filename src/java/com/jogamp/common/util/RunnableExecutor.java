package com.jogamp.common.util;

public interface RunnableExecutor {
    /** {@link RunnableExecutor} implementation simply invoking {@link Runnable#run()}, 
     *  i.e. on the current thread at the time of calling {@link #invoke(boolean, Runnable)}.
     */
    public static final RunnableExecutor currentThreadExecutor = new CurrentThreadExecutor();
    
    /**
     * @param wait if true method waits until {@link Runnable#run()} is completed, otherwise don't wait.  
     * @param r the {@link Runnable} to be executed.
     */
    void invoke(boolean wait, Runnable r);
    
    static class CurrentThreadExecutor implements RunnableExecutor {
        private CurrentThreadExecutor() {}
        
        @Override
        public void invoke(boolean wait, Runnable r) {
            r.run();            
        }        
    }
}
