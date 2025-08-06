package com.example.adproject.api

import com.example.adproject.model.DashboardResponse
import retrofit2.Call
import retrofit2.http.GET

interface DashboardApi {
    @GET("/student/dashboard")
    fun getDashboardData(): Call<DashboardResponse>
}
