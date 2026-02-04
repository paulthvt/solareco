package net.thevenot.comwatt.utils

import android.app.Activity
import android.content.pm.ActivityInfo
import java.lang.ref.WeakReference

/**
 * Android implementation of ScreenOrientationController.
 * Uses the Activity to control screen orientation.
 */
actual object ScreenOrientationController {
    private var activityRef: WeakReference<Activity>? = null

    /**
     * Set the activity reference. Should be called from MainActivity.onCreate().
     */
    fun setActivity(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    actual fun lockLandscape() {
        activityRef?.get()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    actual fun unlock() {
        activityRef?.get()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}
