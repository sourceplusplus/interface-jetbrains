package com.sourceplusplus.portal.extensions

@JsModule("moment")
external val moment: dynamic

//todo: figure out why moment(1602072489484) doesn't equal the same when ran from KotlinJS vs JS
// but moment("1602072489484", "x) does equal the same when ran from KotlinJS vs JS
