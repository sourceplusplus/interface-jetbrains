public class InnerBranchProbability {
    public void booleanConstant() {
        System.out.println(true);

        if (false) {
            if (true) {
                System.out.println(false);
            }
        }
    }
}