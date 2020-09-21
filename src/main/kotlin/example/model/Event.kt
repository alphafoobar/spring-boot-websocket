package example.model

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "event")
open class Event {
    var requestId: String? = null
}

class Error(val reason: String, val code: Int) : Event()

class Disconnect : Event()

class Welcome : Event()
