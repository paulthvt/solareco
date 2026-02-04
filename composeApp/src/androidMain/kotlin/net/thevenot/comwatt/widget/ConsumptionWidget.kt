package net.thevenot.comwatt.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.widget.Toast
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
import kotlinx.serialization.json.Json
import net.thevenot.comwatt.R
import net.thevenot.comwatt.di.Factory
import net.thevenot.comwatt.di.dataRepository
import net.thevenot.comwatt.di.dataStore
import net.thevenot.comwatt.domain.FetchWidgetConsumptionUseCase
import java.io.File
import java.util.concurrent.TimeUnit

private const val WIDGET_UPDATE_WORK_NAME = "widget_update_work"
private const val CHART_FILE_NAME = "widget_chart.png"
private const val CHART_WIDTH_PX = 640
private const val CHART_HEIGHT_PX = 240

private fun isSystemInDarkMode(context: Context) =
    context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

class ConsumptionWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            ConsumptionWidgetContent()
        }
    }

    companion object {
        private val logger = Logger.withTag("ConsumptionWidget")

        suspend fun updateWidgetData(context: Context, showErrorToast: Boolean = false) {
            try {
                val factory = Factory(context)
                val settings = factory.dataRepository.getSettings().first()
                val siteId = settings.siteId ?: return

                val fetchUseCase = FetchWidgetConsumptionUseCase(factory.dataRepository)
                when (val result = fetchUseCase.execute(siteId)) {
                    is Either.Left -> {
                        logger.e { "Failed to fetch widget data: ${result.value}" }
                        if (showErrorToast) {
                            showToast(context, R.string.widget_refresh_failed_no_network)
                        }
                    }
                    is Either.Right -> {
                        WidgetDataRepository(factory.dataStore).saveWidgetData(result.value)
                        val widgetDataJson = Json.encodeToString(result.value)
                        generateAndSaveChartImage(context, result.value)
                        updateAllWidgetInstances(context, widgetDataJson)
                    }
                }
            } catch (e: Exception) {
                logger.e(e) { "Exception while updating widget data" }
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

        private suspend fun generateAndSaveChartImage(
            context: Context,
            data: WidgetConsumptionData
        ) {
            try {
                val chartGenerator = createChartImageGenerator()
                val imageBytes = chartGenerator.generateChartImage(
                    data = data,
                    widthPx = CHART_WIDTH_PX,
                    heightPx = CHART_HEIGHT_PX,
                    isDarkMode = isSystemInDarkMode(context)
                )
                imageBytes?.let { bytes ->
                    File(context.filesDir, CHART_FILE_NAME).writeBytes(bytes)
                }
            } catch (e: Exception) {
                logger.e(e) { "Failed to generate chart image" }
            }
        }

        private suspend fun updateAllWidgetInstances(context: Context, widgetDataJson: String) {
            val glanceIds = GlanceAppWidgetManager(context)
                .getGlanceIds(ConsumptionWidget::class.java)

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[WIDGET_DATA_KEY] = widgetDataJson
                    }
                }
                ConsumptionWidget().update(context, glanceId)
            }
        }

        fun scheduleWidgetUpdates(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WIDGET_UPDATE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        fun cancelWidgetUpdates(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WIDGET_UPDATE_WORK_NAME)
        }
    }
}

class ConsumptionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ConsumptionWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        ConsumptionWidget.scheduleWidgetUpdates(context)
        CoroutineScope(Dispatchers.IO).launch {
            ConsumptionWidget.updateWidgetData(context)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        ConsumptionWidget.cancelWidgetUpdates(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        CoroutineScope(Dispatchers.IO).launch {
            ConsumptionWidget.updateWidgetData(context)
        }
    }
}

class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            ConsumptionWidget.updateWidgetData(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Logger.withTag("WidgetUpdateWorker").e(e) { "Widget update failed" }
            Result.retry()
        }
    }
}
