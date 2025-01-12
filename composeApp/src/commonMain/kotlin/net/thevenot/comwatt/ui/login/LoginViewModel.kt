package net.thevenot.comwatt.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.email_password_required
import comwatt.composeapp.generated.resources.invalid_credentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.client.Password
import net.thevenot.comwatt.client.Session
import net.thevenot.comwatt.database.User
import org.jetbrains.compose.resources.getString

class LoginViewModel(
    private val dataRepository: DataRepository
) : ViewModel() {
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _rememberMe = MutableStateFlow(false)
    val rememberMe: StateFlow<Boolean> = _rememberMe

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> get() = _isLoggingIn

    fun tryAutoLogin(onLogin: (Session) -> Unit) {
        viewModelScope.launch {
            dataRepository.tryAutoLogin(onLogin) { error ->
                _isLoading.value = false
                error?.let {
                    _snackbarMessage.value = it
                }
            }
        }
    }

    fun login(onLogin: (Session) -> Unit) {
        viewModelScope.launch {
            if (validateForm()) {
                _isLoggingIn.value = true
                val password = Password(_password.value)
                val authenticateResponse = dataRepository.api.authenticate(
                        email = _email.value,
                        password = password
                    )

                when(authenticateResponse) {
                    is Either.Left -> {
                        _snackbarMessage.value = getString(Res.string.invalid_credentials)
                        _isLoggingIn.value = false
                        return@launch
                    }
                    is Either.Right -> {
                        if(_rememberMe.value) {
                            dataRepository.addUser(
                                User(
                                    _email.value,
                                    password.encodedValue
                                )
                            )
                        }
                        onLogin(authenticateResponse.value)
                    }
                }
            } else {
                _snackbarMessage.value = getString(Res.string.email_password_required)
            }
        }
    }

    fun updateEmail(username: String) {
        _email.value = username
    }

    fun updatePassword(password: String) {
        _password.value = password
    }

    fun updateRememberMe(rememberMe: Boolean) {
        _rememberMe.value = rememberMe
    }

    private fun validateForm(): Boolean {
        return _email.value.isNotBlank() && _password.value.isNotBlank()
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }
}