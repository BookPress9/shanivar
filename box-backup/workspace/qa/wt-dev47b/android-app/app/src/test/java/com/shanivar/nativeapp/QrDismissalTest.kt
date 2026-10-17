package com.shanivar.nativeapp

import org.junit.Assert.*
import org.junit.Test

class QrDismissalVerifierTest {
    @Test fun exactMatchVerifies() {
        assertTrue(QrDismissalVerifier.matches("shanivar-dismiss-abc123", "shanivar-dismiss-abc123"))
    }

    @Test fun mismatchDoesNotVerify() {
        assertFalse(QrDismissalVerifier.matches("some-other-code", "shanivar-dismiss-abc123"))
    }

    @Test fun surroundingWhitespaceFromScanNoiseIsTolerated() {
        assertTrue(QrDismissalVerifier.matches("  shanivar-dismiss-abc123 \n", "shanivar-dismiss-abc123"))
    }

    @Test fun caseIsSignificant() {
        // Deliberately strict: a printed sticker's payload should decode byte-for-byte, and being
        // lenient here would make it easier to forge/guess a matching sticker.
        assertFalse(QrDismissalVerifier.matches("Shanivar-Dismiss-ABC123", "shanivar-dismiss-abc123"))
    }

    @Test fun blankPayloadsNeverMatchEvenIfExpectedIsAlsoBlank() {
        assertFalse(QrDismissalVerifier.matches("", ""))
        assertFalse(QrDismissalVerifier.matches("   ", ""))
    }
}
