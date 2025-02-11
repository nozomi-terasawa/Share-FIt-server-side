package org.example.project.infrastructure.serializationData

import kotlinx.serialization.Serializable

@Serializable
data class UserLoginRes(
    val userId: Int = -1,
    val name: String = "",
    val token: String = "",
)
