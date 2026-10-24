package com.example.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface GoogleCalendarApiService {

    @GET("calendars/{calendarId}/events")
    suspend fun getEvents(
        @Path("calendarId") calendarId: String = "primary",
        @Query("timeMin") timeMin: String? = null,
        @Query("maxResults") maxResults: Int = 50,
        @Query("singleEvents") singleEvents: Boolean = true,
        @Query("orderBy") orderBy: String = "startTime"
    ): Response<GoogleEventsListResponse>

    @POST("calendars/{calendarId}/events")
    suspend fun createEvent(
        @Path("calendarId") calendarId: String = "primary",
        @Body event: GoogleCalendarEventDto
    ): Response<GoogleCalendarEventDto>

    @PUT("calendars/{calendarId}/events/{eventId}")
    suspend fun updateEvent(
        @Path("calendarId") calendarId: String = "primary",
        @Path("eventId") eventId: String,
        @Body event: GoogleCalendarEventDto
    ): Response<GoogleCalendarEventDto>

    @DELETE("calendars/{calendarId}/events/{eventId}")
    suspend fun deleteEvent(
        @Path("calendarId") calendarId: String = "primary",
        @Path("eventId") eventId: String
    ): Response<Unit>
}
