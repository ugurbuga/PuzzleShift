package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GridPoint

internal object ChainShiftPath {
    fun spiral(config: GameConfig): List<GridPoint> {
        val points = mutableListOf<GridPoint>()
        var left = 0
        var top = 0
        var right = config.columns - 1
        var bottom = config.rows - 1

        while (left <= right && top <= bottom) {
            for (column in left..right) {
                points += GridPoint(column = column, row = top)
            }
            for (row in (top + 1)..bottom) {
                points += GridPoint(column = right, row = row)
            }
            if (top < bottom) {
                for (column in (right - 1) downTo left) {
                    points += GridPoint(column = column, row = bottom)
                }
            }
            if (left < right) {
                for (row in (bottom - 1) downTo (top + 1)) {
                    points += GridPoint(column = left, row = row)
                }
            }
            left += 1
            top += 1
            right -= 1
            bottom -= 1
        }

        return points.distinct()
    }

    fun indexLookup(config: GameConfig): Map<GridPoint, Int> =
        spiral(config).withIndex().associate { it.value to it.index }
}

