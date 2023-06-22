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
package spp.jetbrains

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.util.ExceptionUtil
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.*

@Suppress("ThrowingExceptionsWithoutMessageOrCause")
object ScopeExtensions {

    private val log = logger<ScopeExtensions>()

    fun <T> safeRunBlocking(action: suspend () -> T): T {
        val source = Exception()
        return runBlocking {
            try {
                return@runBlocking action()
            } catch (throwable: Throwable) {
                log.warn(buildString {
                    append(ExceptionUtil.getThrowableText(throwable))
                    append("\n")
                    append("Source: ")
                    append(ExceptionUtil.getThrowableText(source))
                })
                throw throwable
            }
        }
    }

    fun <T> safeRunBlocking(dispatcher: CoroutineDispatcher, action: suspend () -> T): T {
        val source = Exception()
        return runBlocking(dispatcher) {
            try {
                return@runBlocking action()
            } catch (throwable: Throwable) {
                log.warn(buildString {
                    append(ExceptionUtil.getThrowableText(throwable))
                    append("\n")
                    append("Source: ")
                    append(ExceptionUtil.getThrowableText(source))
                })
                throw throwable
            }
        }
    }

    suspend fun safeExecute(source: Exception, action: suspend () -> Unit) {
        try {
            action()
        } catch (throwable: Throwable) {
            log.warn(buildString {
                append(ExceptionUtil.getThrowableText(throwable))
                append("\n")
                append("Source: ")
                append(ExceptionUtil.getThrowableText(source))
            })
        }
    }

    fun safeGlobalLaunch(action: suspend () -> Unit) {
        val source = Exception()
        GlobalScope.launch {
            safeExecute(source) {
                action()
            }
        }
    }

    fun safeGlobalAsync(
        action: suspend CoroutineScope.() -> Unit
    ): Deferred<*> {
        val source = Exception()
        return GlobalScope.async {
            safeExecute(source) {
                try {
                    action()
                } catch (ignored: CancellationException) {
                } catch (throwable: Throwable) {
                    log.warn(buildString {
                        append(ExceptionUtil.getThrowableText(throwable))
                        append("\n")
                        append("Source: ")
                        append(ExceptionUtil.getThrowableText(source))
                    })
                }
            }
        }
    }
}

fun Vertx.safeLaunch(action: suspend () -> Unit): Job {
    val source = Exception()
    return GlobalScope.launch(dispatcher()) {
        ScopeExtensions.safeExecute(source) {
            action()
        }
    }
}

fun Vertx.safeExecuteBlocking(action: suspend () -> Unit) {
//    val source = Exception()
    executeBlocking<Nothing> {
//        Thread.currentThread().name = ExceptionUtil.getThrowableText(source)
        ScopeExtensions.safeRunBlocking(dispatcher()) {
            action()
        }
        it.complete()
    }
}

fun <T> Vertx.executeBlockingReadActionWhenSmart(project: Project, computable: suspend () -> T): Future<T> {
    val promise = Promise.promise<T>()
    DumbService.getInstance(project).runWhenSmart {
        executeBlocking<Nothing> {
            ApplicationManager.getApplication().runReadAction {
                ScopeExtensions.safeRunBlocking(dispatcher()) {
                    try {
                        promise.complete(computable.invoke())
                    } catch (throwable: Throwable) {
                        promise.fail(throwable)
                    }
                    it.complete()
                }
            }
        }
    }
    return promise.future()
}

fun Project.invokeLater(action: () -> Unit) {
    ApplicationManager.getApplication().invokeLater({
        action()
    }, disposed)
}
