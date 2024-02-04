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

package com.jogamp.common.util;

import org.junit.Assert;

import org.junit.Test;

import static com.jogamp.common.util.ValueConv.*;

/**
 * Testing ValueConv's value conversion of primitive types
 */
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestStringUtil {

    @Test
    public void test01IndexOf() {
        Assert.assertEquals(-1, StringUtil.indexOf(null, '\n', 0));
        Assert.assertEquals(-1, StringUtil.indexOf("", '\n', 0));
        Assert.assertEquals(-1, StringUtil.indexOf("Hello", '\n', 0));
        Assert.assertEquals( 5, StringUtil.indexOf("Hello\nJogAmp\n", '\n', 0));
        Assert.assertEquals(12, StringUtil.indexOf("Hello\nJogAmp\n", '\n', 6));

        Assert.assertEquals(-1, StringUtil.indexOf((CharSequence)null, '\n', 0));
        Assert.assertEquals(-1, StringUtil.indexOf((CharSequence)"", '\n', 0));
        Assert.assertEquals(-1, StringUtil.indexOf((CharSequence)"Hello", '\n', 0));
        Assert.assertEquals( 5, StringUtil.indexOf((CharSequence)"Hello\nJogAmp\n", '\n', 0));
        Assert.assertEquals(12, StringUtil.indexOf((CharSequence)"Hello\nJogAmp\n", '\n', 6));
    }

    @Test
    public void test02LineCount() {
        Assert.assertEquals(0, StringUtil.getLineCount(null));
        Assert.assertEquals(0, StringUtil.getLineCount(""));
        Assert.assertEquals(1, StringUtil.getLineCount("Hello"));
        Assert.assertEquals(2, StringUtil.getLineCount("Hello\nJogAmp"));
        Assert.assertEquals(2, StringUtil.getLineCount("Hello\nJogAmp\n"));
        Assert.assertEquals(3, StringUtil.getLineCount("Hello\nJogAmp\nYeah."));
    }

    @Test
    public void test03Strip() {
        {
            Assert.assertEquals("", StringUtil.strip(null, null, null));
            Assert.assertEquals("", StringUtil.strip("", null, null));
            Assert.assertEquals("Hello", StringUtil.strip("Hello", null, null));
            Assert.assertEquals("Hello JogAmp", StringUtil.strip("Hello JogAmp", null, null));
            Assert.assertEquals("Hello JogAmp", StringUtil.strip(" Hello JogAmp ", null, null));
            Assert.assertEquals("Hello JogAmp", StringUtil.strip("Hello  JogAmp", null, null));
            Assert.assertEquals("Hello JogAmp", StringUtil.strip("  Hello  JogAmp  ", null, null));

            final String seperators = " \t\n\r";
            Assert.assertEquals("", StringUtil.strip(null, seperators, null));
            Assert.assertEquals("", StringUtil.strip("", seperators, null));
            Assert.assertEquals("Hello", StringUtil.strip("Hello", seperators, null));
            Assert.assertEquals("Hello JogAmp", StringUtil.strip("Hello JogAmp", seperators, null));
            Assert.assertEquals("Hello JogAmp", StringUtil.strip(" Hello JogAmp ", seperators, null));
            Assert.assertEquals("Hello JogAmp", StringUtil.strip("Hello  JogAmp", seperators, null));
            Assert.assertEquals("Hello JogAmp", StringUtil.strip("  Hello  JogAmp  ", seperators, null));

            Assert.assertEquals("Hello JogAmp", StringUtil.strip("\tHello JogAmp\n", seperators, null));
            Assert.assertEquals("Hello\tJogAmp", StringUtil.strip("Hello\t\tJogAmp", seperators, null));
            Assert.assertEquals("Hello JogAmp", StringUtil.strip("Hello \tJogAmp", seperators, null));
            Assert.assertEquals("Hello\nJogAmp", StringUtil.strip("\t Hello\n\tJogAmp \n", seperators, null));
        }
        {
            final String replacement = "Z";
            Assert.assertEquals("", StringUtil.strip(null, null, replacement));
            Assert.assertEquals("", StringUtil.strip("", null, replacement));
            Assert.assertEquals("Hello", StringUtil.strip("Hello", null, replacement));
            Assert.assertEquals("HelloZJogAmp", StringUtil.strip("Hello JogAmp", null, replacement));
            Assert.assertEquals("HelloZJogAmp", StringUtil.strip(" Hello JogAmp ", null, replacement));
            Assert.assertEquals("HelloZJogAmp", StringUtil.strip("Hello  JogAmp", null, replacement));
            Assert.assertEquals("HelloZJogAmp", StringUtil.strip("  Hello  JogAmp  ", null, replacement));

            final String IDIOSPC = String.valueOf(Character.toChars(0x3000));
            final String seperators = StringUtil.WHITESPACE;
            Assert.assertEquals("", StringUtil.strip(null, seperators, replacement));
            Assert.assertEquals("", StringUtil.strip("", seperators, replacement));
            Assert.assertEquals("Hello", StringUtil.strip("Hello", seperators, replacement));
            Assert.assertEquals("HelloZJogAmp", StringUtil.strip("Hello JogAmp", seperators, replacement));
            Assert.assertEquals("HelloZJogAmp", StringUtil.strip(" Hello"+IDIOSPC+"JogAmp ", seperators, replacement));
            Assert.assertEquals("HelloZJogAmp", StringUtil.strip("Hello  JogAmp", seperators, replacement));
            Assert.assertEquals("HelloZJogAmp", StringUtil.strip("  Hello  JogAmp  ", seperators, replacement));

            Assert.assertEquals("HelloZJogAmp", StringUtil.strip("\tHello JogAmp\n", seperators, replacement));
            Assert.assertEquals("HelloZJogAmp", StringUtil.strip("Hello\t\tJogAmp", seperators, replacement));
            Assert.assertEquals("HelloZJogAmp", StringUtil.strip("Hello \tJogAmp", seperators, replacement));
            Assert.assertEquals("HelloZJogAmp", StringUtil.strip("\t Hello\n\tJogAmp \n", seperators, replacement));
        }
    }

    @Test
    public void test04Split() {
        {
            /**
             * I like to ride my bicycle
             * I like to ride
             * my bicycle
             *
             * I like to ride my bicycle if its without any batteries
             * I like to ride my
             * bicycle if its without
             * any batteries
             */
            final String orig = "I like to ride my bicycle if its without any batteries";
            Assert.assertEquals(orig, StringUtil.split(orig, 1, null, "ZZ"));
            Assert.assertEquals("I like to ride my bicycleZZif its without any batteries", StringUtil.split(orig, 2, null, "ZZ"));
            Assert.assertEquals("I like to ride myZZbicycle if its withoutZZany batteries", StringUtil.split(orig, 3, null, "ZZ"));
            Assert.assertEquals("I like to ride myZZbicycle if its withoutZZany batteries", StringUtil.split(orig, 3, null, "ZZ"));
        }
        {
            /**
             * I like to ride my bicycle
             * I like to ride
             * my bicycle
             *
             * I like to ride my bicycle if its without any batteries
             * I like to ride my
             * bicycle if its without
             * any batteries
             */
            final String IDIOSPC = String.valueOf(Character.toChars(0x3000));
            final String orig = "I like to ride my"+IDIOSPC+"bicycle\tif its without"+IDIOSPC+"any batteries";
            Assert.assertEquals(orig, StringUtil.split(orig, 1, null, "ZZ"));
            Assert.assertEquals("I like to ride my"+IDIOSPC+"bicycleZZif its without"+IDIOSPC+"any batteries", StringUtil.split(orig, 2, StringUtil.WHITESPACE, "ZZ"));
            Assert.assertEquals("I like to ride myZZbicycle\tif its withoutZZany batteries", StringUtil.split(orig, 3, StringUtil.WHITESPACE, "ZZ"));
            Assert.assertEquals("I like to ride myZZbicycle\tif its withoutZZany batteries", StringUtil.split(orig, 3, StringUtil.WHITESPACE, "ZZ"));
        }
        {
            final String orig = "您好，我来自英国政府，给您带来了一个好消息。";
            Assert.assertTrue(StringUtil.isFullwidth(orig.codePointAt(2)));
            Assert.assertTrue(StringUtil.hasSpace(orig.codePointAt(2)));

            Assert.assertEquals(orig, StringUtil.split(orig, 1, null, "ZZ"));
            Assert.assertEquals("您好，我来自英国政府，给您带来了一个好消息。", StringUtil.split(orig, 2, null, "ZZ"));
            Assert.assertEquals("您好，我来自英国政府ZZ给您带来了一个好消息。", StringUtil.split(orig, 2, "\t ，", "ZZ")); // included fullwidth char
            final String orig2 = orig.replace("，", ", ");
            Assert.assertEquals("您好, 我来自英国政府,ZZ给您带来了一个好消息。", StringUtil.split(orig2, 2, null, "ZZ"));
        }
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestStringUtil.class.getName());
    }

}
