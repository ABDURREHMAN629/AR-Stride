package com.stride.fit.training

enum class Pattern(val label: String) {
    SQUAT("Squat"),
    HINGE("Hinge"),
    LUNGE("Lunge"),
    PUSH_HORIZONTAL("Horizontal push"),
    PUSH_VERTICAL("Vertical push"),
    PULL_HORIZONTAL("Horizontal pull"),
    PULL_VERTICAL("Vertical pull"),
    CORE("Core"),
    CARRY("Carry"),
    ARMS("Arms"),
    CALF("Calves"),
    MOBILITY("Mobility"),
    ACTIVATION("Activation"),
    STRETCH("Stretch")
}

enum class Equipment(val label: String) {
    NONE("Bodyweight"),
    DUMBBELL("Dumbbells"),
    BARBELL("Barbell"),
    BAND("Resistance band"),
    KETTLEBELL("Kettlebell"),
    MACHINE("Machine or cable"),
    BENCH("Bench or chair"),
    WALL("Wall or floor"),
    BAR("Pull-up bar")
}

enum class Level(val label: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced")
}

/** How a set is measured. Planks are not counted in reps. */
enum class Measure { REPS, SECONDS, SECONDS_PER_SIDE, REPS_PER_SIDE }

data class Exercise(
    val id: String,
    val name: String,
    val pattern: Pattern,
    val primary: List<String>,
    val secondary: List<String> = emptyList(),
    val equipment: Equipment = Equipment.NONE,
    val level: Level = Level.BEGINNER,
    val measure: Measure = Measure.REPS,
    val steps: List<String>,
    val cues: List<String> = emptyList(),
    val mistakes: List<String> = emptyList(),
    val tempo: String = "",
    val defaultRestSec: Int = 90,
    val breathing: String = ""
) {
    val muscles: String get() = primary.joinToString(", ")
}

/**
 * The exercise catalogue.
 *
 * Every entry carries real coaching detail rather than a name and a rep count,
 * because the difference between a rep that builds you and a rep that hurts you is
 * almost always in the setup.
 */
object Exercises {

    val all: List<Exercise> = listOf(

        Exercise(
            id = "goblet_squat",
            name = "Goblet Squat",
            pattern = Pattern.SQUAT,
            primary = listOf("Quadriceps", "Glutes"),
            secondary = listOf("Core", "Upper back"),
            equipment = Equipment.DUMBBELL,
            steps = listOf(
                "Hold one dumbbell vertically against your chest, both hands cupping the top head.",
                "Stand with feet shoulder-width, toes turned out about 15 degrees.",
                "Brace your stomach as if about to be poked, then sit down between your hips.",
                "Descend until your thigh is at least parallel to the floor, elbows tracking inside your knees.",
                "Drive through the whole foot to stand, squeezing the glutes at the top."
            ),
            cues = listOf(
                "Chest tall, elbows tucked in.",
                "Push the floor apart with your feet to stop the knees caving in.",
                "The weight against your chest is a counterbalance; let it help you stay upright."
            ),
            mistakes = listOf(
                "Heels lifting - widen the stance slightly or raise the heels on a small plate.",
                "Knees collapsing inward at the bottom.",
                "Rounding the lower back below parallel; stop at the depth you can hold neutral."
            ),
            tempo = "3-1-1-0",
            breathing = "Breathe in at the top, hold through the descent, breathe out on the way up.",
            defaultRestSec = 120
        ),
        Exercise(
            id = "bodyweight_squat",
            name = "Bodyweight Squat",
            pattern = Pattern.SQUAT,
            primary = listOf("Quadriceps", "Glutes"),
            equipment = Equipment.NONE,
            steps = listOf(
                "Feet shoulder-width, arms out in front as a counterbalance.",
                "Push the hips back and down, keeping the whole foot on the floor.",
                "Go as deep as you can with a flat back, then stand up tall."
            ),
            cues = listOf("Slow on the way down, fast on the way up.", "Knees track over the middle toes."),
            mistakes = listOf("Rushing the descent.", "Letting the lower back round at the bottom."),
            defaultRestSec = 60
        ),
        Exercise(
            id = "box_squat",
            name = "Box Squat",
            pattern = Pattern.SQUAT,
            primary = listOf("Quadriceps", "Glutes"),
            equipment = Equipment.BENCH,
            steps = listOf(
                "Set a chair or bench behind you at roughly knee height.",
                "Sit back until your glutes touch the surface, without flopping down.",
                "Pause for a beat, then stand without rocking forward."
            ),
            cues = listOf("Touch, do not sit.", "Keep your shins vertical as you sit back."),
            mistakes = listOf("Dropping onto the box, which loads the spine."),
            defaultRestSec = 90
        ),
        Exercise(
            id = "wall_sit",
            name = "Wall Sit",
            pattern = Pattern.SQUAT,
            primary = listOf("Quadriceps"),
            equipment = Equipment.WALL,
            measure = Measure.SECONDS,
            steps = listOf(
                "Lean your back flat against a wall and walk your feet out.",
                "Slide down until knees and hips are both at 90 degrees.",
                "Hold, keeping your weight in your heels."
            ),
            cues = listOf("Knees stacked over ankles, not in front of the toes."),
            mistakes = listOf("Sliding too low and putting the load onto the knee joint."),
            defaultRestSec = 60
        ),

        Exercise(
            id = "db_rdl",
            name = "Dumbbell Romanian Deadlift",
            pattern = Pattern.HINGE,
            primary = listOf("Hamstrings", "Glutes"),
            secondary = listOf("Lower back", "Lats"),
            equipment = Equipment.DUMBBELL,
            level = Level.BEGINNER,
            steps = listOf(
                "Stand with dumbbells in front of your thighs, knees softly bent.",
                "Push your hips straight back, letting the weights slide down your legs.",
                "Stop when you feel a strong stretch in the hamstrings, usually just below the knee.",
                "Drive the hips forward to stand, finishing with glutes squeezed."
            ),
            cues = listOf(
                "This is a hip movement, not a back movement - the knees barely change angle.",
                "Keep the dumbbells in contact with your legs the whole way.",
                "Imagine closing a car door with your backside."
            ),
            mistakes = listOf(
                "Turning it into a squat by bending the knees.",
                "Letting the weights drift away from the body, which loads the lower back.",
                "Chasing depth by rounding the spine."
            ),
            tempo = "3-1-1-0",
            breathing = "Breathe in as you hinge, out as you stand.",
            defaultRestSec = 120
        ),
        Exercise(
            id = "glute_bridge",
            name = "Glute Bridge",
            pattern = Pattern.HINGE,
            primary = listOf("Glutes"),
            secondary = listOf("Hamstrings", "Core"),
            equipment = Equipment.NONE,
            steps = listOf(
                "Lie on your back, knees bent, heels about a hand-span from your glutes.",
                "Tuck the pelvis slightly so the lower back flattens against the floor.",
                "Press through your heels and lift the hips until knees, hips and shoulders line up.",
                "Squeeze hard for a second, then lower with control."
            ),
            cues = listOf(
                "Ribs down - do not arch the lower back to gain height.",
                "The squeeze at the top matters more than how high you get."
            ),
            mistakes = listOf("Arching the lumbar spine instead of extending the hip."),
            defaultRestSec = 60
        ),
        Exercise(
            id = "hip_thrust",
            name = "Hip Thrust",
            pattern = Pattern.HINGE,
            primary = listOf("Glutes"),
            secondary = listOf("Hamstrings"),
            equipment = Equipment.BENCH,
            level = Level.INTERMEDIATE,
            steps = listOf(
                "Sit on the floor with your upper back against a bench, feet flat.",
                "Rest a dumbbell across your hips, padded if needed.",
                "Drive through the heels, extend the hips fully, chin tucked.",
                "Lower until just above the floor and repeat."
            ),
            cues = listOf("Finish with the shins vertical.", "Tuck the chin and look forward, not up."),
            mistakes = listOf("Hyperextending the lower back at the top."),
            defaultRestSec = 120
        ),
        Exercise(
            id = "kb_swing",
            name = "Kettlebell Swing",
            pattern = Pattern.HINGE,
            primary = listOf("Glutes", "Hamstrings"),
            secondary = listOf("Core", "Shoulders"),
            equipment = Equipment.KETTLEBELL,
            level = Level.INTERMEDIATE,
            steps = listOf(
                "Stand with the bell a foot in front of you, hinge and grip it with both hands.",
                "Hike it back between your legs like a rugby pass.",
                "Snap the hips forward hard - the bell floats to chest height on its own.",
                "Let it fall, catch the hinge, repeat."
            ),
            cues = listOf(
                "Power comes from the hips, not from lifting with the arms.",
                "The arms are ropes attached to the bell."
            ),
            mistakes = listOf("Squatting the swing.", "Lifting the bell with the shoulders."),
            defaultRestSec = 90
        ),
        Exercise(
            id = "good_morning",
            name = "Good Morning",
            pattern = Pattern.HINGE,
            primary = listOf("Hamstrings", "Lower back"),
            equipment = Equipment.NONE,
            steps = listOf(
                "Hands behind the head, feet hip-width, knees soft.",
                "Hinge at the hips with a flat back until your torso is near parallel.",
                "Return by squeezing the glutes."
            ),
            cues = listOf("A broomstick along your spine should touch head, mid-back and tailbone."),
            mistakes = listOf("Rounding the upper back."),
            defaultRestSec = 60
        ),

        Exercise(
            id = "reverse_lunge",
            name = "Reverse Lunge",
            pattern = Pattern.LUNGE,
            primary = listOf("Quadriceps", "Glutes"),
            secondary = listOf("Core"),
            equipment = Equipment.NONE,
            measure = Measure.REPS_PER_SIDE,
            steps = listOf(
                "Stand tall, step one foot back and lower the back knee towards the floor.",
                "Both knees finish at about 90 degrees, torso upright.",
                "Push through the front heel to return to standing."
            ),
            cues = listOf(
                "Stepping backwards is far kinder to the knees than stepping forwards.",
                "Keep 80 percent of the weight in the front leg."
            ),
            mistakes = listOf("Short step, which drives the front knee far over the toes."),
            defaultRestSec = 90
        ),
        Exercise(
            id = "split_squat",
            name = "Split Squat",
            pattern = Pattern.LUNGE,
            primary = listOf("Quadriceps", "Glutes"),
            equipment = Equipment.NONE,
            measure = Measure.REPS_PER_SIDE,
            steps = listOf(
                "Set up in a long stride, back heel lifted.",
                "Drop straight down until the back knee nearly touches.",
                "Drive up through the front foot without changing stance."
            ),
            cues = listOf("Move straight up and down, like a lift, not forwards and back."),
            mistakes = listOf("Stance too narrow, which makes balance the limiting factor."),
            defaultRestSec = 90
        ),
        Exercise(
            id = "bulgarian_split_squat",
            name = "Bulgarian Split Squat",
            pattern = Pattern.LUNGE,
            primary = listOf("Quadriceps", "Glutes"),
            equipment = Equipment.BENCH,
            level = Level.INTERMEDIATE,
            measure = Measure.REPS_PER_SIDE,
            steps = listOf(
                "Place the top of your back foot on a bench behind you.",
                "Hop the front foot far enough forward that the front shin stays near vertical.",
                "Lower until the back knee is just off the floor, then drive up."
            ),
            cues = listOf("Lean the torso forward slightly to bias the glute."),
            mistakes = listOf("Front foot too close, which turns it into a knee-crusher."),
            defaultRestSec = 90
        ),
        Exercise(
            id = "step_up",
            name = "Step-Up",
            pattern = Pattern.LUNGE,
            primary = listOf("Quadriceps", "Glutes"),
            equipment = Equipment.BENCH,
            measure = Measure.REPS_PER_SIDE,
            steps = listOf(
                "Set a step at roughly knee height.",
                "Place the whole foot on the step and drive up through that heel.",
                "Lower yourself down under control - do not drop."
            ),
            cues = listOf("The bottom leg should not push off the floor."),
            mistakes = listOf("Bouncing off the trailing foot."),
            defaultRestSec = 75
        ),

        Exercise(
            id = "push_up",
            name = "Push-Up",
            pattern = Pattern.PUSH_HORIZONTAL,
            primary = listOf("Chest", "Triceps"),
            secondary = listOf("Shoulders", "Core"),
            equipment = Equipment.NONE,
            steps = listOf(
                "Hands slightly wider than shoulders, fingers spread.",
                "Body in one straight line from head to heels, glutes and abs braced.",
                "Lower until the chest is a fist from the floor, elbows at about 45 degrees.",
                "Press back up and push the floor away."
            ),
            cues = listOf(
                "Screw the hands into the floor to create shoulder stability.",
                "Elbows at 45 degrees, not flared to 90."
            ),
            mistakes = listOf(
                "Hips sagging, which loads the lower back.",
                "Head poking forward to fake depth."
            ),
            tempo = "2-1-1-0",
            defaultRestSec = 90
        ),
        Exercise(
            id = "incline_push_up",
            name = "Incline Push-Up",
            pattern = Pattern.PUSH_HORIZONTAL,
            primary = listOf("Chest", "Triceps"),
            equipment = Equipment.BENCH,
            steps = listOf(
                "Place your hands on a bench, table or wall - the higher the surface, the easier.",
                "Keep the same straight body line as a floor push-up.",
                "Lower the chest to the surface and press away."
            ),
            cues = listOf("Lower the surface as you get stronger; this is the on-ramp to full push-ups."),
            mistakes = listOf("Letting the hips lead the way down."),
            defaultRestSec = 75
        ),
        Exercise(
            id = "db_bench_press",
            name = "Dumbbell Bench Press",
            pattern = Pattern.PUSH_HORIZONTAL,
            primary = listOf("Chest", "Triceps"),
            secondary = listOf("Shoulders"),
            equipment = Equipment.DUMBBELL,
            steps = listOf(
                "Lie back with a dumbbell in each hand at chest level.",
                "Set the shoulder blades down and back into the bench.",
                "Press up and slightly together, without banging the weights.",
                "Lower until you feel a stretch across the chest."
            ),
            cues = listOf("Keep a small natural arch and the shoulder blades pinned."),
            mistakes = listOf("Shoulders rolling forward at the top."),
            defaultRestSec = 120
        ),
        Exercise(
            id = "floor_press",
            name = "Dumbbell Floor Press",
            pattern = Pattern.PUSH_HORIZONTAL,
            primary = listOf("Chest", "Triceps"),
            equipment = Equipment.DUMBBELL,
            steps = listOf(
                "Lie on the floor, knees bent, dumbbells over your chest.",
                "Lower until the upper arms touch the floor, pause, then press."
            ),
            cues = listOf("The floor caps the range, which is friendly to cranky shoulders."),
            mistakes = listOf("Bouncing the elbows off the floor."),
            defaultRestSec = 90
        ),

        Exercise(
            id = "db_overhead_press",
            name = "Dumbbell Overhead Press",
            pattern = Pattern.PUSH_VERTICAL,
            primary = listOf("Shoulders"),
            secondary = listOf("Triceps", "Core"),
            equipment = Equipment.DUMBBELL,
            steps = listOf(
                "Stand tall, dumbbells at shoulder height, palms facing forward.",
                "Squeeze the glutes and brace the stomach so the ribs stay down.",
                "Press overhead until the arms lock out with the biceps near the ears.",
                "Lower under control to shoulder height."
            ),
            cues = listOf(
                "Do not let the lower back arch to finish the rep.",
                "Think of pushing yourself down into the floor as you press up."
            ),
            mistakes = listOf("Leaning back to press, which turns it into an incline press."),
            defaultRestSec = 120
        ),
        Exercise(
            id = "pike_push_up",
            name = "Pike Push-Up",
            pattern = Pattern.PUSH_VERTICAL,
            primary = listOf("Shoulders"),
            secondary = listOf("Triceps"),
            equipment = Equipment.NONE,
            level = Level.INTERMEDIATE,
            steps = listOf(
                "From a push-up position, walk your feet in and lift the hips into an upside-down V.",
                "Lower the crown of your head towards the floor between your hands.",
                "Press back up."
            ),
            cues = listOf("The higher the hips, the more vertical the press."),
            mistakes = listOf("Letting the hips drop into a normal push-up."),
            defaultRestSec = 90
        ),

        Exercise(
            id = "db_row",
            name = "Single-Arm Dumbbell Row",
            pattern = Pattern.PULL_HORIZONTAL,
            primary = listOf("Lats", "Mid back"),
            secondary = listOf("Biceps", "Rear delts"),
            equipment = Equipment.DUMBBELL,
            measure = Measure.REPS_PER_SIDE,
            steps = listOf(
                "Place one hand and the same-side knee on a bench, back flat and parallel to the floor.",
                "Let the dumbbell hang straight down, shoulder blade stretched forward.",
                "Pull the elbow back towards your hip, leading with the elbow not the hand.",
                "Squeeze the shoulder blade in, then lower all the way."
            ),
            cues = listOf(
                "Row towards the hip, not the armpit.",
                "Let the shoulder blade travel - do not lock the torso rigid."
            ),
            mistakes = listOf(
                "Twisting the torso to heave the weight up.",
                "Shrugging, which turns it into a trap exercise."
            ),
            tempo = "2-1-2-0",
            defaultRestSec = 90
        ),
        Exercise(
            id = "inverted_row",
            name = "Inverted Row",
            pattern = Pattern.PULL_HORIZONTAL,
            primary = listOf("Mid back", "Lats"),
            secondary = listOf("Biceps"),
            equipment = Equipment.BAR,
            steps = listOf(
                "Set a bar or sturdy table edge at hip height and lie underneath it.",
                "Grip slightly wider than shoulders, body in a straight line, heels on the floor.",
                "Pull your chest to the bar, squeezing the shoulder blades together.",
                "Lower with control until the arms are straight."
            ),
            cues = listOf("Walk the feet further out to make it harder."),
            mistakes = listOf("Hips sagging.", "Chin reaching for the bar instead of the chest."),
            defaultRestSec = 90
        ),
        Exercise(
            id = "band_row",
            name = "Seated Band Row",
            pattern = Pattern.PULL_HORIZONTAL,
            primary = listOf("Mid back"),
            secondary = listOf("Biceps", "Rear delts"),
            equipment = Equipment.BAND,
            steps = listOf(
                "Sit with legs straight and loop a band around your feet.",
                "Sit tall, then pull the handles to your ribs, elbows close to the body.",
                "Squeeze for a beat, then let the band pull your arms forward."
            ),
            cues = listOf("Chest up, shoulders down and back."),
            mistakes = listOf("Rocking the torso backwards to move the band."),
            defaultRestSec = 75
        ),
        Exercise(
            id = "chest_supported_row",
            name = "Chest-Supported Row",
            pattern = Pattern.PULL_HORIZONTAL,
            primary = listOf("Mid back", "Rear delts"),
            equipment = Equipment.BENCH,
            steps = listOf(
                "Lie face down on an inclined bench with dumbbells hanging.",
                "Row both elbows back and squeeze the shoulder blades.",
                "Lower fully."
            ),
            cues = listOf("The bench removes the lower back entirely, so you can chase the squeeze."),
            mistakes = listOf("Lifting the chest off the bench."),
            defaultRestSec = 90
        ),

        Exercise(
            id = "lat_pulldown",
            name = "Lat Pulldown",
            pattern = Pattern.PULL_VERTICAL,
            primary = listOf("Lats"),
            secondary = listOf("Biceps", "Mid back"),
            equipment = Equipment.MACHINE,
            steps = listOf(
                "Grip the bar a little wider than shoulders and pin your thighs under the pad.",
                "Lean back slightly, chest up.",
                "Pull the bar to your collarbone by driving the elbows down.",
                "Let it rise until the lats are fully stretched."
            ),
            cues = listOf("Think elbows into your back pockets."),
            mistakes = listOf("Pulling behind the neck.", "Leaning back so far it becomes a row."),
            defaultRestSec = 90
        ),
        Exercise(
            id = "band_pulldown",
            name = "Band Lat Pulldown",
            pattern = Pattern.PULL_VERTICAL,
            primary = listOf("Lats"),
            equipment = Equipment.BAND,
            steps = listOf(
                "Anchor a band above head height, kneel and hold both ends.",
                "Keep a tall torso and pull the hands down and out to your sides.",
                "Control the return."
            ),
            cues = listOf("Start the pull by depressing the shoulder blades, then bend the elbows."),
            mistakes = listOf("Leaning back to create range."),
            defaultRestSec = 75
        ),
        Exercise(
            id = "assisted_pull_up",
            name = "Assisted Pull-Up",
            pattern = Pattern.PULL_VERTICAL,
            primary = listOf("Lats"),
            secondary = listOf("Biceps"),
            equipment = Equipment.BAR,
            level = Level.INTERMEDIATE,
            steps = listOf(
                "Loop a band over the bar and put one foot or knee in it.",
                "Hang with straight arms, shoulders pulled down away from the ears.",
                "Pull until the chin clears the bar, then lower slowly."
            ),
            cues = listOf("The slow lower is where most of the strength is built."),
            mistakes = listOf("Kipping.", "Starting each rep from a dead relaxed hang with loose shoulders."),
            defaultRestSec = 120
        ),

        Exercise(
            id = "db_curl",
            name = "Dumbbell Curl",
            pattern = Pattern.ARMS,
            primary = listOf("Biceps"),
            equipment = Equipment.DUMBBELL,
            steps = listOf(
                "Stand tall, arms at your sides, palms forward.",
                "Curl without letting the elbows drift forward.",
                "Lower all the way to a straight arm."
            ),
            cues = listOf("Elbows pinned to your ribs."),
            mistakes = listOf("Swinging the torso.", "Stopping halfway down."),
            defaultRestSec = 60
        ),
        Exercise(
            id = "hammer_curl",
            name = "Hammer Curl",
            pattern = Pattern.ARMS,
            primary = listOf("Biceps", "Forearms"),
            equipment = Equipment.DUMBBELL,
            steps = listOf(
                "Hold the dumbbells with palms facing each other.",
                "Curl up keeping that neutral grip, then lower slowly."
            ),
            cues = listOf("This hits the brachialis, which pushes the biceps up as it grows."),
            mistakes = listOf("Rotating the wrist at the top."),
            defaultRestSec = 60
        ),
        Exercise(
            id = "triceps_pushdown",
            name = "Triceps Pushdown",
            pattern = Pattern.ARMS,
            primary = listOf("Triceps"),
            equipment = Equipment.BAND,
            steps = listOf(
                "Anchor a band or cable at head height.",
                "Elbows tucked at your sides, push down until the arms lock.",
                "Return only until the forearms are horizontal."
            ),
            cues = listOf("Only the forearm moves."),
            mistakes = listOf("Letting the elbows travel forward and turning it into a press."),
            defaultRestSec = 60
        ),
        Exercise(
            id = "bench_dip",
            name = "Bench Dip",
            pattern = Pattern.ARMS,
            primary = listOf("Triceps"),
            secondary = listOf("Shoulders"),
            equipment = Equipment.BENCH,
            steps = listOf(
                "Sit on the edge of a bench, hands beside your hips, feet out in front.",
                "Slide off and lower until the elbows reach 90 degrees.",
                "Press back up."
            ),
            cues = listOf("Keep your back close to the bench."),
            mistakes = listOf("Dropping too deep and jamming the front of the shoulder."),
            defaultRestSec = 75
        ),

        Exercise(
            id = "plank",
            name = "Front Plank",
            pattern = Pattern.CORE,
            primary = listOf("Core"),
            secondary = listOf("Glutes", "Shoulders"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS,
            steps = listOf(
                "Forearms under the shoulders, elbows shoulder-width.",
                "Tuck the tailbone slightly so the lower back flattens.",
                "Squeeze glutes and quads, and pull the elbows towards your toes without moving them."
            ),
            cues = listOf(
                "A hard 30 seconds beats a floppy two minutes.",
                "Breathe normally - if you cannot talk, you are bracing too hard."
            ),
            mistakes = listOf("Hips sagging or piking up.", "Holding the breath."),
            defaultRestSec = 60
        ),
        Exercise(
            id = "side_plank",
            name = "Side Plank",
            pattern = Pattern.CORE,
            primary = listOf("Obliques", "Glute medius"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS_PER_SIDE,
            steps = listOf(
                "Lie on your side, elbow under the shoulder, knees or feet stacked.",
                "Lift the hips so the body forms one straight line.",
                "Hold, then swap sides."
            ),
            cues = listOf("Push the floor away and lift the bottom ribs."),
            mistakes = listOf("Hips drifting backwards."),
            defaultRestSec = 45
        ),
        Exercise(
            id = "dead_bug",
            name = "Dead Bug",
            pattern = Pattern.CORE,
            primary = listOf("Core"),
            secondary = listOf("Hip flexors"),
            equipment = Equipment.NONE,
            measure = Measure.REPS_PER_SIDE,
            steps = listOf(
                "Lie on your back, arms straight up, hips and knees bent to 90 degrees.",
                "Press the lower back into the floor and keep it there.",
                "Slowly lower one arm and the opposite leg towards the floor.",
                "Return and swap sides."
            ),
            cues = listOf(
                "The lower back must not lift off the floor - that is the whole exercise.",
                "Go only as far as you can while keeping contact."
            ),
            mistakes = listOf("Rushing.", "Arching the back as the leg lowers."),
            defaultRestSec = 45
        ),
        Exercise(
            id = "bird_dog",
            name = "Bird Dog",
            pattern = Pattern.CORE,
            primary = listOf("Core", "Lower back"),
            secondary = listOf("Glutes"),
            equipment = Equipment.NONE,
            measure = Measure.REPS_PER_SIDE,
            steps = listOf(
                "On all fours, hands under shoulders, knees under hips.",
                "Extend one arm forward and the opposite leg back until both are level with the torso.",
                "Hold for two seconds, return, and swap."
            ),
            cues = listOf("Imagine balancing a glass of water on your lower back."),
            mistakes = listOf("Hips rotating open.", "Lifting the leg above hip height and arching."),
            defaultRestSec = 45
        ),
        Exercise(
            id = "pallof_press",
            name = "Pallof Press",
            pattern = Pattern.CORE,
            primary = listOf("Obliques", "Core"),
            equipment = Equipment.BAND,
            measure = Measure.REPS_PER_SIDE,
            steps = listOf(
                "Anchor a band at chest height and stand side-on, holding it with both hands at your sternum.",
                "Step out until there is real tension.",
                "Press the hands straight out and resist the band trying to rotate you.",
                "Return to the chest."
            ),
            cues = listOf("Nothing should rotate. That resistance is the point."),
            mistakes = listOf("Letting the shoulders turn towards the anchor."),
            defaultRestSec = 45
        ),
        Exercise(
            id = "hollow_hold",
            name = "Hollow Hold",
            pattern = Pattern.CORE,
            primary = listOf("Core"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS,
            level = Level.INTERMEDIATE,
            steps = listOf(
                "Lie on your back and press the lower spine into the floor.",
                "Lift the shoulders and legs just off the ground.",
                "Hold that dish shape."
            ),
            cues = listOf("Lower the legs if the back lifts; height is not the goal."),
            mistakes = listOf("Letting the ribs flare."),
            defaultRestSec = 60
        ),
        Exercise(
            id = "hanging_knee_raise",
            name = "Hanging Knee Raise",
            pattern = Pattern.CORE,
            primary = listOf("Core", "Hip flexors"),
            equipment = Equipment.BAR,
            level = Level.INTERMEDIATE,
            steps = listOf(
                "Hang from a bar with the shoulders active, not fully relaxed.",
                "Raise the knees towards the chest by curling the pelvis up.",
                "Lower slowly without swinging."
            ),
            cues = listOf("Curl the tailbone up rather than just lifting the thighs."),
            mistakes = listOf("Swinging for momentum."),
            defaultRestSec = 75
        ),
        Exercise(
            id = "farmer_carry",
            name = "Farmer Carry",
            pattern = Pattern.CARRY,
            primary = listOf("Core", "Grip", "Traps"),
            equipment = Equipment.DUMBBELL,
            measure = Measure.SECONDS,
            steps = listOf(
                "Pick up a heavy dumbbell in each hand.",
                "Stand tall, shoulders back and down, ribs stacked over the pelvis.",
                "Walk with short controlled steps."
            ),
            cues = listOf("This is a moving plank, and it is one of the best posture drills there is."),
            mistakes = listOf("Leaning back or letting the shoulders round forward."),
            defaultRestSec = 75
        ),
        Exercise(
            id = "suitcase_carry",
            name = "Suitcase Carry",
            pattern = Pattern.CARRY,
            primary = listOf("Obliques", "Grip"),
            equipment = Equipment.DUMBBELL,
            measure = Measure.SECONDS_PER_SIDE,
            steps = listOf(
                "Hold one heavy dumbbell in a single hand.",
                "Walk without letting your torso lean towards the weight.",
                "Swap hands."
            ),
            cues = listOf("Stay perfectly upright; the obliques do the work of staying square."),
            mistakes = listOf("Hiking the loaded shoulder up."),
            defaultRestSec = 60
        ),
        Exercise(
            id = "calf_raise",
            name = "Standing Calf Raise",
            pattern = Pattern.CALF,
            primary = listOf("Calves"),
            equipment = Equipment.NONE,
            steps = listOf(
                "Stand with the balls of your feet on a step, heels hanging off.",
                "Rise as high as you can onto the toes.",
                "Lower until you feel a deep stretch."
            ),
            cues = listOf("Pause for a second at both ends."),
            mistakes = listOf("Bouncing through a tiny range."),
            defaultRestSec = 45
        )
    )

    private val byId: Map<String, Exercise> = all.associateBy { it.id }

    operator fun get(id: String): Exercise? =
        byId[id] ?: PostureExercises[id] ?: YogaPoses[id] ?: HomeExtras[id]

    fun require(id: String): Exercise =
        get(id) ?: error("Unknown exercise id: $id")

    fun byPattern(pattern: Pattern): List<Exercise> = all.filter { it.pattern == pattern }

    fun byEquipment(equipment: Equipment): List<Exercise> = all.filter { it.equipment == equipment }

    fun search(query: String): List<Exercise> {
        if (query.isBlank()) return all
        val q = query.trim().lowercase()
        return all.filter { ex ->
            ex.name.lowercase().contains(q) ||
                ex.primary.any { it.lowercase().contains(q) } ||
                ex.pattern.label.lowercase().contains(q) ||
                ex.equipment.label.lowercase().contains(q)
        }
    }
}
