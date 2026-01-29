package com.cosmic_struck.stellar.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmic_struck.stellar.common.util.OnboardingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingManager: OnboardingManager
) : ViewModel() {

    fun completeOnboarding() {
        viewModelScope.launch {
            onboardingManager.saveOnboardingState(true)
        }
    }
}
