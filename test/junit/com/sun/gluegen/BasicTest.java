package com.sun.gluegen;

import com.sun.gluegen.runtime.BufferFactory;
import com.sun.gluegen.runtime.PointerBuffer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.AfterClass;
import org.junit.Test;
import static java.lang.System.*;
import static com.sun.gluegen.BuildUtil.*;

/**
 *
 * @author Michael Bien
 */
public class BasicTest {

    @Test
    public void generateBindingTest() {
        generate("test");
    }

    /**
     * fails if ant script fails (which is a good thing).
     * executeTarget throws RuntimeException on failure
     */
    @Test
    public void compileJavaTest() {
        compileJava();
    }

    /*
     * fails if ant script fails (which is a good thing)
     * executeTarget throws RuntimeException on failure
     */
    @Test
    public void compileNativesTest() {
        compileNatives();
    }

//    @Test
    public void bindingTest() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, InstantiationException {

        String nativesPath = gluegenRoot + "/build/test/build/natives";
        System.load(nativesPath + "/librofl.so");

        Object bindingTest = Class.forName("test.BindingTest").newInstance();

        // test values
        ByteBuffer dbb = BufferFactory.newDirectByteBuffer(32);
        ByteBuffer bb  = ByteBuffer.allocate(32).order(ByteOrder.nativeOrder());

        PointerBuffer dpb = PointerBuffer.allocateDirect(32);
        PointerBuffer pb  = PointerBuffer.allocate(32);

        long[] array = new long[] {1,2,3,4,5,6,7,8,9};
        int offset = 0;
        long id = 42;

        // invoke everything public
        Method[] methods = bindingTest.getClass().getDeclaredMethods();

        for (Method method : methods) {

            // prepare method parameters
            Class<?>[] paramTypes = method.getParameterTypes();
            Object[] paramInstances = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> paramType = paramTypes[i];
                if(paramType.isInstance(dbb)) {
                    paramInstances[i] = dbb;
                }else if(paramType.isInstance(bb)) {
                    paramInstances[i] = bb;
                }else if(paramType.isInstance(dpb)) {
                    paramInstances[i] = dpb;
                }else if(paramType.isInstance(pb)) {
                    paramInstances[i] = pb;
                }else if(paramType.isPrimitive()) { // TODO primitive types
                    paramInstances[i] = offset;
                }else if(paramType.isArray()) {     // TODO array types
                    paramInstances[i] = array;
                }
            }

            out.println("invoking: "+method);
            out.println("with params: ");
            for (Object param : paramInstances)
                out.print(param+", ");
            out.println();

            // TODO fix Exception: ...Caused by: java.lang.UnsatisfiedLinkError: test.BindingTest.arrayTest0(JLjava/lang/Object;I)I

            Object result = method.invoke(bindingTest, paramInstances);
            out.println("result: "+result);
            out.println("success");
        }

    }

    @AfterClass
    public static void tearDown() {
//        cleanGeneratedFiles();
    }

}
