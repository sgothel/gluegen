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
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.sun.gluegen.runtime;

/** Helper class containing constants and methods to assist with the
    manipulation of auto-generated ProcAddressTables. */

public class ProcAddressHelper {
  public static final String PROCADDRESS_VAR_PREFIX = "_addressof_";

  public static void resetProcAddressTable(Object table,
                                           DynamicLookupHelper lookup) throws RuntimeException {
    Class tableClass = table.getClass();
    java.lang.reflect.Field[] fields = tableClass.getFields();
    
    for (int i = 0; i < fields.length; ++i) {
      String addressFieldName = fields[i].getName();
      if (!addressFieldName.startsWith(ProcAddressHelper.PROCADDRESS_VAR_PREFIX)) {
        // not a proc address variable
        continue;
      }
      int startOfMethodName = ProcAddressHelper.PROCADDRESS_VAR_PREFIX.length();
      String funcName = addressFieldName.substring(startOfMethodName);
      try {
        java.lang.reflect.Field addressField = fields[i];
        assert(addressField.getType() == Long.TYPE);
        long newProcAddress = lookup.dynamicLookupFunction(funcName);
        // set the current value of the proc address variable in the table object
        addressField.setLong(table, newProcAddress); 
      } catch (Exception e) {
        throw new RuntimeException("Can not get proc address for method \"" +
                                   funcName + "\": Couldn't set value of field \"" + addressFieldName +
                                   "\" in class " + tableClass.getName(), e);
      }
    }
  }
}
