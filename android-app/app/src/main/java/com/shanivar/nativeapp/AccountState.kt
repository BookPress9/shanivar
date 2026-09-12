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
class AccountState : ViewModel() {
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
    }
    fun logout() {
        invalidate()
        api.logout()
        account = null
        ShanivarSession.api = null
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
