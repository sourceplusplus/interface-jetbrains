public class FieldAndFunction {
    int foo = 42;

    public int add(int x, int y) {
        int result = x + y;
        System.out.println("Adding " + x + " and " + y + " gives " + result);
        return result;
    }
}