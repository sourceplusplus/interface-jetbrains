public class MultiInsight {
    public void probabilityAndDuration() {
        if (java.lang.Math.random() > 0.5) { //sleep 100ms
            if (java.lang.Math.random() > 0.5) { //sleep 100ms
                Thread.sleep(200);
            }
        } else {
            Thread.sleep(200);
        }
    }
}