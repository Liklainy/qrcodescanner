package ru.qrefka.qrcodescanner.util

/** Security scheme advertised by a `WIFI:` QR code. */
internal enum class WifiSecurity { OPEN, WEP, WPA, SAE, ENTERPRISE }

/** Credentials carried by a `WIFI:` QR code. */
internal data class WifiCredentials(
    val ssid: String,
    val password: String,
    val security: WifiSecurity,
    val hidden: Boolean,
)

private const val PREFIX = "WIFI:"

/**
 * Parses the de-facto standard Wi-Fi QR payload, e.g.
 * `WIFI:S:MyNet;T:WPA;P:secret;H:false;;`
 *
 * Fields may appear in any order and may be omitted. Inside a value, a backslash
 * escapes the next character, which is how an SSID or password containing `;`,
 * `:`, `,` or `\` is carried. Returns null for anything that is not a Wi-Fi
 * payload or that names no network.
 */
internal fun parseWifiQr(text: String): WifiCredentials? {
    val trimmed = text.trim()
    if (!trimmed.startsWith(PREFIX, ignoreCase = true)) return null

    var ssid = ""
    var password = ""
    var type = ""
    var hidden = false
    for (field in splitFields(trimmed.substring(PREFIX.length))) {
        val separator = field.indexOf(':')
        if (separator <= 0) continue
        val value = unescape(field.substring(separator + 1))
        when (field.substring(0, separator).uppercase()) {
            "S" -> ssid = value
            "P" -> password = value
            "T" -> type = value
            "H" -> hidden = value.equals("true", ignoreCase = true)
        }
    }
    if (ssid.isEmpty()) return null
    return WifiCredentials(ssid, password, securityOf(type), hidden)
}

private fun securityOf(type: String): WifiSecurity = when {
    type.isEmpty() || type.equals("nopass", ignoreCase = true) -> WifiSecurity.OPEN
    type.contains("EAP", ignoreCase = true) -> WifiSecurity.ENTERPRISE
    type.equals("WEP", ignoreCase = true) -> WifiSecurity.WEP
    type.equals("SAE", ignoreCase = true) || type.contains("WPA3", ignoreCase = true) ->
        WifiSecurity.SAE
    // WPA, WPA2 and anything else password-based behave the same here.
    else -> WifiSecurity.WPA
}

/** Splits on `;` while treating a backslash-escaped `;` as part of the value. */
private fun splitFields(body: String): List<String> {
    val fields = mutableListOf<String>()
    val current = StringBuilder()
    var i = 0
    while (i < body.length) {
        val c = body[i]
        when {
            c == '\\' && i + 1 < body.length -> {
                current.append(c).append(body[i + 1])
                i += 2
            }
            c == ';' -> {
                fields.add(current.toString())
                current.clear()
                i++
            }
            else -> {
                current.append(c)
                i++
            }
        }
    }
    if (current.isNotEmpty()) fields.add(current.toString())
    return fields
}

private fun unescape(value: String): String {
    if (!value.contains('\\')) return value
    val out = StringBuilder(value.length)
    var i = 0
    while (i < value.length) {
        val c = value[i]
        if (c == '\\' && i + 1 < value.length) {
            out.append(value[i + 1])
            i += 2
        } else {
            out.append(c)
            i++
        }
    }
    return out.toString()
}
