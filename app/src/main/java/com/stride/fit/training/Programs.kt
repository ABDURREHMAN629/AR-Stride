package com.stride.fit.training

import com.stride.fit.data.db.SetLog
import java.time.DayOfWeek
import kotlin.math.roundToInt

/** One prescribed slot in a session: an exercise, how many sets, and the target. */
data class Block(
    val exerciseId: String,
    val sets: Int,
    val targetReps: String,
    val restSec: Int,
    val note: String = "",
    val alternativeId: String? = null
) {
    val exercise: Exercise get() = Exercises.require(exerciseId)
    val alternative: Exercise? get() = alternativeId?.let { Exercises[it] }
}

data class ProgramDay(
    val id: String,
    val name: String,
    val focus: String,
    val warmup: List<Block>,
    val main: List<Block>,
    val finisher: List<Block>
) {
    val allBlocks: List<Block> get() = warmup + main + finisher
    val workingSets: Int get() = main.sumOf { it.sets }

    /** Rough session length: working time plus prescribed rest. */
    val estimatedMinutes: Int
        get() {
            val seconds = allBlocks.sumOf { it.sets * (40 + it.restSec) }
            return (seconds / 60.0).roundToInt()
        }
}

data class Program(
    val id: String,
    val name: String,
    val description: String,
    val equipmentNeeded: String,
    val daysPerWeek: Int,
    val days: List<ProgramDay>
)

/**
 * Training plans, both full body three days a week. For someone carrying extra weight
 * and building a base, full body beats a body-part split: every session trains every
 * major pattern, so a missed day costs less and weekly frequency per muscle is higher.
 */
object Programs {

    val fullBodyFoundation = Program(
        id = "foundation",
        name = "Full Body Foundation",
        description = "Three sessions a week covering every movement pattern. Built around " +
            "compound lifts with a posture-friendly pull bias: two pulling movements for every push.",
        equipmentNeeded = "A pair of adjustable dumbbells, a bench or sturdy chair, a resistance band",
        daysPerWeek = 3,
        days = listOf(
            ProgramDay(
                id = "foundation_a",
                name = "Day A",
                focus = "Squat and horizontal push",
                warmup = listOf(
                    Block("cat_cow", 1, "10 slow", 20),
                    Block("glute_bridge", 2, "12", 30),
                    Block("band_pull_apart", 2, "15", 30)
                ),
                main = listOf(
                    Block("goblet_squat", 3, "8-12", 120, "Add weight once you own 12 clean reps"),
                    Block(
                        "db_bench_press", 3, "8-12", 120,
                        "No bench? Use the floor press",
                        alternativeId = "floor_press"
                    ),
                    Block(
                        "db_row", 3, "10-12", 90,
                        "Per side. Full stretch at the bottom of every rep",
                        alternativeId = "chest_supported_row"
                    ),
                    Block("db_rdl", 3, "10-12", 120, "Hips back, not knees down"),
                    Block("plank", 3, "30-45 sec", 60)
                ),
                finisher = listOf(
                    Block("calf_raise", 2, "15-20", 45),
                    Block("dead_bug", 2, "8 per side", 45)
                )
            ),
            ProgramDay(
                id = "foundation_b",
                name = "Day B",
                focus = "Hinge and vertical push",
                warmup = listOf(
                    Block("cat_cow", 1, "10 slow", 20),
                    Block("bird_dog", 2, "8 per side", 30),
                    Block("scap_wall_slide", 2, "10", 30)
                ),
                main = listOf(
                    Block("db_rdl", 3, "8-10", 120, "The main strength lift today"),
                    Block("db_overhead_press", 3, "8-12", 120, "Ribs down, glutes tight"),
                    Block(
                        "band_pulldown", 3, "10-12", 90,
                        "Lat pulldown machine if you have one",
                        alternativeId = "lat_pulldown"
                    ),
                    Block("reverse_lunge", 3, "8-10", 90, "Per side"),
                    Block("pallof_press", 3, "10 per side", 45)
                ),
                finisher = listOf(
                    Block("farmer_carry", 3, "30 sec", 60),
                    Block("hammer_curl", 2, "12-15", 45)
                )
            ),
            ProgramDay(
                id = "foundation_c",
                name = "Day C",
                focus = "Single leg and horizontal pull",
                warmup = listOf(
                    Block("cat_cow", 1, "10 slow", 20),
                    Block("glute_bridge_posture", 2, "12", 30),
                    Block("wall_angel", 2, "10", 30)
                ),
                main = listOf(
                    Block(
                        "split_squat", 3, "8-10", 90,
                        "Per side. Move up to the Bulgarian version when this feels easy",
                        alternativeId = "bulgarian_split_squat"
                    ),
                    Block(
                        "push_up", 3, "As many as possible", 90,
                        "Hands on a bench if full push-ups are not there yet",
                        alternativeId = "incline_push_up"
                    ),
                    Block(
                        "inverted_row", 3, "8-12", 90,
                        "Walk the feet out to make it harder",
                        alternativeId = "band_row"
                    ),
                    Block(
                        "hip_thrust", 3, "12-15", 90,
                        "Squeeze hard for a second at the top",
                        alternativeId = "glute_bridge"
                    ),
                    Block("side_plank", 3, "30 sec per side", 45)
                ),
                finisher = listOf(
                    Block("triceps_pushdown", 2, "12-15", 45),
                    Block("db_curl", 2, "12-15", 45)
                )
            )
        )
    )

    val homeMinimal = Program(
        id = "home",
        name = "Bodyweight and Band",
        description = "The same three-day structure with nothing but your bodyweight, a " +
            "resistance band and a chair. Progress by adding reps and slowing the tempo.",
        equipmentNeeded = "A resistance band and a sturdy chair",
        daysPerWeek = 3,
        days = listOf(
            ProgramDay(
                id = "home_a",
                name = "Day A",
                focus = "Legs and push",
                warmup = listOf(
                    Block("cat_cow", 1, "10 slow", 20),
                    Block("glute_bridge", 2, "12", 30)
                ),
                main = listOf(
                    Block("bodyweight_squat", 3, "15-20", 75, "Three seconds down every rep"),
                    Block("incline_push_up", 3, "10-15", 90, "Lower the surface as you get stronger"),
                    Block("band_row", 3, "12-15", 75),
                    Block("good_morning", 3, "12-15", 60),
                    Block("plank", 3, "30-45 sec", 60)
                ),
                finisher = listOf(Block("calf_raise", 2, "20", 45))
            ),
            ProgramDay(
                id = "home_b",
                name = "Day B",
                focus = "Hinge and pull",
                warmup = listOf(
                    Block("cat_cow", 1, "10 slow", 20),
                    Block("bird_dog", 2, "8 per side", 30)
                ),
                main = listOf(
                    Block("glute_bridge", 3, "15-20", 60, "Pause two seconds at the top"),
                    Block("band_pulldown", 3, "12-15", 75),
                    Block("pike_push_up", 3, "8-12", 90, "Hips high", alternativeId = "incline_push_up"),
                    Block("reverse_lunge", 3, "10", 75, "Per side"),
                    Block("dead_bug", 3, "8 per side", 45)
                ),
                finisher = listOf(Block("band_pull_apart", 2, "20", 45))
            ),
            ProgramDay(
                id = "home_c",
                name = "Day C",
                focus = "Single leg and core",
                warmup = listOf(
                    Block("cat_cow", 1, "10 slow", 20),
                    Block("wall_angel", 2, "10", 30)
                ),
                main = listOf(
                    Block("split_squat", 3, "10", 90, "Per side"),
                    Block("push_up", 3, "As many as possible", 90, alternativeId = "incline_push_up"),
                    Block("inverted_row", 3, "8-12", 90, "Under a table edge", alternativeId = "band_row"),
                    Block("wall_sit", 3, "40 sec", 60),
                    Block("side_plank", 3, "30 sec per side", 45)
                ),
                finisher = listOf(Block("bench_dip", 2, "12-15", 45))
            )
        )
    )

    val all = listOf(fullBodyFoundation, homeMinimal)

    operator fun get(id: String): Program? = all.firstOrNull { it.id == id }

    fun dayById(dayId: String): ProgramDay? =
        all.flatMap { it.days }.firstOrNull { it.id == dayId }

    /** Strength on Mon/Wed/Fri leaves 48 hours between sessions; runs go in between. */
    fun plannedFor(day: DayOfWeek, program: Program = fullBodyFoundation): WeekSlot = when (day) {
        DayOfWeek.MONDAY -> WeekSlot(program.days[0], "Strength", PostureRoutines.deskReset)
        DayOfWeek.TUESDAY -> WeekSlot(null, "Easy run or brisk walk", PostureRoutines.upperCrossFix)
        DayOfWeek.WEDNESDAY -> WeekSlot(program.days[1], "Strength", PostureRoutines.deskReset)
        DayOfWeek.THURSDAY -> WeekSlot(null, "Easy run or brisk walk", PostureRoutines.lowerCrossFix)
        DayOfWeek.FRIDAY -> WeekSlot(program.days[2], "Strength", PostureRoutines.deskReset)
        DayOfWeek.SATURDAY -> WeekSlot(null, "Longer run or a walk", PostureRoutines.fullReset)
        DayOfWeek.SUNDAY -> WeekSlot(null, "Rest and recover", PostureRoutines.morningMobility)
    }

    data class WeekSlot(
        val strength: ProgramDay?,
        val cardioLabel: String,
        val posture: PostureRoutine
    )
}

/**
 * Double progression: stay at the same load until every set reaches the top of the rep
 * range, then add the smallest useful jump and start again at the bottom. Slower than
 * adding weight every session, and the reason progress continues past month three.
 */
object Progression {

    data class Suggestion(
        val weightKg: Double,
        val reps: Int,
        val headline: String,
        val detail: String
    )

    private fun parseRange(target: String): IntRange? {
        val nums = Regex("[0-9]+").findAll(target).map { it.value.toInt() }.toList()
        return when {
            nums.size >= 2 -> nums[0]..nums[1]
            nums.size == 1 -> nums[0]..nums[0]
            else -> null
        }
    }

    /** Smallest jump worth making: lower body tolerates more than shoulders do. */
    private fun increment(exercise: Exercise): Double = when (exercise.pattern) {
        Pattern.SQUAT, Pattern.HINGE, Pattern.LUNGE -> 5.0
        Pattern.PUSH_VERTICAL, Pattern.ARMS -> 2.0
        else -> 2.5
    }

    fun suggest(block: Block, history: List<SetLog>): Suggestion {
        val exercise = block.exercise
        val range = parseRange(block.targetReps)
        val bodyweight = exercise.equipment == Equipment.NONE ||
            exercise.equipment == Equipment.WALL ||
            exercise.equipment == Equipment.BAR

        if (history.isEmpty()) {
            return Suggestion(
                weightKg = 0.0,
                reps = range?.first ?: 10,
                headline = "First time",
                detail = "Pick a load you could stop two or three reps short of failure with. " +
                    "Log it, and the app will take it from there."
            )
        }

        // Only the most recent session for this exercise matters for the next decision.
        val lastTs = history.maxOf { it.ts }
        val lastSession = history.filter { lastTs - it.ts < 6 * 60 * 60 * 1000L }
        val topWeight = lastSession.maxOf { it.weightKg }
        val setsAtTop = lastSession.filter { it.weightKg == topWeight }
        val minReps = setsAtTop.minOfOrNull { it.reps } ?: 0
        val avgRpe = setsAtTop.mapNotNull { it.rpe.takeIf { r -> r > 0 } }.average()
            .let { if (it.isNaN()) 0.0 else it }

        val topOfRange = range?.last ?: 12
        val bottomOfRange = range?.first ?: 8

        return when {
            range != null && minReps >= topOfRange && (avgRpe == 0.0 || avgRpe <= 8.5) -> {
                if (bodyweight) Suggestion(
                    weightKg = topWeight,
                    reps = topOfRange + 2,
                    headline = "Add reps",
                    detail = "You hit $topOfRange on every set. Push for ${topOfRange + 2} this time, " +
                        "or slow the lowering phase to three seconds."
                ) else Suggestion(
                    weightKg = topWeight + increment(exercise),
                    reps = bottomOfRange,
                    headline = "Add weight",
                    detail = "Every set reached $topOfRange reps. Go up to " +
                        "${fmt(topWeight + increment(exercise))} kg and rebuild to $topOfRange."
                )
            }

            minReps < bottomOfRange && avgRpe >= 9.0 -> Suggestion(
                weightKg = (topWeight - increment(exercise)).coerceAtLeast(0.0),
                reps = bottomOfRange,
                headline = "Back off slightly",
                detail = "Last session fell short of $bottomOfRange reps at a hard effort. " +
                    "Drop to ${fmt((topWeight - increment(exercise)).coerceAtLeast(0.0))} kg and rebuild."
            )

            else -> Suggestion(
                weightKg = topWeight,
                reps = (minReps + 1).coerceIn(bottomOfRange, topOfRange),
                headline = "Same weight, one more rep",
                detail = "Stay at ${fmt(topWeight)} kg until all sets hit $topOfRange reps."
            )
        }
    }

    private fun fmt(v: Double): String =
        if (v == v.toInt().toDouble()) v.toInt().toString() else String.format("%.1f", v)
}
