public class CountingLoop {
    public void countingLoop() {
        for (int i = 0; i < 10; i++) {
            Thread.sleep(100);
        }
        for (int i = 0; i < 10; i++) ;
    }
}