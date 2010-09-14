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
package com.sun.gluegen;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 *
 * @author Michael Bien
 */
public class Logging {

    static void init() {

        String pakage = Logging.class.getPackage().getName();
        String property = System.getProperty(pakage+".level");
        Level level;
        if(property != null) {
            level = Level.parse(property);
        }else{
            level = Level.WARNING;
        }

        ConsoleHandler handler = new ConsoleHandler() {
            @Override
            public java.util.logging.Formatter getFormatter() {
                return new PlainLogFormatter();
            }
        };
        handler.setFormatter(new PlainLogFormatter());
        handler.setLevel(level);

        Logger rootPackageLogger = Logger.getLogger(pakage);
        rootPackageLogger.setUseParentHandlers(false);
        rootPackageLogger.setLevel(level);
        rootPackageLogger.addHandler(handler);
    }

    /**
     * This log formatter needs usually one line per log record.
     * @author Michael Bien
     */
    private static class PlainLogFormatter extends Formatter {

        //@Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder(128);
            sb.append("[").append(record.getLevel()).append(' ').append(record.getSourceClassName()).append("]: ");
            sb.append(formatMessage(record)).append("\n");
            return sb.toString();
        }
    }
}
