package com.example.mapdiary.dataclass

import java.io.Serializable

data class Board(
    var boardUid: String = "",
    var boardWriter: String = "",
    var boardTitle:String = "",
    var boardContent: String = "",
    var boardLike: Int = 0,
    var boardCommentCount: Int = 0,
    var boardHits: Int = 0,
    var boardDateTime: String = ""
) : Serializable