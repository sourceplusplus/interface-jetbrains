public class DeadCodeDuration {
    fun code1() {
        if (false) {
            Thread.sleep(200)
        }
        Thread.sleep(200)
    }

    fun code2() {
        if (false) {
            Thread.sleep(200)
        } else {
            Thread.sleep(200)
            Thread.sleep(200)
        }
        Thread.sleep(200)
    }
}