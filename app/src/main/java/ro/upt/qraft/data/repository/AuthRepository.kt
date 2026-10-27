package ro.upt.qraft.data.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<String?>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun register(email: String, password: String): Result<Unit>
    suspend fun logout()
}
