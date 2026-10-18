package com.stride.fit.training

/**
 * Extra bodyweight movements the 30-day plan leans on. Deliberately low-impact: at 82 kg
 * and starting out, jumping drills load the knees and ankles far more than they give
 * back, so every cardio piece here keeps one foot on the floor.
 */
object HomeExtras {

    val all: List<Exercise> = listOf(
        Exercise(
            id = "wall_push_up",
            name = "Wall Push-Up",
            pattern = Pattern.PUSH_HORIZONTAL,
            primary = listOf("Chest", "Triceps"),
            equipment = Equipment.WALL,
            steps = listOf(
                "Stand an arm length from a wall and place your hands on it at chest height.",
                "Keep the body in one straight line from head to heels.",
                "Bend the elbows to bring your chest to the wall, then press away."
            ),
            cues = listOf("Step your feet further back to make it harder."),
            mistakes = listOf("Hips leading the way in."),
            defaultRestSec = 45
        ),
        Exercise(
            id = "knee_push_up",
            name = "Knee Push-Up",
            pattern = Pattern.PUSH_HORIZONTAL,
            primary = listOf("Chest", "Triceps"),
            secondary = listOf("Core"),
            equipment = Equipment.NONE,
            steps = listOf(
                "Kneel and walk the hands forward until the body forms a straight line from knees to head.",
                "Cross the ankles and keep the hips extended - do not sit back.",
                "Lower the chest towards the floor, elbows at 45 degrees, then press up."
            ),
            cues = listOf("The hips must stay open; a bent hip turns this into a half rep."),
            mistakes = listOf("Sticking the backside up in the air."),
            defaultRestSec = 60
        ),
        Exercise(
            id = "mountain_climber",
            name = "Mountain Climber",
            pattern = Pattern.CORE,
            primary = listOf("Core", "Hip flexors"),
            secondary = listOf("Shoulders"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS,
            steps = listOf(
                "Start in a push-up position, hands under the shoulders.",
                "Drive one knee towards the chest, then swap legs.",
                "Keep the hips low and level - no bouncing up and down.",
                "Go at a pace you can sustain for the whole interval."
            ),
            cues = listOf("Shoulders stay stacked over the wrists.", "Slow and controlled beats frantic."),
            mistakes = listOf("Hips rising into a pike.", "Racing until the form falls apart."),
            defaultRestSec = 45
        ),
        Exercise(
            id = "superman",
            name = "Superman Hold",
            pattern = Pattern.ACTIVATION,
            primary = listOf("Spinal erectors", "Glutes"),
            secondary = listOf("Lower traps"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS,
            steps = listOf(
                "Lie face down with arms reaching forward and legs straight.",
                "Lift the arms, chest and legs a few inches off the floor.",
                "Keep the neck long and the gaze at the floor.",
                "Hold, then lower with control."
            ),
            cues = listOf("Reach long rather than lifting high.", "Squeeze the glutes to protect the lower back."),
            mistakes = listOf("Looking up and compressing the neck."),
            defaultRestSec = 45
        ),
        Exercise(
            id = "glute_kickback",
            name = "Glute Kickback",
            pattern = Pattern.HINGE,
            primary = listOf("Glutes"),
            secondary = listOf("Core"),
            equipment = Equipment.NONE,
            measure = Measure.REPS_PER_SIDE,
            steps = listOf(
                "Start on all fours, hands under shoulders, knees under hips.",
                "Keeping the knee bent at 90 degrees, drive one heel towards the ceiling.",
                "Stop when the thigh is in line with the torso.",
                "Lower without letting the knee touch down."
            ),
            cues = listOf("Push the heel up, not the toes.", "Keep the lower back still - the movement is all hip."),
            mistakes = listOf("Arching the lower back to gain height.", "Rotating the hips open."),
            defaultRestSec = 45
        ),
        Exercise(
            id = "lateral_lunge",
            name = "Lateral Lunge",
            pattern = Pattern.LUNGE,
            primary = listOf("Quadriceps", "Glutes", "Adductors"),
            equipment = Equipment.NONE,
            measure = Measure.REPS_PER_SIDE,
            steps = listOf(
                "Stand tall, then take a wide step out to one side.",
                "Sit the hips back and bend that knee while the other leg stays straight.",
                "Keep both feet flat and pointing forward.",
                "Push off the bent leg to return to standing."
            ),
            cues = listOf("Chest up and hips back - this is a sideways hinge, not a side bend."),
            mistakes = listOf("Letting the straight leg's foot roll onto its edge."),
            defaultRestSec = 60
        ),
        Exercise(
            id = "squat_pulse",
            name = "Squat Pulse",
            pattern = Pattern.SQUAT,
            primary = listOf("Quadriceps", "Glutes"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS,
            steps = listOf(
                "Drop into the bottom half of a bodyweight squat.",
                "Pulse up and down through a small range without standing all the way up.",
                "Keep the chest tall and the weight in the heels."
            ),
            cues = listOf("Small range, constant tension. The burn is the point."),
            mistakes = listOf("Standing fully upright and losing the tension."),
            defaultRestSec = 45
        ),
        Exercise(
            id = "step_jack",
            name = "Step Jack",
            pattern = Pattern.CORE,
            primary = listOf("Whole body"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS,
            steps = listOf(
                "Stand tall with arms at your sides.",
                "Step one foot out to the side while sweeping both arms overhead.",
                "Bring it back and repeat on the other side.",
                "Keep a steady rhythm."
            ),
            cues = listOf(
                "A jumping jack with the jump removed: the same warm-up effect, none of the joint load.",
                "Speed it up once it feels easy."
            ),
            mistakes = listOf("Rushing until the shoulders shrug."),
            defaultRestSec = 30
        ),
        Exercise(
            id = "bicycle_crunch",
            name = "Bicycle Crunch",
            pattern = Pattern.CORE,
            primary = listOf("Obliques", "Core"),
            equipment = Equipment.NONE,
            measure = Measure.REPS_PER_SIDE,
            steps = listOf(
                "Lie on your back, hands lightly behind the head, knees bent to 90 degrees.",
                "Lift the shoulder blades off the floor.",
                "Rotate one shoulder towards the opposite knee as the other leg extends.",
                "Alternate slowly."
            ),
            cues = listOf(
                "Rotate from the ribs, not the elbows.",
                "Keep the lower back pressed down; raise the legs higher if it lifts."
            ),
            mistakes = listOf("Yanking on the neck.", "Going fast and rocking side to side."),
            defaultRestSec = 45
        ),
        Exercise(
            id = "bear_hold",
            name = "Bear Hold",
            pattern = Pattern.CORE,
            primary = listOf("Core", "Shoulders"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS,
            steps = listOf(
                "Start on all fours with the knees under the hips.",
                "Tuck the toes and hover the knees an inch off the floor.",
                "Keep the back flat and the hips level.",
                "Hold and breathe."
            ),
            cues = listOf("An inch is enough. If the back rounds, put the knees down."),
            mistakes = listOf("Lifting the hips too high.", "Holding the breath."),
            defaultRestSec = 45
        )
    )

    private val byId = all.associateBy { it.id }

    operator fun get(id: String): Exercise? = byId[id]
}

enum class PlanKind(val label: String) {
    STRENGTH("Strength"),
    YOGA("Yoga"),
    MOBILITY("Posture"),
    CARDIO("Walk"),
    TEST("Benchmark"),
    REST("Rest")
}

data class PlanDay(
    val day: Int,
    val title: String,
    val kind: PlanKind,
    val focus: String,
    val blocks: List<Block> = emptyList(),
    val routineId: String = "",
    val walkMinutes: Int = 0,
    val coach: String = ""
) {
    val id: String get() = "plan_d$day"
    val week: Int get() = (day - 1) / 7 + 1

    val routine: PostureRoutine? get() = AllRoutines[routineId]

    val estimatedMinutes: Int
        get() {
            val strength = blocks.sumOf { it.sets * (40 + it.restSec) } / 60
            val routineMin = routine?.minutes ?: 0
            return strength + routineMin + walkMinutes
        }
}

/**
 * A 30-day home plan, bodyweight plus an optional band, so nothing here needs a gym.
 *
 * Week one learns the patterns at two rounds, week two adds a third, week three swaps in
 * harder variations, week four raises the volume again, and day 29 re-tests the four
 * benchmarks set on day 1 so the improvement is measured rather than felt. Strength days
 * never fall back to back; the days between are walking, yoga and posture work.
 */
object Challenge30 {


    private fun circuitA(
        rounds: Int,
        pushId: String,
        squatReps: String,
        pushReps: String,
        rowReps: String,
        plank: String,
        rest: Int
    ) = listOf(
        Block("bodyweight_squat", rounds, squatReps, rest, "Three seconds down on every rep"),
        Block(pushId, rounds, pushReps, rest, "Stop two reps short of failure"),
        Block("band_row", rounds, rowReps, rest, "No band? Use a towel around a door handle",
            alternativeId = "inverted_row"),
        Block("glute_bridge", rounds, "15", rest, "Two second squeeze at the top"),
        Block("plank", rounds, plank, rest)
    )

    private fun circuitB(
        rounds: Int,
        pushId: String,
        lungeReps: String,
        pushReps: String,
        rest: Int
    ) = listOf(
        Block("good_morning", rounds, "12-15", rest, "Hips back, flat back"),
        Block(pushId, rounds, pushReps, rest, "Hips high to load the shoulders"),
        Block("band_pulldown", rounds, "12-15", rest, alternativeId = "band_row"),
        Block("reverse_lunge", rounds, lungeReps, rest, "Per side"),
        Block("dead_bug", rounds, "8 per side", rest, "Lower back stays glued to the floor")
    )

    private fun circuitC(
        rounds: Int,
        squatId: String,
        pushId: String,
        squatReps: String,
        pushReps: String,
        holdSec: String,
        rest: Int
    ) = listOf(
        Block(squatId, rounds, squatReps, rest, "Per side"),
        Block(pushId, rounds, pushReps, rest),
        Block("superman", rounds, "20-30 sec", rest, "Reach long, do not lift high"),
        Block("wall_sit", rounds, holdSec, rest),
        Block("side_plank", rounds, "25 sec per side", rest),
        Block("mountain_climber", rounds, "30 sec", rest, "Steady pace, hips low")
    )

    private val benchmark = listOf(
        Block("push_up", 1, "Max reps", 120, "One set, as many clean reps as you can",
            alternativeId = "knee_push_up"),
        Block("bodyweight_squat", 1, "Max reps in 60 sec", 120, "Full depth only"),
        Block("plank", 1, "Max hold", 120, "Stop when the hips drop"),
        Block("wall_sit", 1, "Max hold", 120, "Thighs parallel")
    )


    val days: List<PlanDay> = listOf(

        PlanDay(1, "Baseline and Circuit A", PlanKind.TEST,
            "Set your four benchmarks, then train",
            blocks = benchmark + circuitA(2, "knee_push_up", "12", "8-12", "12", "20-30 sec", 60),
            coach = "Record the four benchmark numbers honestly. On day 29 you repeat them " +
                "exactly, and that comparison is the whole point of the month."),
        PlanDay(2, "Morning Yoga and a Walk", PlanKind.YOGA,
            "Open the hips and spine",
            routineId = "yoga_morning", walkMinutes = 20,
            coach = "Yesterday was new for your body. Today keeps the blood moving without adding damage."),
        PlanDay(3, "Circuit B", PlanKind.STRENGTH,
            "Hinge, vertical push, lunge",
            blocks = circuitB(2, "incline_push_up", "8 per side", "8-12", 60),
            coach = "The good morning teaches the hinge. Get it right here and every deadlift " +
                "you ever do will be safer."),
        PlanDay(4, "Posture Reset and a Walk", PlanKind.MOBILITY,
            "Undo the desk",
            routineId = "desk_reset", walkMinutes = 25,
            coach = "Short and daily beats long and occasional for posture work."),
        PlanDay(5, "Circuit C", PlanKind.STRENGTH,
            "Single leg, core, low-impact conditioning",
            blocks = circuitC(2, "split_squat", "wall_push_up", "8 per side", "12-15", "25 sec", 60),
            coach = "Single leg work exposes the side that is weaker. Do not let the strong " +
                "side set the reps."),
        PlanDay(6, "Yoga for Runners", PlanKind.YOGA,
            "Hips, hamstrings, calves",
            routineId = "yoga_runners", walkMinutes = 30,
            coach = "Legs up the wall at the end is not filler. It is the best thing you can " +
                "do for tired legs."),
        PlanDay(7, "Rest", PlanKind.REST,
            "Full recovery",
            routineId = "yoga_evening",
            coach = "Muscle is built while you rest, not while you train. Take the day."),

        PlanDay(8, "Circuit A, three rounds", PlanKind.STRENGTH,
            "Squat, push, pull, core",
            blocks = circuitA(3, "knee_push_up", "15", "10-12", "12-15", "30-40 sec", 60),
            coach = "Same movements, one more round. If week one felt survivable this will too."),
        PlanDay(9, "Yoga for Back and Posture", PlanKind.YOGA,
            "Thoracic spine and chest",
            routineId = "yoga_back", walkMinutes = 25),
        PlanDay(10, "Circuit B, three rounds", PlanKind.STRENGTH,
            "Hinge, vertical push, lunge",
            blocks = circuitB(3, "incline_push_up", "10 per side", "10-12", 60),
            coach = "Lower the surface for your incline push-ups if last week felt easy."),
        PlanDay(11, "Lower Body Posture Fix", PlanKind.MOBILITY,
            "Hip flexors and glutes",
            routineId = "lower_cross", walkMinutes = 30,
            coach = "Tuck the tailbone before you lunge forward in the hip flexor stretch. " +
                "Skip that and you stretch your lower back instead."),
        PlanDay(12, "Circuit C, three rounds", PlanKind.STRENGTH,
            "Single leg and core",
            blocks = circuitC(3, "split_squat", "knee_push_up", "10 per side", "10-12", "30 sec", 60)),
        PlanDay(13, "Sun Salutations and a Long Walk", PlanKind.YOGA,
            "Flow and breath",
            routineId = "yoga_sun_a", walkMinutes = 35,
            coach = "Run the flow three times through. Let the breath set the pace."),
        PlanDay(14, "Rest", PlanKind.REST,
            "Two weeks down",
            routineId = "yoga_evening",
            coach = "Check your step trend on the Today screen. Two weeks is enough to see one."),

        PlanDay(15, "Circuit A+", PlanKind.STRENGTH,
            "Full push-ups enter the plan",
            blocks = circuitA(3, "push_up", "18", "6-10", "15", "40-50 sec", 75),
            coach = "Full push-ups now. If you cannot get six, stay on knees and add reps " +
                "instead. Neither choice is a failure."),
        PlanDay(16, "Morning Yoga", PlanKind.YOGA,
            "Mobility maintenance",
            routineId = "yoga_morning", walkMinutes = 30),
        PlanDay(17, "Circuit B+", PlanKind.STRENGTH,
            "Pike push-ups and deeper lunges",
            blocks = circuitB(3, "pike_push_up", "12 per side", "8-12", 75),
            coach = "Walk the feet closer in the pike push-up to load the shoulders harder."),
        PlanDay(18, "Upper Body Posture Fix", PlanKind.MOBILITY,
            "Forward head and rounded shoulders",
            routineId = "upper_cross", walkMinutes = 30),
        PlanDay(19, "Circuit C+", PlanKind.STRENGTH,
            "Bulgarian split squats",
            blocks = circuitC(3, "bulgarian_split_squat", "push_up", "8 per side", "8-12", "40 sec", 75),
            coach = "Back foot on a chair. Front shin stays near vertical or your knee will " +
                "let you know about it."),
        PlanDay(20, "Yoga for Runners", PlanKind.YOGA,
            "Recovery and hips",
            routineId = "yoga_runners", walkMinutes = 40,
            coach = "Try a few one minute jogs inside the walk if the knees feel good."),
        PlanDay(21, "Rest", PlanKind.REST,
            "Recover before the last push",
            routineId = "yoga_evening"),

        PlanDay(22, "Circuit A, four rounds", PlanKind.STRENGTH,
            "Peak volume week",
            blocks = circuitA(4, "push_up", "20", "8-12", "15-20", "45-60 sec", 75),
            coach = "The hardest week. Four rounds is a lot - keep the rest honest and the " +
                "reps clean rather than chasing the clock."),
        PlanDay(23, "Yoga for Back and Posture", PlanKind.YOGA,
            "Stay loose under the volume",
            routineId = "yoga_back", walkMinutes = 30),
        PlanDay(24, "Circuit B, four rounds", PlanKind.STRENGTH,
            "Hinge and vertical push",
            blocks = circuitB(4, "pike_push_up", "12 per side", "10-12", 75)),
        PlanDay(25, "Full Posture Reset", PlanKind.MOBILITY,
            "Head to hips",
            routineId = "full_reset", walkMinutes = 30),
        PlanDay(26, "Circuit C, four rounds", PlanKind.STRENGTH,
            "Last hard session",
            blocks = circuitC(4, "bulgarian_split_squat", "push_up", "10 per side", "10-15", "45 sec", 75),
            coach = "Last hard one. Everything after this is lighter."),
        PlanDay(27, "Sun Salutations and a Long Walk", PlanKind.YOGA,
            "Flush the legs out",
            routineId = "yoga_sun_a", walkMinutes = 45),
        PlanDay(28, "Rest", PlanKind.REST,
            "Fresh legs for the retest",
            routineId = "yoga_evening",
            coach = "Rest properly. Tomorrow you retest, and a tired retest tells you nothing."),

        PlanDay(29, "Retest", PlanKind.TEST,
            "The same four benchmarks as day 1",
            blocks = benchmark,
            coach = "Same four tests, same rules, same rest. Compare them against day 1 in " +
                "your session history. This is the number that matters, not the scale."),
        PlanDay(30, "Finish", PlanKind.YOGA,
            "Earned",
            routineId = "yoga_morning", walkMinutes = 45,
            coach = "Thirty days done. Move straight onto the Bodyweight and Band programme " +
                "on the Train tab, or run the whole plan again with harder variations.")
    )

    /** Plan days reuse the workout player, which speaks ProgramDay. */
    fun asProgramDay(planDay: PlanDay): ProgramDay = ProgramDay(
        id = planDay.id,
        name = "Day ${planDay.day}: ${planDay.title}",
        focus = planDay.focus,
        warmup = emptyList(),
        main = planDay.blocks,
        finisher = emptyList()
    )

    operator fun get(day: Int): PlanDay? = days.firstOrNull { it.day == day }

    fun byId(id: String): PlanDay? = days.firstOrNull { it.id == id }

    val totalStrengthDays: Int get() = days.count { it.kind == PlanKind.STRENGTH }
    val totalYogaDays: Int get() = days.count { it.kind == PlanKind.YOGA }
}
