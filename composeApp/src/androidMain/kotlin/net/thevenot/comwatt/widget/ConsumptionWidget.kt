package net.thevenot.comwatt.widget

import android.content.Context
import android.content.res.Configuration
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.thevenot.comwatt.di.Factory
import net.thevenot.comwatt.di.dataRepository
import net.thevenot.comwatt.di.dataStore
import net.thevenot.comwatt.domain.FetchWidgetConsumptionUseCase
import java.util.concurrent.TimeUnit

/**
 * Check if system is in dark mode
 */
private fun isSystemInDarkMode(context: Context): Boolean {
    val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
}

/**
 * Android Widget using Glance
 *
 * Note: This widget operates independently of the main app's UI and ViewModel.
 * It uses WorkManager for periodic updates (every 15 minutes) and can be
 * manually updated via WidgetViewModel in the main app.
 */
class ConsumptionWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            ConsumptionWidgetContent()
        }
    }

    companion object {
        private val logger = Logger.withTag("ConsumptionWidget")

        /**
         * Update widget data and refresh UI
         */
        suspend fun updateWidgetData(context: Context) {
            logger.d { "Updating widget data" }

            try {
                val factory = Factory(context)
                val widgetDataRepository = WidgetDataRepository(factory.dataStore)
                val settings = factory.dataRepository.getSettings().first()

                val siteId = settings.siteId
                if (siteId == null) {
                    logger.w { "No site ID configured, skipping widget update" }
                    return
                }

                // Fetch fresh data
                val fetchUseCase = FetchWidgetConsumptionUseCase(factory.dataRepository)
                when (val result = fetchUseCase.execute(siteId)) {
                    is Either.Left -> {
                        logger.e { "Failed to fetch widget data: ${result.value}" }
                    }

                    is Either.Right -> {
                        // Save data for widget
                        widgetDataRepository.saveWidgetData(result.value)
                        logger.d { "Widget data saved successfully" }

                        // Generate and save chart image
                        try {
                            val chartGenerator = createChartImageGenerator()
                            val imageBytes = chartGenerator.generateChartImage(
                                data = result.value,
                                widthPx = 640,
                                heightPx = 240,
                                isDarkMode = isSystemInDarkMode(context)
                            )

                            imageBytes?.let { bytes ->
                                val chartFile = java.io.File(context.filesDir, "widget_chart.png")
                                chartFile.writeBytes(bytes)
                                logger.d { "Chart image saved: ${bytes.size} bytes" }
                            }
                        } catch (e: Exception) {
                            logger.e(e) { "Failed to generate/save chart image" }
                        }

                        // Update all widget instances
                        val glanceIds = GlanceAppWidgetManager(context)
                            .getGlanceIds(ConsumptionWidget::class.java)

                        glanceIds.forEach { glanceId ->
                            updateAppWidgetState(
                                context,
                                PreferencesGlanceStateDefinition,
                                glanceId
                            ) { prefs ->
                                prefs.toMutablePreferences().apply {
                                    // Trigger UI update by changing state
                                }
                            }
                            ConsumptionWidget().update(context, glanceId)
                        }

                        logger.d { "Updated ${glanceIds.size} widget instance(s)" }
                    }
                }
            } catch (e: Exception) {
                logger.e(e) { "Exception while updating widget data" }
            }
        }

        /**
         * Schedule periodic widget updates
         * Note: Android system may delay updates to optimize battery.
         * Use the refresh button in the widget for immediate updates.
         */
        fun scheduleWidgetUpdates(context: Context) {
            logger.d { "Scheduling widget updates" }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES) // Start updating after 1 minute
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "widget_update_work",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )

            logger.d { "Widget updates scheduled with 15-minute interval" }
        }

        /**
         * Cancel scheduled widget updates
         */
        fun cancelWidgetUpdates(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork("widget_update_work")
            logger.d { "Widget updates cancelled" }
        }
    }
}

/**
 * Widget receiver to handle widget lifecycle events
 */
class ConsumptionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ConsumptionWidget()

    private val logger = Logger.withTag("ConsumptionWidgetReceiver")

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        logger.d { "Widget enabled, scheduling updates and triggering immediate refresh" }
        ConsumptionWidget.scheduleWidgetUpdates(context)
        // Trigger immediate update
        CoroutineScope(Dispatchers.IO).launch {
            ConsumptionWidget.updateWidgetData(context)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        logger.d { "Widget disabled, cancelling updates" }
        ConsumptionWidget.cancelWidgetUpdates(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        logger.d { "Widget update requested (configuration change or system update)" }
        // Trigger update on configuration changes (like theme changes)
        CoroutineScope(Dispatchers.IO).launch {
            ConsumptionWidget.updateWidgetData(context)
        }
    }
}

/**
 * Worker to periodically update widget data
 */
class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val logger = Logger.withTag("WidgetUpdateWorker")

    override suspend fun doWork(): Result {
        logger.d { "Widget update worker started" }

        return try {
            ConsumptionWidget.updateWidgetData(applicationContext)
            logger.d { "Widget update worker completed successfully" }
            Result.success()
        } catch (e: Exception) {
            logger.e(e) { "Widget update worker failed" }
            Result.retry()
        }
    }
}
