package com.stride.fit.training

/**
 * Posture work, kept out of the strength catalogue because the goal is different: these
 * drills restore positions you have lost rather than adding load.
 *
 * Upper-crossed: head drifts forward, shoulders round in - tight chest and upper traps,
 * weak deep neck flexors and lower traps. Lower-crossed: pelvis tips forward, lower back
 * over-arches - tight hip flexors and erectors, weak glutes and deep abdominals.
 * Stretching the tight side alone rarely holds, so each routine pairs a release with the
 * strengthening drill that keeps the new position.
 */
object PostureExercises {

    val all: List<Exercise> = listOf(

        Exercise(
            id = "chin_tuck",
            name = "Chin Tuck",
            pattern = Pattern.ACTIVATION,
            primary = listOf("Deep neck flexors"),
            equipment = Equipment.NONE,
            measure = Measure.REPS,
            steps = listOf(
                "Sit or stand tall, eyes level.",
                "Without tipping your head down, glide it straight backwards.",
                "You should feel a double chin and a gentle stretch at the base of the skull.",
                "Hold for five seconds, then release."
            ),
            cues = listOf(
                "Think of sliding your head back along a shelf, not nodding.",
                "This is the single most useful drill for a forward head."
            ),
            mistakes = listOf("Tucking the chin down to the chest instead of gliding back."),
            defaultRestSec = 15,
            breathing = "Breathe out slowly as you hold."
        ),
        Exercise(
            id = "levator_stretch",
            name = "Levator Scapulae Stretch",
            pattern = Pattern.STRETCH,
            primary = listOf("Levator scapulae", "Upper traps"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS_PER_SIDE,
            steps = listOf(
                "Sit on one hand to anchor that shoulder down.",
                "Turn your head about 45 degrees away from that side.",
                "Look down towards your armpit and use the free hand to add gentle pressure.",
                "Hold and breathe."
            ),
            cues = listOf("Anchoring the shoulder is what makes this work."),
            mistakes = listOf("Yanking the head; the pressure should be light."),
            defaultRestSec = 10
        ),
        Exercise(
            id = "upper_trap_stretch",
            name = "Upper Trap Stretch",
            pattern = Pattern.STRETCH,
            primary = listOf("Upper traps"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS_PER_SIDE,
            steps = listOf(
                "Sit tall and hold the edge of the chair with one hand.",
                "Tip your ear towards the opposite shoulder.",
                "Add light pressure with the free hand and hold."
            ),
            cues = listOf("Keep the nose pointing forward; this is a side bend, not a turn."),
            mistakes = listOf("Shrugging the anchored shoulder up."),
            defaultRestSec = 10
        ),

        Exercise(
            id = "doorway_pec_stretch",
            name = "Doorway Pec Stretch",
            pattern = Pattern.STRETCH,
            primary = listOf("Chest", "Front delts"),
            equipment = Equipment.WALL,
            measure = Measure.SECONDS_PER_SIDE,
            steps = listOf(
                "Place one forearm on a door frame with the elbow at shoulder height.",
                "Step through with the same-side foot and turn your chest away.",
                "Hold where you feel a stretch across the front of the chest, not in the shoulder joint."
            ),
            cues = listOf(
                "Run it three times per side at low, middle and high elbow positions to cover the whole fan of the pec."
            ),
            mistakes = listOf("Pushing into shoulder pain rather than chest stretch."),
            defaultRestSec = 10
        ),
        Exercise(
            id = "wall_angel",
            name = "Wall Angel",
            pattern = Pattern.ACTIVATION,
            primary = listOf("Lower traps", "Rear delts"),
            secondary = listOf("Thoracic spine"),
            equipment = Equipment.WALL,
            measure = Measure.REPS,
            steps = listOf(
                "Stand with your back to a wall, feet a foot away from it.",
                "Flatten the lower back by tucking the pelvis slightly.",
                "Put the backs of the hands, elbows, upper back and head against the wall.",
                "Slide the arms up as far as you can keep every contact point, then back down."
            ),
            cues = listOf(
                "Range is irrelevant if the contact breaks.",
                "If the lower back arches away, you have gone too high."
            ),
            mistakes = listOf("Ribs flaring off the wall.", "Hands leaving the wall."),
            tempo = "Slow, 3 seconds up and 3 down",
            defaultRestSec = 30
        ),
        Exercise(
            id = "band_pull_apart",
            name = "Band Pull-Apart",
            pattern = Pattern.ACTIVATION,
            primary = listOf("Rear delts", "Rhomboids"),
            equipment = Equipment.BAND,
            measure = Measure.REPS,
            steps = listOf(
                "Hold a light band at shoulder width, arms straight out in front.",
                "Pull the band apart until the arms are wide, band touching the chest.",
                "Squeeze the shoulder blades, then return slowly."
            ),
            cues = listOf("Keep the shoulders down; do not shrug the band open."),
            mistakes = listOf("Using a band so heavy the traps take over."),
            defaultRestSec = 30
        ),
        Exercise(
            id = "face_pull",
            name = "Face Pull",
            pattern = Pattern.ACTIVATION,
            primary = listOf("Rear delts", "Lower traps"),
            equipment = Equipment.BAND,
            measure = Measure.REPS,
            steps = listOf(
                "Anchor a band slightly above head height.",
                "Pull the band towards your forehead, splitting the hands apart.",
                "Finish with the knuckles beside your ears and the elbows high.",
                "Return slowly."
            ),
            cues = listOf("Think of showing your biceps at the end of every rep."),
            mistakes = listOf("Pulling to the chest, which makes it a row."),
            defaultRestSec = 45
        ),
        Exercise(
            id = "prone_ytw",
            name = "Prone Y-T-W",
            pattern = Pattern.ACTIVATION,
            primary = listOf("Lower traps", "Rhomboids", "Rear delts"),
            equipment = Equipment.NONE,
            measure = Measure.REPS,
            steps = listOf(
                "Lie face down, forehead resting on a towel.",
                "Y: arms overhead at 45 degrees, thumbs up, lift them off the floor.",
                "T: arms straight out to the sides, lift.",
                "W: elbows bent at your ribs, squeeze the shoulder blades and lift.",
                "One rep is all three shapes."
            ),
            cues = listOf("Lift from between the shoulder blades, not from the hands."),
            mistakes = listOf("Lifting the head and cranking the neck."),
            defaultRestSec = 45
        ),
        Exercise(
            id = "scap_wall_slide",
            name = "Scapular Wall Slide",
            pattern = Pattern.ACTIVATION,
            primary = listOf("Serratus anterior", "Lower traps"),
            equipment = Equipment.WALL,
            measure = Measure.REPS,
            steps = listOf(
                "Stand facing a wall with your forearms on it, elbows at shoulder height.",
                "Slide the forearms up the wall while pushing gently into it.",
                "At the top, let the shoulder blades rotate up around the ribs.",
                "Slide back down."
            ),
            cues = listOf("Keep pressing into the wall the whole time."),
            mistakes = listOf("Shrugging instead of rotating the shoulder blade."),
            defaultRestSec = 30
        ),
        Exercise(
            id = "prone_cobra",
            name = "Prone Cobra",
            pattern = Pattern.ACTIVATION,
            primary = listOf("Lower traps", "Spinal erectors"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS,
            steps = listOf(
                "Lie face down, arms at your sides, palms down.",
                "Lift the chest slightly, rotate the palms to face outward, thumbs up.",
                "Pull the shoulder blades down and together, tuck the chin.",
                "Hold."
            ),
            cues = listOf("Chin stays tucked; the lift comes from the upper back."),
            mistakes = listOf("Cranking the neck back to gain height."),
            defaultRestSec = 45
        ),

        Exercise(
            id = "cat_cow",
            name = "Cat-Cow",
            pattern = Pattern.MOBILITY,
            primary = listOf("Spine"),
            equipment = Equipment.NONE,
            measure = Measure.REPS,
            steps = listOf(
                "On all fours, hands under shoulders and knees under hips.",
                "Breathe out and round the whole spine, tucking the tailbone and chin.",
                "Breathe in and reverse: chest through, tailbone up, eyes forward.",
                "Move segment by segment rather than as one block."
            ),
            cues = listOf("Let the breath set the pace."),
            mistakes = listOf("Only moving the lower back."),
            defaultRestSec = 15
        ),
        Exercise(
            id = "thoracic_extension",
            name = "Thoracic Extension over a Roller",
            pattern = Pattern.MOBILITY,
            primary = listOf("Thoracic spine"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS,
            steps = listOf(
                "Place a foam roller or rolled towel across your mid back.",
                "Support your head with your hands and keep the ribs down.",
                "Breathe out and let the upper back drape backwards over the roller.",
                "Move the roller an inch and repeat over two or three positions."
            ),
            cues = listOf("Extend over the roller, not from the lower back."),
            mistakes = listOf("Letting the ribs flare and hinging at the waist."),
            defaultRestSec = 20
        ),
        Exercise(
            id = "thread_the_needle",
            name = "Thread the Needle",
            pattern = Pattern.MOBILITY,
            primary = listOf("Thoracic spine", "Rear delts"),
            equipment = Equipment.NONE,
            measure = Measure.REPS_PER_SIDE,
            steps = listOf(
                "Start on all fours.",
                "Slide one arm underneath the body, palm up, until the shoulder rests on the floor.",
                "Then reverse and reach that arm up to the ceiling, opening the chest.",
                "Follow the hand with your eyes."
            ),
            cues = listOf("Keep the hips square and stacked over the knees."),
            mistakes = listOf("Rotating from the hips instead of the ribcage."),
            defaultRestSec = 15
        ),
        Exercise(
            id = "seated_thoracic_rotation",
            name = "Seated Thoracic Rotation",
            pattern = Pattern.MOBILITY,
            primary = listOf("Thoracic spine"),
            equipment = Equipment.BENCH,
            measure = Measure.REPS_PER_SIDE,
            steps = listOf(
                "Sit tall on a chair with your knees together and hands behind your head.",
                "Rotate your ribcage to one side as far as it will go without the knees moving.",
                "Return to the middle and repeat."
            ),
            cues = listOf("Knees locked together isolates the rotation to the mid back."),
            mistakes = listOf("Letting the pelvis turn with the torso."),
            defaultRestSec = 15
        ),

        Exercise(
            id = "kneeling_hip_flexor",
            name = "Kneeling Hip Flexor Stretch",
            pattern = Pattern.STRETCH,
            primary = listOf("Hip flexors"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS_PER_SIDE,
            steps = listOf(
                "Half-kneel with the back knee on something soft.",
                "Before moving forward, tuck the tailbone under and squeeze that side glute.",
                "Only then shift your weight forward until you feel a stretch at the front of the hip.",
                "Reach the same-side arm overhead and lean slightly away to deepen it."
            ),
            cues = listOf(
                "The glute squeeze is what makes this stretch the hip and not the lower back.",
                "If you feel it in your lower back, you have skipped the tuck."
            ),
            mistakes = listOf("Arching the lower back to lunge forward."),
            defaultRestSec = 15
        ),
        Exercise(
            id = "hamstring_stretch",
            name = "Supine Hamstring Stretch",
            pattern = Pattern.STRETCH,
            primary = listOf("Hamstrings"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS_PER_SIDE,
            steps = listOf(
                "Lie on your back with one knee bent and that foot flat.",
                "Loop a towel or band around the other foot and raise that leg.",
                "Keep the knee mostly straight and pull gently until you feel a stretch.",
                "Keep the lower back flat on the floor."
            ),
            cues = listOf("Lying down protects the lower back, unlike a standing toe-touch."),
            mistakes = listOf("Rounding the back to reach further."),
            defaultRestSec = 15
        ),
        Exercise(
            id = "hip_90_90",
            name = "90/90 Hip Switch",
            pattern = Pattern.MOBILITY,
            primary = listOf("Hips"),
            equipment = Equipment.NONE,
            measure = Measure.REPS,
            steps = listOf(
                "Sit with one leg bent in front at 90 degrees and the other bent behind at 90 degrees.",
                "Sit as tall as you can.",
                "Rotate both knees over to the other side and settle into the mirrored position."
            ),
            cues = listOf("Keep the chest lifted; do not collapse backwards."),
            mistakes = listOf("Using the hands to force the knees down."),
            defaultRestSec = 20
        ),
        Exercise(
            id = "glute_bridge_posture",
            name = "Glute Bridge with Posterior Tilt",
            pattern = Pattern.ACTIVATION,
            primary = listOf("Glutes", "Deep abdominals"),
            equipment = Equipment.NONE,
            measure = Measure.REPS,
            steps = listOf(
                "Lie on your back with knees bent, feet flat.",
                "Before lifting, tilt the pelvis so the lower back presses into the floor.",
                "Keep that tilt as you lift the hips.",
                "Lower slowly and reset the tilt each rep."
            ),
            cues = listOf(
                "Height is not the point; keeping the pelvis tucked is.",
                "This directly trains the position that undoes an anterior pelvic tilt."
            ),
            mistakes = listOf("Arching the back to get higher."),
            defaultRestSec = 30
        ),
        Exercise(
            id = "childs_pose",
            name = "Child Pose",
            pattern = Pattern.STRETCH,
            primary = listOf("Lower back", "Lats"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS,
            steps = listOf(
                "Kneel and sit back onto your heels with the knees wide.",
                "Walk the hands forward and let the chest sink towards the floor.",
                "Breathe into the back of your ribs."
            ),
            cues = listOf("Walk the hands to one side to bias the lat on the opposite side."),
            mistakes = listOf("Holding tension in the shoulders."),
            defaultRestSec = 10
        ),
        Exercise(
            id = "standing_forward_fold",
            name = "Standing Forward Fold",
            pattern = Pattern.STRETCH,
            primary = listOf("Hamstrings", "Lower back"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS,
            steps = listOf(
                "Stand with the feet hip-width and a soft bend in the knees.",
                "Hinge forward and let the head and arms hang heavy.",
                "Breathe and let gravity do the work."
            ),
            cues = listOf("Bend the knees as much as you need to; the goal is decompression."),
            mistakes = listOf("Locking the knees and straining."),
            defaultRestSec = 10
        ),
        Exercise(
            id = "desk_chest_opener",
            name = "Standing Wall Chest Opener",
            pattern = Pattern.STRETCH,
            primary = listOf("Chest", "Biceps"),
            equipment = Equipment.WALL,
            measure = Measure.SECONDS_PER_SIDE,
            steps = listOf(
                "Stand side-on to a wall and place your palm flat on it behind you, arm straight.",
                "Turn your body slowly away from the wall.",
                "Hold where the chest stretches."
            ),
            cues = listOf("Keep the shoulder pulled down away from the ear."),
            mistakes = listOf("Turning so far it pinches the front of the shoulder."),
            defaultRestSec = 10
        )
    )

    private val byId = all.associateBy { it.id }

    operator fun get(id: String): Exercise? = byId[id]
}

/** One timed or counted step inside a guided routine. */
data class RoutineStep(
    val exerciseId: String,
    val seconds: Int,
    val reps: Int = 0,
    val perSide: Boolean = false,
    val note: String = ""
) {
    val exercise: Exercise
        get() = PostureExercises[exerciseId] ?: YogaPoses[exerciseId]
            ?: HomeExtras[exerciseId] ?: Exercises.require(exerciseId)

    /** Timed routines run per side, so a two-sided step costs twice as long. */
    val totalSeconds: Int get() = if (perSide) seconds * 2 else seconds
}

/** Guided routines come in two flavours; the player treats them identically. */
enum class RoutineCategory(val label: String) {
    POSTURE("Posture"),
    YOGA("Yoga")
}

data class PostureRoutine(
    val id: String,
    val name: String,
    val subtitle: String,
    val targets: String,
    val whenToDo: String,
    val steps: List<RoutineStep>,
    val category: RoutineCategory = RoutineCategory.POSTURE
) {
    val totalSeconds: Int get() = steps.sumOf { it.totalSeconds }
    val minutes: Int get() = (totalSeconds + 59) / 60
}

object PostureRoutines {

    val deskReset = PostureRoutine(
        id = "desk_reset",
        name = "Desk Reset",
        subtitle = "Five minutes, no equipment, do it at your desk",
        targets = "Forward head, rounded shoulders, stiff mid back",
        whenToDo = "Every two hours of sitting. This is the one that actually prevents the problem.",
        steps = listOf(
            RoutineStep("chin_tuck", seconds = 45, reps = 8, note = "Hold each for 5 seconds"),
            RoutineStep("upper_trap_stretch", seconds = 30, perSide = true),
            RoutineStep("seated_thoracic_rotation", seconds = 45, reps = 8, perSide = false),
            RoutineStep("desk_chest_opener", seconds = 30, perSide = true),
            RoutineStep("standing_forward_fold", seconds = 45)
        )
    )

    val upperCrossFix = PostureRoutine(
        id = "upper_cross",
        name = "Upper Body Posture Fix",
        subtitle = "Forward head and rounded shoulders",
        targets = "Deep neck flexors, lower traps, serratus, pecs, upper traps",
        whenToDo = "Daily, or at minimum after every upper body session.",
        steps = listOf(
            RoutineStep("cat_cow", seconds = 60, reps = 10),
            RoutineStep("thoracic_extension", seconds = 60, note = "Two or three positions along the mid back"),
            RoutineStep("doorway_pec_stretch", seconds = 40, perSide = true, note = "Low, middle and high"),
            RoutineStep("levator_stretch", seconds = 30, perSide = true),
            RoutineStep("chin_tuck", seconds = 60, reps = 10, note = "Hold each for 5 seconds"),
            RoutineStep("wall_angel", seconds = 60, reps = 10),
            RoutineStep("band_pull_apart", seconds = 45, reps = 15),
            RoutineStep("prone_ytw", seconds = 60, reps = 8),
            RoutineStep("prone_cobra", seconds = 45)
        )
    )

    val lowerCrossFix = PostureRoutine(
        id = "lower_cross",
        name = "Lower Body Posture Fix",
        subtitle = "Anterior pelvic tilt and a over-arched lower back",
        targets = "Hip flexors, spinal erectors, glutes, deep abdominals",
        whenToDo = "Daily if you sit for most of the day.",
        steps = listOf(
            RoutineStep("cat_cow", seconds = 60, reps = 10),
            RoutineStep("kneeling_hip_flexor", seconds = 45, perSide = true, note = "Tuck the tailbone first"),
            RoutineStep("hamstring_stretch", seconds = 40, perSide = true),
            RoutineStep("hip_90_90", seconds = 60, reps = 10),
            RoutineStep("glute_bridge_posture", seconds = 60, reps = 12),
            RoutineStep("dead_bug", seconds = 60, reps = 8, perSide = false),
            RoutineStep("bird_dog", seconds = 60, reps = 8),
            RoutineStep("side_plank", seconds = 30, perSide = true),
            RoutineStep("childs_pose", seconds = 45)
        )
    )

    val fullReset = PostureRoutine(
        id = "full_reset",
        name = "Full Posture Reset",
        subtitle = "The complete head-to-hip session",
        targets = "Everything above and below, in one pass",
        whenToDo = "Two or three times a week, or on rest days.",
        steps = listOf(
            RoutineStep("cat_cow", seconds = 60, reps = 10),
            RoutineStep("thoracic_extension", seconds = 60),
            RoutineStep("thread_the_needle", seconds = 40, perSide = true),
            RoutineStep("doorway_pec_stretch", seconds = 40, perSide = true),
            RoutineStep("levator_stretch", seconds = 30, perSide = true),
            RoutineStep("kneeling_hip_flexor", seconds = 45, perSide = true),
            RoutineStep("hamstring_stretch", seconds = 40, perSide = true),
            RoutineStep("hip_90_90", seconds = 60, reps = 10),
            RoutineStep("chin_tuck", seconds = 60, reps = 10),
            RoutineStep("wall_angel", seconds = 60, reps = 10),
            RoutineStep("scap_wall_slide", seconds = 45, reps = 10),
            RoutineStep("band_pull_apart", seconds = 45, reps = 15),
            RoutineStep("glute_bridge_posture", seconds = 60, reps = 12),
            RoutineStep("bird_dog", seconds = 60, reps = 8),
            RoutineStep("side_plank", seconds = 30, perSide = true),
            RoutineStep("prone_cobra", seconds = 45),
            RoutineStep("childs_pose", seconds = 45)
        )
    )

    val morningMobility = PostureRoutine(
        id = "morning",
        name = "Morning Mobility",
        subtitle = "Loosen up before the day sets in",
        targets = "Spine, hips, shoulders",
        whenToDo = "First thing, before you sit down anywhere.",
        steps = listOf(
            RoutineStep("cat_cow", seconds = 60, reps = 10),
            RoutineStep("childs_pose", seconds = 40),
            RoutineStep("thread_the_needle", seconds = 40, perSide = true),
            RoutineStep("hip_90_90", seconds = 60, reps = 10),
            RoutineStep("kneeling_hip_flexor", seconds = 40, perSide = true),
            RoutineStep("standing_forward_fold", seconds = 45),
            RoutineStep("wall_angel", seconds = 45, reps = 8)
        )
    )

    val all = listOf(deskReset, upperCrossFix, lowerCrossFix, fullReset, morningMobility)

    operator fun get(id: String): PostureRoutine? = all.firstOrNull { it.id == id }
}
