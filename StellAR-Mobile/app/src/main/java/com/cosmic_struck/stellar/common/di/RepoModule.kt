package com.cosmic_struck.stellar.common.di

import com.cosmic_struck.stellar.classroom.data.repository.ClassroomModuleServiceRepository
import com.cosmic_struck.stellar.classroom.data.repository.ClassroomModuleServiceRepositoryImpl
import com.cosmic_struck.stellar.classroom.data.service.ClassroomModuleService
import com.cosmic_struck.stellar.stellar.scantext.data.remote.ScanService
import com.cosmic_struck.stellar.stellar.scantext.data.repository.ScanImageRepoImpl
import com.cosmic_struck.stellar.stellar.scantext.domain.repository.ScanImageRepo
import com.cosmic_struck.stellar.stellar.pdfar.domain.repository.PdfArRepository
import com.cosmic_struck.stellar.stellar.pdfar.data.repository.PdfArRepositoryImpl
import com.cosmic_struck.stellar.stellar.pdfar.data.remote.PdfArService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepoModule {
    @Provides
    @Singleton
    fun provideScanImageRepo(api: ScanService): ScanImageRepo{
        return ScanImageRepoImpl(
            api = api
        )
    }

    @Provides
    @Singleton
    fun provideClassroomModuleServiceRepo(api: ClassroomModuleService) : ClassroomModuleServiceRepository{
        return ClassroomModuleServiceRepositoryImpl(
            classroomModuleService = api
        )
    }

    @Provides
    @Singleton
    fun providePdfArRepository(api: PdfArService): PdfArRepository {
        return PdfArRepositoryImpl(api)
    }
}