package spp.jetbrains.sourcemarker.status.util

import java.util.ArrayList

class CircularList<E>(val maxCapacity: Int) : ArrayList<E>() {

    override fun add(element: E): Boolean {
        if(size == maxCapacity) {
            removeLast()
        }
        return super.add(element)
    }

    override fun add(index: Int, element: E) {
        if(size == maxCapacity) {
            removeLast()
        }
        super.add(index, element)
    }
}