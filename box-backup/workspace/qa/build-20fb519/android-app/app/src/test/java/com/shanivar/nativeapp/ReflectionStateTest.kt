package com.shanivar.nativeapp

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.IOException
import java.time.LocalDate

fun reflectionFixture(count: Int = 0, status: String = if(count==5) "completed" else "awaiting_response",
    provenance: String = "general", date: String = LocalDate.now().toString()): JSONObject {
    fun card(i:Int) = JSONObject().put("id","card-$i").put("index",i).put("sender","Shanivar")
        .put("message","The reply is already typed. There is still a moment before you send it.")
        .put("question","What do you want them to understand?")
        .put("options",org.json.JSONArray("""[{"id":"fits","label":"This fits"},{"id":"not_today","label":"Not today"},{"id":"unsure","label":"I'm unsure"},{"id":"prefer_not_to_share","label":"Prefer not to share"}]"""))
        .put("context_refs",org.json.JSONArray()).put("provenance_label",provenance)
    val history=org.json.JSONArray()
    repeat(count) { history.put(JSONObject().put("card",card(it)).put("response",
        JSONObject().put("kind","option").put("value","prefer_not_to_share").put("created_at","2026-09-10T09:00:00Z"))) }
    return JSONObject().put("session_id","session-1").put("version",count).put("status",status)
        .put("mode",if(provenance=="general_fallback") "personalized" else provenance)
        .put("context_as_of","2026-09-10T09:00:00Z").put("local_date",date).put("timezone","UTC")
        .put("answered_count",count).put("session_limit",5).put("current_card",if(status=="awaiting_response") card(count) else JSONObject.NULL)
        .put("answered_history",history)
}
class MemoryReflectionStore : ReflectionStore {
    var owner: String?=null
    var value: String?=null
    override fun read(owner:String):JSONObject? { if(this.owner!=owner) { clear();return null };return value?.let(::JSONObject) }
    override fun write(owner:String,value:JSONObject) {this.owner=owner;this.value=value.toString()}
    override fun clear() {owner=null;value=null}
}
class FakeReflectionRemote : ReflectionRemote {
    var value=reflectionFixture()
    val calls=mutableListOf<Triple<String,String,String?>>()
    var failAfterAcceptance=false
    var failBeforeAcceptance=false
    var reject:Int?=null
    override suspend fun call(path:String, method:String,body:JSONObject?,key:String?):JSONObject {
        calls.add(Triple(path,method,key))
        if(method=="DELETE") return JSONObject()
        if(path.endsWith("/responses")) {
            reject?.let { throw ApiFailure(it,"fixture error") }
            if(failBeforeAcceptance) throw IOException("offline")
            value=reflectionFixture(value.getInt("answered_count")+1)
            if(failAfterAcceptance) throw IOException("lost response")
        }
        if(path.endsWith("/retry")) value=reflectionFixture(value.getInt("answered_count"))
        if(path.endsWith("/close")) value=reflectionFixture(value.getInt("answered_count"),"closed")
        return JSONObject(value.toString())
    }
}
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],manifest=Config.NONE)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class ReflectionStateTest {
    private val dispatcher=StandardTestDispatcher()
    private lateinit var api:FakeReflectionRemote
    private lateinit var store:MemoryReflectionStore
    private lateinit var state:ReflectionState
    @Before fun setup() { Dispatchers.setMain(dispatcher);api=FakeReflectionRemote();store=MemoryReflectionStore();state=ReflectionState();state.bind("A",api,store) }
    @After fun cleanup() {state.clear();Dispatchers.resetMain()}
    private fun drain() {dispatcher.scheduler.advanceUntilIdle()}
    private fun start() {state.acceptConsent(ReflectionEntryPoint.TODAY);drain()}
    @Test fun noAdvanceUntilAcceptedAndDuplicateTapProducesOneResponse() {
        start();state.next();assertEquals("card-0",state.displayedId)
        state.choose("prefer_not_to_share");state.submit();state.submit();drain()
        assertEquals(1,api.calls.count { it.first.endsWith("/responses") })
        assertEquals("card-0",state.displayedId);assertTrue(state.canContinue)
        state.next();assertEquals("card-1",state.displayedId)
    }
    @Test fun timeoutAfterAcceptanceReconcilesOnRestartWithoutSecondResponse() {
        start();state.choose("fits");api.failAfterAcceptance=true;state.submit();drain()
        assertNotNull(state.pending)
        val restored=ReflectionState();restored.bind("A",api,store);restored.refresh();drain()
        assertNull(restored.pending);assertEquals(1,restored.session!!.answered)
        assertEquals(1,api.calls.count {it.first.endsWith("/responses")});restored.clear()
    }
    @Test fun timeoutBeforeAcceptanceReplaysExactPersistedKeyAndPayload() {
        start();state.edit("A private draft");api.failBeforeAcceptance=true;state.submit();drain()
        val key=state.pending!!.getString("key")
        val restored=ReflectionState();restored.bind("A",api,store)
        assertEquals("A private draft",restored.draft)
        api.failBeforeAcceptance=false;restored.refresh();drain()
        assertEquals(listOf(key,key),api.calls.filter {it.first.endsWith("/responses")}.map {it.third})
        assertEquals(1,restored.session!!.answered);restored.clear()
    }
    @Test fun invalidTextNeverSendsAnd422PreservesDraft() {
        start()
        listOf("   ","x".repeat(1001)).forEach {state.edit(it);state.submit();drain()}
        assertFalse(api.calls.any {it.first.endsWith("/responses")})
        state.edit("Keep this");api.reject=422;state.submit();drain()
        assertEquals("Keep this",state.draft);assertNull(state.pending);assertNotNull(state.error)
    }
    @Test fun accountSwitchAndLogoutClearPrivateState() {
        start();state.edit("private A");state.bind("B",api,store)
        assertNull(state.session);assertEquals("",state.draft);assertNull(store.value)
        state.clear();assertNull(state.owner)
    }
    @Test fun fallbackLabelOverridesPersonalizedSessionAndSavedDateIsVisible() {
        val s=ReflectionSession.parse(reflectionFixture(provenance="general_fallback",date="2000-01-01"))
        assertEquals("personalized",s.mode)
        assertEquals("General reflection · service fallback",s.current!!.contextLabel())
        assertTrue(s.savedDay())
    }
    @Test fun generationFailureRetriesWithoutAnotherAnswerAndCompletesAtFive() {
        api.value=reflectionFixture(1,"generation_failed");start()
        state.retryGeneration();drain()
        assertEquals(0,api.calls.count {it.first.endsWith("/responses")})
        repeat(4) { state.showCard(state.session!!.current!!.id);state.choose("prefer_not_to_share");state.submit();drain() }
        assertEquals("completed",state.session!!.status);assertNull(state.session!!.current)
        assertFalse(state.canContinue)
    }
    @Test fun conflictReconcilesAnotherDeviceAndDeleteClearsCache() {
        start();state.choose("fits")
        api.value=reflectionFixture(1);api.reject=409;state.submit();drain()
        assertEquals(1,state.session!!.answered);assertNull(state.pending)
        state.deleteSession();drain();assertNull(state.session);assertEquals("",state.draft)
    }
    @Test fun rateLimitRetainsPendingRequestAndPreventsImmediateReplay() {
        start();state.choose("fits");api.reject=429;state.submit();drain()
        val count=api.calls.size;state.refresh();drain()
        assertEquals(count,api.calls.size);assertNotNull(state.pending)
    }
    @Test fun authenticatedTransportUsesHeaderAndAcceptsPersisted503Envelope() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"token":"fixture-token"}"""))
            server.enqueue(MockResponse().setBody("""{"id":"A","display_name":"Observer"}"""))
            server.enqueue(MockResponse().setResponseCode(503).setBody(reflectionFixture(1,"generation_failed").toString()))
            val real=ShanivarApi(server.url("/api").toString());real.login("a@example.com","password")
            val body=JSONObject().put("card_id","card-0").put("expected_version",0)
                .put("response",JSONObject().put("kind","option").put("value","prefer_not_to_share"))
            val result=ReflectionApi(real).call("/sessions/session-1/responses","POST",body,"durable-key")
            assertEquals("generation_failed",result.getString("status"))
            server.takeRequest();server.takeRequest()
            val request=server.takeRequest()
            assertEquals("Bearer fixture-token",request.getHeader("Authorization"))
            assertEquals("durable-key",request.getHeader("Idempotency-Key"))
            assertEquals("/api/reflections/sessions/session-1/responses",request.path)
            assertEquals(0,JSONObject(request.body.readUtf8()).getInt("expected_version"))
        }
    }

    @Test fun failedDurableWriteNeverSendsEvenDuringRecovery() {
        val broken=object:ReflectionStore {
            override fun read(owner:String):JSONObject?=null
            override fun write(owner:String,value:JSONObject) {throw IOException("disk full")}
            override fun clear() {}
        }
        state.bind("C",api,broken);state.acceptConsent(ReflectionEntryPoint.TODAY);drain()
        state.refresh();drain()
        assertTrue(api.calls.isEmpty());assertNotNull(state.pending)
    }
    @Test fun generationPollingStopsAndOffersRecovery() {
        api.value=reflectionFixture(1,"generating");start()
        assertFalse(state.busy);assertEquals("generating",state.session!!.status)
        assertEquals(5,api.calls.count {it.second=="GET"})
        assertTrue(state.error!!.contains("taking longer"))
    }
    @Test fun lateResultCannotRepopulateAnotherAccount() {
        val response=CompletableDeferred<JSONObject>()
        val delayed=object:ReflectionRemote {
            override suspend fun call(path:String,method:String,body:JSONObject?,key:String?) =
                withContext(NonCancellable) {response.await()}
        }
        state.bind("C",delayed,store);state.acceptConsent(ReflectionEntryPoint.TODAY)
        dispatcher.scheduler.runCurrent()
        state.bind("D",api,store)
        response.complete(reflectionFixture());drain()
        assertEquals("D",state.owner);assertNull(state.session)
    }
    @Test fun parallelDeviceAdvanceKeepsOldDraftSeparateFromNewCard() {
        start();state.edit("My unfinished wording")
        api.value=reflectionFixture(1);state.refresh();drain()
        assertEquals("My unfinished wording",state.earlierDraft)
        state.next();assertEquals("card-1",state.displayedId)
        assertEquals("",state.draft);assertFalse(state.validInput)
        val restored=ReflectionState();restored.bind("A",api,store)
        assertEquals("My unfinished wording",restored.earlierDraft)
        restored.clear()
    }
}

