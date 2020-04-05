package test.integration.trace

class TraceTest {

    static void threeStaticMethodCallDepth() {
        firstMethod()
    }

    private static void firstMethod() {
        secondMethod()
    }

    private static secondMethod() {
        thirdMethod()
    }

    private static void thirdMethod() {
        Thread.sleep(500)
    }
}
