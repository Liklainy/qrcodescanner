package ru.qrefka.qrcodescanner.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UriUtilsTest {

    @Test
    fun `common schemes are openable`() {
        listOf(
            "https://example.com",
            "http://example.com/path?q=1#frag",
            "mailto:someone@example.com",
            "tel:+79001234567",
            "geo:55.75,37.61",
            "WIFI:S:net;T:WPA;P:secret;;",
            "market://details?id=ru.qrefka.qrcodescanner",
            "x-custom.scheme+v2:payload"
        ).forEach { assertTrue(it, isOpenableUri(it)) }
    }

    @Test
    fun `surrounding whitespace is ignored`() {
        assertTrue(isOpenableUri("  https://example.com\n"))
    }

    @Test
    fun `plain text is not openable`() {
        listOf(
            "",
            "   ",
            "hello world",
            "just some text",
            "1234567890",
            "example.com"
        ).forEach { assertFalse(it, isOpenableUri(it)) }
    }

    @Test
    fun `prose containing a colon is not openable`() {
        assertFalse(isOpenableUri("Note: buy milk"))
        assertFalse(isOpenableUri("Meeting at 10:30 tomorrow"))
    }

    @Test
    fun `scheme must start the string and begin with a letter`() {
        assertFalse(isOpenableUri("1http://example.com"))
        assertFalse(isOpenableUri("://example.com"))
        assertFalse(isOpenableUri("-mailto:someone@example.com"))
    }

    @Test
    fun `hyphenated prefix still forms a valid scheme`() {
        // RFC 3986 allows "-" inside a scheme, so this is openable by the same rule
        // as any custom scheme; a device with no handler falls back to a toast.
        assertTrue(isOpenableUri("see-https://example.com"))
    }

    @Test
    fun `app deep links are openable`() {
        listOf(
            "tg://resolve?domain=telegram",
            "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP",
            "bitcoin:1BoatSLRHtKNngkdXEeobR76b53LETtpyT"
        ).forEach { assertTrue(it, isOpenableUri(it)) }
    }

    @Test
    fun `dangerous pseudo-schemes are not openable`() {
        listOf(
            "javascript:alert(1)",
            "file:///sdcard/secret.txt",
            "content://com.example.provider/data",
            "data:text/html,Hello",
            "blob:https://example.com/uuid",
            "about:blank"
        ).forEach { assertFalse(it, isOpenableUri(it)) }
    }
}

