/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
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
package spp.jetbrains.marker.source.mark.api.event

import spp.jetbrains.marker.source.mark.api.SourceMark

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
open class SourceMarkEvent {

    val sourceMark: SourceMark
    val eventCode: IEventCode
    val params: Array<out Any?>

    constructor(sourceMark: SourceMark, eventCode: IEventCode, params: List<Any?>) {
        this.sourceMark = sourceMark
        this.eventCode = eventCode
        this.params = params.toTypedArray()
    }

    constructor(sourceMark: SourceMark, eventCode: IEventCode, vararg params: Any?) :
            this(sourceMark, eventCode, params.toList())

    override fun toString(): String {
        return if (params.isEmpty()) {
            "Event: $eventCode - Source: $sourceMark"
        } else {
            "Event: $eventCode - Source: $sourceMark - Params: $params"
        }
    }
}
