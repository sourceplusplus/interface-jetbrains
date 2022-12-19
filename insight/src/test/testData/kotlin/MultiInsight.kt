public class MultiInsight {
    fun probabilityAndDuration() {
        if (Math.random() > 0.5) { //sleep 100ms
            if (Math.random() > 0.5) { //sleep 100ms
                println(false) //sleep 200ms
            }
        } else {
            println(true) //sleep 200ms
        }
    }
}