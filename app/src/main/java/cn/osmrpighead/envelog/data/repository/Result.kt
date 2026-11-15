package cn.osmrpighead.envelog.data.repository

enum class ArchiveResult {
    Archived, NotFoundOrAlreadyArchived,
}

enum class EnvelopeDeleteResult {
    Deleted, NotFound, Constraint,
}

enum class DeleteResult {
    Deleted, NotFoundOrAlreadyDeleted,
}

enum class BudgetWriteResult {
    EnvelopeArchived, BudgetOverlaps, Success,
}

enum class EnvelopeUpdateResult {
    NotFound, Success,
}
