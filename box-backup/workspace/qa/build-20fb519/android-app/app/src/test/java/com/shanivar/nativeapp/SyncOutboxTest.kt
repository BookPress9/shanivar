package com.shanivar.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncOutboxTest {
    @Test
    fun queueIsIdempotentPersistedAndDrainsInOrder() {
        val store = MemDraftStore()
        val box = SyncOutbox(store)
        box.enqueue("attempt", "a1", "x"); box.enqueue("self_report", "s1", "y"); box.enqueue("attempt", "a1", "x2")
        assertEquals(listOf("self_report:s1", "attempt:a1"), box.pending.map { it.key })
        val reloaded = SyncOutbox(store)
        assertEquals("x2", reloaded.pending.last().payload)
        var calls = 0
        assertEquals(1, reloaded.drain { calls++; calls == 1 })
        assertEquals(listOf("attempt:a1"), SyncOutbox(store).pending.map { it.key })
        assertEquals(1, SyncOutbox(store).drain { true })
        assertEquals(0, SyncOutbox(store).pending.size)
    }
}
