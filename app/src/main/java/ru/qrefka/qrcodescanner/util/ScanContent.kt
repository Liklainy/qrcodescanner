package ru.qrefka.qrcodescanner.util

import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** What a decoded payload turned out to be, with the fields each kind acts on. */
internal sealed interface ScanContent {
    data class Wifi(val credentials: WifiCredentials) : ScanContent
    data class Link(val uri: String) : ScanContent
    data class Phone(val number: String) : ScanContent
    data class Sms(val number: String, val body: String) : ScanContent
    data class Email(val address: String, val subject: String, val body: String) : ScanContent

    /** [label] is the `q=` query of a `geo:` URI, empty when there is none. */
    data class Location(val latitude: Double, val longitude: Double, val label: String) : ScanContent {
        /**
         * `geo:0,0?q=...` is the standard way to ask for a place by name: the
         * coordinates are a placeholder and [label] is what to search for.
         */
        val isAddressQuery: Boolean
            get() = latitude == 0.0 && longitude == 0.0 && label.isNotBlank()
    }

    data class Contact(
        val name: String,
        val phones: List<String>,
        val emails: List<String>,
        val organization: String,
        val title: String,
        val address: String,
        val url: String,
        val note: String,
    ) : ScanContent

    /**
     * Times are epoch millis. For an all-day event they are UTC midnights, which is
     * what the calendar provider expects, and [end] is exclusive.
     */
    data class Event(
        val title: String,
        val start: Long?,
        val end: Long?,
        val allDay: Boolean,
        val location: String,
        val description: String,
    ) : ScanContent

    /** A retail barcode (EAN, UPC): digits that only mean something to a search. */
    data class Product(val code: String) : ScanContent
    data class Text(val text: String) : ScanContent
}

/**
 * Classifies a decoded payload. [isProductCode] comes from the barcode format, since
 * a string of digits is a product number when it came off an EAN and plain text when
 * someone put it in a QR code.
 */
internal fun parseScanContent(text: String, isProductCode: Boolean = false): ScanContent {
    val trimmed = text.trim()
    parseWifiQr(trimmed)?.let { return ScanContent.Wifi(it) }
    return parseVCard(trimmed)
        ?: parseMeCard(trimmed)
        ?: parseVEvent(trimmed)
        ?: parseEmail(trimmed)
        ?: parsePhone(trimmed)
        ?: parseSms(trimmed)
        ?: parseGeo(trimmed)
        ?: trimmed.takeIf { isProductCode && it.all(Char::isDigit) }?.let { ScanContent.Product(it) }
        ?: trimmed.takeIf { isOpenableUri(it) }?.let { ScanContent.Link(it) }
        ?: ScanContent.Text(text)
}

// --- Email, phone, SMS, location ---------------------------------------------------

private val EMAIL_REGEX = Regex("^[^\\s@:/?]+@[^\\s@:/?]+\\.[^\\s@:/?]+$")

private fun parseEmail(text: String): ScanContent.Email? {
    stripPrefix(text, "mailto:")?.let { rest ->
        val (address, query) = splitQuery(rest)
        return ScanContent.Email(
            address = decode(address),
            subject = query["subject"].orEmpty(),
            body = query["body"].orEmpty()
        )
    }
    stripPrefix(text, "MATMSG:")?.let { rest ->
        val fields = fieldMap(rest)
        return ScanContent.Email(
            address = fields["TO"].orEmpty(),
            subject = fields["SUB"].orEmpty(),
            body = fields["BODY"].orEmpty()
        )
    }
    return if (EMAIL_REGEX.matches(text)) ScanContent.Email(text, "", "") else null
}

private fun parsePhone(text: String): ScanContent.Phone? =
    stripPrefix(text, "tel:")?.let { ScanContent.Phone(decode(it)) }?.takeIf { it.number.isNotBlank() }

/**
 * Covers both conventions in the wild: the URI form `sms:+123?body=Hi` and the
 * older `SMSTO:+123:Hi` that most generators still write. MMS variants read the same.
 */
private fun parseSms(text: String): ScanContent.Sms? {
    val rest = listOf("smsto:", "sms:", "mmsto:", "mms:").firstNotNullOfOrNull { stripPrefix(text, it) }
        ?: return null
    // A '?' after the number's ':' is part of an SMSTO message, not a URI query.
    val question = rest.indexOf('?')
    val colon = rest.indexOf(':')
    if (question >= 0 && (colon < 0 || question < colon)) {
        val (number, query) = splitQuery(rest)
        return ScanContent.Sms(decode(number), query["body"].orEmpty())
    }
    return if (colon < 0) {
        ScanContent.Sms(rest, "")
    } else {
        ScanContent.Sms(rest.substring(0, colon), rest.substring(colon + 1))
    }
}

private val GEO_REGEX = Regex("^(-?\\d+(?:\\.\\d+)?),\\s*(-?\\d+(?:\\.\\d+)?)")

private fun parseGeo(text: String): ScanContent.Location? {
    val rest = stripPrefix(text, "geo:") ?: return null
    val (coordinates, query) = splitQuery(rest)
    val match = GEO_REGEX.find(coordinates) ?: return null
    val latitude = match.groupValues[1].toDouble()
    val longitude = match.groupValues[2].toDouble()
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    return ScanContent.Location(latitude, longitude, query["q"].orEmpty())
}

// --- Contacts ----------------------------------------------------------------------

private fun parseVCard(text: String): ScanContent.Contact? {
    if (!text.startsWith("BEGIN:VCARD", ignoreCase = true)) return null
    val props = contentLines(text)
    fun first(name: String) = props.firstOrNull { it.name == name }
    fun all(name: String) = props.filter { it.name == name }

    val structuredName = first("N")?.let { n ->
        // N is family;given;additional;prefix;suffix - read out in speaking order.
        val parts = splitComponents(n.value).map(::unescapeText)
        listOf(3, 1, 2, 0, 4).mapNotNull { parts.getOrNull(it)?.takeIf(String::isNotBlank) }
            .joinToString(" ")
    }.orEmpty()
    val address = first("ADR")?.let { adr ->
        // ADR is pobox;extended;street;city;region;postcode;country.
        splitComponents(adr.value).map(::unescapeText).filter(String::isNotBlank).joinToString(", ")
    }.orEmpty()
    return ScanContent.Contact(
        name = first("FN")?.let { unescapeText(it.value) }?.takeIf(String::isNotBlank)
            ?: structuredName,
        phones = all("TEL").map { unescapeText(it.value).removePrefix("tel:") }.filter(String::isNotBlank),
        emails = all("EMAIL").map { unescapeText(it.value) }.filter(String::isNotBlank),
        // ORG can carry departments after the company name; the company is enough.
        organization = first("ORG")?.let { unescapeText(splitComponents(it.value).first()) }.orEmpty(),
        title = first("TITLE")?.let { unescapeText(it.value) }.orEmpty(),
        address = address,
        url = first("URL")?.let { unescapeText(it.value) }.orEmpty(),
        note = first("NOTE")?.let { unescapeText(it.value) }.orEmpty()
    )
}

/** DoCoMo's `MECARD:N:Doe,John;TEL:123;EMAIL:a@b.c;;`, still common on business cards. */
private fun parseMeCard(text: String): ScanContent.Contact? {
    val rest = stripPrefix(text, "MECARD:") ?: return null
    val fields = splitFields(rest).mapNotNull { field ->
        val separator = field.indexOf(':')
        if (separator <= 0) null
        else field.substring(0, separator).uppercase() to unescape(field.substring(separator + 1))
    }
    fun first(key: String) = fields.firstOrNull { it.first == key }?.second.orEmpty()
    fun all(key: String) = fields.filter { it.first == key && it.second.isNotBlank() }.map { it.second }
    // N is "Family,Given".
    val name = first("N").split(',').map(String::trim).reversed().filter(String::isNotEmpty)
        .joinToString(" ")
    return ScanContent.Contact(
        name = name,
        phones = all("TEL"),
        emails = all("EMAIL"),
        organization = first("ORG"),
        title = first("TITLE"),
        address = first("ADR"),
        url = first("URL"),
        note = first("NOTE")
    )
}

// --- Calendar events ---------------------------------------------------------------

private fun parseVEvent(text: String): ScanContent.Event? {
    val begin = text.indexOf("BEGIN:VEVENT", ignoreCase = true)
    if (begin < 0) return null
    val props = contentLines(text.substring(begin))
    fun first(name: String) = props.firstOrNull { it.name == name }
    val start = first("DTSTART")?.let(::parseTime)
    val end = first("DTEND")?.let(::parseTime)
    return ScanContent.Event(
        title = first("SUMMARY")?.let { unescapeText(it.value) }.orEmpty(),
        start = start?.millis,
        end = end?.millis,
        allDay = start?.dateOnly == true,
        location = first("LOCATION")?.let { unescapeText(it.value) }.orEmpty(),
        description = first("DESCRIPTION")?.let { unescapeText(it.value) }.orEmpty()
    )
}

private class ParsedTime(val millis: Long, val dateOnly: Boolean)

private val DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
private val DATE_TIME_NO_SECONDS = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm")
private val DATE = DateTimeFormatter.ofPattern("yyyyMMdd")

/**
 * iCalendar times come in three shapes: UTC with a trailing `Z`, local to the zone
 * named by a TZID parameter, or floating (no zone at all), which means the reader's
 * own zone. A bare date marks an all-day event.
 */
private fun parseTime(prop: ContentLine): ParsedTime? {
    val value = prop.value.trim()
    return try {
        if (value.length == 8) {
            val date = LocalDate.parse(value, DATE)
            return ParsedTime(date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), true)
        }
        val utc = value.endsWith("Z", ignoreCase = true)
        val local = value.removeSuffix("Z").removeSuffix("z")
        val dateTime = try {
            LocalDateTime.parse(local, DATE_TIME)
        } catch (_: DateTimeParseException) {
            LocalDateTime.parse(local, DATE_TIME_NO_SECONDS)
        }
        val zone = when {
            utc -> ZoneOffset.UTC
            else -> prop.params["TZID"]?.let { runCatching { ZoneId.of(it) }.getOrNull() }
                ?: ZoneId.systemDefault()
        }
        ParsedTime(dateTime.atZone(zone).toInstant().toEpochMilli(), false)
    } catch (_: DateTimeParseException) {
        null
    }
}

// --- vCard / iCalendar content lines -----------------------------------------------

private class ContentLine(val name: String, val params: Map<String, String>, val value: String)

/**
 * Splits vCard or iCalendar text into properties. Folded lines (a continuation starts
 * with a space or tab) are joined first. Group prefixes such as `item1.` are dropped,
 * and a bare vCard 2.1 parameter like `TEL;CELL:` is kept under its own name.
 *
 * vCard 2.1 values marked `ENCODING=QUOTED-PRINTABLE` - which is how Android's own
 * contact export writes any non-ASCII name - are decoded here, in the named CHARSET.
 * Their soft line breaks (a trailing `=`) are joined before the value is decoded, and
 * ahead of unfolding, so a continuation line that starts with a space keeps it.
 */
private fun contentLines(text: String): List<ContentLine> {
    val lines = text.replace("\r\n", "\n").replace('\r', '\n').lines()
    val result = mutableListOf<ContentLine>()
    var index = 0
    fun folded() = index < lines.size && lines[index].let { it.startsWith(' ') || it.startsWith('\t') }
    while (index < lines.size) {
        var line = lines[index++]
        while (':' !in line && folded()) line += lines[index++].substring(1)
        val colon = line.indexOf(':')
        if (colon <= 0) continue
        val head = line.substring(0, colon).split(';')
        val params = head.drop(1).associate { param ->
            val eq = param.indexOf('=')
            if (eq < 0) param.uppercase() to ""
            else param.substring(0, eq).uppercase() to param.substring(eq + 1).trim('"')
        }
        val quotedPrintable = params["ENCODING"].equals("QUOTED-PRINTABLE", ignoreCase = true) ||
            "QUOTED-PRINTABLE" in params
        var value = line.substring(colon + 1)
        while (true) {
            value = when {
                quotedPrintable && value.endsWith('=') && index < lines.size ->
                    value.dropLast(1) + lines[index++]
                folded() -> value + lines[index++].substring(1)
                else -> break
            }
        }
        if (quotedPrintable) value = decodeQuotedPrintable(value, params["CHARSET"])
        result.add(ContentLine(head[0].substringAfterLast('.').uppercase(), params, value))
    }
    return result
}

/** `=XX` hex escapes to bytes, read in [charsetName] (UTF-8 when absent or unknown). */
private fun decodeQuotedPrintable(value: String, charsetName: String?): String {
    val charset = charsetName
        ?.let { runCatching { Charset.forName(it) }.getOrNull() }
        ?: Charsets.UTF_8
    val bytes = ByteArrayOutputStream(value.length)
    var i = 0
    while (i < value.length) {
        val c = value[i]
        if (c == '=' && i + 2 < value.length) {
            val high = value[i + 1].digitToIntOrNull(16)
            val low = value[i + 2].digitToIntOrNull(16)
            if (high != null && low != null) {
                bytes.write(high * 16 + low)
                i += 3
                continue
            }
        }
        bytes.write(c.toString().toByteArray(charset))
        i++
    }
    return String(bytes.toByteArray(), charset)
}

/** Splits a structured value (N, ADR, ORG) on `;`, leaving escaped `\;` intact. */
private fun splitComponents(value: String): List<String> {
    val parts = mutableListOf<String>()
    val current = StringBuilder()
    var i = 0
    while (i < value.length) {
        val c = value[i]
        if (c == '\\' && i + 1 < value.length) {
            current.append(c).append(value[i + 1])
            i += 2
            continue
        }
        if (c == ';') {
            parts.add(current.toString())
            current.clear()
        } else {
            current.append(c)
        }
        i++
    }
    parts.add(current.toString())
    return parts
}

/** vCard/iCalendar text escapes: `\n` is a line break, anything else is literal. */
private fun unescapeText(value: String): String {
    if (!value.contains('\\')) return value.trim()
    val out = StringBuilder(value.length)
    var i = 0
    while (i < value.length) {
        val c = value[i]
        if (c == '\\' && i + 1 < value.length) {
            val next = value[i + 1]
            out.append(if (next == 'n' || next == 'N') '\n' else next)
            i += 2
        } else {
            out.append(c)
            i++
        }
    }
    return out.toString().trim()
}

// --- Helpers -----------------------------------------------------------------------

private fun stripPrefix(text: String, prefix: String): String? =
    if (text.startsWith(prefix, ignoreCase = true)) text.substring(prefix.length) else null

/** `KEY:value;` fields as in MATMSG, keyed in upper case. */
private fun fieldMap(body: String): Map<String, String> =
    splitFields(body).mapNotNull { field ->
        val separator = field.indexOf(':')
        if (separator <= 0) null
        else field.substring(0, separator).uppercase() to unescape(field.substring(separator + 1))
    }.toMap()

/** Splits `path?a=1&b=2` into the path and its decoded, lower-cased-key parameters. */
private fun splitQuery(text: String): Pair<String, Map<String, String>> {
    val question = text.indexOf('?')
    if (question < 0) return text to emptyMap()
    val query = text.substring(question + 1).split('&').mapNotNull { pair ->
        val eq = pair.indexOf('=')
        if (eq <= 0) null else pair.substring(0, eq).lowercase() to decode(pair.substring(eq + 1))
    }.toMap()
    return text.substring(0, question) to query
}

/**
 * Percent-decoding for URI parts. URLDecoder is a form decoder and would turn `+`
 * into a space, which would mangle phone numbers, so a literal `+` is protected first.
 */
private fun decode(value: String): String =
    try {
        URLDecoder.decode(value.replace("+", "%2B"), "UTF-8")
    } catch (_: IllegalArgumentException) {
        value
    }
