class PassVariable {
    fun literalPass() {
        doSleep(true)
    }

    private fun doSleep(sleep: Boolean) {
        if (sleep) {
            Thread.sleep(200)
        }
    }

    fun literalPass2() {
        doSleep2(true);
    }

    private fun doSleep2(sleep: Boolean) {
        doSleep(sleep);
    }

    fun literalPass3() {
        literalPass2();
    }

    fun literalPass4() {
        doSleep4(false)
    }

    private fun doSleep4(sleep: Boolean) {
        if (sleep) {
        }
        Thread.sleep(200)
    }

    fun literalPass5() {
        doSleep5(false)
    }

    private fun doSleep5(sleep: Boolean) {
        if (sleep) {
            Thread.sleep(400)
        }
        Thread.sleep(200)
    }

    fun literalPass6() {
        doSleep(false)
    }
}