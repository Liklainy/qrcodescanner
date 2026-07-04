package ru.qrefka.qrcodescanner.util

private val URI_SCHEME_REGEX = Regex("^([a-zA-Z][a-zA-Z0-9+.-]*):")
private val BLOCKED_SCHEMES = setOf("javascript", "file", "content", "data", "blob", "about")

/**
 * Whether [text] looks like something an intent could open, e.g. `https://…`,
 * `mailto:…`, `geo:…` or app deep links (`tg:…`, `otpauth:…`).
 */
internal fun isOpenableUri(text: String): Boolean {
    val trimmed = text.trim()
    // A URI is a single token; prose that happens to contain a colon is not openable.
    if (trimmed.any { it.isWhitespace() }) return false
    val match = URI_SCHEME_REGEX.find(trimmed) ?: return false
    val scheme = match.groupValues[1].lowercase()
    return scheme !in BLOCKED_SCHEMES
}

