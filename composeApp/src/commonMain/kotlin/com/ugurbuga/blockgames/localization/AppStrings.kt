package com.ugurbuga.blockgames.localization

import androidx.compose.runtime.Composable
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.app_title_chainshift
import blockgames.composeapp.generated.resources.app_title_blockwise
import blockgames.composeapp.generated.resources.app_title_blocksort
import blockgames.composeapp.generated.resources.app_title_boomblocks
import blockgames.composeapp.generated.resources.app_title_mergeshift
import blockgames.composeapp.generated.resources.app_title_stackshift
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun appStringResource(resource: StringResource, vararg args: Any): String {
    return formatAppString(stringResource(resource, *args))
}

@Composable
fun appNameStringResource(): String {
    return formatAppString(stringResource(appNameResourceId()))
}

fun appNameResourceId(): StringResource {
    return when (GlobalPlatformConfig.gameplayStyle) {
        GameplayStyle.BlockWise -> Res.string.app_title_blockwise
        GameplayStyle.ChainShift -> Res.string.app_title_chainshift
        GameplayStyle.MergeShift -> Res.string.app_title_mergeshift
        GameplayStyle.BoomBlocks -> Res.string.app_title_boomblocks
        GameplayStyle.BlockSort -> Res.string.app_title_blocksort
        else -> Res.string.app_title_stackshift
    }
}

@Composable
fun formatAppString(raw: String): String {
    val style = GlobalPlatformConfig.gameplayStyle
    val stackShiftTitle = stringResource(Res.string.app_title_stackshift)
    
    return when (style) {
        GameplayStyle.BlockWise -> {
            raw.replace(stackShiftTitle, stringResource(Res.string.app_title_blockwise))
        }
        GameplayStyle.ChainShift -> {
            raw.replace(stackShiftTitle, stringResource(Res.string.app_title_chainshift))
        }
        GameplayStyle.MergeShift -> {
            raw.replace(stackShiftTitle, stringResource(Res.string.app_title_mergeshift))
        }
        GameplayStyle.BoomBlocks -> {
            raw.replace(stackShiftTitle, stringResource(Res.string.app_title_boomblocks))
        }
        GameplayStyle.BlockSort -> {
            raw.replace(stackShiftTitle, stringResource(Res.string.app_title_blocksort))
        }
        else -> raw
    }
}
