package com.jogamp.gluegen.test.junit.structgen;

import com.jogamp.gluegen.structgen.CStruct;
import com.jogamp.junit.util.JunitTracer;

import org.junit.Assert;
import org.junit.Test;

public class TestStructGen01 extends JunitTracer {

    // APT is only triggered for fields,
    // hence we use unused fields in this unit test!
    
    // @CStruct(name="RenderingConfig", header="TestStruct01.h")
    // MyRenderingConfig config;
    
    @CStruct(header="TestStruct01.h")
    RenderingConfig config0;
    
    @Test
    public void test01() {
        RenderingConfig config = RenderingConfig.create();
        Vec3f dir = config.getCamera().getDir();
        dir.setX(0.5f);
        dir.setY(0.6f);
        dir.setZ(0.7f);
        Vec3f dir2 = Vec3f.create(dir.getBuffer());
        Assert.assertEquals(dir.getX(), dir2.getX(), 0.0001f);
        Assert.assertEquals(dir.getY(), dir2.getY(), 0.0001f);
        Assert.assertEquals(dir.getZ(), dir2.getZ(), 0.0001f);
    }

    @Test
    public void test02() {
        Camera cam = Camera.create();
        Vec3f cam_dir = cam.getDir();
        Vec3f cam_orig = cam.getOrig();
        cam_dir.setX(1);
        cam_dir.setY(2);
        cam_dir.setZ(3);
        cam_orig.setX(0);
        cam_orig.setY(1);
        cam_orig.setZ(2);
    }
    
    public static void main(String args[]) {
        String tstname = TestStructGen01.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
    
}
