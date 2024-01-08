/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.view.trace.column

import com.intellij.util.ui.ColumnInfo

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PairColumnInfo(name: String, private val key: Boolean) : ColumnInfo<Pair<String, Any?>, String>(name) {

    override fun getComparator(): Comparator<Pair<String, Any?>> {
        return Comparator { o1, o2 ->
            if (key) {
                o1.first.compareTo(o2.first)
            } else {
                o1.second.toString().compareTo(o2.second.toString())
            }
        }
    }

    override fun valueOf(item: Pair<String, Any?>): String {
        return if (key) {
            item.first
        } else {
            item.second.toString()
        }
    }
}
