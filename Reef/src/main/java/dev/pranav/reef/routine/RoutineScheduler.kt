package dev.pranav.reef.routine

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.pranav.reef.data.Routine
import dev.pranav.reef.data.RoutineSchedule
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Manages scheduling of routines using WorkManager for reliable execution.
 * Handles scheduling activation and deactivation work requests.
 */
object RoutineScheduler {
    private const val TAG = "RoutineScheduler"

    /**
     * Schedule all enabled routines.
     */
    fun scheduleAllRoutines(context: Context) {
        val routines = dev.pranav.reef.util.RoutineManager.getRoutines().filter { it.isEnabled }
        routines.forEach { routine ->
            scheduleRoutine(context, routine)
        }
    }

    /**
     * Schedule a single routine. Determines if it should be active now,
     * and schedules appropriate activation/deactivation work.
     */
    fun scheduleRoutine(context: Context, routine: Routine) {
        if (!routine.isEnabled || routine.schedule.type == RoutineSchedule.ScheduleType.MANUAL) {
            return
        }

        if (RoutineScheduleCalculator.isRoutineActiveNow(routine)) {
            RoutineExecutor.activateRoutine(context, routine)

            if (routine.schedule.endTime != null) {
                scheduleDeactivation(context, routine)
            }
        } else {
            scheduleActivation(context, routine)

            if (routine.schedule.endTime != null) {
                scheduleDeactivation(context, routine)
            }
        }
    }

    /**
     * Schedule routine activation using WorkManager.
     */
    fun scheduleActivation(context: Context, routine: Routine) {
        scheduleWork(context, routine, isActivation = true)
    }

    /**
     * Schedule routine deactivation using WorkManager.
     */
    fun scheduleDeactivation(context: Context, routine: Routine) {
        scheduleWork(context, routine, isActivation = false)
    }

    /**
     * Cancel all scheduled work for a routine.
     */
    fun cancelRoutine(context: Context, routineId: String) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(RoutineWorker.getActivationWorkName(routineId))
        workManager.cancelUniqueWork(RoutineWorker.getDeactivationWorkName(routineId))
        Log.d(TAG, "Cancelled all work for routine: $routineId")
    }

    private fun scheduleWork(context: Context, routine: Routine, isActivation: Boolean) {
        val triggerTime = RoutineScheduleCalculator.calculateNextTriggerTime(
            routine.schedule,
            useStartTime = isActivation
        ) ?: return

        val delay = triggerTime - System.currentTimeMillis()
        if (delay <= 0) {
            Log.w(TAG, "Trigger time already passed for ${routine.name}, skipping")
            return
        }

        val inputData = Data.Builder()
            .putString(RoutineWorker.KEY_ROUTINE_ID, routine.id)
            .putBoolean(RoutineWorker.KEY_IS_ACTIVATION, isActivation)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<RoutineWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        val workName = if (isActivation) {
            RoutineWorker.getActivationWorkName(routine.id)
        } else {
            RoutineWorker.getDeactivationWorkName(routine.id)
        }

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        val action = if (isActivation) "activation" else "deactivation"
        Log.d(TAG, "Scheduled ${routine.name} $action with WorkManager for ${Date(triggerTime)}")
    }
}
