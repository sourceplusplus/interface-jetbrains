public class DeadCodeDuration {
    public void code1() {
        if (false) {
            Thread.sleep(200);
        }
        Thread.sleep(200);
    }

    public void code2() {
        if (false) {
            Thread.sleep(200);
        } else {
            Thread.sleep(200);
            Thread.sleep(200);
        }
        Thread.sleep(200);
    }
}