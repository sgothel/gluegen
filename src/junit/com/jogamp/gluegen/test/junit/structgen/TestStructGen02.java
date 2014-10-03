package com.jogamp.gluegen.test.junit.structgen;

import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestStructGen02 extends SingletonJunitCase {

    @BeforeClass
    public static void init() {
        // Enforce dependency,
        // i.e. CStruct annotation processor to generate the types 'RenderingConfig' etc.
        BuildStruct01.initSingleton();
    }

    @Test
    public void test01() {
        final Pixel pixel = Pixel.create();
        final Col4f color = pixel.getColor();
        color.setR(1f);
        color.setG(2f);
        color.setB(3f);
        color.setA(4f);
        final Vec3f pos = pixel.getPos();
        pos.setX(0.5f);
        pos.setY(0.6f);
        pos.setZ(0.7f);

        final Pixel pixel2 = Pixel.create(pixel.getBuffer());
        final Col4f color2 = pixel2.getColor();
        Assert.assertEquals(color.getR(), color2.getR(), 0.0001f);
        Assert.assertEquals(color.getG(), color2.getG(), 0.0001f);
        Assert.assertEquals(color.getB(), color2.getB(), 0.0001f);
        Assert.assertEquals(color.getA(), color2.getA(), 0.0001f);
        final Vec3f pos2 = pixel2.getPos();
        Assert.assertEquals(pos.getX(), pos2.getX(), 0.0001f);
        Assert.assertEquals(pos.getY(), pos2.getY(), 0.0001f);
        Assert.assertEquals(pos.getZ(), pos2.getZ(), 0.0001f);
    }

    public static void main(final String args[]) {
        final String tstname = TestStructGen02.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
