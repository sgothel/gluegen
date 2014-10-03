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

package com.jogamp.common.util;

import java.util.*;
import java.io.IOException;

import org.junit.Test;

import com.jogamp.common.os.Platform;
import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestIteratorIndexCORE extends SingletonJunitCase {

    static int elems = 10;
    static int loop = ( Platform.getCPUFamily() == Platform.CPUFamily.ARM ) ? 20 : 9999999;

    public void populate(final List l, int len) {
        while(len>0) {
            l.add(new Integer(len--));
        }
    }

    @Test
    public void test01ArrayListIterator() {
        int sum=0;
        final ArrayList l = new ArrayList();
        populate(l, elems);

        for(int j=loop; j>0; j--) {
            for(final Iterator iter = l.iterator(); iter.hasNext(); ) {
                final Integer i = (Integer)iter.next();
                sum+=i.intValue();
            }
        }
        System.err.println("test01-arraylist-iterator sum: "+sum);
    }

    @Test
    public void test0ArrayListIndex() {
        int sum=0;
        final ArrayList l = new ArrayList();
        populate(l, elems);

        for(int j=loop; j>0; j--) {
            for(int k = 0; k < l.size(); k++) {
                final Integer i = (Integer)l.get(k);
                sum+=i.intValue();
            }
        }
        System.err.println("test01-arraylist-index sum: "+sum);
    }

    @Test
    public void test01LinkedListListIterator() {
        int sum=0;
        final LinkedList l = new LinkedList();
        populate(l, elems);

        for(int j=loop; j>0; j--) {
            for(final Iterator iter = l.iterator(); iter.hasNext(); ) {
                final Integer i = (Integer)iter.next();
                sum+=i.intValue();
            }
        }
        System.err.println("test01-linkedlist-iterator sum: "+sum);
    }

    @Test
    public void test01LinkedListListIndex() {
        int sum=0;
        final LinkedList l = new LinkedList();
        populate(l, elems);

        for(int j=loop; j>0; j--) {
            for(int k = 0; k < l.size(); k++) {
                final Integer i = (Integer)l.get(k);
                sum+=i.intValue();
            }
        }
        System.err.println("test01-linkedlist-index sum: "+sum);
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestIteratorIndexCORE.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
