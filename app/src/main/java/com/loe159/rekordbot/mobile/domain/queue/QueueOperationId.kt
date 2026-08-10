package com.loe159.rekordbot.mobile.domain.queue

import java.util.UUID

fun interface QueueOperationIdFactory {
    fun create(): String
}

object UuidQueueOperationIdFactory : QueueOperationIdFactory {
    override fun create(): String = UUID.randomUUID().toString()
}
