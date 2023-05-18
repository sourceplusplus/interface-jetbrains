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
package spp.jetbrains.view.window.util

import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import spp.jetbrains.view.ResumableView
import spp.jetbrains.view.ResumableViewCollection
import javax.swing.JComponent

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class TabbedResumableView : ResumableViewCollection() {

    val component: JBTabbedPane = JBTabbedPane()

    init {
        component.tabComponentInsets = JBUI.emptyInsets()
    }

    fun addTab(title: String, view: ResumableView, viewComponent: JComponent) {
        component.insertTab(title, null, viewComponent, null, size)
        addView(view)
    }
}
