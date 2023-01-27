public class UnbalancedBranchProbability {
    fun unbalancedBranchProbability() {
        if (Math.random() > 0.75) {
            println(true) //25% probability
        } else {
            println(false) //75% probability
        }
    }
}