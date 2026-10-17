package com.shanivar.nativeapp

import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.shanivar.nativeapp.sync.AppSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** QA temp (throwaway merge 9806c99 + 2fce42d): real DEV-45 engine + DEV-47 AppSync/AccountState logout. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QaDev47LogoutLeakTest {
    @get:org.junit.Rule val outboxReset = AppSyncOutboxReset()
    private lateinit var server: MockWebServer
    private val ctx get() = RuntimeEnvironment.getApplication()

    @Before fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(ctx,
            Configuration.Builder().setMinimumLoggingLevel(Log.WARN).setExecutor(SynchronousExecutor()).build())
        server = MockWebServer().apply { start() }
    }
    @After fun tearDown() { server.shutdown(); Dev45Sync.current = null; AppSync.resetForTests() }

    private fun runQueued() {
        val wm = WorkManager.getInstance(ctx); val d = WorkManagerTestInitHelper.getTestDriver(ctx)!!
        repeat(3) {
            listOf("outbox-flush").flatMap { wm.getWorkInfosForUniqueWork(it).get() }
                .filter { it.state == WorkInfo.State.ENQUEUED }
                .forEach { runCatching { d.setAllConstraintsMet(it.id); d.setInitialDelayMet(it.id) } }
        }
    }

    private fun scenario(flushFails: Boolean) {
        val engine = Dev45SyncEngineFactory.build(ctx, server.url("/").toString(), "anon", cipher = XorCipher())
        val sync = AppSync.create(true, "u", "k", CoroutineScope(SupervisorJob() + Dispatchers.IO)) { engine }
        AppSync.install(sync)
        // Guest A
        server.enqueue(authReply())
        runBlocking { engine.session.ensureGuest() }
        assertEquals(UID_A, engine.session.userId)
        engine.outbox.enqueue("attempt", "a1", "{}")
        engine.outbox.enqueue("self_report", "s1", "{}")
        // Logout: the flush fails (500s) or never runs (constraint never met = offline).
        val phase = java.util.concurrent.atomic.AtomicReference("logout")
        if (flushFails) server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(r: okhttp3.mockwebserver.RecordedRequest): MockResponse = when {
                r.path!!.startsWith("/auth/") -> authReply(uid = UID_B, access = "acc-b", refresh = "ref-b")
                phase.get() == "logout" -> MockResponse().setResponseCode(500)
                else -> MockResponse().setResponseCode(201).setBody("[]")
            }
        }
        println("QA state before logout: ${engine.status.state.value}")
        val before = server.requestCount
        val t0 = System.nanoTime()
        val acct = AccountState { sync }
        acct.logout()
        if (flushFails) { val end = System.nanoTime() + 1_500_000_000L; while (System.nanoTime() < end && acct.syncSignOut?.isActive == true) { runQueued(); Thread.sleep(50) } }
        runBlocking { acct.syncSignOut!!.join() }
        val tookMs = (System.nanoTime() - t0) / 1_000_000
        println("QA logout flushFails=$flushFails took ${tookMs}ms, requests during logout=${server.requestCount - before}")
        assertTrue("bounded by ~3 s, took $tookMs", tookMs < 3_800)
        assertNull(engine.session.userId)
        assertTrue(engine.outbox.pending.all { it.ownerUserId == UID_A })
        phase.set("b")
        // Drain whatever MockWebServer saw so far
        while (server.takeRequest(10, java.util.concurrent.TimeUnit.MILLISECONDS) != null) {}
        // New guest B, flush online
        if (!flushFails) server.enqueue(authReply(uid = UID_B, access = "acc-b", refresh = "ref-b"))
        runBlocking { engine.session.ensureGuest() }
        assertEquals(UID_B, engine.session.userId)
        if (!flushFails) repeat(10) { server.enqueue(MockResponse().setResponseCode(201).setBody("[]")) }
        engine.outbox.enqueue("self_report", "b1", "{}")
        engine.flushNow(); runQueued()
        val seen = generateSequence { server.takeRequest(200, java.util.concurrent.TimeUnit.MILLISECONDS) }.toList()
        seen.forEach { println("QA after B: ${it.method} ${it.path} auth=${it.getHeader("Authorization")} body=${it.body.readUtf8()}") }
        val rest = seen.filter { it.path!!.startsWith("/rest/") }
        assertTrue("no request carries A's token", seen.none { it.getHeader("Authorization") == "Bearer acc-1" })
        assertEquals(setOf(UID_A), engine.outbox.pending.map { it.ownerUserId }.toSet())
        assertEquals(setOf("attempt:a1", "self_report:s1"), engine.outbox.pending.map { it.key }.toSet())
    }

    @Test fun logoutWithFailingFlushThenNewGuestLeaksNothing() = scenario(flushFails = true)
    @Test fun logoutOfflineThenNewGuestLeaksNothing() = scenario(flushFails = false)
}
