package com.example.mapdiary.dataclass

import java.io.Serializable

data class Comment(
    val commentTitle: String = "",
    val commentDateTime: String = "",
    val commentUser: String = ""
) :
    Serializable