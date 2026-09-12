package com.shanivar.nativeapp

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class RoutineApiTest {
    private fun login(api: ShanivarApi, server: MockWebServer) = runBlocking {
        server.enqueue(MockResponse().setBody("""{"token":"token"}"""))
        server.enqueue(MockResponse().setBody("""{"id":"u1","display_name":"Observer","streak_days":0}"""))
        api.login("observer@example.com", "password")
        server.takeRequest(); server.takeRequest()
    }

    @Test fun rhythmSaveAndAlarmConfirmationUseTheSupabaseBackedContract() = runBlocking {
        MockWebServer().use { server ->
            val api = ShanivarApi(server.url("/api").toString())
            login(api, server)
            val preference = """{"wake_time":"06:45:00","quiet_time":"21:15:00","focus_minutes":45,"preferred_language":"hi","dismissal_method":"movement","movement_destination":"kitchen","step_target":16,"scene_confirmation":true,"whatsapp_time":"08:15:00","whatsapp_tone":"gentle","daily_note_enabled":true,"block_package_list":["com.instagram.android"]}"""
            server.enqueue(MockResponse().setBody(preference))
            val saved = RoutineApi(api).save(Rhythm(wakeTime="06:45", quietTime="21:15", focusMinutes=45,
                movementDestination="kitchen", stepTarget=16, sceneConfirmation=true, whatsappTime="08:15", whatsappTone="Gentle"), "hi")
            assertEquals("kitchen", saved.movementDestination)
            val saveRequest = server.takeRequest()
            assertEquals("PUT", saveRequest.method)
            val saveBody = JSONObject(saveRequest.body.readUtf8())
            assertEquals("movement", saveBody.getString("dismissal_method"))
            assertEquals(16, saveBody.getInt("step_target"))
            assertEquals("hi", saveBody.getString("preferred_language"))

            server.enqueue(MockResponse().setBody("""{"id":"11111111-1111-1111-1111-111111111111","next_confirmed_occurrence":null}"""))
            val alarm = RoutineApi(api).createAlarm(Rhythm(movementDestination="kitchen", stepTarget=16))
            assertEquals("11111111-1111-1111-1111-111111111111", alarm.id)
            assertEquals("movement", JSONObject(server.takeRequest().body.readUtf8()).getString("dismissal_method"))

            server.enqueue(MockResponse().setBody("""{"id":"11111111-1111-1111-1111-111111111111","next_confirmed_occurrence":"2026-09-13T05:00:00Z"}"""))
            val confirmed = RoutineApi(api).confirmAlarm(alarm.id, "2026-09-13T05:00:00Z")
            assertEquals("2026-09-13T05:00:00Z", confirmed.nextConfirmedOccurrence)
            assertTrue(server.takeRequest().path!!.endsWith("/confirm-scheduled"))
        }
    }
}
