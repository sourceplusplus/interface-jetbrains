package kotlinx.coroutines

import kotlin.coroutines.CoroutineContext

fun handleCoroutineExceptionImpl(context: CoroutineContext, exception: Throwable) {
    exception.printStackTrace();
}