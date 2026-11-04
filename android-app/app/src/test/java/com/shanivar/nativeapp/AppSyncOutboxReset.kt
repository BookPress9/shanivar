package com.shanivar.nativeapp

import org.junit.rules.ExternalResource

/** Resets the [AppSyncOutbox] singleton before and after each test so it never leaks across tests. */
class AppSyncOutboxReset : ExternalResource() {
    override fun before() = AppSyncOutbox.reset()
    override fun after() = AppSyncOutbox.reset()
}
