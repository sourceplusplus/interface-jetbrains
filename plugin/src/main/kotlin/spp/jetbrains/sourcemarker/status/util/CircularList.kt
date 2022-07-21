/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.sourcemarker.status.util

class CircularList<E>(private val maxCapacity: Int) : ArrayList<E>() {

    override fun add(element: E): Boolean {
        if (size == maxCapacity) {
            removeLast()
        }
        return super.add(element)
    }

    override fun add(index: Int, element: E) {
        if (size == maxCapacity) {
            removeLast()
        }
        super.add(index, element)
    }
}
