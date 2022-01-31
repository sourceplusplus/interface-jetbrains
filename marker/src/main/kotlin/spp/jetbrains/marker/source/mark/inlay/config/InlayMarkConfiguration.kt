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
package spp.jetbrains.marker.source.mark.inlay.config

import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.util.Ref
import spp.jetbrains.marker.source.mark.api.component.api.SourceMarkComponentProvider
import spp.jetbrains.marker.source.mark.api.component.jcef.SourceMarkJcefComponentProvider
import spp.jetbrains.marker.source.mark.api.config.SourceMarkConfiguration
import spp.jetbrains.marker.source.mark.api.filter.ApplySourceMarkFilter
import spp.jetbrains.marker.source.mark.inlay.InlayMark

/**
 * Used to configure [InlayMark]s.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class InlayMarkConfiguration(
    override var applySourceMarkFilter: ApplySourceMarkFilter = ApplySourceMarkFilter.NONE,
    var strictlyManualCreation: Boolean = false,
    var virtualText: InlayMarkVirtualText? = null,
    var showComponentInlay: Boolean = false,
    var inlayRef: Ref<Inlay<*>>? = null,
    var activateOnMouseClick: Boolean = true,
    override var activateOnKeyboardShortcut: Boolean = false, //todo: remove
    override var componentProvider: SourceMarkComponentProvider = SourceMarkJcefComponentProvider()
) : SourceMarkConfiguration
