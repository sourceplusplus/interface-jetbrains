public class SimplifyBranch {
    fun simplifyBranch() {
        if (true) {
            Thread.sleep(100)
        }
        Thread.sleep(100)
    }

    fun simplifyBranch2() {
        Thread.sleep(100)
        if (true) {
            Thread.sleep(100)
        }
        Thread.sleep(100)
    }
}