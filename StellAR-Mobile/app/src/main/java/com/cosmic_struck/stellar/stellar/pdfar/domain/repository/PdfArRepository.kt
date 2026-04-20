package com.cosmic_struck.stellar.stellar.pdfar.domain.repository

import com.cosmic_struck.stellar.stellar.pdfar.data.models.ProcessPdfResponse
import com.cosmic_struck.stellar.stellar.pdfar.data.models.ConceptDetailsResponse
import com.cosmic_struck.stellar.stellar.pdfar.data.models.ResolveEntityResponse
import com.cosmic_struck.stellar.stellar.pdfar.data.models.TaskStatusResponse
import java.io.File

interface PdfArRepository {
    suspend fun processPdf(pdfFile: File, domain: String): Result<ProcessPdfResponse>
    suspend fun getConceptDetails(conceptId: String): Result<ConceptDetailsResponse>
    suspend fun resolveEntity(entityName: String): Result<ResolveEntityResponse>
    suspend fun getTaskStatus(taskId: String): Result<TaskStatusResponse>
    suspend fun downloadModel(url: String, destinationFile: File): Result<File>
}
