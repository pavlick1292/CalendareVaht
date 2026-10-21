package com.example.calendarevaht.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface CalendarApiService {
    @GET("api/v1/calendars/RU-FEDERAL/years/{year}")
    suspend fun getYear(@Path("year") year: Int): WorkCalendarResponse
}

object CalendarApi {
    private const val BASE_URL = "https://calendar.legalic.ru/"

    val service: CalendarApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CalendarApiService::class.java)
    }
}