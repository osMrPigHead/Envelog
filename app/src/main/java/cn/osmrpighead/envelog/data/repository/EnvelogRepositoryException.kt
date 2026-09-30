package cn.osmrpighead.envelog.data.repository

import cn.osmrpighead.envelog.util.EnvelogExceptionCompanion

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
