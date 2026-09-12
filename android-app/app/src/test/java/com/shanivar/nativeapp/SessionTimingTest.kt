package com.shanivar.nativeapp

import org.junit.Assert.*
import org.junit.Test

class SessionTimingTest {
    @Test fun passageBoundariesUseElapsedTimeAndStayWithinContent() {
        assertEquals(0, passageIndex(240000, 240000, 8))
        assertEquals(0, passageIndex(210001, 240000, 8))
        assertEquals(1, passageIndex(210000, 240000, 8))
        assertEquals(4, passageIndex(120000, 240000, 8))
        assertEquals(7, passageIndex(0, 240000, 8))
        assertEquals(7, passageIndex(-500, 240000, 8))
    }
    @Test fun returningFromBackgroundSkipsToCurrentPassage() {
        assertEquals(6, passageIndex(45000, 180000, 8))
    }
}
