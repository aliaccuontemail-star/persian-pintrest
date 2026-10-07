package ir.bumo.app.work

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ir.bumo.app.data.BumoRepository
import ir.bumo.app.data.SessionManager

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repo: BumoRepository,
    private val session: SessionManager
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        session.load()
        val uriText = inputData.getString("uri") ?: return Result.failure()
        if (session.accessToken.isNullOrBlank()) return Result.failure()
        return runCatching {
            repo.upload(
                applicationContext,
                Uri.parse(uriText),
                inputData.getString("title").orEmpty(),
                inputData.getString("description").orEmpty(),
                inputData.getString("tags").orEmpty(),
                inputData.getString("board_id").orEmpty()
            )
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }
}
