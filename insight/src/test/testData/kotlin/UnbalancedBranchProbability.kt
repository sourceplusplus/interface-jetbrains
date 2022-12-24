public class UnbalancedBranchProbability {
    fun unbalancedBranchProbability() {
        if (Math.random() > 0.75) {
            println(true) //75% probability
        } else {
            println(false) //25% probability
        }
    }
}