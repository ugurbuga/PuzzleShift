package com.ugurbuga.blockgames.ui.game.properties

import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.piece_properties_none
import blockgames.composeapp.generated.resources.special_column_clearer
import blockgames.composeapp.generated.resources.special_ghost
import blockgames.composeapp.generated.resources.special_heavy
import blockgames.composeapp.generated.resources.special_row_clearer
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import kotlin.test.Test
import kotlin.test.assertEquals

class PropertiesScreensCommonTest {

    @Test
    fun resolveSpecialLabelRes_mapsEverySpecialType() {
        assertEquals(Res.string.piece_properties_none, resolveSpecialLabelRes(SpecialBlockType.None))
        assertEquals(Res.string.special_column_clearer, resolveSpecialLabelRes(SpecialBlockType.ColumnClearer))
        assertEquals(Res.string.special_row_clearer, resolveSpecialLabelRes(SpecialBlockType.RowClearer))
        assertEquals(Res.string.special_ghost, resolveSpecialLabelRes(SpecialBlockType.Ghost))
        assertEquals(Res.string.special_heavy, resolveSpecialLabelRes(SpecialBlockType.Heavy))
    }
}

