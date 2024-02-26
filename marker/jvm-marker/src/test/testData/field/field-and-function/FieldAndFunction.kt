class FieldAndFunction {
    val foo = 42
    fun add(x: Int, y: Int): Int {
        val result = x + y
        println("Adding $x and $y gives $result")
        return result
    }
}