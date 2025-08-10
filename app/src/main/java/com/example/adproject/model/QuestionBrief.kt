package com.example.adproject.model

data class QuestionBrief(
    val id: Int,
    val title: String,
    val subject: String? = null,
    val grade: String? = null,
    val difficulty: String? = null
)
