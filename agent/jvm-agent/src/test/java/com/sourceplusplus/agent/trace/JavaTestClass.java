package com.sourceplusplus.agent.trace;

/**
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.1
 * @since 0.1.0
 */
public class JavaTestClass {

    static void staticMethod() {
        System.out.println("invoked staticMethod()");
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
    }
}
