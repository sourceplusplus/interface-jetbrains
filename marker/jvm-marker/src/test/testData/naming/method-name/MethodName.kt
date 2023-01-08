class MethodName {
    fun foo1() {
    }

    fun foo2(string: String) {
    }

    fun foo3(
        string: String,
        int: Int,
        long: Long,
        double: Double,
        float: Float,
        boolean: Boolean,
        char: Char,
        byte: Byte,
        short: Short
    ) {
    }

    fun foo4(myObject: MyObject) {
    }

    class MyObject {}

    fun foo5(
        strings: Array<String>,
        ints: IntArray,
        longs: LongArray,
        doubles: DoubleArray,
        floats: FloatArray,
        booleans: BooleanArray,
        chars: CharArray,
        bytes: ByteArray,
        shorts: ShortArray
    ) {
    }
}