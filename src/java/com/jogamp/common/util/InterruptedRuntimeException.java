/**
 * Copyright 2015 JogAmp Community. All rights reserved.
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

package com.jogamp.common.util;

import com.jogamp.common.JogampRuntimeException;

/**
 * <i>Unchecked exception</i> propagating an {@link InterruptedException}
 * where handling of the latter is not desired.
 * <p>
 * {@link InterruptedRuntimeException} may be thrown either by waiting for any {@link Runnable}
 * to be completed, or during its execution.
 * </p>
 * <p>
 * The propagated {@link InterruptedException} may be of type {@link SourcedInterruptedException}.
 * </p>
 * <p>
 * </p>
 */
@SuppressWarnings("serial")
public class InterruptedRuntimeException extends JogampRuntimeException {

  /**
   * Constructor attempts to {@link SourcedInterruptedException#wrap(InterruptedException) wrap}
   * the given {@link InterruptedException} {@code cause} into a {@link SourcedInterruptedException}.
   *
   * @param message the message of this exception
   * @param cause the propagated {@link InterruptedException}
   */
  public InterruptedRuntimeException(final String message, final InterruptedException cause) {
    super(message, SourcedInterruptedException.wrap(cause));
  }

  /**
   * Constructor attempts to {@link SourcedInterruptedException#wrap(InterruptedException) wrap}
   * the given {@link InterruptedException} {@code cause} into a {@link SourcedInterruptedException}.
   *
   * @param cause the propagated {@link InterruptedException}
   */
  public InterruptedRuntimeException(final InterruptedException cause) {
    super(SourcedInterruptedException.wrap(cause));
  }

  /**
   * Returns the propagated {@link InterruptedException}, i.e. the cause of this exception.
   * <p>
   * {@inheritDoc}
   * </p>
   */
  @Override
  public InterruptedException getCause() {
      return (InterruptedException)super.getCause();
  }
}
