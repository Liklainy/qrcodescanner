package ru.qrefka.qrcodescanner.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiQrTest {

    @Test
    fun `parses a standard payload`() {
        val wifi = parseWifiQr("WIFI:S:MyNet;T:WPA;P:s3cret;H:false;;")!!
        assertEquals("MyNet", wifi.ssid)
        assertEquals("s3cret", wifi.password)
        assertEquals(WifiSecurity.WPA, wifi.security)
        assertFalse(wifi.hidden)
    }

    @Test
    fun `field order does not matter and hidden is honoured`() {
        val wifi = parseWifiQr("WIFI:P:pass word;H:true;S:Cafe Net;T:WPA2;;")!!
        assertEquals("Cafe Net", wifi.ssid)
        assertEquals("pass word", wifi.password)
        assertEquals(WifiSecurity.WPA, wifi.security)
        assertTrue(wifi.hidden)
    }

    @Test
    fun `backslash escapes are unescaped in values`() {
        val wifi = parseWifiQr("""WIFI:S:Guest\;Net;T:WPA;P:a\\b\:c\,d;;""")!!
        assertEquals("Guest;Net", wifi.ssid)
        assertEquals("""a\b:c,d""", wifi.password)
    }

    @Test
    fun `open networks carry no password`() {
        val nopass = parseWifiQr("WIFI:S:FreeWifi;T:nopass;;")!!
        assertEquals(WifiSecurity.OPEN, nopass.security)
        assertEquals("", nopass.password)

        // A missing T field means an open network too.
        assertEquals(WifiSecurity.OPEN, parseWifiQr("WIFI:S:FreeWifi;;")!!.security)
    }

    @Test
    fun `security types map to the schemes the connect flow supports`() {
        assertEquals(WifiSecurity.WEP, parseWifiQr("WIFI:S:N;T:WEP;P:p;;")!!.security)
        assertEquals(WifiSecurity.SAE, parseWifiQr("WIFI:S:N;T:SAE;P:p;;")!!.security)
        assertEquals(WifiSecurity.SAE, parseWifiQr("WIFI:S:N;T:WPA3;P:p;;")!!.security)
        assertEquals(
            WifiSecurity.ENTERPRISE,
            parseWifiQr("WIFI:S:N;T:WPA2-EAP;P:p;;")!!.security
        )
    }

    @Test
    fun `prefix and field keys are case insensitive`() {
        val wifi = parseWifiQr("wifi:s:MyNet;t:wpa;p:secret;;")!!
        assertEquals("MyNet", wifi.ssid)
        assertEquals("secret", wifi.password)
        assertEquals(WifiSecurity.WPA, wifi.security)
    }

    @Test
    fun `trailing separators and surrounding whitespace are tolerated`() {
        assertEquals("MyNet", parseWifiQr("  WIFI:S:MyNet;T:WPA;P:p;;  \n")!!.ssid)
        assertEquals("MyNet", parseWifiQr("WIFI:S:MyNet;T:WPA;P:p")!!.ssid)
    }

    @Test
    fun `non-wifi payloads are rejected`() {
        assertNull(parseWifiQr("https://example.com"))
        assertNull(parseWifiQr("just some text"))
        assertNull(parseWifiQr(""))
        assertNull(parseWifiQr("WIFIS:MyNet;;"))
    }

    @Test
    fun `a payload without an SSID is rejected`() {
        assertNull(parseWifiQr("WIFI:T:WPA;P:secret;;"))
        assertNull(parseWifiQr("WIFI:S:;T:WPA;P:secret;;"))
    }
}
