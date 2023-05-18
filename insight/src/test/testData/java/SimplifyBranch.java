public class SimplifyBranch {
    public void simplifyBranch() {
        if (true) {
            Thread.sleep(100);
        }
        Thread.sleep(100);
    }

    public void simplifyBranch2() {
        Thread.sleep(100);
        if (true) {
            Thread.sleep(100);
        }
        Thread.sleep(100);
    }
}