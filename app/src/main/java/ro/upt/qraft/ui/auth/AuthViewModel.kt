package ro.upt.qraft.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ro.upt.qraft.data.repository.AuthRepository
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.login(email.trim(), password)
            _uiState.value = if (result.isSuccess) AuthUiState.Success
            else AuthUiState.Error(errorMessage(result.exceptionOrNull(), "Autentificarea a eșuat."))
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.register(email.trim(), password)
            _uiState.value = if (result.isSuccess) AuthUiState.Success
            else AuthUiState.Error(errorMessage(result.exceptionOrNull(), "Crearea contului a eșuat."))
        }
    }

    /** Mesajele Firebase sunt in engleza; le traducem pe cele uzuale. */
    private fun errorMessage(error: Throwable?, fallback: String): String = when (error) {
        // WeakPassword extinde InvalidCredentials, deci trebuie verificat inainte.
        is FirebaseAuthWeakPasswordException -> "Parola trebuie să aibă cel puțin 6 caractere."
        is FirebaseAuthUserCollisionException -> "Există deja un cont cu acest email."
        is FirebaseAuthInvalidUserException -> "Nu există niciun cont cu acest email sau contul este dezactivat."
        is FirebaseAuthInvalidCredentialsException ->
            if (error.errorCode == "ERROR_INVALID_EMAIL") "Adresa de email nu este validă."
            else "Email sau parolă greșită."
        is FirebaseNetworkException -> "Nu există conexiune la internet."
        is FirebaseTooManyRequestsException -> "Prea multe încercări. Încearcă din nou peste câteva minute."
        else -> fallback
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
