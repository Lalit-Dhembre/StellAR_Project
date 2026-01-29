package com.cosmic_struck.stellar.auth.domain

import android.util.Log
import com.cosmic_struck.stellar.common.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    operator fun invoke(email:String, password:String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val user = supabaseClient.auth.signInWith(Email){
                this.email = email
                this.password = password
            }
            Log.d("LoginUseCase",user.toString())
            emit(Resource.Success(true))
        }catch (e: Exception){
            emit(Resource.Error(e.localizedMessage ?: "Unknown Error"))
            Log.d("LoginUseCase",e.localizedMessage ?: "Unknown Error")

        }
    }
}