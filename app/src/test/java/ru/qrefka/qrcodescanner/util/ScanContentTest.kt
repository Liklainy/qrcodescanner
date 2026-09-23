package ru.qrefka.qrcodescanner.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ScanContentTest {

    @Test
    fun `wifi payloads keep their own type`() {
        assertTrue(parseScanContent("WIFI:S:Net;T:WPA;P:pw;;") is ScanContent.Wifi)
    }

    @Test
    fun `web links stay links`() {
        assertEquals(ScanContent.Link("https://example.com"), parseScanContent("https://example.com"))
    }

    @Test
    fun `plain text stays text`() {
        assertEquals(ScanContent.Text("hello world"), parseScanContent("hello world"))
    }

    @Test
    fun `tel uri is a phone number with its plus kept`() {
        assertEquals(ScanContent.Phone("+79001234567"), parseScanContent("tel:+79001234567"))
    }

    @Test
    fun `smsto form carries number and body`() {
        assertEquals(ScanContent.Sms("+123", "Hi: there"), parseScanContent("SMSTO:+123:Hi: there"))
    }

    @Test
    fun `smsto body may contain a question mark`() {
        assertEquals(ScanContent.Sms("+123", "Are you coming?"), parseScanContent("SMSTO:+123:Are you coming?"))
    }

    @Test
    fun `sms uri form reads body from the query`() {
        assertEquals(ScanContent.Sms("+123", "Hi there"), parseScanContent("sms:+123?body=Hi%20there"))
    }

    @Test
    fun `mailto with subject and body`() {
        assertEquals(
            ScanContent.Email("a@b.com", "Hello", "Line one"),
            parseScanContent("mailto:a@b.com?subject=Hello&body=Line%20one")
        )
    }

    @Test
    fun `matmsg email`() {
        assertEquals(
            ScanContent.Email("a@b.com", "Sub", "Body"),
            parseScanContent("MATMSG:TO:a@b.com;SUB:Sub;BODY:Body;;")
        )
    }

    @Test
    fun `bare address is an email`() {
        assertEquals(ScanContent.Email("user@example.org", "", ""), parseScanContent("user@example.org"))
    }

    @Test
    fun `geo uri with a label`() {
        assertEquals(
            ScanContent.Location(55.7558, 37.6173, "Red Square"),
            parseScanContent("geo:55.7558,37.6173?q=Red%20Square")
        )
    }

    @Test
    fun `geo zero point with a query is an address search`() {
        val location = parseScanContent("geo:0,0?q=1600%20Amphitheatre%20Pkwy") as ScanContent.Location
        assertTrue(location.isAddressQuery)
        assertEquals("1600 Amphitheatre Pkwy", location.label)
    }

    @Test
    fun `geo with coordinates out of range falls back to a link`() {
        assertTrue(parseScanContent("geo:95,10") is ScanContent.Link)
    }

    @Test
    fun `vcard fields are read and unescaped`() {
        val card = """
            BEGIN:VCARD
            VERSION:3.0
            N:Doe;John;;Dr.;
            FN:John Doe
            ORG:Acme\, Inc.;Research
            TITLE:Engineer
            TEL;TYPE=CELL:+1 555 0100
            item1.TEL:+1 555 0101
            EMAIL:john@acme.test
            ADR;TYPE=WORK:;;1 Main St;Springfield;;12345;USA
            URL:https://acme.test
            NOTE:First line\nsecond line
            END:VCARD
        """.trimIndent()
        val contact = parseScanContent(card) as ScanContent.Contact
        assertEquals("John Doe", contact.name)
        assertEquals(listOf("+1 555 0100", "+1 555 0101"), contact.phones)
        assertEquals(listOf("john@acme.test"), contact.emails)
        assertEquals("Acme, Inc.", contact.organization)
        assertEquals("Engineer", contact.title)
        assertEquals("1 Main St, Springfield, 12345, USA", contact.address)
        assertEquals("https://acme.test", contact.url)
        assertEquals("First line\nsecond line", contact.note)
    }

    @Test
    fun `vcard without FN builds the name from N`() {
        val contact = parseScanContent("BEGIN:VCARD\r\nN:Doe;Jane\r\nEND:VCARD") as ScanContent.Contact
        assertEquals("Jane Doe", contact.name)
    }

    @Test
    fun `folded vcard lines are joined`() {
        val contact = parseScanContent("BEGIN:VCARD\nNOTE:long\n  note\nEND:VCARD") as ScanContent.Contact
        assertEquals("long note", contact.note)
    }

    @Test
    fun `quoted-printable vcard 2_1 fields are decoded`() {
        // As Android's own contact export writes a non-ASCII name, soft line break included.
        val card = "BEGIN:VCARD\r\nVERSION:2.1\r\n" +
            "N;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:=D0=98=D0=B2=D0=B0=D0=BD=D0=BE=D0=B2;=D0=98=\r\n" +
            "=D0=B2=D0=B0=D0=BD\r\n" +
            "NOTE;QUOTED-PRINTABLE:caf=C3=A9 =3D ok\r\n" +
            "END:VCARD"
        val contact = parseScanContent(card) as ScanContent.Contact
        assertEquals("Иван Иванов", contact.name)
        assertEquals("café = ok", contact.note)
    }

    @Test
    fun `quoted-printable soft break keeps a leading space on the next line`() {
        // Outlook and Nokia vCard 2.1 exports wrap quoted-printable text with literal spaces.
        val card = "BEGIN:VCARD\r\nVERSION:2.1\r\n" +
            "NOTE;ENCODING=QUOTED-PRINTABLE:Meet at=\r\n" +
            " AB Street, caf=\r\n" +
            "=C3=A9\r\n" +
            "END:VCARD"
        val contact = parseScanContent(card) as ScanContent.Contact
        assertEquals("Meet at AB Street, café", contact.note)
    }

    @Test
    fun `mecard contact`() {
        val contact = parseScanContent(
            "MECARD:N:Doe,John;TEL:+123;EMAIL:j@d.test;ADR:Somewhere\\, 1;;"
        ) as ScanContent.Contact
        assertEquals("John Doe", contact.name)
        assertEquals(listOf("+123"), contact.phones)
        assertEquals(listOf("j@d.test"), contact.emails)
        assertEquals("Somewhere, 1", contact.address)
    }

    @Test
    fun `utc event`() {
        val event = parseScanContent(
            """
            BEGIN:VEVENT
            SUMMARY:Standup
            DTSTART:20260115T090000Z
            DTEND:20260115T093000Z
            LOCATION:Room 1
            END:VEVENT
            """.trimIndent()
        ) as ScanContent.Event
        assertEquals("Standup", event.title)
        assertEquals(ZonedDateTime.of(2026, 1, 15, 9, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli(), event.start)
        assertEquals(ZonedDateTime.of(2026, 1, 15, 9, 30, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli(), event.end)
        assertFalse(event.allDay)
        assertEquals("Room 1", event.location)
    }

    @Test
    fun `event inside a calendar with a TZID`() {
        val event = parseScanContent(
            "BEGIN:VCALENDAR\nBEGIN:VEVENT\nSUMMARY:Call\nDTSTART;TZID=Europe/Moscow:20260115T120000\nEND:VEVENT\nEND:VCALENDAR"
        ) as ScanContent.Event
        val expected = ZonedDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneId.of("Europe/Moscow"))
        assertEquals(expected.toInstant().toEpochMilli(), event.start)
    }

    @Test
    fun `date-only event is all day at utc midnight`() {
        val event = parseScanContent(
            "BEGIN:VEVENT\nSUMMARY:Holiday\nDTSTART;VALUE=DATE:20260501\nDTEND;VALUE=DATE:20260502\nEND:VEVENT"
        ) as ScanContent.Event
        assertTrue(event.allDay)
        assertEquals(LocalDate.of(2026, 5, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), event.start)
    }

    @Test
    fun `digits from a retail barcode are a product`() {
        assertEquals(ScanContent.Product("4600000000001"), parseScanContent("4600000000001", isProductCode = true))
    }

    @Test
    fun `digits from a qr code are text`() {
        assertEquals(ScanContent.Text("4600000000001"), parseScanContent("4600000000001"))
    }
}
