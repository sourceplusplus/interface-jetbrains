public class MultiInsight {
    public void probabilityAndDuration() {
        if (java.lang.Math.random() > 0.5) { //sleep 100ms
            if (java.lang.Math.random() > 0.5) { //sleep 100ms
                System.out.println(false); //sleep 200ms
            }
        } else {
            System.out.println(true); //sleep 200ms
        }
    }
}