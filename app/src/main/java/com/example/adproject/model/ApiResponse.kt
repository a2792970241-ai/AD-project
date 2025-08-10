package com.example.adproject.model

// 通用外层
data class ApiResponse<T>(
    val code: Int,
    val msg: String?,
    val data: T?
)
