package com.ugurbuga.blockgames.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.app_title_puzzle_shift
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.localization.LocalAppSettings
import com.ugurbuga.blockgames.localization.appStringResource
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.ui.game.BlockCellPreview
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens

val AppFooterBannerHeight = 50.dp

@Composable
fun AppFooterAdSlot(
    adController: GameAdController,
    onOpenSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (adController.bannerPresentationMode == BannerPresentationMode.External) {
        return
    }

    var bannerEverLoaded by remember { mutableStateOf(false) }
    val uiColors = BlockGamesThemeTokens.uiColors

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(uiColors.panel.copy(alpha = 0.96f)),
        color = uiColors.panel.copy(alpha = 0.96f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppFooterBannerHeight),
        ) {
            if (!bannerEverLoaded) {
                AppBrandedBannerPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                    onOpenSelection = onOpenSelection,
                )
            }
            adController.Banner(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (bannerEverLoaded) 1f else 0f },
                onLoadStateChanged = { loaded ->
                    if (loaded) {
                        bannerEverLoaded = true
                    }
                },
            )
        }
    }
}

@Composable
private fun AppBrandedBannerPlaceholder(
    modifier: Modifier = Modifier,
    onOpenSelection: () -> Unit,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val settings = LocalAppSettings.current
    Surface(
        modifier = modifier,
        color = uiColors.panel.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onOpenSelection)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            uiColors.panelHighlight.copy(alpha = 0.26f),
                            uiColors.launchGlow.copy(alpha = 0.14f),
                            uiColors.gameSurface.copy(alpha = 0.18f),
                        ),
                    ),
                )
                .padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(
                        CellTone.Cyan to SpecialBlockType.ColumnClearer,
                        CellTone.Gold to SpecialBlockType.RowClearer,
                        CellTone.Violet to SpecialBlockType.Ghost,
                        CellTone.Coral to SpecialBlockType.Heavy,
                    ).forEachIndexed { index, (tone, special) ->
                        BlockCellPreview(
                            tone = tone,
                            palette = settings.blockColorPalette,
                            style = settings.blockVisualStyle,
                            size = if (index in 1..2) 17.dp else 18.dp,
                            special = special,
                        )
                    }
                }
                Text(
                    text = appStringResource(Res.string.app_title_puzzle_shift),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = uiColors.gameSurface.copy(alpha = 0.84f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).size(18.dp),
                    )
                }
            }
        }
    }
}

private class PreviewBannerAdController(
    private val showBanner: Boolean,
    override val bannerPresentationMode: BannerPresentationMode = BannerPresentationMode.Inline,
) : GameAdController {
    override fun showRestartInterstitial(onFinished: () -> Unit) = onFinished()

    override fun showRewardedRevive(onResult: (Boolean) -> Unit) = onResult(false)

    override fun showRewardedAd(onResult: (Boolean) -> Unit) = onResult(false)

    @Composable
    override fun Banner(modifier: Modifier, onLoadStateChanged: (Boolean) -> Unit) {
        SideEffect {
            onLoadStateChanged(showBanner)
        }
        if (!showBanner) return

        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Preview Banner Ad",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "320×50",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Preview
@Composable
private fun AppFooterAdSlotPlaceholderPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        AppFooterAdSlot(
            adController = PreviewBannerAdController(showBanner = false),
            onOpenSelection = {},
        )
    }
}

@Preview
@Composable
private fun AppFooterAdSlotLoadedBannerPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        AppFooterAdSlot(
            adController = PreviewBannerAdController(showBanner = true),
            onOpenSelection = {},
        )
    }
}

