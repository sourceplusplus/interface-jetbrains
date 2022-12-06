private class CallerFunctions {
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