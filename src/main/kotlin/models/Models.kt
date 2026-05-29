package models

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val store: String,
    val priceCents: Int?,
    val link: String,
    val productName: String? = null
)

@Serializable
data class ScrapeRequest(
    val query: String,
    val zipCode: String,
    val isHeadless: Boolean = true,
    val selectedStores: List<String>? = null
)

sealed interface ScrapeResult {
    data class Success(val results: List<Product>) : ScrapeResult
    data class Failure(val store: String, val reason: String) : ScrapeResult
    data class RetryNonHeadless(val store: String, val reason: String) : ScrapeResult
}

@Serializable
data class FinalResponse(
    val results: List<Product>,
    val failures: List<FailureReason>
)

@Serializable
data class FailureReason(
    val store: String,
    val reason: String
)

@Serializable
sealed interface ScrapeState {
    val store: String

    @Serializable
    @kotlinx.serialization.SerialName("Navigating")
    data class Navigating(override val store: String, val url: String) : ScrapeState
    
    @Serializable
    @kotlinx.serialization.SerialName("SettingLocation")
    data class SettingLocation(override val store: String, val zipCode: String) : ScrapeState
    
    @Serializable
    @kotlinx.serialization.SerialName("WaitingForResults")
    data class WaitingForResults(override val store: String, val additionalInfo: String? = null) : ScrapeState
    
    @Serializable
    @kotlinx.serialization.SerialName("Parsing")
    data class Parsing(override val store: String) : ScrapeState
    
    @Serializable
    @kotlinx.serialization.SerialName("Warning")
    data class Warning(override val store: String, val message: String) : ScrapeState
}
