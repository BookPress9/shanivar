package com.shanivar.nativeapp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SankalpStoreTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Before fun clean() {
        context.getSharedPreferences(SankalpStore.NAMESPACE, 0).edit().clear().commit()
        context.getSharedPreferences(BreathingPreferences.NAMESPACE, 0).edit().clear().commit()
    }

    @Test fun keepsTheLineVerbatimWithoutTrimmingOrEditing() {
        val store = SankalpStore(context)
        val line = "  Be  present, ऊर्जा & calm.  "
        assertTrue(store.save(line))
        assertEquals(line, SankalpStore(context).line)
    }

    @Test fun sixtyCharactersIsTheLimit() {
        val store = SankalpStore(context)
        assertTrue(store.save("a".repeat(60)))
        assertFalse(store.save("a".repeat(61)))
        // The rejected save left the old line alone.
        assertEquals("a".repeat(60), store.line)
        assertEquals(60, SankalpStore.limitInput("b".repeat(90)).length)
        assertFalse(SankalpStore.limitInput("one\ntwo").contains('\n'))
    }

    @Test fun blankIsNotSavedAndClearRemovesTheLine() {
        val store = SankalpStore(context)
        assertFalse(store.save("   "))
        assertNull(store.line)
        store.save("steady")
        store.clear()
        assertNull(store.line)
    }

    @Test fun livesInItsOwnFileAndNeverInTheBreathingJournalOrItsClearAll() {
        val store = SankalpStore(context)
        store.save("my own words")
        val breathing = BreathingPreferences(context)
        breathing.append(BreathingRecord("s", "external_grounding", "v1", 1, BreathingEntry.HOME, 180, 180, BreathingPhase.ENDED_TIMER))
        val raw = context.getSharedPreferences(BreathingPreferences.NAMESPACE, 0).all.values.joinToString()
        assertFalse(raw.contains("my own words"))
        breathing.clearAll()
        assertEquals("my own words", SankalpStore(context).line)
    }

    @Test fun onlyTheLineIsStored() {
        SankalpStore(context).save("x")
        val keys = context.getSharedPreferences(SankalpStore.NAMESPACE, 0).all.keys
        assertEquals(setOf("line"), keys)
    }
}
