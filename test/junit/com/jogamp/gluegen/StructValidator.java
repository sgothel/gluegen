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

package com.jogamp.gluegen;

import java.lang.reflect.InvocationTargetException;
import org.junit.Ignore;

import static org.junit.Assert.*;

/**
 * this file will not compile unless {@link com.jogamp.gluegen.StructAccessorTest} has been run.
 * @author Michael Bien
 */
@Ignore
public class StructValidator {

    // invoked via reflection from StructAccessorTest1
    public static void validate() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {

        System.out.println("validating struct accessors...");

        final float[] mu = new float[] {1, 2, 3, 4};
        final float[] light = new float[] {5, 6, 7};
        final int fastRendering = 1;
        final int shadow = 42;
        final int iterations = 512;
        final int sss = 12;
        final float epsilon = (float) Math.PI;
        final int height = 640;
        final int width = 480;

        final structtest.RenderingConfig config = structtest.RenderingConfig.create();

        //set
        config.setLight(light);
        config.setMu(mu);
        config.setActvateFastRendering(fastRendering);
        config.setEnableShadow(shadow);
        config.setMaxIterations(iterations);
        config.setEpsilon(epsilon);
        config.setSuperSamplingSize(sss);
        config.setWidth(width);
        config.setHeight(height);

        final structtest.Camera camera = config.getCamera();
        camera.getOrig().setX(1001).setY(1002).setZ(1003);
        camera.getDir().setX(2001).setY(2002).setZ(2003);

        //get and validate
        assertArrayEquals(mu, config.getMu());
        assertArrayEquals(light, config.getLight());

        assertEquals(fastRendering, config.getActvateFastRendering());
        assertEquals(shadow, config.getEnableShadow());
        assertEquals(iterations, config.getMaxIterations());
        assertEquals(epsilon, config.getEpsilon(), 0.01f);
        assertEquals(sss, config.getSuperSamplingSize());
        assertEquals(width, config.getWidth());
        assertEquals(height, config.getHeight());

        assertEquals(camera.getOrig().getX(), 1001, 0.001);
        assertEquals(camera.getOrig().getY(), 1002, 0.001);
        assertEquals(camera.getOrig().getZ(), 1003, 0.001);

        assertEquals(camera.getDir().getX(), 2001, 0.001);
        assertEquals(camera.getDir().getY(), 2002, 0.001);
        assertEquals(camera.getDir().getZ(), 2003, 0.001);

        System.out.println("done");

    }

    private static final void assertArrayEquals(final float[] a, final float[] b) {
        for (int i = 0; i < b.length; i++) {
            assertEquals(a[i], b[i], 0.0001f);
        }
    }

}
