package com.example.adproject.network

import com.example.adproject.model.DashboardResponse
import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    @GET("/student/dashboard")
    fun getDashboardData(): Call<DashboardResponse>
}
