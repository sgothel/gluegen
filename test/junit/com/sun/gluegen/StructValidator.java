package com.sun.gluegen;

import java.lang.reflect.InvocationTargetException;
import org.junit.Ignore;

import static org.junit.Assert.*;

/**
 * this file will not compile unless {@link com.sun.gluegen.StructAccessorTest} has been run.
 * @author Michael Bien
 */
@Ignore
public class StructValidator {

    // invoked via reflection from StructAccessorTest1
    public static void validate() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {

        System.out.println("validating struct accessors...");

        float[] mu = new float[] {1, 2, 3, 4};
        float[] light = new float[] {5, 6, 7};
        int fastRendering = 1;
        int shadow = 42;
        int iterations = 512;
        int sss = 12;
        float epsilon = (float) Math.PI;
        int height = 640;
        int width = 480;

        structtest.RenderingConfig config = structtest.RenderingConfig.create();

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

        structtest.Camera camera = config.getCamera();
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

    private static final void assertArrayEquals(float[] a, float[] b) {
        for (int i = 0; i < b.length; i++) {
            assertEquals(a[i], b[i], 0.0001f);
        }
    }

}
