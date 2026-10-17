package com.shanivar.nativeapp

import org.json.JSONObject
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
