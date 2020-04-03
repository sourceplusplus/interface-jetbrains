package com.sourceplusplus.agent.trace;

/**
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.5
 * @since 0.1.0
 */
public class JavaTestClass {

    static void staticMethod() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignore) {
        }
    }
}
