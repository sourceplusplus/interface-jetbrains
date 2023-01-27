public class UnbalancedBranchProbability {
    public void unbalancedBranchProbability() {
        if (java.lang.Math.random() > 0.75) {
            System.out.println(true); //25% probability
        } else {
            System.out.println(false); //75% probability
        }
    }
}