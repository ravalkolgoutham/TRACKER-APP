package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EventDateTimeDto(
    @Json(name = "dateTime") val dateTime: String? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "timeZone") val timeZone: String? = null
)

@JsonClass(generateAdapter = true)
data class GoogleCalendarEventDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "summary") val summary: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "start") val start: EventDateTimeDto,
    @Json(name = "end") val end: EventDateTimeDto,
    @Json(name = "recurrence") val recurrence: List<String>? = null,
    @Json(name = "updated") val updated: String? = null,
    @Json(name = "status") val status: String? = null
)

@JsonClass(generateAdapter = true)
data class GoogleEventsListResponse(
    @Json(name = "items") val items: List<GoogleCalendarEventDto>? = emptyList(),
    @Json(name = "nextPageToken") val nextPageToken: String? = null,
    @Json(name = "summary") val summary: String? = null
)
