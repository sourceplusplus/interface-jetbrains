class FieldAndFunction {
    def foo = 42

    def add(int x, int y) {
        def result = x + y
        println "Adding $x and $y gives $result"
        return result
    }
}