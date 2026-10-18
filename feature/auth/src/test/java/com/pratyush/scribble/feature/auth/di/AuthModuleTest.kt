package com.pratyush.scribble.feature.auth.di

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthModuleTest {

    @Test
    fun `resolveWebClientId returns trimmed explicit webClientId when provided`() {
        var fallbackCalled = false
        val resolved = AuthModule.resolveWebClientId(
            explicitWebClientId = "  test-web-client-id.apps.googleusercontent.com  ",
            resourceLookup = {
                fallbackCalled = true
                "fallback-client-id"
            }
        )

        assertEquals("test-web-client-id.apps.googleusercontent.com", resolved)
        assertEquals(false, fallbackCalled)
    }

    @Test
    fun `resolveWebClientId falls back to resource lookup when explicit webClientId is null or blank`() {
        val resolvedFromNull = AuthModule.resolveWebClientId(
            explicitWebClientId = null,
            resourceLookup = { "  resource-web-client-id.apps.googleusercontent.com  " }
        )
        val resolvedFromBlank = AuthModule.resolveWebClientId(
            explicitWebClientId = "   ",
            resourceLookup = { "resource-web-client-id.apps.googleusercontent.com" }
        )

        assertEquals("resource-web-client-id.apps.googleusercontent.com", resolvedFromNull)
        assertEquals("resource-web-client-id.apps.googleusercontent.com", resolvedFromBlank)
    }
}
