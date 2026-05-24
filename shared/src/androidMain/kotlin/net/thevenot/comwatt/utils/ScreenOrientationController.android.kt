package net.thevenot.comwatt.utils

import android.app.Activity
import android.content.pm.ActivityInfo
import net.thevenot.comwatt.utils.ScreenOrientationController.lockLandscape
import java.lang.ref.WeakReference

/**
 * Android implementation of ScreenOrientationController.
 * Uses the Activity to control screen orientation.
 *
 * Note (Android 16 / API 36): On large-screen devices (smallest width >= 600dp),
 * calls to [Activity.setRequestedOrientation] are ignored by the system. This means
 * [lockLandscape] will have no effect on tablets, foldables, and desktop windows.
 * Apps targeting SDK 36 should support adaptive layouts for all screen sizes.
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