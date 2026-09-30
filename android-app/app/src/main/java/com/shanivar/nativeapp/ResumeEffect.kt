package com.shanivar.nativeapp

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** The mirror of [OnAppResume]. The breathing player uses it to stop cues and pause the moment the
 * app loses the foreground, rather than counting down behind a lock screen. */
@Composable
fun OnAppPause(action: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val latest by rememberUpdatedState(action)
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) latest()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

@Composable
fun OnAppResume(action: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val latest by rememberUpdatedState(action)
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) latest()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}
