package com.example.adproject.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // 模拟器访问本机服务：10.0.2.2；必须以 "/" 结尾
    private const val BASE_URL = "http://10.0.2.2:8080/"

    val dashboardApi: DashboardApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DashboardApi::class.java)
    }
}
