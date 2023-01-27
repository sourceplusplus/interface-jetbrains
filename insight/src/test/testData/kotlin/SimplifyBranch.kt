public class SimplifyBranch {
    fun simplifyBranch() {
        if (true) {
            println(true) //sleep 100ms
        }
        println(false) //sleep 100ms
    }

    fun simplifyBranch2() {
        println(true) //sleep 100ms
        if (true) {
            println(false) //sleep 100ms
        }
        println(true) //sleep 100ms
    }
}