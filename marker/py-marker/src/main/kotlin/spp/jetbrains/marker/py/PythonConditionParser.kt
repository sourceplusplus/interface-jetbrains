package spp.jetbrains.marker.py

import com.intellij.psi.PsiElement
import spp.jetbrains.marker.InstrumentConditionParser

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PythonConditionParser : InstrumentConditionParser() {

    override fun getCondition(condition: String, context: PsiElement): String {
        return condition
    }
}
