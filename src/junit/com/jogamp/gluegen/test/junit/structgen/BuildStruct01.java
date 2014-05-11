package com.jogamp.gluegen.test.junit.structgen;

import com.jogamp.gluegen.structgen.CStruct;

/**
 * Class simply triggering CStruct annotation processor to generate the types 'RenderingConfig' etc.
 * <p>
 * Due to Java8 issues, see Bug 923,
 * using {@link package-info} is more elegant to kick-off the annotation processor.
 * </p>
 */
public class BuildStruct01 {

    // APT is only triggered for fields,
    // hence we use unused fields in this unit test!

    // @CStruct(name="RenderingConfig", header="TestStruct01.h")
    // MyRenderingConfig config;

    // @CStruct(header="TestStruct01.h")
    // MyRenderingConfig config;

    /**
     * Java8: We cannot use type 'RenderingConfig' yet (bug?) even if not compiling.
     * Hence we force the type-name via 'jname' and use a dummy variable!
     */
    @CStruct(name="RenderingConfig", jname="RenderingConfig", header="TestStruct01.h")
    boolean dummy1;

    @CStruct(name="Pixel", jname="Pixel", header="TestStruct02.h")
    boolean dummy2;

    public static void initSingleton() {}
}
