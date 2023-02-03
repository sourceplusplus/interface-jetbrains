public class PassVariable {
    public void literalPass() {
        doSleep(true);
    }

    private void doSleep(boolean sleep) {
        if (sleep) {
            Thread.sleep(200)
        }
    }
}