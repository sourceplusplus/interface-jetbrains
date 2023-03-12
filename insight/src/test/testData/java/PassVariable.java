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

    private void literalPass6() {
        doSleep(false);
    }
}