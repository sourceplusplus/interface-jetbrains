@file:Suppress("UNUSED_PARAMETER")

package com.sourceplusplus.portal.extensions

import kotlin.js.Json

val vertx: Vertx = Vertx()
val eb: Vertx.EventBus = vertx.eventBus.init()

@Suppress("unused")
class Vertx {
    private val _eventBus: EventBus = EventBus()

    val eventBus: EventBus
        get() = _eventBus

    class EventBus {
        companion object {
            @JsName("callKotlinOnOpen")
            val callKotlinOnOpen by lazy { return@lazy eb.onopen::invoke }
        }

        @JsName("kotlinEventBusOnOpen")
        var onopen: () -> Unit = { }

        fun init(): EventBus {
            js(
                """
                    eb.onopen = function () { 
                        portal.com.sourceplusplus.portal.extensions.Vertx.EventBus.Companion.callKotlinOnOpen(); 
                    }
                """
            )
            return this
        }

        fun send(address: String, json: Json) {
            js("eb.send(address, json);")
        }

        fun publish(address: String, json: Json) {
            js("eb.publish(address, json);")
        }

        fun registerHandler(address: String, handler: (error: String, message: Any) -> Unit) {
            js("eb.registerHandler(address, handler);")
        }
    }
}
