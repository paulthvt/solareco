package net.thevenot.comwatt.model

import kotlinx.serialization.Serializable

/**
 * The paged envelope returned by `/api/typicaldays` and `/api/plannings`.
 * Only [content] and [totalElements] are used by the app; the rest is kept so
 * the shape is documented and future paging is possible.
 */
@Serializable
data class PagedResponseDto<T>(
    val content: List<T> = emptyList(),
    val totalElements: Int = 0,
    val totalPages: Int = 0,
    val currentPageIndex: Int = 0,
    val numberOfElements: Int = 0,
    val paginationSize: Int = 0,
    val first: Boolean = true,
    val last: Boolean = true,
)
