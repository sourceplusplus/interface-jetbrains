package com.sourceplusplus.core.api.trace

class TesterClass {

    static void staticMethod() {
        try {
            Thread.sleep(500)
        } catch (InterruptedException ignore) {
        }
    }
}
