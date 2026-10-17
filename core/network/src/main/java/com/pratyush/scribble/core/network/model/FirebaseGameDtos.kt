package com.pratyush.scribble.core.network.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName
import com.pratyush.scribble.core.model.DrawingPoint
import com.pratyush.scribble.core.model.DrawingStroke
import com.pratyush.scribble.core.model.DrawingTool

@IgnoreExtraProperties
data class FirebaseStrokeDto(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("turnId") @set:PropertyName("turnId") var turnId: String = "",
    @get:PropertyName("drawerId") @set:PropertyName("drawerId") var drawerId: String = "",
    @get:PropertyName("colorArgb") @set:PropertyName("colorArgb") var colorArgb: Long = -16777216L,
    @get:PropertyName("strokeWidth") @set:PropertyName("strokeWidth") var strokeWidth: Float = 6f,
    @get:PropertyName("points") @set:PropertyName("points") var points: List<Float> = emptyList(),
    @get:PropertyName("seq") @set:PropertyName("seq") var seq: Long = 0L,
    @get:PropertyName("deleted") @set:PropertyName("deleted") var deleted: Boolean = false
) {
    fun toDrawingStroke(): DrawingStroke {
        val pts = ArrayList<DrawingPoint>(points.size / 2)
        var i = 0
        while (i + 1 < points.size) {
            pts.add(DrawingPoint(points[i], points[i + 1]))
            i += 2
        }
        return DrawingStroke(
            id = id,
            points = pts,
            colorArgb = colorArgb,
            strokeWidth = strokeWidth,
            tool = DrawingTool.PEN
        )
    }

    companion object {
        fun fromDrawingStroke(stroke: DrawingStroke, turnId: String, drawerId: String, seq: Long): FirebaseStrokeDto {
            val flatPts = ArrayList<Float>(stroke.points.size * 2)
            for (p in stroke.points) {
                flatPts.add(p.x)
                flatPts.add(p.y)
            }
            return FirebaseStrokeDto(
                id = stroke.id,
                turnId = turnId,
                drawerId = drawerId,
                colorArgb = stroke.colorArgb,
                strokeWidth = stroke.strokeWidth,
                points = flatPts,
                seq = seq,
                deleted = false
            )
        }
    }
}

@IgnoreExtraProperties
data class FirebaseChatMessageDto(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("senderId") @set:PropertyName("senderId") var senderId: String = "",
    @get:PropertyName("senderName") @set:PropertyName("senderName") var senderName: String = "",
    @get:PropertyName("text") @set:PropertyName("text") var text: String = "",
    @get:PropertyName("timestamp") @set:PropertyName("timestamp") var timestamp: Long = 0L,
    @get:PropertyName("type") @set:PropertyName("type") var type: String = "CHAT"
)

@IgnoreExtraProperties
data class FirebaseWordChoiceDto(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("word") @set:PropertyName("word") var word: String = "",
    @get:PropertyName("difficulty") @set:PropertyName("difficulty") var difficulty: String = "MEDIUM"
)

@IgnoreExtraProperties
data class FirebaseDrawerSecretDto(
    @get:PropertyName("turnId") @set:PropertyName("turnId") var turnId: String = "",
    @get:PropertyName("drawerId") @set:PropertyName("drawerId") var drawerId: String = "",
    @get:PropertyName("selectedWord") @set:PropertyName("selectedWord") var selectedWord: String? = null,
    @get:PropertyName("wordChoices") @set:PropertyName("wordChoices") var wordChoices: List<FirebaseWordChoiceDto> = emptyList()
)

@IgnoreExtraProperties
data class FirebaseGameStateDto(
    @get:PropertyName("phase") @set:PropertyName("phase") var phase: String = "SELECTING_WORD",
    @get:PropertyName("round") @set:PropertyName("round") var round: Int = 1,
    @get:PropertyName("totalRounds") @set:PropertyName("totalRounds") var totalRounds: Int = 3,
    @get:PropertyName("turnNumber") @set:PropertyName("turnNumber") var turnNumber: Int = 1,
    @get:PropertyName("turnId") @set:PropertyName("turnId") var turnId: String = "turn_1",
    @get:PropertyName("drawerId") @set:PropertyName("drawerId") var drawerId: String = "",
    @get:PropertyName("turnDeadline") @set:PropertyName("turnDeadline") var turnDeadline: Long = 0L,
    @get:PropertyName("maskedWord") @set:PropertyName("maskedWord") var maskedWord: String = "",
    @get:PropertyName("wordLength") @set:PropertyName("wordLength") var wordLength: Int = 0,
    @get:PropertyName("wordHash") @set:PropertyName("wordHash") var wordHash: String = "",
    @get:PropertyName("revealedWord") @set:PropertyName("revealedWord") var revealedWord: String? = null,
    @get:PropertyName("correctGuessers") @set:PropertyName("correctGuessers") var correctGuessers: Map<String, Long> = emptyMap(),
    @get:PropertyName("turnOrder") @set:PropertyName("turnOrder") var turnOrder: List<String> = emptyList(),
    @get:PropertyName("clearSeq") @set:PropertyName("clearSeq") var clearSeq: Long = 0L,
    @get:PropertyName("scores") @set:PropertyName("scores") var scores: Map<String, Int> = emptyMap()
)

@IgnoreExtraProperties
data class FirebaseReplayVotingDto(
    @get:PropertyName("status") @set:PropertyName("status") var status: String = "VOTING",
    @get:PropertyName("deadline") @set:PropertyName("deadline") var deadline: Long = 0L,
    @get:PropertyName("votes") @set:PropertyName("votes") var votes: Map<String, Boolean> = emptyMap(),
    @get:PropertyName("voteCount") @set:PropertyName("voteCount") var voteCount: Int = 0,
    @get:PropertyName("requiredMajority") @set:PropertyName("requiredMajority") var requiredMajority: Int = 1
)
