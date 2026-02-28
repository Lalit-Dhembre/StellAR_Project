package com.cosmic_struck.stellar.stellar.scantext.domain.usecase

import android.util.Log
import com.cosmic_struck.stellar.common.util.Resource
import com.cosmic_struck.stellar.stellar.scantext.data.dto.JobStatusDTO
import com.cosmic_struck.stellar.stellar.scantext.domain.repository.ScanImageRepo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class PollJobStatusUseCase @Inject constructor(
    private val scanImageRepo: ScanImageRepo
) {
    /**
     * Polls the job status endpoint until the job is complete or an error occurs.
     * Emits intermediate updates so the UI can progressively display content.
     */
    operator fun invoke(
        jobId: String,
        pollingIntervalMs: Long = 3000L,
        maxAttempts: Int = 100
    ): Flow<Resource<JobStatusDTO>> = flow {
        try {
            emit(Resource.Loading())
            var attempts = 0

            while (attempts < maxAttempts) {
                val status = scanImageRepo.getJobStatus(jobId)
                Log.d("PollJobStatus", "Job $jobId: status=${status.status}")

                // Emit current state (even if partial)
                emit(Resource.Success(status))

                // If complete or error, stop polling
                if (status.status == "complete" || status.status == "error") {
                    Log.d("PollJobStatus", "Job $jobId finished: ${status.status}")
                    break
                }

                attempts++
                delay(pollingIntervalMs)
            }
        } catch (e: Exception) {
            Log.e("PollJobStatus", "Polling error: ${e.message}")
            emit(Resource.Error(e.message ?: "Failed to poll job status"))
        }
    }
}
