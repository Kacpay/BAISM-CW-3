package com.example.baism_cw_3

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.security.cert.CertPathValidatorException
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLHandshakeException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalTlsTest {
    @Test
    fun checksPinnedApiThroughTlsTunnel() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("labTlsTunnel") == "true")

        val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("10.0.2.2", 8081))
        val url = URL("https://rickandmortyapi.com/api/character/1")
        val connection = url.openConnection(proxy) as HttpsURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.instanceFollowRedirects = false

        try {
            val status = connection.responseCode
            Log.i("Lab3TLS", "Tunel HTTPS: GET $url -> HTTP $status")
            assertEquals(200, status)
        } finally {
            connection.disconnect()
        }
    }

    @Test
    fun checksTrustForLocalServer() {
        val expected = InstrumentationRegistry.getArguments().getString("labTlsExpected")
        assumeNotNull(expected)
        require(expected in listOf("untrusted", "trusted", "pin-mismatch"))

        val url = URL("https://10.0.2.2:8443/")
        val connection = url.openConnection() as HttpsURLConnection
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        connection.instanceFollowRedirects = false

        val observed = try {
            assertEquals(200, connection.responseCode)
            "trusted"
        } catch (error: SSLHandshakeException) {
            val causes = generateSequence<Throwable>(error) { it.cause }.toList()
            when {
                causes.any { it.message?.contains("Pin verification failed", ignoreCase = true) == true } -> {
                    Log.i("Lab3TLS", "Odrzucono certyfikat: niezgodny pin (Pin verification failed).")
                    "pin-mismatch"
                }
                else -> {
                    assertTrue(
                        "Oczekiwano błędu weryfikacji ścieżki certyfikatu: ${error.message}",
                        causes.any { it is CertPathValidatorException }
                    )
                    Log.i("Lab3TLS", "Odrzucono certyfikat: ${error.message}")
                    "untrusted"
                }
            }
        } finally {
            connection.disconnect()
        }

        Log.i("Lab3TLS", "$url: oczekiwano $expected, wynik $observed")
        assertEquals(expected, observed)
    }
}
