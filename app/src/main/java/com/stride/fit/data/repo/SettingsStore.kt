package com.stride.fit.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stride.fit.health.HealthMath
import com.stride.fit.sensors.Sensitivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "stride_settings")

/**
 * The user profile and preferences.
 *
 * Seeded with the numbers this build was made for - male, 82 kg, 162.6 cm - but every
 * field is editable in the Profile screen, and the maths reads from here, never from
 * hard-coded constants.
 */
data class Profile(
    val name: String = "",
    val male: Boolean = HealthMath.DEFAULT_SEX_MALE,
    val age: Int = HealthMath.DEFAULT_AGE,
    val heightCm: Double = HealthMath.DEFAULT_HEIGHT_CM,
    val weightKg: Double = HealthMath.DEFAULT_WEIGHT_KG,
    val targetWeightKg: Double = 70.0,
    val restingHr: Int = 70,
    val stepGoal: Int = 8000,
    val calorieBurnGoal: Int = 550,
    val strengthDaysGoal: Int = 3,
    val postureMinutesGoal: Int = 10,
    val sensitivity: Sensitivity = Sensitivity.BALANCED,
    val useMetric: Boolean = true,
    val autoPauseRuns: Boolean = true,
    val voiceCuesEnabled: Boolean = false,
    val programId: String = "foundation",
    val planStartDay: Long = 0L,
    val onboarded: Boolean = false,
    val lastGoalReviewDay: Long = 0L
) {
    val bmi: Double get() = HealthMath.bmi(weightKg, heightCm)
    val bmr: Double get() = HealthMath.bmr(weightKg, heightCm, age, male)
    val healthyRange get() = HealthMath.healthyWeightRangeKg(heightCm)
    val maxHr: Int get() = HealthMath.maxHr(age)
}

class SettingsStore(private val context: Context) {

    private object Keys {
        val NAME = stringPreferencesKey("name")
        val MALE = booleanPreferencesKey("male")
        val AGE = intPreferencesKey("age")
        val HEIGHT = doublePreferencesKey("height_cm")
        val WEIGHT = doublePreferencesKey("weight_kg")
        val TARGET_WEIGHT = doublePreferencesKey("target_weight_kg")
        val RESTING_HR = intPreferencesKey("resting_hr")
        val STEP_GOAL = intPreferencesKey("step_goal")
        val CAL_GOAL = intPreferencesKey("calorie_goal")
        val STRENGTH_DAYS = intPreferencesKey("strength_days")
        val POSTURE_MIN = intPreferencesKey("posture_minutes")
        val SENSITIVITY = stringPreferencesKey("sensitivity")
        val METRIC = booleanPreferencesKey("metric")
        val AUTO_PAUSE = booleanPreferencesKey("auto_pause")
        val VOICE = booleanPreferencesKey("voice_cues")
        val PROGRAM = stringPreferencesKey("program_id")
        val PLAN_START = longPreferencesKey("plan_start_day")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val GOAL_REVIEW = longPreferencesKey("goal_review_day")
    }

    val profile: Flow<Profile> = context.dataStore.data.map { p ->
        val defaults = Profile()
        Profile(
            name = p[Keys.NAME] ?: defaults.name,
            male = p[Keys.MALE] ?: defaults.male,
            age = p[Keys.AGE] ?: defaults.age,
            heightCm = p[Keys.HEIGHT] ?: defaults.heightCm,
            weightKg = p[Keys.WEIGHT] ?: defaults.weightKg,
            targetWeightKg = p[Keys.TARGET_WEIGHT] ?: defaults.targetWeightKg,
            restingHr = p[Keys.RESTING_HR] ?: defaults.restingHr,
            stepGoal = p[Keys.STEP_GOAL] ?: defaults.stepGoal,
            calorieBurnGoal = p[Keys.CAL_GOAL] ?: defaults.calorieBurnGoal,
            strengthDaysGoal = p[Keys.STRENGTH_DAYS] ?: defaults.strengthDaysGoal,
            postureMinutesGoal = p[Keys.POSTURE_MIN] ?: defaults.postureMinutesGoal,
            sensitivity = p[Keys.SENSITIVITY]?.let { runCatching { Sensitivity.valueOf(it) }.getOrNull() }
                ?: defaults.sensitivity,
            useMetric = p[Keys.METRIC] ?: defaults.useMetric,
            autoPauseRuns = p[Keys.AUTO_PAUSE] ?: defaults.autoPauseRuns,
            voiceCuesEnabled = p[Keys.VOICE] ?: defaults.voiceCuesEnabled,
            programId = p[Keys.PROGRAM] ?: defaults.programId,
            planStartDay = p[Keys.PLAN_START] ?: defaults.planStartDay,
            onboarded = p[Keys.ONBOARDED] ?: defaults.onboarded,
            lastGoalReviewDay = p[Keys.GOAL_REVIEW] ?: defaults.lastGoalReviewDay
        )
    }

    suspend fun update(block: (Profile) -> Profile) {
        context.dataStore.edit { prefs ->
            val current = Profile(
                name = prefs[Keys.NAME] ?: "",
                male = prefs[Keys.MALE] ?: HealthMath.DEFAULT_SEX_MALE,
                age = prefs[Keys.AGE] ?: HealthMath.DEFAULT_AGE,
                heightCm = prefs[Keys.HEIGHT] ?: HealthMath.DEFAULT_HEIGHT_CM,
                weightKg = prefs[Keys.WEIGHT] ?: HealthMath.DEFAULT_WEIGHT_KG,
                targetWeightKg = prefs[Keys.TARGET_WEIGHT] ?: 70.0,
                restingHr = prefs[Keys.RESTING_HR] ?: 70,
                stepGoal = prefs[Keys.STEP_GOAL] ?: 8000,
                calorieBurnGoal = prefs[Keys.CAL_GOAL] ?: 550,
                strengthDaysGoal = prefs[Keys.STRENGTH_DAYS] ?: 3,
                postureMinutesGoal = prefs[Keys.POSTURE_MIN] ?: 10,
                sensitivity = prefs[Keys.SENSITIVITY]
                    ?.let { runCatching { Sensitivity.valueOf(it) }.getOrNull() }
                    ?: Sensitivity.BALANCED,
                useMetric = prefs[Keys.METRIC] ?: true,
                autoPauseRuns = prefs[Keys.AUTO_PAUSE] ?: true,
                voiceCuesEnabled = prefs[Keys.VOICE] ?: false,
                programId = prefs[Keys.PROGRAM] ?: "foundation",
                planStartDay = prefs[Keys.PLAN_START] ?: 0L,
                onboarded = prefs[Keys.ONBOARDED] ?: false,
                lastGoalReviewDay = prefs[Keys.GOAL_REVIEW] ?: 0L
            )
            val next = block(current)
            prefs[Keys.NAME] = next.name
            prefs[Keys.MALE] = next.male
            prefs[Keys.AGE] = next.age
            prefs[Keys.HEIGHT] = next.heightCm
            prefs[Keys.WEIGHT] = next.weightKg
            prefs[Keys.TARGET_WEIGHT] = next.targetWeightKg
            prefs[Keys.RESTING_HR] = next.restingHr
            prefs[Keys.STEP_GOAL] = next.stepGoal
            prefs[Keys.CAL_GOAL] = next.calorieBurnGoal
            prefs[Keys.STRENGTH_DAYS] = next.strengthDaysGoal
            prefs[Keys.POSTURE_MIN] = next.postureMinutesGoal
            prefs[Keys.SENSITIVITY] = next.sensitivity.name
            prefs[Keys.METRIC] = next.useMetric
            prefs[Keys.AUTO_PAUSE] = next.autoPauseRuns
            prefs[Keys.VOICE] = next.voiceCuesEnabled
            prefs[Keys.PROGRAM] = next.programId
            prefs[Keys.PLAN_START] = next.planStartDay
            prefs[Keys.ONBOARDED] = next.onboarded
            prefs[Keys.GOAL_REVIEW] = next.lastGoalReviewDay
        }
    }
}
