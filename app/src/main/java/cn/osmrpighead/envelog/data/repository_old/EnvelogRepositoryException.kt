package cn.osmrpighead.envelog.data.repository_old

import cn.osmrpighead.envelog.core.exception.EnvelogExceptionCompanion

@Suppress("CanSealedSubClassBeObject")
sealed class EnvelogRepositoryException : RuntimeException() {
    sealed class ArgumentException : EnvelogRepositoryException() {
        sealed class NoSuch : ArgumentException() {
            class Envelope : NoSuch()
            class Budget : NoSuch()
            class Transaction : NoSuch()
        }

        class EnvelopeReferenced : ArgumentException()
        class NameBlank : ArgumentException()
    }

    sealed class RuleViolation : EnvelogRepositoryException() {
        class AllocationMismatch : RuleViolation()
        class BudgetOverlap: RuleViolation()
    }

    companion object : EnvelogExceptionCompanion<EnvelogRepositoryException>(EnvelogRepositoryException::class.java)
}
