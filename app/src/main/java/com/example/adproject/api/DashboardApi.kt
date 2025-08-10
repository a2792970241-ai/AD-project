package com.example.adproject.api

import com.example.adproject.model.DashboardResponse
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

interface DashboardApi {

    // 仪表盘
    @GET("student/dashboard")
    fun getDashboardData(): Call<DashboardResponse>

    // 触发推荐
    @PUT("student/recommend")
    fun triggerRecommend(): Call<JsonObject>

    // 获取推荐题目 ID 列表
    @GET("student/getRecommend")
    fun getRecommendIds(): Call<JsonObject>

    // ✅ 按 ID 获取题目详情（你后端的真实接口）
    // 例如：GET http://10.0.2.2:8080/student/doquestion?id=1
    @GET("student/doquestion")
    fun getQuestionDetail(@Query("id") id: Int): Call<JsonObject>
}
