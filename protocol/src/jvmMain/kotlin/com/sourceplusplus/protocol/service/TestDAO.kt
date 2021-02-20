package com.sourceplusplus.protocol.service

import io.vertx.codegen.annotations.DataObject
import io.vertx.core.json.JsonObject

@DataObject(generateConverter = true)
data class TestDAO(
    var subject: String? = null
) {
    constructor(jsonObject: JsonObject) : this() {
        TestDAOConverter.fromJson(jsonObject, this)
    }

    fun toJson(): JsonObject {
        val json = JsonObject()
        TestDAOConverter.toJson(this, json)
        return json
    }
}
