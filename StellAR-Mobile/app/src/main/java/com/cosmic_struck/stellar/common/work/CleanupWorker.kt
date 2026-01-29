package com.cosmic_struck.stellar.common.work

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File

class CleanupWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Log.d("CleanupWorker", "doWork() started")

        val folder = File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"StellarModels")

        Log.d("CleanupWorker", "Folder path: ${folder.absolutePath}")
        Log.d("CleanupWorker", "Folder exists: ${folder.exists()}")

        val success = deleteFilesInFolder(folder)

        return if (success) {
            Log.d("CleanupWorker", "Cleanup successful")
            if (folder.listFiles().isNullOrEmpty()) {
                folder.delete()
                Log.d("CleanupWorker", "Root folder deleted")
            }
            Result.success()
        } else {
            Log.e("CleanupWorker", "Cleanup failed")
            Result.failure()
        }
    }

    private fun deleteFilesInFolder(folder: File): Boolean {
        if (!folder.exists() || !folder.isDirectory) {
            Log.d("CleanupWorker", "Folder doesn't exist or isn't a directory")
            return false
        }

        val files = folder.listFiles()
        if (files.isNullOrEmpty()) {
            Log.d("CleanupWorker", "Folder is empty")
            return true
        }

        Log.d("CleanupWorker", "Found ${files.size} items to delete")
        var allDeleted = true
        for (file in files) {
            if (file.isDirectory) {
                deleteFilesInFolder(file)
            }
            val deleted = file.delete()
            if (!deleted) {
                allDeleted = false
                Log.e("CleanupWorker", "Failed to delete: ${file.name}")
            } else {
                Log.d("CleanupWorker", "Deleted: ${file.name}")
            }
        }
        return allDeleted
    }
}