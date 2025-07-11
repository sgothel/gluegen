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

package com.jogamp.common.os;

import jogamp.common.Debug;

/** Interface callers may use ProcAddressHelper's
 * {@link com.jogamp.gluegen.runtime.ProcAddressTable#reset(com.jogamp.common.os.DynamicLookupHelper) reset}
 *  helper method to install function pointers into a
 *  ProcAddressTable. This must typically be written with native
 *  code. */
public interface DynamicLookupHelper {
  public static final boolean DEBUG = Debug.debug("NativeLibrary");
  public static final boolean DEBUG_LOOKUP = Debug.debug("NativeLibrary.Lookup");

  /**
   * @throws SecurityException if user is not granted access for the library set.
   */
  public void claimAllLinkPermission() throws SecurityException;
  /**
   * @throws SecurityException if user is not granted access for the library set.
   */
  public void releaseAllLinkPermission() throws SecurityException;

  /** Returns true if library is loaded and open, otherwise false. */
  public boolean isOpen();

  /**
   * Returns the function handle for function 'funcName'.
   * @throws SecurityException if user is not granted access for the library set.
   */
  public long dynamicLookupFunction(String funcName) throws SecurityException;

  /**
   * Queries whether function 'funcName' is available.
   * @throws SecurityException if user is not granted access for the library set.
   */
  public boolean isFunctionAvailable(String funcName) throws SecurityException;
}
