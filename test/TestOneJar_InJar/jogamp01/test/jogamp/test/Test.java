/*
 * Copyright (c) 2004-2010, P. Simon Tuffs (simon@simontuffs.com)
 * All rights reserved.
 *
 * See the full license at http://one-jar.sourceforge.net/one-jar-license.html
 * This license is also included in the distributions of this software
 * under doc/one-jar-license.txt
 */
package jogamp.test;

import com.simontuffs.onejar.test.Testable;

public class Test extends Testable {
    
    public static void main(String args[]) throws Exception {
        Test test = new Test();
        test.runTests();
    }
    
    // Test other aspects of the application at unit level (e.g. library
    // methods).
    public void testJogamp011() {
        System.out.println("testJogamp011: OK");
    }
    public void testJogamp012() {
        System.out.println("testJogamp012: OK");
    }
    public void testJogamp013() {
        System.out.println("testJogamp013: OK");
    }
    
}
