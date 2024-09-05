package electionguard.util

external fun require(module: String): dynamic

inline fun <T> jsObject(init: dynamic.() -> Unit): T {
    val o = js("{}")
    init(o)
    return o as T
}
