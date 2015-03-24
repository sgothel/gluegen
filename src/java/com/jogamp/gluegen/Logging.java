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

/*
 * Created on Wednesday, March 31 2010 13:30
 */
package com.jogamp.gluegen;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import jogamp.common.Debug;

import com.jogamp.common.util.PropertyAccess;
import com.jogamp.gluegen.cgram.types.AliasedSymbol;
import com.jogamp.gluegen.cgram.types.Type;

/**
 *
 * @author Michael Bien, et.al.
 */
public class Logging {
    public static final boolean DEBUG = Debug.debug("Logging");

    /**
     * An interface for {@link Logger}.
     */
    public static interface LoggerIf {
        /**
         * See {@link Logger#info(String)}
         */
        void info(final String msg);
        /**
         * See {@link Logger#info(String)}
         */
        void info(final ASTLocusTag loc, final String msg);

        /**
         * See {@link Logger#warning(String)}
         */
        void warning(final String msg);
        /**
         * See {@link Logger#warning(String)}
         */
        void warning(final ASTLocusTag loc, final String msg);

        /**
         * Calls {@link #log(Level, String)} w/ {@link Level#FINE}.
         */
        void debug(final String msg);
        /**
         * Calls {@link #log(Level, ASTLocusTag, String)} w/ {@link Level#FINE}.
         */
        void debug(final ASTLocusTag loc, final String msg);

        /**
         * See {@link Logger#log(Level, String)}
         */
        void log(final Level level, final String msg);
        /**
         * See {@link Logger#log(Level, String, Object)}
         */
        void log(final Level level, final String msg, final Object param);
        /**
         * See {@link Logger#log(Level, String, Object[])}
         */
        void log(final Level level, final String msg, final Object ... params);

        /**
         * See {@link Logger#log(Level, String)}
         */
        void log(final Level level, final ASTLocusTag loc, final String msg);
        /**
         * See {@link Logger#log(Level, String, Object)}
         */
        void log(final Level level, final ASTLocusTag loc, final String msg, final Object param);
        /**
         * See {@link Logger#log(Level, String, Object[])}
         */
        void log(final Level level, final ASTLocusTag loc, final String msg, final Object ... params);

        /**
         * See {@link Logger#setLevel(Level)}
         */
        void setLevel(final Level newLevel) throws SecurityException;
        /**
         * See {@link Handler#setLevel(Level)}
         */
        void setLevelOfAllHandler(final Level newLevel) throws SecurityException;
        /**
         * See {@link Logger#getLevel()}
         */
        Level getLevel();
        /**
         * See {@link Logger#isLoggable(Level)}
         */
        boolean isLoggable(Level level);
        /**
         * See {@link Logger#getName()}
         */
        String getName();
        /**
         * See {@link Logger#getHandlers()}
         */
        Handler[] getHandlers();
        /**
         * See {@link LogRecord#getSourceClassName()}
         */
        String getSourceClassName();
    }
    /* pp */ static class FQNLogger implements LoggerIf {
        public final Logger impl;
        public final PlainLogConsoleHandler handler;
        /* pp */ FQNLogger(final String fqnClassName, final String simpleClassName, final Level level) {
            this.impl = Logger.getLogger(fqnClassName);
            this.handler = new PlainLogConsoleHandler(new PlainLogFormatter(simpleClassName), Level.ALL);
            this.impl.setUseParentHandlers(false);
            this.impl.setLevel(level);
            this.impl.addHandler(this.handler);
            this.impl.log(Level.INFO, "Logging.new: "+impl.getName()+": level "+level+
                                      ": obj 0x"+Integer.toHexString(impl.hashCode()));
        }
        @Override
        public void info(final String msg) {
            impl.info(msg);
        }
        @Override
        public void info(final ASTLocusTag loc, final String msg) {
            handler.plf.setASTLocusTag(loc);
            try {
                impl.info(msg);
            } finally {
                handler.plf.setASTLocusTag(null);
            }
        }

        @Override
        public void warning(final String msg) {
            impl.warning(msg);
        }
        @Override
        public void warning(final ASTLocusTag loc, final String msg) {
            handler.plf.setASTLocusTag(loc);
            try {
                impl.warning(msg);
            } finally {
                handler.plf.setASTLocusTag(null);
            }
        }

        @Override
        public void debug(final String msg) {
            log(Level.FINE, msg);
        }
        @Override
        public void debug(final ASTLocusTag loc, final String msg) {
            log(Level.FINE, loc, msg);
        }

        @Override
        public void log(final Level level, final String msg) {
            impl.log(level, msg);
        }
        @Override
        public void log(final Level level, final String msg, final Object param) {
            impl.log(level, msg, param);
        }
        @Override
        public void log(final Level level, final String msg, final Object ... params) {
            impl.log(level, msg, params);
        }

        @Override
        public void log(final Level level, final ASTLocusTag loc, final String msg) {
            handler.plf.setASTLocusTag(loc);
            try {
                impl.log(level, msg);
            } finally {
                handler.plf.setASTLocusTag(null);
            }
        }
        @Override
        public void log(final Level level, final ASTLocusTag loc, final String msg, final Object param) {
            handler.plf.setASTLocusTag(loc);
            try {
                impl.log(level, msg, param);
            } finally {
                handler.plf.setASTLocusTag(null);
            }
        }
        @Override
        public void log(final Level level, final ASTLocusTag loc, final String msg, final Object ... params) {
            handler.plf.setASTLocusTag(loc);
            try {
                impl.log(level, msg, params);
            } finally {
                handler.plf.setASTLocusTag(null);
            }
        }

        @Override
        public void setLevel(final Level newLevel) throws SecurityException {
            impl.setLevel(newLevel);
        }
        @Override
        public void setLevelOfAllHandler(final Level newLevel) throws SecurityException {
            final Handler[] hs = getHandlers();
            for(final Handler h:hs) {
                h.setLevel(newLevel);
            }
        }
        @Override
        public Level getLevel() {
            return impl.getLevel();
        }
        @Override
        public boolean isLoggable(final Level level) {
            return impl.isLoggable(level);
        }
        @Override
        public String getName() {
            return impl.getName();
        }
        @Override
        public synchronized Handler[] getHandlers() {
            return impl.getHandlers();
        }
        @Override
        public String getSourceClassName() {
            return handler.plf.simpleClassName;
        }
    }
    static class PlainLogConsoleHandler extends ConsoleHandler {
        final PlainLogFormatter plf;
        PlainLogConsoleHandler(final PlainLogFormatter plf, final Level level) {
            this.plf = plf;
            setFormatter(plf);
            setLevel(level);
        }
        @Override
        public java.util.logging.Formatter getFormatter() {
            return plf;
        }
    }
    static class PlainLogFormatter extends Formatter {
        final String simpleClassName;
        ASTLocusTag astLocus;
        PlainLogFormatter(final String simpleClassName) {
            this.simpleClassName = simpleClassName;
        }
        public void setASTLocusTag(final ASTLocusTag loc) { astLocus = loc; }
        @Override
        public String format(final LogRecord record) {
            // Replace [Type, JavaType] -> its debug string!
            final Object[] params = record.getParameters();
            if( null != params ) {
                for(int i=params.length-1; 0<=i; i--) {
                    final Object o = params[i];
                    if( o instanceof Type ) {
                        params[i] = ((Type)o).getDebugString();
                    } else if( o instanceof JavaType ) {
                        params[i] = ((JavaType)o).getDebugString();
                    } else if( o instanceof AliasedSymbol ) {
                        params[i] = ((AliasedSymbol)o).getAliasedString();
                    }
                }
            }
            final StringBuilder sb = new StringBuilder(256);
            if( null != astLocus ) {
                astLocus.toString(sb, getCanonicalName(record.getLevel()), GlueGen.debug()).append(": ");
            }
            if( GlueGen.debug() ) {
                sb.append(simpleClassName).append(": ");
            }
            sb.append(formatMessage(record)).append("\n");
            return sb.toString();
        }
    }

    private final static Map<String, LoggerIf> loggers;
    private final static FQNLogger rootPackageLogger;
    static {
        loggers = new HashMap<String, LoggerIf>();
        final String packageName = Logging.class.getPackage().getName();
        final String property = PropertyAccess.getProperty(packageName+".level", true);
        Level level;
        if(property != null) {
            level = Level.parse(property);
        } else {
            if( DEBUG || GlueGen.debug() ) {
                level = Level.ALL;
            } else {
                level = Level.WARNING;
            }
        }
        final String simpleClassName = Logging.class.getSimpleName();
        final String fqnClassName = packageName+"."+simpleClassName;
        rootPackageLogger = new FQNLogger(fqnClassName, simpleClassName, level);
        loggers.put(fqnClassName, rootPackageLogger);
    }

    /** provokes static initialization */
    static void init() { }

    public static String getCanonicalName(final Level level) {
        if( Level.CONFIG == level ) {
            return "config";
        } else if( Level.FINER == level ) {
            return "verbose";
        } else if( Level.FINE == level ) {
            return "debug";
        } else if( Level.INFO == level ) {
            return "info";
        } else if( Level.WARNING == level ) {
            return "warning";
        } else if( Level.SEVERE == level ) {
            return "error";
        } else {
            return level.getName().toLowerCase();
        }
    }

    /** Returns the <i>root package logger</i>. */
    public static LoggerIf getLogger() {
        return rootPackageLogger;
    }
    /** Returns the demanded logger, while aligning its log-level to the root logger's level. */
    public static synchronized LoggerIf getLogger(final Class<?> clazz) {
        return getLogger(clazz.getPackage().getName(), clazz.getSimpleName());
    }

    /** Returns the demanded logger, while aligning its log-level to the root logger's level. */
    public static synchronized LoggerIf getLogger(final String packageName, final String simpleClassName) {
        final String fqnClassName = packageName+"."+simpleClassName;
        LoggerIf res = loggers.get(fqnClassName);
        if( null == res ) {
            res = new FQNLogger(fqnClassName, simpleClassName, rootPackageLogger.getLevel());
            loggers.put(fqnClassName, res);
        }
        return res;
    }
    /** Align log-level of given logger to the root logger's level. */
    public static void alignLevel(final LoggerIf l) {
        alignLevel(l, rootPackageLogger.getLevel());
    }
    /** Align log-level of given logger and all its handlers to the given level. */
    public static void alignLevel(final LoggerIf l, final Level level) {
        l.setLevel(level);
        l.setLevelOfAllHandler(level);
    }
}
