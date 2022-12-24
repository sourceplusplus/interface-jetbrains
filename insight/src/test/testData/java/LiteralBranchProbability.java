public class LiteralBranchProbability {
    public void booleanConstant() {
        if (true) {
            System.out.println(true);
        } else {
            System.out.println(false);
        }
    }

    public void numberCompare() {
        if (1 == 1) {
            System.out.println(true);
        } else {
            System.out.println(false);
        }
    }
}