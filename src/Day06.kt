import kotlin.system.measureTimeMillis

fun main() {
    val test = GuardPositionGame.fromLines(readInput("Day06_test"))
    check(test.countUniquePositionsUntilEscape() == 41)
    check(test.countWaysToBlockGuardPath() == 6)

    val input = GuardPositionGame.fromLines(readInput("Day06"))
    println("Part 1: " + input.countUniquePositionsUntilEscape())
    val part2Duration = measureTimeMillis {
        println("Part 2: " + input.countWaysToBlockGuardPath())
    }
    println("Part 2 took $part2Duration ms")
}

data class GuardPositionGame(
    private val guardPosition: Position,
    private val grid: Grid,
) {
    private fun createGuard() = Guard(guardPosition, Heading.NORTH)

    fun countUniquePositionsUntilEscape(): Int {
        val guard = createGuard()
        return when (val route = guard.getGuardRoute(grid)) {
            GuardRoute.Invalid -> throw IllegalStateException("No guard route available")
            is GuardRoute.Valid -> route.countUniquePositions()
        }
    }

    fun countWaysToBlockGuardPath(): Int {
        val guardPath = createGuard().getGuardRoute(grid) as GuardRoute.Valid
        // Get all unique positions on the path that aren't the first one (the guards position)
        val cellsToBlock = guardPath.uniquePositions().drop(1)

        var cellsThatBlockEscape = 0
        for (cell in cellsToBlock) {
            grid.setCellEmptyState(cell, isEmpty = false)
            when (createGuard().getGuardRoute(grid)) {
                GuardRoute.Invalid -> cellsThatBlockEscape++
                is GuardRoute.Valid -> Unit
            }
            grid.setCellEmptyState(cell, isEmpty = true)
        }
        return cellsThatBlockEscape
    }

    companion object {
        fun fromLines(input: List<String>): GuardPositionGame {
            val cells = mutableListOf<GridCell>()

            var guardPosition: Position? = null
            input.forEachIndexed { row, line ->
                line.forEachIndexed { column, character ->
                    val position = Position(row, column)
                    cells.add(GridCell(position, isEmpty = character != '#'))
                    if (character == '^') {
                        guardPosition = position
                    }
                }
            }

            val grid = Grid(
                columnCount = input.first().length,
                rowCount = input.size,
                cells = cells,
            )

            return GuardPositionGame(requireNotNull(guardPosition), grid)
        }
    }
}

data class Grid(
    private val columnCount: Int,
    private val rowCount: Int,
    private val cells: List<GridCell>,
) {
    private fun toIndex(position: Position): Int {
        return position.row * columnCount + position.col
    }

    fun isOutOfBounds(position: Position): Boolean {
        return position.row < 0 || position.row >= rowCount
            || position.col < 0 || position.col >= columnCount
    }

    fun isEmpty(position: Position): Boolean {
        return cells[toIndex(position)].isEmpty
    }

    fun setCellEmptyState(position: Position, isEmpty: Boolean) {
        cells[toIndex(position)].isEmpty = isEmpty
    }
}

data class Guard(
    var position: Position,
    var heading: Heading,
) {

    fun getGuardRoute(grid: Grid): GuardRoute {
        val positions = mutableSetOf<DirectionalPosition>()

        while (!grid.isOutOfBounds(position)) {
            val newPosition = DirectionalPosition(position, heading)
            // A loop was found
            if (positions.contains(newPosition)) {
                return GuardRoute.Invalid
            }
            positions.add(newPosition)
            moveToNextPosition(grid)
        }

        return GuardRoute.Valid(positions)
    }

    private fun moveToNextPosition(grid: Grid) {
        this.position = getNextEmptyPosition(grid)
    }

    private fun getNextEmptyPosition(grid: Grid): Position {
        val newPosition = getNextPositionAlongHeading()
        return if (grid.isOutOfBounds(newPosition) || grid.isEmpty(newPosition)) {
            newPosition
        } else {
            rotate()
            getNextEmptyPosition(grid)
        }
    }

    private fun getNextPositionAlongHeading(): Position {
        return when (heading) {
            Heading.NORTH -> position.copy(row = position.row - 1)
            Heading.SOUTH -> position.copy(row = position.row + 1)
            Heading.EAST -> position.copy(col = position.col + 1)
            Heading.WEST -> position.copy(col = position.col - 1)
        }
    }

    private fun rotate() {
        heading = when (heading) {
            Heading.NORTH -> Heading.EAST
            Heading.SOUTH -> Heading.WEST
            Heading.EAST -> Heading.SOUTH
            Heading.WEST -> Heading.NORTH
        }
    }
}

sealed interface GuardRoute {
    data class Valid(val path: Set<DirectionalPosition>) : GuardRoute {
        fun uniquePositions() = path.map { it.position }.toSet()
        fun countUniquePositions() = uniquePositions().size
    }

    data object Invalid : GuardRoute
}

data class DirectionalPosition(val position: Position, val heading: Heading)
data class Position(val row: Int, val col: Int)
data class GridCell(val position: Position, var isEmpty: Boolean)

enum class Heading {
    NORTH,
    SOUTH,
    EAST,
    WEST;
}
