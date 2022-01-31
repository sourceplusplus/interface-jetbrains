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
package spp.jetbrains.marker.jvm.psi.sqlsource

import com.intellij.psi.impl.compiled.ClsMethodImpl
import com.intellij.psi.util.PsiUtil
import spp.jetbrains.marker.jvm.psi.PluginSqlProducerSearch
import spp.jetbrains.marker.jvm.psi.PluginSqlProducerSearch.CalledMethod
import org.jooq.Query

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SpringDataSqlSource : PluginSqlProducerSearch.SqlSourceDeterminer {

    private val findAll = "org.springframework.data.repository.CrudRepository.findAll"

    override fun isSqlSource(query: Query, calledMethod: CalledMethod): Boolean {
        if (calledMethod.method is ClsMethodImpl) {
            if (PsiUtil.getMemberQualifiedName(calledMethod.method) == findAll) {
                //todo: more strict verification
                //java.lang.Iterable<qualifiedClassName>
                val iterableType = calledMethod.call.returnType!!.canonicalText
                val queryType = iterableType.substring(0, iterableType.length - 1)
                    .replace("java.lang.Iterable<", "")
                var tableName = queryType
                if (tableName.contains(".")) {
                    tableName = tableName.substring(tableName.lastIndexOf(".") + 1).toUpperCase()
                }
                val tableNames = query.tableNames()
                if (tableNames.size == 1) {
                    return tableNames[0] == tableName
                }
                return false
            } else {
                return false
            }
        } else {
            return false
        }
    }

    //todo: remove when jooq offers public access to parsed queries
    private fun Query.tableNames(): List<String> {
        val fromField = javaClass.getDeclaredField("from")
        fromField.isAccessible = true
        val fromTables = fromField.get(this) as List<*>
        return fromTables.map {
            val aliasField = it!!.javaClass.getDeclaredField("alias")
            aliasField.isAccessible = true
            val alias = aliasField.get(it)
            val wrappedField = alias.javaClass.getDeclaredField("wrapped")
            wrappedField.isAccessible = true
            val wrapped = wrappedField.get(alias)
            wrapped.toString()
        }
    }
}
