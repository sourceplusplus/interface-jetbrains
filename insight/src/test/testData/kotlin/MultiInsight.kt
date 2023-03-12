public class MultiInsight {
    fun probabilityAndDuration() {
        if (Math.random() > 0.5) { //sleep 100ms
            if (Math.random() > 0.5) { //sleep 100ms
                Thread.sleep(200)
            }
        } else {
            Thread.sleep(200)
        }
    }
}