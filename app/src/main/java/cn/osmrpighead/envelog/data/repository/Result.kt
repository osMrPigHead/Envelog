package cn.osmrpighead.envelog.data.repository

sealed interface ArchiveResult {
    data object Archived : ArchiveResult
    data object NotFound : ArchiveResult
}

sealed interface DeleteResult {
    data object Deleted : ArchiveResult
    data object NotFound : ArchiveResult
}
