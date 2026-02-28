package com.cosmic_struck.stellar.auth.domain

import android.util.Log
import com.cosmic_struck.stellar.common.util.Resource
import io.appwrite.services.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val account: Account
) {
    operator fun invoke(email: String, password: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val session = account.createEmailPasswordSession(
                email = email,
                password = password
            )
            Log.d("LoginUseCase", session.toString())
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Unknown Error"))
            Log.d("LoginUseCase", e.localizedMessage ?: "Unknown Error")
        }
    }
}