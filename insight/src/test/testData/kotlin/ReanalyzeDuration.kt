class ReanalyzeDuration {
    fun code1() {
        code2()
    }

    fun code2() {
        Thread.sleep(200)
    }
}