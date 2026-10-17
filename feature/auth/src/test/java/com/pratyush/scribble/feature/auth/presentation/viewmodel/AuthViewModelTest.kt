package com.pratyush.scribble.feature.auth.presentation.viewmodel

import com.pratyush.scribble.core.common.result.AppResult
import com.pratyush.scribble.core.model.AvatarId
import com.pratyush.scribble.core.model.UserProfile
import com.pratyush.scribble.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        viewModel = AuthViewModel(fakeAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is not loading and has no user or error`() = runTest {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.authenticatedUser)
        assertNull(state.errorMessage)
    }

    @Test
    fun `when repository has initial user, uiState observes authenticatedUser`() = runTest {
        val existingUser = UserProfile(id = "user_456", displayName = "ExistingPlayer", avatarId = AvatarId.FOX)
        fakeAuthRepository.setCurrentUser(existingUser)

        val newViewModel = AuthViewModel(fakeAuthRepository)
        advanceUntilIdle()

        assertEquals(existingUser, newViewModel.uiState.value.authenticatedUser)
    }

    @Test
    fun `google sign in success updates authenticatedUser and stops loading`() = runTest {
        val expectedUser = UserProfile(id = "user_123", displayName = "GoogleUser", avatarId = AvatarId.FOX)
        fakeAuthRepository.googleSignInResult = AppResult.Success(expectedUser)

        viewModel.onGoogleSignInClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(expectedUser, state.authenticatedUser)
        assertNull(state.errorMessage)
    }

    @Test
    fun `google sign in cancellation sets error message and stops loading`() = runTest {
        fakeAuthRepository.googleSignInResult = AppResult.Error("Sign-in cancelled")

        viewModel.onGoogleSignInClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.authenticatedUser)
        assertEquals("Sign-in cancelled", state.errorMessage)
    }

    @Test
    fun `guest sign in success updates authenticatedUser`() = runTest {
        val guestUser = UserProfile(id = "guest_1", displayName = "SpeedyDoodler", avatarId = AvatarId.CAT)
        fakeAuthRepository.guestSignInResult = AppResult.Success(guestUser)

        viewModel.onGuestSignInClicked("SpeedyDoodler")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(guestUser, state.authenticatedUser)
        assertNull(state.errorMessage)
    }

    @Test
    fun `guest sign in failure sets error message`() = runTest {
        fakeAuthRepository.guestSignInResult = AppResult.Error("Network error")

        viewModel.onGuestSignInClicked("Guest")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Network error", state.errorMessage)
    }

    @Test
    fun `dismiss error clears errorMessage in uiState`() = runTest {
        fakeAuthRepository.googleSignInResult = AppResult.Error("Some error")
        viewModel.onGoogleSignInClicked()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.onDismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    private class FakeAuthRepository : AuthRepository {
        private val _currentUser = MutableStateFlow<UserProfile?>(null)
        override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

        var googleSignInResult: AppResult<UserProfile> = AppResult.Error("Not configured")
        var guestSignInResult: AppResult<UserProfile> = AppResult.Error("Not configured")

        fun setCurrentUser(user: UserProfile?) {
            _currentUser.value = user
        }

        override suspend fun signInWithGoogle(): AppResult<UserProfile> {
            if (googleSignInResult is AppResult.Success) {
                _currentUser.value = (googleSignInResult as AppResult.Success<UserProfile>).data
            }
            return googleSignInResult
        }

        override suspend fun signInAsGuest(displayName: String): AppResult<UserProfile> {
            if (guestSignInResult is AppResult.Success) {
                _currentUser.value = (guestSignInResult as AppResult.Success<UserProfile>).data
            }
            return guestSignInResult
        }

        override suspend fun signOut(): AppResult<Unit> {
            _currentUser.value = null
            return AppResult.Success(Unit)
        }

        override suspend fun getIdToken(forceRefresh: Boolean): AppResult<String> {
            val user = _currentUser.value
            return if (user != null) {
                AppResult.Success("fake_id_token_${user.id}")
            } else {
                AppResult.Error("No user logged in")
            }
        }
    }
}
