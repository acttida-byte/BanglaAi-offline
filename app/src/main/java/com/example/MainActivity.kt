package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppTab
import com.example.ui.BondhuUiState
import com.example.ui.BondhuViewModel
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.MediaStudioScreen
import com.example.ui.screens.PinLockScreen
import com.example.ui.screens.SecurityVaultScreen
import com.example.ui.screens.TextStudioScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: BondhuViewModel = viewModel(
                factory = BondhuViewModel.provideFactory(applicationContext)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.securitySettings.isScreenPrivacyEnabled) {
                if (uiState.securitySettings.isScreenPrivacyEnabled) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            MyApplicationTheme {
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BondhuRootApp(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

private data class NavDestination(
    val tab: AppTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
private fun BondhuRootApp(
    uiState: BondhuUiState,
    viewModel: BondhuViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    if (uiState.isLocked) {
        PinLockScreen(
            errorMessage = uiState.pinError,
            onPinSubmitted = viewModel::unlockWithPin
        )
        return
    }

    if (uiState.selectedTab != AppTab.CHAT) {
        BackHandler { viewModel.selectTab(AppTab.CHAT) }
    }

    val destinations = remember {
        listOf(
            NavDestination(
                tab = AppTab.CHAT,
                labelRes = R.string.nav_chat,
                selectedIcon = Icons.Filled.ChatBubble,
                unselectedIcon = Icons.Outlined.ChatBubbleOutline
            ),
            NavDestination(
                tab = AppTab.TEXT,
                labelRes = R.string.nav_text_studio,
                selectedIcon = Icons.Filled.Psychology,
                unselectedIcon = Icons.Outlined.Psychology
            ),
            NavDestination(
                tab = AppTab.MEDIA,
                labelRes = R.string.nav_media_studio,
                selectedIcon = Icons.Filled.AutoAwesome,
                unselectedIcon = Icons.Outlined.AutoAwesome
            ),
            NavDestination(
                tab = AppTab.SECURITY,
                labelRes = R.string.nav_security,
                selectedIcon = Icons.Filled.Security,
                unselectedIcon = Icons.Outlined.Security
            )
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        destinations.forEach { dest ->
                            val selected = uiState.selectedTab == dest.tab
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.testTag("nav_${dest.tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        destinations.forEach { dest ->
                            val selected = uiState.selectedTab == dest.tab
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_${dest.tab.name.lowercase()}")
                            )
                        }
                    }
                }

                when (uiState.selectedTab) {
                    AppTab.CHAT -> ChatScreen(
                        uiState = uiState,
                        onSendMessage = viewModel::sendMessage,
                        onPokeAvatar = viewModel::pokeAiAvatar,
                        onApologize = viewModel::apologizeToAi,
                        onClearChat = viewModel::clearChat,
                        onLockApp = viewModel::lockAppNow,
                        modifier = Modifier.weight(1f)
                    )

                    AppTab.TEXT -> TextStudioScreen(
                        uiState = uiState,
                        onTextChange = viewModel::updateTextInput,
                        onLoadSample = viewModel::loadSampleBengaliText,
                        onRunTask = viewModel::runTextTask,
                        onSaveToVault = viewModel::saveProcessedTextToVault,
                        modifier = Modifier.weight(1f)
                    )

                    AppTab.MEDIA -> MediaStudioScreen(
                        uiState = uiState,
                        onSelectSubTab = viewModel::selectMediaSubTab,
                        onImagePromptChange = viewModel::updateImagePrompt,
                        onSelectArtStyle = viewModel::selectArtStyle,
                        onSelectArtAspect = viewModel::selectArtAspect,
                        onGenerateImage = viewModel::generateOfflineImage,
                        onUseImageInVideo = viewModel::useImageInVideoMaker,
                        onAddPickedPhotos = viewModel::addPickedPhotosToVideo,
                        onRemoveVideoSlide = viewModel::removeVideoSlide,
                        onVideoTitleChange = viewModel::updateVideoTitle,
                        onVideoCaptionChange = viewModel::updateVideoCaption,
                        onVideoDurationChange = viewModel::setVideoSlideDuration,
                        onGenerateVideo = viewModel::generateMp4Video,
                        onOpenVideoPlayer = viewModel::openVideoPlayer,
                        onDeleteCreation = viewModel::deleteCreation,
                        modifier = Modifier.weight(1f)
                    )

                    AppTab.SECURITY -> SecurityVaultScreen(
                        uiState = uiState,
                        onSetupPin = viewModel::setupNewPin,
                        onDisablePin = viewModel::disablePin,
                        onLockNow = viewModel::lockAppNow,
                        onToggleScreenPrivacy = viewModel::toggleScreenPrivacy,
                        onDeleteVaultNote = viewModel::deleteVaultNote,
                        onWipeAllData = viewModel::wipeAllData,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
