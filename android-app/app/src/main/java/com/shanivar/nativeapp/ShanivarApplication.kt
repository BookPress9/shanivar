package com.shanivar.nativeapp

import android.app.Application
import android.app.Activity
import android.os.Bundle
import java.io.File
import com.shanivar.nativeapp.sync.AppSync
import com.shanivar.nativeapp.sync.SyncEngineFactory
import com.shanivar.nativeapp.sync.SyncEngines

/**
 * DEV-47: creates the single [AppSync]. Sync is on only when BuildConfig.SYNC_ENABLED (debug only until
 * DEV-44) and SUPABASE_URL / SUPABASE_ANON_KEY are set, and the DEV-45 engine exists. Otherwise it is
 * the disabled no-op and every screen behaves as before.
 */
open class ShanivarApplication : Application() {
    protected open fun engineFactory(): SyncEngineFactory? = SyncEngines.dev45
    /** Never on under Robolectric: debug unit tests share the debug BuildConfig (with the real URL/key). */
    protected open val syncFlag: Boolean get() = BuildConfig.SYNC_ENABLED && android.os.Build.FINGERPRINT != "robolectric"
    protected open val supabaseUrl: String get() = BuildConfig.SUPABASE_URL
    protected open val supabaseAnonKey: String get() = BuildConfig.SUPABASE_ANON_KEY

    override fun onCreate() {
        super.onCreate()
        val url = supabaseUrl; val key = supabaseAnonKey
        val sync = AppSync.create(syncFlag, url, key) { engineFactory()?.create(this, url, key) }
        AppSync.install(sync)
        if (!sync.enabled) return
        watchForeground(sync::onForeground)
        sync.onAppStart { InitiationGuestRetry.allowed(File(filesDir, InitiationGuestRetry.DRAFT_PATH)) }
    }

    /**
     * App foreground (AC3): the first started activity after none were started. A plain started-activity
     * count instead of lifecycle-process, so no process-wide static or startup provider is added.
     */
    protected open fun watchForeground(onForeground: () -> Unit) {
        registerActivityLifecycleCallbacks(ForegroundCounter(onForeground))
    }
}

/** Calls [onForeground] when the started-activity count goes from 0 to 1 (main thread, no I/O). */
internal class ForegroundCounter(private val onForeground: () -> Unit) : Application.ActivityLifecycleCallbacks {
    private var started = 0
    override fun onActivityStarted(activity: Activity) { if (started++ == 0) onForeground() }
    override fun onActivityStopped(activity: Activity) { if (started > 0) started-- }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}

/**
 * AC2 retry rule: an app-start guest sign-in only retries an earlier guest step, i.e. only when the
 * Initiation draft says it is complete and the person chose "continue on this device".
 */
object InitiationGuestRetry {
    /** Same file InitiationApp's controller uses. */
    const val DRAFT_PATH = "initiation/draft.properties"
    fun allowed(draft: OnboardingDraft): Boolean =
        draft.checkpoint == InitiationCheckpoint.COMPLETE && draft.identityMode == IdentityMode.GUEST
    fun allowed(draftFile: File): Boolean =
        runCatching { allowed(OnboardingDraftRepository(FileDraftStore(draftFile)).draft) }.getOrDefault(false)
}
