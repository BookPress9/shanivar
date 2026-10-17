package com.shanivar.nativeapp

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.json.JSONObject
import java.time.ZoneId
import java.util.UUID

class ReflectionState : ViewModel() {
    var owner by mutableStateOf<String?>(null); private set
    var session by mutableStateOf<ReflectionSession?>(null); private set
    var displayedId by mutableStateOf<String?>(null); private set
    var draft by mutableStateOf(""); private set
    var selected by mutableStateOf<String?>(null); private set
    var earlierDraft by mutableStateOf(""); private set
    var earlierQuestion by mutableStateOf(""); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var offline by mutableStateOf(false); private set
    var consent by mutableStateOf(false); private set
    var retryAt by mutableStateOf(0L); private set
    var pending by mutableStateOf<JSONObject?>(null); private set
    private var remote: ReflectionRemote? = null
    private var store: ReflectionStore? = null
    private var raw: JSONObject? = null
    private var epoch = 0
    private var job: Job? = null
    val displayed: ReflectionCard? get() = session?.let { s -> s.history.find { it.card.id == displayedId }?.card
        ?: s.current?.takeIf { it.id == displayedId } }
    val answered: ReflectionAnswer? get() = session?.history?.find { it.card.id == displayedId }
    val canContinue get() = answered != null && session?.current != null && displayedId != session?.current?.id
    val canAnswer get() = !busy && pending == null && session?.status == "awaiting_response" &&
        displayedId == session?.current?.id && answered == null
    val validInput get() = selected != null || draft.trim().length in 1..1000

    fun bind(id: String, api: ReflectionRemote, persistence: ReflectionStore) {
        if (owner == id) return
        job?.cancel(); epoch++
        owner = id; remote = api; store = persistence
        val cache = persistence.read(id)
        raw = cache?.optJSONObject("session")
        session = raw?.let { runCatching { ReflectionSession.parse(it) }.getOrNull() }
        displayedId = session?.current?.id ?: session?.history?.lastOrNull()?.card?.id
        draft = cache?.optString("draft").orEmpty()
        selected = cache?.optString("selected")?.takeIf { it.isNotEmpty() }
        earlierDraft = cache?.optString("earlier_draft").orEmpty()
        earlierQuestion = cache?.optString("earlier_question").orEmpty()
        pending = cache?.optJSONObject("pending")
        consent = cache?.optBoolean("consent") ?: false
        busy = false; offline = session != null; error = null; retryAt = 0
    }
    fun clear() {
        job?.cancel(); epoch++; store?.clear()
        owner = null; raw = null; session = null; displayedId = null
        draft = ""; selected = null; pending = null; consent = false; earlierDraft=""; earlierQuestion=""
        error = null; busy = false; offline = false; retryAt = 0
    }
    private fun persist() {
        val id = owner ?: return
        store?.write(id, JSONObject().put("session",raw).put("displayed",displayedId)
            .put("draft",draft).put("selected",selected).put("pending",pending).put("consent",consent)
            .put("earlier_draft",earlierDraft).put("earlier_question",earlierQuestion))
    }
    fun edit(value: String) { if (canAnswer) { draft=value; selected=null; saveDraft() } }
    fun choose(id: String) { if (canAnswer && displayed?.options?.any { it.id == id } == true) { selected=id; draft=""; saveDraft() } }
    private fun saveDraft() { runCatching { persist() }.onFailure { error="Your draft could not be saved on this device." } }
    fun showCard(id: String) {
        val s=session ?: return
        if (s.history.any { it.card.id == id } || s.current?.id == id) { displayedId=id; saveDraft() }
    }
    fun next() { if (canContinue) session?.current?.id?.let(::showCard) }
    fun acceptConsent(entry: ReflectionEntryPoint) { consent=true; start(entry) }

    private fun run(block: suspend () -> Unit) {
        if (busy || owner == null) return
        if (System.currentTimeMillis() < retryAt) { error="Please wait before trying again."; return }
        busy=true; error=null
        val expected=epoch
        job=viewModelScope.launch {
            try { block() }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) {
                if(expected==epoch) {
                    offline=e !is ApiFailure
                    error=when(e) {
                        is ApiFailure -> when(e.status) {
                            401,403 -> "Your sign-in expired. Go back and sign in again. Your unsent response stays on this device."
                            404 -> "This session is no longer available."
                            409 -> "The session changed elsewhere. Check its current state before responding."
                            422 -> "That response was not accepted. Choose an option or enter 1–1,000 characters."
                            429 -> "Too many requests. Wait before trying again."
                            else -> "We could not confirm the request. Your response is kept for recovery."
                        }
                        else -> "Connection unavailable. You can read saved messages or leave at any time."
                    }
                    if(e is ApiFailure && e.status==422) { pending=null; persist() }
                    if(e is ApiFailure && e.status==429) retryAt=System.currentTimeMillis()+e.retryAfterSeconds*1000L
                    if(e is ApiFailure && e.status==404) { raw=null; session=null; displayedId=null; pending=null; draft=""; selected=null; earlierDraft=""; earlierQuestion=""; persist() }
                }
            } finally { if(expected==epoch) busy=false }
        }
    }
    private suspend fun call(path:String, method:String="GET", body:JSONObject?=null, key:String?=null): JSONObject {
        val expected=epoch
        val result=checkNotNull(remote).call(path,method,body,key)
        check(expected==epoch) { "Account changed" }
        offline=false
        return result
    }
    private fun applyState(value: JSONObject) {
        val next=ReflectionSession.parse(value)
        val oldCard=session?.current
        if(oldCard!=null && oldCard.id!=next.current?.id) {
            val submitted=pending?.takeIf { it.optString("operation")=="response" }?.optJSONObject("body")
            val received=next.history.find { it.card.id==oldCard.id }
            val matchesOwnSubmission=submitted?.optString("card_id")==oldCard.id && received!=null &&
                received.kind==submitted?.optJSONObject("response")?.optString("kind") &&
                received.value==submitted?.optJSONObject("response")?.optString("value")
            if(!matchesOwnSubmission && (draft.isNotEmpty() || selected!=null)) {
                earlierDraft=selected?.let { id -> oldCard.options.find { it.id==id }?.label } ?: draft
                earlierQuestion=oldCard.question
            }
            draft=""; selected=null
        }
        raw=value; session=next
        val ids=next.history.map { it.card.id } + listOfNotNull(next.current?.id)
        if(displayedId !in ids) displayedId=next.current?.id ?: next.history.lastOrNull()?.card?.id
        val p=pending
        if(p?.optString("operation")=="response" &&
            next.history.any { it.card.id==p.getJSONObject("body").optString("card_id") }) {
            pending=null; draft=""; selected=null
        }
        persist()
    }
    private fun journal(operation: String, path:String, body:JSONObject = JSONObject()) {
        check(pending==null)
        pending=JSONObject().put("operation",operation).put("path",path).put("body",body).put("key",UUID.randomUUID().toString())
        persist() // No mutation is sent unless its exact retry key/payload has been durably saved.
    }
    private suspend fun executePending() {
        val p=pending ?: return
        persist() // Recheck durable storage before every replay, including recovery after a write failure.
        val result=call(p.getString("path"),"POST",p.getJSONObject("body"),p.getString("key"))
        applyState(result)
        pending=null
        if(p.getString("operation")=="response") { draft=""; selected=null }
        persist()
        poll()
    }
    fun start(entry: ReflectionEntryPoint) = run {
        if(!consent) return@run
        if(pending!=null) { recoverInternal(); return@run }
        raw=null; session=null; displayedId=null; draft=""; selected=null
        journal("create","/sessions",JSONObject().put("entry_point",entry.wire).put("timezone",ZoneId.systemDefault().id))
        executePending()
    }
    fun refresh() = run { recoverInternal() }
    private suspend fun recoverInternal() {
        val s=session
        if(s!=null) {
            val p=pending
            applyState(call("/sessions/${s.id}"))
            if(p!=null && pending!=null) {
                when(p.getString("operation")) {
                    "response" -> {
                        val b=p.getJSONObject("body")
                        if(session?.version!=b.optInt("expected_version") || session?.current?.id!=b.optString("card_id")) {
                            pending=null
                            error="The session changed. Review its current message before sending again."
                            persist()
                        } else executePending()
                    }
                    "close" -> if(session?.status in listOf("closed","completed")) { pending=null; persist() } else executePending()
                    else -> executePending()
                }
            }
            poll()
        } else if(pending!=null) executePending()
    }
    fun submit() = run {
        val s=session ?: return@run
        if(pending!=null || s.status!="awaiting_response" || displayedId!=s.current?.id || !validInput) return@run
        val response=JSONObject().put("kind",if(selected!=null) "option" else "text").put("value",selected ?: draft.trim())
        journal("response","/sessions/${s.id}/responses",JSONObject().put("card_id",s.current!!.id).put("expected_version",s.version).put("response",response))
        try { executePending() }
        catch(e: ApiFailure) {
            if(e.status==422) { pending=null; persist() }
            if(e.status==409) recoverInternal() else throw e
        }
    }
    private suspend fun poll() {
        for(ms in listOf(1000L,2000L,4000L,8000L,8000L)) {
            if(session?.status!="generating") return
            delay(ms)
            applyState(call("/sessions/${session!!.id}"))
        }
        if(session?.status=="generating") error="Your response is saved. The next message is taking longer. You can retry generation or leave."
    }
    fun retryGeneration() = run {
        if(pending!=null) { recoverInternal(); return@run }
        val s=session ?: return@run
        journal("retry","/sessions/${s.id}/retry")
        executePending()
    }
    fun closeSession() = run {
        if(pending!=null) { recoverInternal(); return@run }
        val s=session ?: return@run
        journal("close","/sessions/${s.id}/close")
        executePending()
    }
    fun deleteSession() = run {
        val s=session ?: return@run
        // Deletion is retried by GET after ambiguous failure; a 404 means the cache is removed.
        call("/sessions/${s.id}","DELETE")
        raw=null; session=null; displayedId=null; pending=null; draft=""; selected=null; earlierDraft=""; earlierQuestion=""
        persist()
    }
}
