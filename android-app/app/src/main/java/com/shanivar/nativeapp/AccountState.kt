package com.shanivar.nativeapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Survives activity recreation; logout invalidates pending personal-data responses. */
class AccountState(
    /** DEV-47 AC6: process sync; logout signs the sync session out. */
    private val sync: () -> com.shanivar.nativeapp.sync.AppSync = { com.shanivar.nativeapp.sync.AppSync.current },
) : ViewModel() {
    private var savedSession: ReflectionStore? = null
    private var restored = false
    /** Separate encrypted file/key from reflection content; never persist passwords. */
    fun restore(context: android.content.Context) {
        if (restored) return
        restored = true
        savedSession = EncryptedReflectionStore(context, "account-session.enc", "shanivar-account-session")
        val saved = savedSession?.read("account") ?: return
        runCatching {
            api.restoreSession(saved.getString("token"))
            account = Account(saved.getString("id"), saved.getString("name"), saved.optInt("streak"))
            ShanivarSession.api = api
        }.onFailure { savedSession?.clear(); api.logout(); account = null }
    }
    val api = ShanivarApi()
    var account by mutableStateOf<Account?>(null)
        private set
    var note by mutableStateOf<DailyNote?>(null)
        private set
    var noteBusy by mutableStateOf(false)
        private set
    var noteError by mutableStateOf<String?>(null)
        private set
    private var noteJob: Job? = null
    private var revision = 0

    fun connect(value: Account) {
        invalidate()
        account = value
        ShanivarSession.api = api
        api.sessionToken()?.let { token ->
            runCatching { savedSession?.write("account", org.json.JSONObject()
                .put("token", token).put("id", value.id).put("name", value.name).put("streak", value.streak)) }
        }
    }
    fun logout() {
        invalidate()
        api.logout()
        savedSession?.clear()
        account = null
        ShanivarSession.api = null
        // DEV-47 AC6: logout clears the sync session (signOut), not a reset: it keeps the person's local
        // records, and the engine keeps queued items stamped to this user so none go out under a new one.
        runCatching { sync().requestSignOut() }
    }
    private fun invalidate() {
        revision++
        noteJob?.cancel()
        note = null
        noteBusy = false
        noteError = null
    }
    fun refreshNote() {
        if (account == null || noteBusy) return
        val expectedRevision = revision
        noteBusy = true
        noteError = null
        noteJob = viewModelScope.launch {
            try {
                val result = api.today()
                if (revision == expectedRevision) note = result
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (revision == expectedRevision)
                    noteError = if (e is ApiFailure) e.message else "Your note could not load. Try again when you're connected."
            } finally {
                if (revision == expectedRevision) noteBusy = false
            }
        }
    }
}
