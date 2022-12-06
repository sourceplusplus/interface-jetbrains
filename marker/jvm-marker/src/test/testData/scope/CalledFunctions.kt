private class CalledFunctions {
    private fun callerFunction() {
        directCalledFunction()
    }

    private fun directCalledFunction() {
        indirectCalledFunction()
    }

    private fun indirectCalledFunction() {
        println(true)
    }
}