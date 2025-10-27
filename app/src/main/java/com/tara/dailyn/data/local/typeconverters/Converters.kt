package com.tara.dailyn.data.local.typeconverters

import androidx.room.TypeConverter
import com.tara.dailyn.data.local.model.*
import java.time.*

class Converters {
    @TypeConverter fun fromLocalDate(v: LocalDate?): String? = v?.toString()
    @TypeConverter fun toLocalDate(v: String?): LocalDate? = v?.let(LocalDate::parse)

    @TypeConverter fun fromLocalTime(v: LocalTime?): String? = v?.toString()
    @TypeConverter fun toLocalTime(v: String?): LocalTime? = v?.let(LocalTime::parse)

    @TypeConverter fun fromInstant(v: Instant?): Long? = v?.toEpochMilli()
    @TypeConverter fun toInstant(v: Long?): Instant? = v?.let { Instant.ofEpochMilli(it) }

    @TypeConverter fun fromFrequencyType(v: FrequencyType?): String? = v?.name
    @TypeConverter fun toFrequencyType(v: String?): FrequencyType? = v?.let(FrequencyType::valueOf)

    @TypeConverter fun fromPeriodType(v: PeriodType?): String? = v?.name
    @TypeConverter fun toPeriodType(v: String?): PeriodType? = v?.let(PeriodType::valueOf)

    @TypeConverter fun fromOverflowPolicy(v: OverflowPolicy?): String? = v?.name
    @TypeConverter fun toOverflowPolicy(v: String?): OverflowPolicy? = v?.let(OverflowPolicy::valueOf)

    @TypeConverter fun fromLogStatus(v: LogStatus?): String? = v?.name
    @TypeConverter fun toLogStatus(v: String?): LogStatus? = v?.let(LogStatus::valueOf)
}
