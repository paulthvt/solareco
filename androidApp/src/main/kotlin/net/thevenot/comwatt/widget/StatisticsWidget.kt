package net.thevenot.comwatt.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.thevenot.comwatt.R
import net.thevenot.comwatt.di.Factory
import net.thevenot.comwatt.di.dataRepository
import net.thevenot.comwatt.domain.FetchWidgetStatisticsUseCase
import java.io.File
import java.util.concurrent.TimeUnit

private const val STATISTICS_WIDGET_UPDATE_WORK_NAME = "statistics_widget_update_work"
private const val STATISTICS_WIDGET_REFRESH_WORK_NAME = "statistics_widget_refresh_work"
private const val WORKER_KEY_SHOW_ERROR_TOAST = "show_error_toast"
private const val DONUT_SELF_CONSUMPTION_FILE = "widget_donut_self_consumption.png"
private const val DONUT_AUTONOMY_FILE = "widget_donut_autonomy.png"
private const val DONUT_SIZE_PX = 200
private const val REFRESH_NOTIFICATION_CHANNEL_ID = "widget_refresh"
private const val REFRESH_NOTIFICATION_ID = 1002

private fun isSystemInDarkMode(context: Context) =
    context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

class StatisticsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            StatisticsWidgetContent()
        }
    }

    companion object {
        private val logger = Logger.withTag("StatisticsWidget")

        suspend fun updateWidgetData(context: Context, showErrorToast: Boolean = false) {
            try {
                val factory = Factory(context)
                val settings = factory.dataRepository.getSettings().first()
                val siteId = settings.siteId ?: return

                val fetchUseCase = FetchWidgetStatisticsUseCase(factory.dataRepository)
                when (val result = fetchUseCase.execute(siteId)) {
                    is Either.Left -> {
                        logger.e { "Failed to fetch statistics widget data: ${result.value}" }
                        if (showErrorToast) {
                            showToast(context, R.string.widget_refresh_failed_no_network)
                        }
                    }

                    is Either.Right -> {
                        val widgetDataJson = Json.encodeToString(result.value)
                        generateAndSaveDonutImages(context, result.value)
                        updateAllWidgetInstances(context, widgetDataJson)
                    }
                }
            } catch (e: Exception) {
                logger.e(e) { "Exception while updating statistics widget data" }
                if (showErrorToast) {
                    showToast(context, R.string.widget_refresh_failed)
                }
            }
        }

        private fun showToast(context: Context, messageResId: Int) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, messageResId, Toast.LENGTH_SHORT).show()
            }
        }

        private suspend fun generateAndSaveDonutImages(
            context: Context,
            data: WidgetStatisticsData,
        ) {
            try {
                val generator = createStatisticsImageGenerator()
                val isDark = isSystemInDarkMode(context)

                generator.generateDonutImage(
                    DonutImageParams(
                        percentage = data.selfConsumptionRate?.toFloat(),
                        label = "Self-cons.",
                        sizePx = DONUT_SIZE_PX,
                        isDarkMode = isDark,
                        isSecondary = false,
                    ),
                )?.let { bytes ->
                    File(context.filesDir, DONUT_SELF_CONSUMPTION_FILE).writeBytes(bytes)
                }

                generator.generateDonutImage(
                    DonutImageParams(
                        percentage = data.autonomyRate.toFloat(),
                        label = "Autonomy",
                        sizePx = DONUT_SIZE_PX,
                        isDarkMode = isDark,
                        isSecondary = true,
                    ),
                )?.let { bytes ->
                    File(context.filesDir, DONUT_AUTONOMY_FILE).writeBytes(bytes)
                }
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate donut images" }
            }
        }

        private suspend fun updateAllWidgetInstances(context: Context, widgetDataJson: String) {
            val glanceIds = GlanceAppWidgetManager(context)
                .getGlanceIds(StatisticsWidget::class.java)

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[STATISTICS_WIDGET_DATA_KEY] = widgetDataJson
                    }
                }
                StatisticsWidget().update(context, glanceId)
            }
        }

        fun scheduleWidgetUpdates(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<StatisticsWidgetUpdateWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                STATISTICS_WIDGET_UPDATE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest,
            )
        }

        fun requestImmediateRefresh(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<StatisticsWidgetRefreshWorker>()
                .setInputData(workDataOf(WORKER_KEY_SHOW_ERROR_TOAST to true))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                STATISTICS_WIDGET_REFRESH_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                workRequest,
            )
        }

        fun cancelWidgetUpdates(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(STATISTICS_WIDGET_UPDATE_WORK_NAME)
        }
    }
}

class StatisticsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StatisticsWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        StatisticsWidget.scheduleWidgetUpdates(context)
        CoroutineScope(Dispatchers.IO).launch {
            StatisticsWidget.updateWidgetData(context)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        StatisticsWidget.cancelWidgetUpdates(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        CoroutineScope(Dispatchers.IO).launch {
            StatisticsWidget.updateWidgetData(context)
        }
    }
}

class StatisticsWidgetUpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            StatisticsWidget.updateWidgetData(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Logger.withTag("StatisticsWidgetUpdateWorker").e(e) { "Widget update failed" }
            Result.retry()
        }
    }
}

class StatisticsWidgetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        createNotificationChannel()
        val notification =
            NotificationCompat.Builder(applicationContext, REFRESH_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bolt)
                .setContentTitle(applicationContext.getString(R.string.widget_refresh_notification))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(
                REFRESH_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(REFRESH_NOTIFICATION_ID, notification)
        }
    }

    override suspend fun doWork(): Result {
        setForeground(getForegroundInfo())
        val showErrorToast = inputData.getBoolean(WORKER_KEY_SHOW_ERROR_TOAST, false)
        return try {
            StatisticsWidget.updateWidgetData(applicationContext, showErrorToast = showErrorToast)
            Result.success()
        } catch (e: Exception) {
            Logger.withTag("StatisticsWidgetRefreshWorker").e(e) { "Widget refresh failed" }
            Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REFRESH_NOTIFICATION_CHANNEL_ID,
                applicationContext.getString(R.string.widget_refresh_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            )
            val notificationManager =
                applicationContext.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}

class RefreshStatisticsWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        StatisticsWidget.requestImmediateRefresh(context)
    }
}
