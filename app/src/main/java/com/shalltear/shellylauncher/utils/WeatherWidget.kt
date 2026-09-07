package com.shalltear.shellylauncher.utils

/**
 * Weather widget data and utilities.
 * Currently provides mock weather data; can be extended with real weather API integration.
 */
object WeatherWidget {
    
    data class WeatherData(
        val temperature: Int,
        val condition: String,  // "Sunny", "Cloudy", "Rainy", "Snowy"
        val humidity: Int,
        val windSpeed: Int,  // km/h
        val location: String,
    )

    // Mock weather data - in production would fetch from API
    private var cachedWeather = WeatherData(
        temperature = 22,
        condition = "Sunny",
        humidity = 65,
        windSpeed = 12,
        location = "Current Location",
    )

    fun getCurrentWeather(): WeatherData = cachedWeather

    fun setWeather(weather: WeatherData) {
        cachedWeather = weather
    }

    fun getWeatherEmoji(condition: String): String {
        return when (condition.lowercase()) {
            "sunny" -> "☀️"
            "cloudy" -> "☁️"
            "rainy" -> "🌧️"
            "snowy" -> "❄️"
            "stormy" -> "⛈️"
            "foggy" -> "🌫️"
            else -> "🌡️"
        }
    }

    fun getTemperatureColor(temp: Int): String {
        return when {
            temp < 0 -> "#4A90FF"    // Blue for freezing
            temp < 15 -> "#7BB7FF"   // Light blue for cold
            temp < 25 -> "#7B6FEF"   // Purple for cool/normal
            temp < 35 -> "#FF9500"   // Orange for warm
            else -> "#FF6B9D"        // Pink for hot
        }
    }
}
