package com.jogamp.gluegen.test.junit.structgen;

import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestStructGen01 extends SingletonJunitCase {

    @BeforeClass
    public static void init() {
        // Enforce dependency,
        // i.e. CStruct annotation processor to generate the types 'RenderingConfig' etc.
        BuildStruct01.initSingleton();
    }

    @Test
    public void test01() {
        final RenderingConfig config = RenderingConfig.create();
        final Vec3f dir = config.getCamera().getDir();
        dir.setX(0.5f);
        dir.setY(0.6f);
        dir.setZ(0.7f);
        final Vec3f dir2 = Vec3f.create(dir.getBuffer());
        Assert.assertEquals(dir.getX(), dir2.getX(), 0.0001f);
        Assert.assertEquals(dir.getY(), dir2.getY(), 0.0001f);
        Assert.assertEquals(dir.getZ(), dir2.getZ(), 0.0001f);
    }

    @Test
    public void test02() {
        final Camera cam = Camera.create();
        final Vec3f cam_dir = cam.getDir();
        final Vec3f cam_orig = cam.getOrig();
        cam_dir.setX(1);
        cam_dir.setY(2);
        cam_dir.setZ(3);
        cam_orig.setX(0);
        cam_orig.setY(1);
        cam_orig.setZ(2);
    }

    public static void main(final String args[]) {
        final String tstname = TestStructGen01.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
