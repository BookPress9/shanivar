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
            val alarm = RoutineApi(api).createAlarm(
                localTime = "06:45", daysOfWeek = setOf(1, 2, 3, 4, 5),
                movementDestination = "kitchen", stepTarget = 16, accessibleMovement = false,
            )
            assertEquals("11111111-1111-1111-1111-111111111111", alarm.id)
            val createBody = JSONObject(server.takeRequest().body.readUtf8())
            assertEquals("movement", createBody.getString("dismissal_method"))
            assertEquals(5, createBody.getJSONArray("days_of_week").length())

            server.enqueue(MockResponse().setBody("""{"id":"11111111-1111-1111-1111-111111111111","next_confirmed_occurrence":"2026-09-13T05:00:00Z"}"""))
            val confirmed = RoutineApi(api).confirmAlarm(alarm.id, "2026-09-13T05:00:00Z")
            assertEquals("2026-09-13T05:00:00Z", confirmed.nextConfirmedOccurrence)
            assertTrue(server.takeRequest().path!!.endsWith("/confirm-scheduled"))
        }
    }

    @Test fun alarmEditEnableListAndDeleteUseTheExpectedHttpVerbsAndPaths() = runBlocking {
        MockWebServer().use { server ->
            val api = ShanivarApi(server.url("/api").toString())
            login(api, server)
            val id = "11111111-1111-1111-1111-111111111111"

            // Edit resets next_confirmed_occurrence server-side. PUT, not PATCH - PATCH isn't in
            // the JDK HttpURLConnection method whitelist this client is built on.
            server.enqueue(MockResponse().setBody("""{"id":"$id","local_time":"06:30:00","days_of_week":[1,3,5],"enabled":true,"next_confirmed_occurrence":null}"""))
            val edited = RoutineApi(api).editAlarm(id, "06:30", setOf(1, 3, 5), "kitchen", 20, false)
            val editRequest = server.takeRequest()
            assertEquals("PUT", editRequest.method)
            assertEquals(setOf(1, 3, 5), edited.daysOfWeek)
            assertNull(edited.nextConfirmedOccurrence)

            server.enqueue(MockResponse().setBody("""{"id":"$id","enabled":false}"""))
            val disabled = RoutineApi(api).setAlarmEnabled(id, false)
            assertEquals("PUT", server.takeRequest().method)
            assertFalse(disabled.enabled)

            server.enqueue(MockResponse().setBody("""[{"id":"$id","local_time":"06:30:00","days_of_week":[1,3,5],"enabled":false}]"""))
            val list = RoutineApi(api).listAlarms()
            assertEquals("GET", server.takeRequest().method)
            assertEquals(1, list.size)
            assertEquals(id, list.first().id)

            // Matches the real route (backend/src/routes/discipline.js): 200 with a JSON body,
            // not a bare 204 - ShanivarApi.request() cannot parse an empty success body.
            server.enqueue(MockResponse().setBody("""{"message":"Alarm deleted"}"""))
            RoutineApi(api).deleteAlarm(id)
            assertEquals("DELETE", server.takeRequest().method)
        }
    }

    @Test fun focusScheduleCrudUsesTheExpectedVerbsAndPreservesTheWhenThenCommitmentVerbatim() = runBlocking {
        MockWebServer().use { server ->
            val api = ShanivarApi(server.url("/api").toString())
            login(api, server)
            val id = "66666666-6666-6666-6666-666666666666"

            server.enqueue(MockResponse().setBody(
                """{"id":"$id","mode":"days","days_of_week":[1,2,3,4,5],"start_time":"09:00:00","end_time":"17:00:00","block_package_list":["com.instagram.android"],"intention_label":"Deep work","commitment_trigger":"When I reach for my phone","commitment_action":"I will remember why I muted it","enabled":true}"""
            ))
            val created = RoutineApi(api).createFocusSchedule(
                mode = "days", days = setOf(1, 2, 3, 4, 5), startTime = "09:00", endTime = "17:00",
                blockedPackages = setOf("com.instagram.android"), intentionLabel = "Deep work",
                commitmentTrigger = "When I reach for my phone", commitmentAction = "I will remember why I muted it",
            )
            val createRequest = server.takeRequest()
            assertEquals("POST", createRequest.method)
            val createBody = JSONObject(createRequest.body.readUtf8())
            // The commitment must reach the server exactly as typed - never rewritten, never AI-generated.
            assertEquals("When I reach for my phone", createBody.getString("commitment_trigger"))
            assertEquals("I will remember why I muted it", createBody.getString("commitment_action"))
            assertEquals("days", created.mode)
            assertEquals(setOf(1, 2, 3, 4, 5), created.daysOfWeek)

            server.enqueue(MockResponse().setBody("""{"id":"$id","mode":"all_day","days_of_week":[0,1,2,3,4,5,6],"start_time":"00:00:00","end_time":"00:00:00","enabled":true}"""))
            val edited = RoutineApi(api).editFocusSchedule(
                id, mode = "all_day", days = (0..6).toSet(), startTime = "00:00", endTime = "00:00",
                blockedPackages = setOf("com.instagram.android"), intentionLabel = "Deep work",
                commitmentTrigger = "", commitmentAction = "",
            )
            assertEquals("PUT", server.takeRequest().method) // not PATCH - see editAlarm's doc comment
            assertEquals("all_day", edited.mode)
            assertEquals(7, edited.daysOfWeek.size)

            server.enqueue(MockResponse().setBody("""{"id":"$id","enabled":false}"""))
            val disabled = RoutineApi(api).setFocusScheduleEnabled(id, false)
            assertEquals("PUT", server.takeRequest().method)
            assertFalse(disabled.enabled)

            server.enqueue(MockResponse().setBody("""[{"id":"$id","mode":"custom","enabled":false}]"""))
            val list = RoutineApi(api).listFocusSchedules()
            assertEquals("GET", server.takeRequest().method)
            assertEquals(1, list.size)

            server.enqueue(MockResponse().setBody("""{"message":"Focus schedule deleted"}"""))
            RoutineApi(api).deleteFocusSchedule(id)
            assertEquals("DELETE", server.takeRequest().method)
        }
    }
}
