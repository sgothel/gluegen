/*
 * Copyright (c) 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 */
package com.jogamp.gluegen.runtime;

import com.jogamp.common.os.DynamicLookupHelper;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Superclass for all generated ProcAddressTables.
 * @author Kenneth Russel
 * @author Michael Bien
 * @author Sven Gothel
 */
public abstract class ProcAddressTable {

    public static final String PROCADDRESS_VAR_PREFIX = "_addressof_";

    protected static boolean DEBUG;
    protected static String DEBUG_PREFIX;
    protected static int debugNum;

    private final FunctionAddressResolver resolver;

    static {
        AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                DEBUG = (System.getProperty("jogamp.debug.ProcAddressHelper") != null);
                if (DEBUG) {
                    DEBUG_PREFIX = System.getProperty("jogamp.debug.ProcAddressHelper.prefix");
                }
                return null;
            }
        });
    }

    public ProcAddressTable() {
        this(new One2OneResolver());
    }

    public ProcAddressTable(FunctionAddressResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Returns the address for the given function name.
     * @throws RuntimeException if the Table has not been initialized yet.
     */
    public abstract long getAddressFor(String functionName);


    public void reset(DynamicLookupHelper lookup) throws RuntimeException {
        if(null==lookup) {
            throw new RuntimeException("Passed null DynamicLookupHelper");
        }

        Class tableClass = getClass();
        Field[] fields = tableClass.getFields();
        PrintStream dout = getDebugOutStream();

        if (DEBUG) {
            dout.println("ProcAddressTable.reset(" + getClass().getName() + ")");
        }
        for (int i = 0; i < fields.length; ++i) {
            String addressFieldName = fields[i].getName();
            if (!addressFieldName.startsWith(PROCADDRESS_VAR_PREFIX)) {
                // not a proc address variable
                continue;
            }
            int startOfMethodName = PROCADDRESS_VAR_PREFIX.length();
            String funcName = addressFieldName.substring(startOfMethodName);
            try {
                Field addressField = fields[i];
                assert (addressField.getType() == Long.TYPE);

                long newProcAddress = resolver.resolve(funcName, lookup);

                // set the current value of the proc address variable in the table object
                addressField.setLong(this, newProcAddress);
                if (DEBUG) {
                    dout.println("  " + addressField.getName() + " -> 0x" + Long.toHexString(newProcAddress));
                }
            } catch (Exception e) {
                throw new RuntimeException("Can not get proc address for method \""
                        + funcName + "\": Couldn't set value of field \"" + addressFieldName
                        + "\" in class " + tableClass.getName(), e);
            }
        }
        if (DEBUG) {
            dout.flush();
            if (DEBUG_PREFIX != null) {
                dout.close();
            }
        }
    }

    protected static PrintStream getDebugOutStream() {
        PrintStream out = null;
        if (DEBUG) {
            if (DEBUG_PREFIX != null) {
                try {
                    out = new PrintStream(new BufferedOutputStream(new FileOutputStream(DEBUG_PREFIX + File.separatorChar
                            + "procaddresstable-" + (++debugNum) + ".txt")));
                } catch (IOException e) {
                    e.printStackTrace();
                    out = System.err;
                }
            } else {
                out = System.err;
            }
        }
        return out;
    }


    private static class One2OneResolver implements FunctionAddressResolver {
        public long resolve(String name, DynamicLookupHelper lookup) {
            return lookup.dynamicLookupFunction(name);
        }
    }


}
