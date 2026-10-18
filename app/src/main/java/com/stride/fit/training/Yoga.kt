package com.stride.fit.training

/**
 * Yoga poses and flows.
 *
 * Sanskrit names are kept alongside the English because that is what you will hear in
 * any class, but every instruction is in plain language. Nothing here needs a mat you
 * do not have or a flexibility you have not built yet - each pose carries the honest
 * easier option rather than pretending everyone can reach the floor.
 */
object YogaPoses {

    val all: List<Exercise> = listOf(

        Exercise(
            id = "y_mountain",
            name = "Mountain Pose (Tadasana)",
            pattern = Pattern.MOBILITY,
            primary = listOf("Posture", "Whole body"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS,
            steps = listOf(
                "Stand with feet hip-width, weight even across both feet.",
                "Spread your toes and press down through the big toe, little toe and heel.",
                "Lengthen up through the crown of the head, letting the ribs settle over the hips.",
                "Arms relaxed by your sides, palms forward, shoulders down and back."
            ),
            cues = listOf(
                "This is the reference posture every standing pose returns to.",
                "Think tall rather than stiff - there should be no gripping anywhere."
            ),
            mistakes = listOf("Locking the knees.", "Letting the ribs flare and the back arch."),
            breathing = "Slow and even through the nose. Five to eight breaths.",
            defaultRestSec = 0
        ),
        Exercise(
            id = "y_downdog",
            name = "Downward Dog (Adho Mukha Svanasana)",
            pattern = Pattern.STRETCH,
            primary = listOf("Hamstrings", "Calves", "Shoulders"),
            secondary = listOf("Lats", "Spine"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS,
            steps = listOf(
                "From hands and knees, tuck the toes and lift the hips up and back.",
                "Push the floor away through the whole hand, especially the index finger knuckle.",
                "Bend the knees as much as you need so the back can stay long.",
                "Let the head hang between the arms, ears in line with the biceps."
            ),
            cues = listOf(
                "A long spine beats straight legs. Bend the knees freely.",
                "Pedal the heels one at a time to wake up the calves."
            ),
            mistakes = listOf(
                "Forcing the heels down and rounding the lower back.",
                "Collapsing between the shoulder blades."
            ),
            breathing = "Five to eight slow breaths.",
            defaultRestSec = 0
        ),
        Exercise(
            id = "y_cobra",
            name = "Cobra (Bhujangasana)",
            pattern = Pattern.MOBILITY,
            primary = listOf("Spinal erectors", "Chest"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS,
            steps = listOf(
                "Lie face down, hands under the shoulders, elbows tucked in.",
                "Press the tops of the feet and pubic bone into the floor.",
                "Peel the chest up, leading with the sternum rather than the chin.",
                "Keep a slight bend in the elbows and the shoulders drawing back."
            ),
            cues = listOf(
                "Use the back muscles, not the arms - try lifting the hands a moment to test it.",
                "Length before height."
            ),
            mistakes = listOf("Cranking the neck back.", "Shoulders creeping up to the ears."),
            breathing = "Inhale to lift, breathe steadily at the top.",
            defaultRestSec = 0
        ),
        Exercise(
            id = "y_warrior1",
            name = "Warrior I (Virabhadrasana I)",
            pattern = Pattern.LUNGE,
            primary = listOf("Quadriceps", "Glutes", "Hip flexors"),
            secondary = listOf("Shoulders"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS_PER_SIDE,
            steps = listOf(
                "Step one foot back about a leg length, back toes turned out 45 degrees.",
                "Bend the front knee towards 90 degrees, tracking over the middle toes.",
                "Square both hips towards the front of the room.",
                "Reach the arms overhead, ribs down."
            ),
            cues = listOf(
                "Widen the stance side to side if squaring the hips feels impossible.",
                "Press the outer edge of the back foot down."
            ),
            mistakes = listOf(
                "Front knee collapsing inward.",
                "Over-arching the lower back to get the arms up."
            ),
            breathing = "Five breaths each side.",
            defaultRestSec = 0
        ),
        Exercise(
            id = "y_warrior2",
            name = "Warrior II (Virabhadrasana II)",
            pattern = Pattern.LUNGE,
            primary = listOf("Quadriceps", "Glutes", "Adductors"),
            secondary = listOf("Shoulders"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS_PER_SIDE,
            steps = listOf(
                "From a wide stance, turn the front foot out 90 degrees and the back foot in slightly.",
                "Bend the front knee over the ankle.",
                "Open the hips and chest to the side, arms reaching front and back at shoulder height.",
                "Gaze over the front hand."
            ),
            cues = listOf(
                "Front thigh works toward parallel with the floor, but only as far as you can hold it.",
                "Keep the torso stacked directly over the hips, not leaning forward."
            ),
            mistakes = listOf("Leaning the torso over the front leg.", "Front knee drifting inward."),
            breathing = "Five breaths each side.",
            defaultRestSec = 0
        ),
        Exercise(
            id = "y_triangle",
            name = "Triangle (Trikonasana)",
            pattern = Pattern.STRETCH,
            primary = listOf("Hamstrings", "Obliques"),
            secondary = listOf("Hips", "Shoulders"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS_PER_SIDE,
            steps = listOf(
                "Wide stance, front foot turned out 90 degrees, both legs straight but not locked.",
                "Reach forward over the front leg, then lower the front hand to the shin or a block.",
                "Stack the top shoulder over the bottom one and reach the top arm up.",
                "Turn the chest open towards the ceiling."
            ),
            cues = listOf(
                "Rest the hand higher up the shin rather than reaching the floor and collapsing.",
                "This is a side bend, not a forward fold."
            ),
            mistakes = listOf("Rounding the spine to touch the floor.", "Hyperextending the front knee."),
            breathing = "Five breaths each side.",
            defaultRestSec = 0
        ),
        Exercise(
            id = "y_tree",
            name = "Tree Pose (Vrksasana)",
            pattern = Pattern.ACTIVATION,
            primary = listOf("Glute medius", "Ankle stabilisers", "Core"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS_PER_SIDE,
            steps = listOf(
                "Stand tall and shift your weight into one foot.",
                "Place the other foot on the ankle, calf, or inner thigh - never on the knee.",
                "Press foot and leg into each other to create stability.",
                "Hands at the chest or reaching overhead."
            ),
            cues = listOf(
                "Fix your eyes on one unmoving point. Balance is mostly a vision problem.",
                "A hand on a wall is not cheating."
            ),
            mistakes = listOf("Foot pressing on the side of the knee joint.", "Hip jutting out to the side."),
            breathing = "Five to eight breaths each side.",
            defaultRestSec = 0
        ),
        Exercise(
            id = "y_chair",
            name = "Chair Pose (Utkatasana)",
            pattern = Pattern.SQUAT,
            primary = listOf("Quadriceps", "Glutes"),
            secondary = listOf("Shoulders", "Core"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS,
            steps = listOf(
                "Feet together or hip-width, bend the knees and sit the hips back.",
                "Shift the weight into the heels until you can wiggle your toes.",
                "Reach the arms alongside the ears, ribs down.",
                "Hold, breathing steadily."
            ),
            cues = listOf("Sit back, do not just bend forward.", "Lower the arms if the shoulders shrug."),
            mistakes = listOf("Weight in the toes.", "Lower back over-arching."),
            breathing = "Five breaths.",
            defaultRestSec = 0
        ),
        Exercise(
            id = "y_low_lunge",
            name = "Low Lunge (Anjaneyasana)",
            pattern = Pattern.STRETCH,
            primary = listOf("Hip flexors", "Quadriceps"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS_PER_SIDE,
            steps = listOf(
                "From a lunge, lower the back knee to the floor and untuck the back toes.",
                "Tuck the tailbone under and squeeze the back glute.",
                "Sink the hips forward and down until the front of the back hip stretches.",
                "Reach the arms overhead if it feels available."
            ),
            cues = listOf(
                "The glute squeeze is what makes this reach the hip instead of the lower back.",
                "Pad the back knee with a folded towel."
            ),
            mistakes = listOf("Arching the lower back to sink lower."),
            breathing = "Five to eight breaths each side.",
            defaultRestSec = 0
        ),
        Exercise(
            id = "y_pigeon",
            name = "Pigeon Pose (Eka Pada Rajakapotasana)",
            pattern = Pattern.STRETCH,
            primary = listOf("Glutes", "Hip rotators"),
            equipment = Equipment.NONE,
            level = Level.INTERMEDIATE,
            measure = Measure.SECONDS_PER_SIDE,
            steps = listOf(
                "From all fours, bring one knee forward behind the same-side wrist.",
                "Slide the shin across, angling it so the ankle points towards the other hand.",
                "Extend the back leg straight behind you, hips square to the floor.",
                "Stay upright on the hands, or walk them forward and fold."
            ),
            cues = listOf(
                "Put a cushion under the hip of the bent leg - almost everyone needs it.",
                "The less the shin is parallel to the front of the mat, the gentler it is."
            ),
            mistakes = listOf(
                "Letting the hips tip to one side.",
                "Pushing into knee pain - back off immediately if the knee complains."
            ),
            breathing = "Eight slow breaths each side. This one takes time to release.",
            defaultRestSec = 0
        ),
        Exercise(
            id = "y_seated_fold",
            name = "Seated Forward Fold (Paschimottanasana)",
            pattern = Pattern.STRETCH,
            primary = listOf("Hamstrings", "Lower back"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS,
            steps = listOf(
                "Sit with legs straight out in front. Sit on a folded blanket if the back rounds immediately.",
                "Inhale and lengthen the spine upward.",
                "Exhale and hinge forward from the hips, reaching the chest towards the shins.",
                "Hold wherever the stretch is honest."
            ),
            cues = listOf(
                "Bend the knees generously. Lead with the chest, not the forehead.",
                "It does not matter at all whether you reach your toes."
            ),
            mistakes = listOf("Yanking on the feet and rounding the whole spine."),
            breathing = "Eight slow breaths, softening a little on each exhale.",
            defaultRestSec = 0
        ),
        Exercise(
            id = "y_bridge",
            name = "Bridge Pose (Setu Bandha Sarvangasana)",
            pattern = Pattern.HINGE,
            primary = listOf("Glutes", "Hamstrings"),
            secondary = listOf("Chest", "Spine"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS,
            steps = listOf(
                "Lie on your back, knees bent, feet hip-width and close to the glutes.",
                "Press through the feet and lift the hips.",
                "Roll the shoulders underneath you and clasp the hands if comfortable.",
                "Lift the chest towards the chin rather than the chin towards the chest."
            ),
            cues = listOf("Press the inner edges of the feet down to stop the knees splaying."),
            mistakes = listOf("Turning the head while the neck is loaded.", "Squeezing the glutes so hard the back cramps."),
            breathing = "Five to eight breaths.",
            defaultRestSec = 0
        ),
        Exercise(
            id = "y_supine_twist",
            name = "Supine Twist (Supta Matsyendrasana)",
            pattern = Pattern.MOBILITY,
            primary = listOf("Spine", "Glutes"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS_PER_SIDE,
            steps = listOf(
                "Lie on your back and hug one knee into the chest.",
                "Guide that knee across the body towards the floor with the opposite hand.",
                "Extend the other arm out to the side and turn your head away from the knee.",
                "Let gravity do the work."
            ),
            cues = listOf(
                "Keep both shoulders heavy on the floor, even if the knee stays in the air.",
                "Support the knee on a cushion if it hangs."
            ),
            mistakes = listOf("Forcing the knee down and lifting the opposite shoulder."),
            breathing = "Eight slow breaths each side.",
            defaultRestSec = 0
        ),
        Exercise(
            id = "y_legs_up_wall",
            name = "Legs Up the Wall (Viparita Karani)",
            pattern = Pattern.STRETCH,
            primary = listOf("Hamstrings", "Recovery"),
            equipment = Equipment.WALL,
            measure = Measure.SECONDS,
            steps = listOf(
                "Sit side-on to a wall, then swing the legs up as you lie back.",
                "Shuffle the hips as close to the wall as is comfortable.",
                "Arms out to the sides, palms up. Let the legs be completely passive.",
                "Stay and breathe."
            ),
            cues = listOf(
                "The single best thing you can do for tired legs after a long walk or run.",
                "Slide the hips further from the wall if the hamstrings complain."
            ),
            mistakes = listOf("Tensing the legs to hold them up - the wall does that job."),
            breathing = "Long, slow breaths. Two to five minutes is ideal.",
            defaultRestSec = 0
        ),
        Exercise(
            id = "y_happy_baby",
            name = "Happy Baby (Ananda Balasana)",
            pattern = Pattern.STRETCH,
            primary = listOf("Hips", "Adductors", "Lower back"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS,
            steps = listOf(
                "Lie on your back and draw both knees towards your armpits.",
                "Hold the outer edges of the feet, or the backs of the thighs.",
                "Stack ankles over knees, shins vertical, and gently pull down.",
                "Keep the tailbone heavy on the floor."
            ),
            cues = listOf("Rock gently side to side to massage the lower back."),
            mistakes = listOf("Lifting the tailbone and hips off the floor."),
            breathing = "Six to eight breaths.",
            defaultRestSec = 0
        ),
        Exercise(
            id = "y_boat",
            name = "Boat Pose (Navasana)",
            pattern = Pattern.CORE,
            primary = listOf("Core", "Hip flexors"),
            equipment = Equipment.NONE,
            level = Level.INTERMEDIATE,
            measure = Measure.SECONDS,
            steps = listOf(
                "Sit with knees bent, feet on the floor, hands behind the thighs.",
                "Lift the chest and lean back until you balance just behind the sit bones.",
                "Lift the shins parallel to the floor.",
                "Straighten the legs only if the back can stay long."
            ),
            cues = listOf("A long spine with bent knees beats straight legs with a rounded back."),
            mistakes = listOf("Collapsing the chest and rounding the lower back."),
            breathing = "Five breaths, then rest. Repeat if you have it in you.",
            defaultRestSec = 30
        ),
        Exercise(
            id = "y_sphinx",
            name = "Sphinx Pose",
            pattern = Pattern.MOBILITY,
            primary = listOf("Lower back", "Chest"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS,
            steps = listOf(
                "Lie face down and prop yourself on the forearms, elbows under the shoulders.",
                "Press the forearms down and draw the chest forward and up.",
                "Relax the glutes and legs completely.",
                "Let the lower back curve gently."
            ),
            cues = listOf("A far gentler backbend than cobra, and a good place to start."),
            mistakes = listOf("Shrugging the shoulders towards the ears."),
            breathing = "Eight slow breaths.",
            defaultRestSec = 0
        ),
        Exercise(
            id = "y_savasana",
            name = "Corpse Pose (Savasana)",
            pattern = Pattern.STRETCH,
            primary = listOf("Recovery"),
            equipment = Equipment.NONE,
            measure = Measure.SECONDS,
            steps = listOf(
                "Lie flat on your back, legs a little wider than the hips, feet flopping outward.",
                "Arms slightly away from the body, palms facing up.",
                "Close the eyes and let the whole body be heavy.",
                "Do nothing at all."
            ),
            cues = listOf(
                "This is not optional filler. It is where the nervous system takes the session in.",
                "If the mind races, count the breath backwards from twenty."
            ),
            mistakes = listOf("Skipping it."),
            breathing = "Natural and unforced. Two to five minutes.",
            defaultRestSec = 0
        )
    )

    private val byId = all.associateBy { it.id }

    operator fun get(id: String): Exercise? = byId[id]
}

object YogaFlows {

    val sunSalutation = PostureRoutine(
        id = "yoga_sun_a",
        name = "Sun Salutation A",
        subtitle = "The classic warm-up flow",
        targets = "Whole body, breath and spine",
        whenToDo = "As a warm-up for anything, or on its own to start the day.",
        category = RoutineCategory.YOGA,
        steps = listOf(
            RoutineStep("y_mountain", seconds = 30, note = "Settle and find the breath"),
            RoutineStep("y_chair", seconds = 25),
            RoutineStep("y_seated_fold", seconds = 30, note = "Standing version is fine too"),
            RoutineStep("y_downdog", seconds = 45),
            RoutineStep("plank", seconds = 25),
            RoutineStep("y_cobra", seconds = 30),
            RoutineStep("y_downdog", seconds = 45),
            RoutineStep("y_mountain", seconds = 30, note = "Notice what changed")
        )
    )

    val morningFlow = PostureRoutine(
        id = "yoga_morning",
        name = "Morning Yoga Flow",
        subtitle = "Fifteen minutes to open everything up",
        targets = "Spine, hips, shoulders, hamstrings",
        whenToDo = "First thing, before you sit down anywhere.",
        category = RoutineCategory.YOGA,
        steps = listOf(
            RoutineStep("cat_cow", seconds = 60, reps = 10),
            RoutineStep("y_downdog", seconds = 45),
            RoutineStep("y_low_lunge", seconds = 40, perSide = true),
            RoutineStep("y_warrior1", seconds = 35, perSide = true),
            RoutineStep("y_warrior2", seconds = 35, perSide = true),
            RoutineStep("y_triangle", seconds = 35, perSide = true),
            RoutineStep("y_tree", seconds = 30, perSide = true),
            RoutineStep("y_cobra", seconds = 35),
            RoutineStep("y_seated_fold", seconds = 45),
            RoutineStep("y_supine_twist", seconds = 40, perSide = true),
            RoutineStep("y_savasana", seconds = 90)
        )
    )

    val eveningWindDown = PostureRoutine(
        id = "yoga_evening",
        name = "Evening Wind-Down",
        subtitle = "Slow, floor-based, no standing",
        targets = "Hips, lower back, nervous system",
        whenToDo = "Last thing at night, lights low. Helps you sleep.",
        category = RoutineCategory.YOGA,
        steps = listOf(
            RoutineStep("childs_pose", seconds = 60),
            RoutineStep("cat_cow", seconds = 45, reps = 8),
            RoutineStep("y_sphinx", seconds = 50),
            RoutineStep("y_pigeon", seconds = 60, perSide = true, note = "Cushion under the hip"),
            RoutineStep("y_seated_fold", seconds = 60),
            RoutineStep("y_happy_baby", seconds = 45),
            RoutineStep("y_supine_twist", seconds = 50, perSide = true),
            RoutineStep("y_legs_up_wall", seconds = 120),
            RoutineStep("y_savasana", seconds = 120)
        )
    )

    val forRunners = PostureRoutine(
        id = "yoga_runners",
        name = "Yoga for Runners",
        subtitle = "Everything running tightens",
        targets = "Hip flexors, hamstrings, calves, glutes",
        whenToDo = "After a run, or on the evening of a run day.",
        category = RoutineCategory.YOGA,
        steps = listOf(
            RoutineStep("y_downdog", seconds = 60, note = "Pedal the heels to work the calves"),
            RoutineStep("y_low_lunge", seconds = 50, perSide = true),
            RoutineStep("y_warrior1", seconds = 35, perSide = true),
            RoutineStep("y_pigeon", seconds = 60, perSide = true),
            RoutineStep("hamstring_stretch", seconds = 45, perSide = true),
            RoutineStep("y_bridge", seconds = 40),
            RoutineStep("y_supine_twist", seconds = 45, perSide = true),
            RoutineStep("y_legs_up_wall", seconds = 120, note = "The best recovery pose there is"),
            RoutineStep("y_savasana", seconds = 90)
        )
    )

    val backAndPosture = PostureRoutine(
        id = "yoga_back",
        name = "Yoga for Back and Posture",
        subtitle = "Undo a day at a desk",
        targets = "Thoracic spine, chest, hip flexors, deep core",
        whenToDo = "Any day you have been sitting a lot.",
        category = RoutineCategory.YOGA,
        steps = listOf(
            RoutineStep("cat_cow", seconds = 60, reps = 10),
            RoutineStep("thread_the_needle", seconds = 40, perSide = true),
            RoutineStep("y_sphinx", seconds = 45),
            RoutineStep("y_cobra", seconds = 35),
            RoutineStep("childs_pose", seconds = 45),
            RoutineStep("y_low_lunge", seconds = 45, perSide = true),
            RoutineStep("y_bridge", seconds = 40),
            RoutineStep("bird_dog", seconds = 60, reps = 8),
            RoutineStep("y_supine_twist", seconds = 45, perSide = true),
            RoutineStep("y_savasana", seconds = 90)
        )
    )

    val all = listOf(sunSalutation, morningFlow, eveningWindDown, forRunners, backAndPosture)

    operator fun get(id: String): PostureRoutine? = all.firstOrNull { it.id == id }
}

/** Every guided routine in the app, whichever library it came from. */
object AllRoutines {
    val all: List<PostureRoutine> get() = PostureRoutines.all + YogaFlows.all

    operator fun get(id: String): PostureRoutine? = all.firstOrNull { it.id == id }
}
