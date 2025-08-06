package com.example.adproject.model

data class DashboardResponse(
    val code: Int,
    val msg: String?,
    val data: AccuracyData
)

data class AccuracyData(
    val accuracyRates: List<Float>
)
