public class BranchProbability {
    fun oneFourthProbability() {
        if (Math.random() > 0.5) {
            if (Math.random() > 0.5) {
                println(true)
            }
        }
    }
}