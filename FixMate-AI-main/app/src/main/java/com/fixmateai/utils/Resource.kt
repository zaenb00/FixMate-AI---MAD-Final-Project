package com.fixmateai.utils

/**
 * A generic wrapper that represents the state of any asynchronous operation.
 *
 * Repositories return [Resource] so the UI can react to Loading / Success / Error
 * without throwing exceptions across layers. This is the backbone of our error
 * handling strategy.
 */
sealed class Resource<out T> {

    /** Operation in progress — show a progress indicator. */
    data object Loading : Resource<Nothing>()

    /** Operation succeeded and carries [data]. */
    data class Success<T>(val data: T) : Resource<T>()

    /**
     * Operation failed. [message] is a user-friendly description and
     * [throwable] is the original cause (useful for logging).
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : Resource<Nothing>()
}
