public class DeadCodeDuration {
    fun code1() {
        if (false) {
            println(true) //sleep 200ms
        }
        println(false) //sleep 200ms
    }

    fun code2() {
        if (false) {
            println(true) //sleep 200ms
        } else {
            println(false) //sleep 200ms
            println(false) //sleep 200ms
        }
        println(false) //sleep 200ms
    }
}