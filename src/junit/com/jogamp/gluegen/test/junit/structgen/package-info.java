/**
 * Package scope generation of {@link CStruct}s
 * avoiding Java8 issues w/ annotation processing
 * where the generated class is not yet available.
 * <p>
 * See Bug 923.
 * </p>
 * @see BuildStruct01
 */
@CStructs({@CStruct(name="RenderingConfig", header="TestStruct01.h"), @CStruct(name="Pixel", header="TestStruct02.h")})
package com.jogamp.gluegen.test.junit.structgen;

import com.jogamp.gluegen.structgen.CStructs;
import com.jogamp.gluegen.structgen.CStruct;

