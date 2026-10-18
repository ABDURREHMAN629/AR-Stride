package com.stride.fit.sensors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

/**
 * Receives Google Play services activity updates and hands them to the gate as a veto.
 *
 * This is the second opinion that catches the awkward case the accelerometer alone
 * can struggle with: a motorbike on a broken road, where the jolts are strong and
 * rhythmic enough to resemble footfalls.
 */
class ActivityGateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityRecognitionResult.hasResult(intent)) return
        val result = ActivityRecognitionResult.extractResult(intent) ?: return
        val most = result.mostProbableActivity

        StepService.onRecognisedActivity(map(most.type), most.confidence)
    }

    private fun map(type: Int): RecognisedActivity = when (type) {
        DetectedActivity.IN_VEHICLE -> RecognisedActivity.IN_VEHICLE
        DetectedActivity.ON_BICYCLE -> RecognisedActivity.ON_BICYCLE
        DetectedActivity.ON_FOOT -> RecognisedActivity.ON_FOOT
        DetectedActivity.WALKING -> RecognisedActivity.WALKING
        DetectedActivity.RUNNING -> RecognisedActivity.RUNNING
        DetectedActivity.STILL -> RecognisedActivity.STILL
        DetectedActivity.TILTING -> RecognisedActivity.TILTING
        else -> RecognisedActivity.UNKNOWN
    }
}
