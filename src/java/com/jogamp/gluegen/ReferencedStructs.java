/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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

package com.jogamp.gluegen;

import java.util.*;
import com.jogamp.gluegen.cgram.types.*;

public class ReferencedStructs implements TypeVisitor {

    private final Map<String, Type> resultMap = new HashMap<String, Type>();
    private final Set<CompoundType> layoutSet = new HashSet<CompoundType>();
    private final Set<Type> skip = new HashSet<Type>();

    public void clear() {
        resultMap.clear();
    }

    public Iterator<Type> results() {
        return resultMap.values().iterator();
    }
    public Iterator<CompoundType> layouts() {
        return layoutSet.iterator();
    }

    @Override
    public void visitType(final Type t) {
        if( skip.contains(t) ) {
            return;
        }
        if ( t.isPointer() ) {
            final PointerType p = t.asPointer();
            final CompoundType c = p.getTargetType().asCompound();
            if( p.isTypedef() && null != c ) {
                // If containing pointer is typedef, use it (preferred)
                skip.add(c); // earmark to skip the compound!
                resultMap.put(c.getName(), p);
                layoutSet.add(c);
            } else {
                // .. otherwise skip pointer and use followup compound
            }
        } else if( t.isCompound() ) {
            // Use compound if not yet mapped, e.g. by typedef'ed (preferred)
            if( !resultMap.containsKey(t.getName()) ) {
                resultMap.put(t.getName(), t);
            }
            layoutSet.add(t.asCompound()); // always: could be const/volatile variants ..
        }
    }
}
