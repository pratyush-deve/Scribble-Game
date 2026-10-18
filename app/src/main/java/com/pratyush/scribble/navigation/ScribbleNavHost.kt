package com.pratyush.scribble.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.compose.ui.platform.LocalContext
import com.pratyush.scribble.core.model.AuthState
import com.pratyush.scribble.core.navigation.ScreenDestination
import com.pratyush.scribble.feature.auth.di.AuthModule
import com.pratyush.scribble.feature.auth.presentation.screen.AuthScreen
import com.pratyush.scribble.feature.auth.presentation.viewmodel.AuthViewModel
import com.pratyush.scribble.feature.game.presentation.screen.GameScreen
import com.pratyush.scribble.feature.game.presentation.viewmodel.GameViewModel
import com.pratyush.scribble.feature.home.presentation.screen.HomeScreen
import com.pratyush.scribble.feature.home.presentation.viewmodel.HomeViewModel
import com.pratyush.scribble.feature.lobby.presentation.screen.LobbyScreen
import com.pratyush.scribble.feature.lobby.presentation.viewmodel.LobbyViewModel
import com.pratyush.scribble.feature.profile.presentation.screen.ProfileScreen
import com.pratyush.scribble.feature.profile.presentation.viewmodel.ProfileViewModel
import com.pratyush.scribble.feature.results.presentation.screen.ResultsScreen
import com.pratyush.scribble.feature.results.presentation.viewmodel.ResultsViewModel
import com.pratyush.scribble.ui.SplashScreen
import kotlinx.coroutines.launch

@Composable
fun ScribbleNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val profileRepository = androidx.compose.runtime.remember {
        com.pratyush.scribble.core.network.di.NetworkModule.provideProfileRepository()
    }
    val authRepository = androidx.compose.runtime.remember(context, profileRepository) {
        AuthModule.provideAuthRepository(context, profileRepository)
    }
    val roomRepository = androidx.compose.runtime.remember {
        com.pratyush.scribble.core.network.di.NetworkModule.provideRoomRepository()
    }
    val gameRepository = androidx.compose.runtime.remember {
        com.pratyush.scribble.core.network.di.NetworkModule.provideGameRepository()
    }

    val authState by authRepository.authState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = ScreenDestination.Splash,
        modifier = modifier
    ) {
        composable<ScreenDestination.Splash> {
            LaunchedEffect(authState) {
                when (authState) {
                    is AuthState.Loading -> {
                        // Awaiting initial authentication resolution from Firebase
                    }
                    is AuthState.Authenticated -> {
                        navController.navigate(ScreenDestination.Home) {
                            popUpTo(ScreenDestination.Splash) { inclusive = true }
                        }
                    }
                    is AuthState.Unauthenticated -> {
                        navController.navigate(ScreenDestination.Auth) {
                            popUpTo(ScreenDestination.Splash) { inclusive = true }
                        }
                    }
                }
            }

            SplashScreen()
        }
        composable<ScreenDestination.Auth> {
            val activityContext = LocalContext.current
            val authViewModel = viewModel { AuthViewModel(authRepository) }
            val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

            AuthScreen(
                uiState = uiState,
                onGoogleSignInClicked = { authViewModel.onGoogleSignInClicked(activityContext) },
                onGuestSignInClicked = authViewModel::onGuestSignInClicked,
                onNavigateToHome = {
                    navController.navigate(ScreenDestination.Home) {
                        popUpTo(ScreenDestination.Auth) { inclusive = true }
                    }
                }
            )
        }

        composable<ScreenDestination.Home> {
            val activityContext = LocalContext.current
            val homeViewModel = viewModel { HomeViewModel(authRepository, roomRepository, profileRepository) }
            val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

            HomeScreen(
                uiState = uiState,
                onHostRoomClicked = homeViewModel::onHostRoomClicked,
                onJoinRoomClicked = homeViewModel::onJoinRoomClicked,
                onHowToPlayClicked = homeViewModel::onHowToPlayClicked,
                onDismissHowToPlay = homeViewModel::onDismissHowToPlayDialog,
                onProfileClicked = { navController.navigate(ScreenDestination.Profile) },
                onRoomCodeChanged = homeViewModel::onRoomCodeChanged,
                onConfirmJoinRoom = {
                    homeViewModel.validateAndJoinRoom { code ->
                        navController.navigate(ScreenDestination.Lobby(code))
                    }
                },
                onConfirmHostRoom = {
                    homeViewModel.createHostRoom { code ->
                        navController.navigate(ScreenDestination.Lobby(code))
                    }
                },
                onDismissDialogs = homeViewModel::onDismissDialogs,
                onUpgradeToGoogleClicked = { homeViewModel.onUpgradeToGoogleClicked(activityContext) },
                onDismissGuestLimitDialog = homeViewModel::onDismissGuestLimitDialog
            )
        }

        composable<ScreenDestination.Profile> {
            val profileViewModel = viewModel {
                ProfileViewModel(
                    authRepository = authRepository,
                    profileRepository = profileRepository,
                    roomRepository = roomRepository
                )
            }
            val uiState by profileViewModel.uiState.collectAsStateWithLifecycle()

            ProfileScreen(
                uiState = uiState,
                onAvatarSelected = profileViewModel::onAvatarSelected,
                onDisplayNameChanged = profileViewModel::onDisplayNameChanged,
                onSaveProfile = {
                    profileViewModel.onSaveProfile {
                        navController.popBackStack()
                    }
                },
                onSignOut = {
                    coroutineScope.launch {
                        authRepository.signOut()
                        navController.navigate(ScreenDestination.Auth) {
                            popUpTo(ScreenDestination.Home) { inclusive = true }
                        }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<ScreenDestination.Lobby> { backStackEntry ->
            val lobbyDestination = backStackEntry.toRoute<ScreenDestination.Lobby>()
            val lobbyViewModel = viewModel {
                LobbyViewModel(
                    initialRoomCode = lobbyDestination.roomCode,
                    authRepository = authRepository,
                    roomRepository = roomRepository,
                    profileRepository = profileRepository
                )
            }
            val uiState by lobbyViewModel.uiState.collectAsStateWithLifecycle()

            LobbyScreen(
                uiState = uiState,
                onRoundsChanged = lobbyViewModel::onRoundsChanged,
                onDrawTimeChanged = lobbyViewModel::onDrawTimeChanged,
                onDifficultyChanged = lobbyViewModel::onDifficultyChanged,
                onAvatarSelected = lobbyViewModel::onAvatarSelected,
                onStartGame = lobbyViewModel::onStartGameClicked,
                onLeaveRoom = {
                    lobbyViewModel.onLeaveRoom {
                        navController.popBackStack()
                    }
                },
                onNavigateToGame = { roomCode ->
                    navController.navigate(ScreenDestination.Game(roomCode)) {
                        popUpTo(ScreenDestination.Lobby(roomCode)) { inclusive = true }
                    }
                }
            )
        }

        composable<ScreenDestination.Game> { backStackEntry ->
            val gameDestination = backStackEntry.toRoute<ScreenDestination.Game>()
            val gameViewModel = viewModel {
                GameViewModel(
                    roomCode = gameDestination.roomCode,
                    authRepository = authRepository,
                    roomRepository = roomRepository,
                    gameRepository = gameRepository
                )
            }
            val uiState by gameViewModel.gameUiState.collectAsStateWithLifecycle()
            val canvasState by gameViewModel.canvasState.collectAsStateWithLifecycle()

            GameScreen(
                uiState = uiState,
                canvasState = canvasState,
                onStrokeCompleted = gameViewModel::onStrokeCompleted,
                onEraserDragSegment = gameViewModel::onEraserDragSegment,
                onEraserGestureCompleted = gameViewModel::onEraserGestureCompleted,
                onUndoClicked = gameViewModel::onUndoClicked,
                onClearClicked = gameViewModel::onClearClicked,
                onToolSelected = gameViewModel::onToolSelected,
                onColorSelected = gameViewModel::onColorSelected,
                onStrokeWidthSelected = gameViewModel::onStrokeWidthSelected,
                onWordSelected = gameViewModel::onWordSelected,
                onChatInputChanged = gameViewModel::onChatInputChanged,
                onSendGuess = gameViewModel::onSendGuess,
                onRequestLeaveGame = gameViewModel::onRequestLeaveGame,
                onConfirmLeaveGame = gameViewModel::onConfirmLeaveGame,
                onDismissLeaveConfirmation = gameViewModel::onDismissLeaveConfirmation,
                onDepartedNavigateHome = {
                    navController.navigate(ScreenDestination.Home) {
                        popUpTo(ScreenDestination.Home) { inclusive = true }
                    }
                },
                onNavigateToResults = { roomCode, defaultWinReason ->
                    navController.navigate(ScreenDestination.Results(roomCode, defaultWinReason)) {
                        popUpTo(ScreenDestination.Game(roomCode)) { inclusive = true }
                    }
                }
            )
        }

        composable<ScreenDestination.Results> { backStackEntry ->
            val resultsDestination = backStackEntry.toRoute<ScreenDestination.Results>()
            val resultsViewModel = viewModel {
                ResultsViewModel(
                    roomCode = resultsDestination.roomCode,
                    winReason = resultsDestination.winReason,
                    roomRepository = roomRepository,
                    gameRepository = gameRepository,
                    authRepository = authRepository
                )
            }
            val uiState by resultsViewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.shouldNavigateHome) {
                if (uiState.shouldNavigateHome) {
                    navController.navigate(ScreenDestination.Home) {
                        popUpTo(ScreenDestination.Home) { inclusive = true }
                    }
                }
            }

            LaunchedEffect(uiState.isGameRestarted) {
                if (uiState.isGameRestarted) {
                    navController.navigate(ScreenDestination.Game(resultsDestination.roomCode)) {
                        popUpTo(ScreenDestination.Home) { inclusive = false }
                    }
                }
            }

            ResultsScreen(
                uiState = uiState,
                onPlayAgain = resultsViewModel::onToggleVotePlayAgain,
                onReturnToHome = resultsViewModel::onReturnToHome
            )
        }
    }
}
