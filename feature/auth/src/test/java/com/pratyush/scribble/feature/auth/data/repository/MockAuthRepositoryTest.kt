package com.pratyush.scribble.feature.auth.data.repository

import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.AuthState
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.UserProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockAuthRepositoryTest {

    @Test
    fun `initial authState is Unauthenticated when initialUser is null`() = runTest {
        val repository = MockAuthRepository(initialUser = null)
        assertEquals(AuthState.Unauthenticated, repository.authState.value)
    }

    @Test
    fun `initial authState is Authenticated when initialUser is provided`() = runTest {
        val user = UserProfile(id = "user_1", displayName = "Player1", avatarId = AvatarId.CAT)
        val repository = MockAuthRepository(initialUser = user)
        val state = repository.authState.value
        assertTrue(state is AuthState.Authenticated)
        assertEquals(user, (state as AuthState.Authenticated).user)
    }

    @Test
    fun `signInWithGoogle updates authState to Authenticated`() = runTest {
        val repository = MockAuthRepository(initialUser = null)
        val result = repository.signInWithGoogle()
        assertTrue(result is AppResult.Success)
        val state = repository.authState.value
        assertTrue(state is AuthState.Authenticated)
        assertEquals((result as AppResult.Success).data, (state as AuthState.Authenticated).user)
    }

    @Test
    fun `signInAsGuest updates authState to Authenticated`() = runTest {
        val repository = MockAuthRepository(initialUser = null)
        val result = repository.signInAsGuest("GuestDoodler")
        assertTrue(result is AppResult.Success)
        val state = repository.authState.value
        assertTrue(state is AuthState.Authenticated)
        assertEquals("GuestDoodler", (state as AuthState.Authenticated).user.displayName)
    }

    @Test
    fun `signOut updates authState to Unauthenticated`() = runTest {
        val user = UserProfile(id = "user_1", displayName = "Player1", avatarId = AvatarId.CAT)
        val repository = MockAuthRepository(initialUser = user)
        assertEquals(AuthState.Authenticated(user), repository.authState.value)

        repository.signOut()
        assertEquals(AuthState.Unauthenticated, repository.authState.value)
    }
}
