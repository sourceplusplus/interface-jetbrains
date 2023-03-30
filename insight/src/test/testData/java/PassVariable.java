public class PassVariable {
    public void literalPass() {
        doSleep(true);
    }

    private void doSleep(boolean sleep) {
        if (sleep) {
            Thread.sleep(200)
        }
    }

    public void literalPass2() {
        doSleep2(true);
    }

    private void doSleep2(boolean sleep) {
        doSleep(sleep);
    }

    public void literalPass3() {
        literalPass2();
    }

    public void literalPass4() {
        doSleep4(false);
    }

    private void doSleep4(boolean sleep) {
        if (sleep) {
        }
        Thread.sleep(200);
    }

    private void literalPass5() {
        doSleep5(false);
    }

    private void doSleep5(boolean sleep) {
        if (sleep) {
            Thread.sleep(400);
        }
        Thread.sleep(200);
    }

    private void literalPass6() {
        doSleep(false);
    }

    private void literalPass7() {
        literalPass7_1(false);
    }

    private void literalPass7_1(boolean sleep) {
        literalPass7_2(sleep);
    }

    private void literalPass7_2(boolean sleep) {
        if (sleep) {
            Thread.sleep(100);
            Thread.sleep(100);
        }
        Thread.sleep(100);
    }
}