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
package spp.jetbrains.monitor.skywalking.model

import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.trace.TraceOrderType

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class GetEndpointTraces(
    val artifactQualifiedName: ArtifactQualifiedName,
    val serviceId: String? = null,
    val serviceInstanceId: String? = null,
    val endpointId: String? = null,
    val endpointName: String? = null,
    val zonedDuration: ZonedDuration,
    val orderType: TraceOrderType = TraceOrderType.LATEST_TRACES,
    val pageNumber: Int = 1,
    val pageSize: Int = 10
)
