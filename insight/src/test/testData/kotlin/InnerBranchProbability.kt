class InnerBranchProbability {
    fun booleanConstant() {
        println(true)

        if (false) {
            if (true) {
                println(false)
            }
        }
    }
}