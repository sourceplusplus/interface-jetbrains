class SequentialMethodCalls {
    fun oneCall() {
        duration500ms()
    }

    fun twoCalls() {
        duration500ms()
        duration500ms()
    }

    fun duration500ms() {}
}