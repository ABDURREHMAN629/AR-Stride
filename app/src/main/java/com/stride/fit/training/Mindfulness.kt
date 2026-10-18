package com.stride.fit.training

import com.stride.fit.Brand

/** One side of a breathing pattern. */
data class BreathPhase(
    val label: String,
    val seconds: Int,
    val instruction: String,
    /** Target size of the pacer circle at the end of this phase, 0..1. */
    val targetScale: Float
)

data class BreathPattern(
    val id: String,
    val name: String,
    val subtitle: String,
    val phases: List<BreathPhase>,
    val defaultRounds: Int,
    val benefit: String,
    val posture: List<String>,
    val notes: List<String> = emptyList()
) {
    val cycleSeconds: Int get() = phases.sumOf { it.seconds }
    fun totalSeconds(rounds: Int): Int = cycleSeconds * rounds
    fun totalMinutes(rounds: Int): Int = (totalSeconds(rounds) + 59) / 60
}

/**
 * Paced breathing. A long, controlled exhale raises vagal tone, which slows the heart
 * and pulls you out of a sympathetic stress response, so every pattern below makes the
 * out-breath at least as long as the in-breath.
 */
object BreathingPatterns {

    /** Box breathing: four equal sides, the pattern used under real pressure. */
    val box = BreathPattern(
        id = "box",
        name = "Box Breathing",
        subtitle = "4 in, 4 hold, 4 out, 4 hold",
        defaultRounds = 8,
        benefit = "Steadies you when your mind is racing. Four equal sides means nothing " +
            "to think about except the count.",
        posture = listOf(
            "Sit upright with both feet flat on the floor.",
            "Stack your ribs over your hips - no slouching, no military arch.",
            "Roll the shoulders back and down, then let the arms rest heavy.",
            "Chin level and slightly tucked, as if holding an egg under it.",
            "Let the belly be soft. A tight stomach cannot breathe into the diaphragm."
        ),
        notes = listOf(
            "Breathe through the nose the whole way round.",
            "The hold is a pause, not a clench. Keep the throat open and the face soft.",
            "If four seconds feels like a strain, drop to three. The evenness matters " +
                "far more than the number.",
            "Stop and breathe normally if you feel light-headed."
        ),
        phases = listOf(
            BreathPhase("Breathe in", 4, "Through the nose, into the belly", 1.0f),
            BreathPhase("Hold", 4, "Stay full, throat open, face soft", 1.0f),
            BreathPhase("Breathe out", 4, "Slow and controlled through the nose", 0.35f),
            BreathPhase("Hold", 4, "Stay empty, shoulders relaxed", 0.35f)
        )
    )

    /** The long exhale pattern. Strongest downshift of the four; good before sleep. */
    val fourSevenEight = BreathPattern(
        id = "478",
        name = "4-7-8 Breathing",
        subtitle = "4 in, 7 hold, 8 out",
        defaultRounds = 6,
        benefit = "The longest exhale of any pattern here, which makes it the strongest " +
            "downshift. Use it in bed, or after something stressful.",
        posture = listOf(
            "Sit or lie down - this one works lying flat.",
            "Rest the tip of the tongue behind the top front teeth.",
            "Let the shoulders drop completely."
        ),
        notes = listOf(
            "Exhale through the mouth with a soft whoosh.",
            "Six rounds is plenty. This one is potent and more is not better."
        ),
        phases = listOf(
            BreathPhase("Breathe in", 4, "Quietly through the nose", 1.0f),
            BreathPhase("Hold", 7, "Comfortably full, no straining", 1.0f),
            BreathPhase("Breathe out", 8, "Slow whoosh through the mouth", 0.3f)
        )
    )

    /** Equal in and out at ~5.5 s, the rate that maximises heart rate variability. */
    val coherent = BreathPattern(
        id = "coherent",
        name = "Coherent Breathing",
        subtitle = "6 in, 6 out, no holds",
        defaultRounds = 10,
        benefit = "Roughly five breaths a minute, the rate at which heart rate variability " +
            "peaks. The easiest pattern to hold for ten minutes.",
        posture = listOf(
            "Sit tall, feet flat, hands resting on the thighs.",
            "Breathe low - the belly should move before the chest does."
        ),
        notes = listOf(
            "No holds at all. Just a smooth turnaround at each end.",
            "This is the one to use if the holds in box breathing feel like work."
        ),
        phases = listOf(
            BreathPhase("Breathe in", 6, "Smooth and low into the belly", 1.0f),
            BreathPhase("Breathe out", 6, "Same speed, no rush at the end", 0.3f)
        )
    )

    /** Two quick inhales then a long sigh: the fastest acute stress reset there is. */
    val physiologicalSigh = BreathPattern(
        id = "sigh",
        name = "Physiological Sigh",
        subtitle = "Double inhale, long exhale",
        defaultRounds = 5,
        benefit = "The fastest way down from a spike of stress. Two inhales reinflate " +
            "collapsed air sacs, and the long exhale dumps carbon dioxide quickly.",
        posture = listOf(
            "Anywhere, any position. This one is for the moment you need it.",
            "Sit or stand tall enough that the ribs can move."
        ),
        notes = listOf(
            "The second inhale is a short sip on top of the first, not a full breath.",
            "Three to five rounds is usually enough to feel the shift."
        ),
        phases = listOf(
            BreathPhase("Breathe in", 4, "Through the nose, most of the way full", 0.85f),
            BreathPhase("Sip more in", 2, "A short extra sip on top", 1.0f),
            BreathPhase("Long sigh out", 8, "Through the mouth, let it all go", 0.25f)
        )
    )

    val all = listOf(box, coherent, fourSevenEight, physiologicalSigh)

    operator fun get(id: String): BreathPattern? = all.firstOrNull { it.id == id }
}

/** One sense in the 5-4-3-2-1 sequence. */
data class GroundingStep(
    val count: Int,
    val sense: String,
    val prompt: String,
    val seconds: Int,
    val guidance: List<String>,
    val eyesClosed: Boolean = false
)

/**
 * The 5-4-3-2-1 grounding exercise. It works by giving attention a job: naming what is
 * physically around you right now forces attention into the present, where rumination
 * cannot live. Two minutes per sense rather than the usual quick run-through - the rush
 * is what makes most people report it "not working".
 */
object Grounding {

    const val ID = "grounding_54321"
    const val NAME = "5-4-3-2-1 Grounding"

    val steps: List<GroundingStep> = listOf(
        GroundingStep(
            count = 5,
            sense = "See",
            prompt = "Five things you can see",
            seconds = 120,
            guidance = listOf(
                "Look slowly around from where you are sitting.",
                "Name five separate things, silently or out loud.",
                "Do not just label them - describe them. Not 'a cup', but 'a white cup with " +
                    "a chip on the handle and light catching the rim'.",
                "Include one thing you have never actually noticed before.",
                "Take the full two minutes. Rushing is what stops this working."
            )
        ),
        GroundingStep(
            count = 4,
            sense = "Touch",
            prompt = "Four things you can touch from where you sit",
            seconds = 120,
            guidance = listOf(
                "Without getting up, reach for four different textures.",
                "Your clothes, the chair, the floor under your feet, the phone in your hand.",
                "Press into each one. Is it warm or cool? Rough or smooth? Hard or giving?",
                "Notice the weight of your body pressing down into the seat.",
                "Feel where your feet meet the ground."
            )
        ),
        GroundingStep(
            count = 3,
            sense = "Hear",
            prompt = "Three sounds, with your eyes closed",
            seconds = 120,
            eyesClosed = true,
            guidance = listOf(
                "Close your eyes. Closing them makes hearing sharper almost immediately.",
                "Find three distinct sounds and hold each one for a while.",
                "Start with the most obvious, then listen underneath it for a quieter one.",
                "The third is usually the hardest - a fan, distant traffic, your own breathing.",
                "Do not judge the sounds as good or annoying. Just note that they are there."
            )
        ),
        GroundingStep(
            count = 2,
            sense = "Feel",
            prompt = "Two subtle sensations you can tell apart",
            seconds = 120,
            eyesClosed = true,
            guidance = listOf(
                "Eyes still closed. Now go inward and find two faint sensations.",
                "Air moving across your skin. The temperature difference between your " +
                    "hands and your face.",
                "Your heartbeat, if you sit still enough to find it.",
                "A tingle in the fingertips, or the pull of clothing on one shoulder.",
                "The classic version of this exercise uses two smells instead - use whichever " +
                    "you can actually find right now."
            )
        ),
        GroundingStep(
            count = 1,
            sense = "Taste",
            prompt = "One thing you can taste",
            seconds = 120,
            guidance = listOf(
                "Open your eyes.",
                "Notice whatever is already in your mouth - tea, toothpaste, the last thing " +
                    "you ate, or simply the neutral taste of your own mouth.",
                "There is always something, even if it takes a moment to find.",
                "Run your tongue slowly across your teeth and the roof of your mouth.",
                "Sit with that one taste until the timer ends, then notice how you feel " +
                    "compared to ten minutes ago."
            )
        )
    )

    val totalSeconds: Int get() = steps.sumOf { it.seconds }
    val totalMinutes: Int get() = (totalSeconds + 59) / 60
}

/** Short attention drills. Presence is trained, not summoned. */
data class PresenceExercise(
    val id: String,
    val name: String,
    val subtitle: String,
    val minutes: Int,
    val whenToUse: String,
    val steps: List<String>,
    val why: String
)

object PresenceExercises {

    val all: List<PresenceExercise> = listOf(
        PresenceExercise(
            id = "breath_count",
            name = "Breath Counting",
            subtitle = "Count to ten, start again",
            minutes = 5,
            whenToUse = "When your head will not stop jumping between things.",
            steps = listOf(
                "Sit upright and breathe normally - do not control the breath at all.",
                "Count each exhale: one, two, three, up to ten.",
                "At ten, start again at one.",
                "The moment you notice you have lost count, or gone past ten on autopilot, " +
                    "go back to one.",
                "Losing count is not failure. Noticing that you lost count is the entire rep."
            ),
            why = "Every time you catch your mind wandering and bring it back, you have done " +
                "one repetition of attention. That noticing is the muscle, not the counting."
        ),
        PresenceExercise(
            id = "body_scan",
            name = "Body Scan",
            subtitle = "Head to feet, no fixing",
            minutes = 8,
            whenToUse = "Before sleep, or when you are wound up and cannot say why.",
            steps = listOf(
                "Lie on your back or sit with your head supported.",
                "Start at the crown of your head. Notice whatever is there - or nothing.",
                "Move down slowly: face, jaw, neck, shoulders, arms, hands.",
                "Then chest, belly, lower back, hips, thighs, knees, calves, feet.",
                "Spend about twenty seconds on each area.",
                "Do not try to relax anything. Just notice it. Tension often lets go on its " +
                    "own once it has been seen."
            ),
            why = "Most stress is held somewhere physical that you have stopped noticing. " +
                "The jaw and the shoulders are the usual suspects."
        ),
        PresenceExercise(
            id = "stop",
            name = "The STOP Practice",
            subtitle = "Sixty seconds, anywhere",
            minutes = 1,
            whenToUse = "Mid-argument, before a hard message, or any time you are about to react.",
            steps = listOf(
                "S - Stop. Whatever you are doing, pause it.",
                "T - Take a breath. One slow one, with a longer exhale than inhale.",
                "O - Observe. What is happening in your body? What are you feeling? " +
                    "What is the thought driving it?",
                "P - Proceed. Choose what to do next, rather than continuing on momentum."
            ),
            why = "It puts a gap between the trigger and the response. That gap is where every " +
                "choice you actually get to make lives."
        ),
        PresenceExercise(
            id = "single_task",
            name = "Single-Task Focus",
            subtitle = "One thing, fully",
            minutes = 10,
            whenToUse = "When you have been switching between tasks all day and finished none.",
            steps = listOf(
                "Pick one task. Put the phone in another room, face down.",
                "Close every tab and window that is not that task.",
                "Work on only that for ten minutes. No checking anything.",
                "When the urge to switch arrives - and it will, usually within two minutes - " +
                    "notice it, name it as an urge, and stay.",
                "Build up to twenty-five minutes over a couple of weeks."
            ),
            why = "Task switching has a real cost every time. What feels like multitasking is " +
                "just doing several things worse and more slowly."
        ),
        PresenceExercise(
            id = "mindful_walk",
            name = "Mindful Walking",
            subtitle = "Steps that count twice",
            minutes = 10,
            whenToUse = "On any walk. Turns a step count into attention training.",
            steps = listOf(
                "Walk at a normal pace, no phone, no headphones.",
                "Put your attention on the soles of your feet.",
                "Feel each part of the step: heel down, weight rolling forward, toes pushing off.",
                "When your mind wanders, bring it back to your feet. It will wander constantly.",
                "Every few minutes, widen out: sounds, air on your skin, what you can see."
            ),
            why = "${Brand.NAME} is already counting these steps. This makes the same ten minutes do " +
                "two jobs at once."
        )
    )

    operator fun get(id: String): PresenceExercise? = all.firstOrNull { it.id == id }
}
