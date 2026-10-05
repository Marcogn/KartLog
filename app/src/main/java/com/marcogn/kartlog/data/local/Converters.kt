package com.marcogn.kartlog.data.local

import androidx.room.TypeConverter
import com.marcogn.kartlog.domain.model.Cc
import com.marcogn.kartlog.domain.model.EventType
import com.marcogn.kartlog.domain.model.TrophyRank

class Converters {

    // "" (unit separator): non compare mai nei nomi dei cibi del seed (né nei livelli di boost).
    @TypeConverter
    fun fromFoodList(foods: List<String>): String = foods.joinToString("")

    @TypeConverter
    fun toFoodList(raw: String): List<String> = if (raw.isEmpty()) emptyList() else raw.split("")

    @TypeConverter
    fun fromEventType(value: EventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): EventType = EventType.valueOf(value)

    @TypeConverter
    fun fromCc(value: Cc): String = value.name

    @TypeConverter
    fun toCc(value: String): Cc = Cc.valueOf(value)

    @TypeConverter
    fun fromTrophyRank(value: TrophyRank): String = value.name

    @TypeConverter
    fun toTrophyRank(value: String): TrophyRank = TrophyRank.valueOf(value)
}
