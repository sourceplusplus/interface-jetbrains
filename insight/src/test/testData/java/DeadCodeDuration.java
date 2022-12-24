public class DeadCodeDuration {
    public void code1() {
        if (false) {
            System.out.println(true); //sleep 200ms
        }
        System.out.println(false); //sleep 200ms
    }

    public void code2() {
        if (false) {
            System.out.println(true); //sleep 200ms
        } else {
            System.out.println(false); //sleep 200ms
            System.out.println(false); //sleep 200ms
        }
        System.out.println(false); //sleep 200ms
    }
}