class PassVariable {
    fun literalPass() {
        doSleep(true)
    }

    private fun doSleep(sleep: Boolean) {
        if (sleep) {
            Thread.sleep(200)
        }
    }
}