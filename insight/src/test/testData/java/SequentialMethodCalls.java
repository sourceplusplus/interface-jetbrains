public class SequentialMethodCalls {
    public void oneCall() {
        duration500ms();
    }

    public void twoCalls() {
        duration500ms();
        duration500ms();
    }

    public void duration500ms() {
    }
}