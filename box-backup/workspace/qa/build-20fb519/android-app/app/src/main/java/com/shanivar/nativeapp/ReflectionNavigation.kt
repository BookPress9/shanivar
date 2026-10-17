package com.shanivar.nativeapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Same-process UI navigation only. MainActivity never populates this from external Intent data. */
internal object ReflectionNavigation {
    var requested by mutableStateOf<ReflectionEntryPoint?>(null)
        private set
    fun fromBlockedSurface() { requested=ReflectionEntryPoint.BLOCKED_APP_SURFACE }
    fun consume() { requested=null }
}

