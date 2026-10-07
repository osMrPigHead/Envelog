package cn.osmrpighead.envelog.data.repository

import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.model.ExpenditureEnvelopeAllocation
import cn.osmrpighead.envelog.data.model.Transaction
import cn.osmrpighead.envelog.data.model.TransactionType
import cn.osmrpighead.envelog.data.query.EnvelopeResult
import cn.osmrpighead.envelog.data.query.ExpenditureResult
import cn.osmrpighead.envelog.data.repository.EnvelogRepositoryException.*
import cn.osmrpighead.envelog.data.repository.EnvelogRepositoryException.ArgumentException.NoSuch as NoSuch
import cn.osmrpighead.envelog.util.HasId
import cn.osmrpighead.envelog.util.mapEach
import cn.osmrpighead.envelog.util.trimAndCheck
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.collections.filter

class InMemoryEnvelogRepository(
    envelopes: List<Envelope> = emptyList(),
    budgets: List<Budget> = emptyList(),
    transactions: List<Transaction> = emptyList(),
    allocations: List<ExpenditureEnvelopeAllocation> = emptyList(),
) : EnvelogRepository {
    private val envelopes: MutableStateFlow<List<Envelope>> = MutableStateFlow(envelopes)
    private val budgets: MutableStateFlow<List<Budget>> = MutableStateFlow(budgets)
    private val transactions: MutableStateFlow<List<Transaction>> = MutableStateFlow(transactions)
    private val allocations: MutableStateFlow<List<ExpenditureEnvelopeAllocation>> = MutableStateFlow(allocations)

    override fun observeActiveEnvelopes(): Flow<List<Envelope>> =
        envelopes.map { envelopes -> envelopes.filter { !it.archived } }

    override fun observeActiveEnvelopesWithExtra(startDate: LocalDate, endDate: LocalDate): Flow<List<EnvelopeResult>> =
        observeActiveEnvelopes().mapEach {
            combine(
                if (it.isBudgeted) observeBudgets(it.id, startDate, endDate) else flowOf(null),
                observeExpenditureEnvelopeAllocations(it.id, startDate, endDate),
            ) { budgets, allocations -> EnvelopeResult(it, budgets, allocations) }
        }

    override fun observeBudgets(envelopeId: UUID, startDate: LocalDate, endDate: LocalDate): Flow<List<Budget>> =
        budgets.map { budgets -> budgets.filter { it.startDate in startDate..<endDate && it.envelopeId == envelopeId } }

    override fun observeExpenditureEnvelopeAllocations(
        envelopeId: UUID, startDate: LocalDate, endDate: LocalDate,
    ): Flow<List<ExpenditureResult>> =
        allocations.map { allocations ->
            allocations.mapNotNull {
                val transaction = transactions.value.find { transaction -> it.transactionId == transaction.id }
                if (transaction != null) ExpenditureResult(transaction = transaction, allocations = listOf(it))
                else null
            }.filter { (transaction, allocations) ->
                transaction.dateTime.toLocalDate() in startDate..<endDate && allocations[0].envelopeId == envelopeId
            }
        }

    override fun observeTransactions(startDate: LocalDate, endDate: LocalDate): Flow<List<Transaction>> =
        transactions.map { transactions -> transactions.filter { it.dateTime.toLocalDate() in startDate..<endDate } }

    override fun observeTransactions(
        type: TransactionType,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<Transaction>> = transactions.map { transactions ->
        transactions.filter { it.dateTime.toLocalDate() in startDate..<endDate && it.type == type }
    }

    override suspend fun createEnvelope(name: String, isBudgeted: Boolean): Result<UUID> =
        EnvelogRepositoryException.catch {
            envelopes.create { id, _ ->
                Envelope(
                    id = id,
                    name = name.trimAndCheck(ArgumentException.NameBlank()),
                    isBudgeted = isBudgeted,
                    archived = false,
                )
            }
        }

    override suspend fun renameEnvelope(id: UUID, name: String): Result<Unit> = EnvelogRepositoryException.catch {
        envelopes.findIdAndApply(id, NoSuch.Envelope()) {
            this[it] = this[it].copy(
                name = name.trimAndCheck(ArgumentException.NameBlank()),
            )
        }
    }

    override suspend fun archiveEnvelope(id: UUID): Result<Unit> = EnvelogRepositoryException.catch {
        envelopes.findIdAndApply(id, NoSuch.Envelope()) { this[it] = this[it].copy(archived = true) }
    }

    override suspend fun deleteEnvelope(id: UUID): Result<Unit> = EnvelogRepositoryException.catch {
        envelopes.findIdAndApply(id, NoSuch.Envelope()) {
            if (allocations.value.any { allocation -> allocation.envelopeId == id })
                throw ArgumentException.EnvelopeReferenced()
            removeAt(it)
        }
    }

    override suspend fun clearBudget(id: UUID): Result<Budget> = EnvelogRepositoryException.catch {
        var budget: Budget? = null
        budgets.findIdAndApply(id, NoSuch.Budget()) {
            budget = this[it]
            removeAt(it)
        }
        budget!!
    }

    private fun clearBudget(
        startDate: LocalDate,
        endDate: LocalDate,
        function: (Budget) -> Boolean,
    ): Result<List<Budget>> {
        var results = emptyList<Budget>()
        budgets.update { budgets ->
            results =
                budgets.filter { function(it) && maxOf(startDate, it.startDate) < minOf(endDate, it.endDate) }
            budgets.toMutableList().apply {
                removeAll(results)
            }
        }
        return Result.success(results)
    }

    override suspend fun clearBudget(envelopeId: UUID, startDate: LocalDate, endDate: LocalDate): Result<List<Budget>> =
        clearBudget(startDate, endDate) { it.envelopeId == envelopeId }

    override suspend fun clearBudget(startDate: LocalDate, endDate: LocalDate): Result<List<Budget>> =
        clearBudget(startDate, endDate) { true }

    override suspend fun setBudget(
        envelopeId: UUID,
        startDate: LocalDate, endDate: LocalDate,
        amount: Long,
    ): Result<UUID> = EnvelogRepositoryException.catch {
        budgets.create { id, budgets ->
            if (budgets.any { budget ->
                    budget.envelopeId == envelopeId && startDate < budget.endDate && budget.startDate < endDate
                }) throw RuleViolation.BudgetOverlap()
            if (!envelopes.value.any { envelope -> envelope.id == envelopeId }) throw NoSuch.Envelope()
            Budget(
                id = id,
                envelopeId = envelopeId,
                startDate = startDate,
                endDate = endDate,
                amount = amount,
            )
        }
    }

    override suspend fun setBudget(id: UUID, amount: Long): Result<Unit> = EnvelogRepositoryException.catch {
        budgets.update {
            val index = it.indexOfFirst { budget -> budget.id == id }
            if (index == -1) throw NoSuch.Budget()
            it.toMutableList().apply { this[index] = this[index].copy(amount = amount) }
        }
    }

    private fun validateAllocations(amount: Long, allocations: Map<UUID, Long>) {
        if (allocations.values.sum() != amount) throw RuleViolation.AllocationMismatch()
        val envelopes = envelopes.value
        if (!allocations.keys.all { envelopes.any { envelope -> envelope.id == it } }) throw NoSuch.Envelope()
    }

    private fun replaceAllocations(transactionId: UUID, allocations: Map<UUID, Long>, removeOld: Boolean = true) {
        this.allocations.update {
            it.toMutableList().apply {
                if (removeOld) removeAll { allocation -> allocation.transactionId == transactionId }
                addAll(
                    allocations.map { (id, amount) ->
                        ExpenditureEnvelopeAllocation(
                            transactionId = transactionId,
                            envelopeId = id,
                            amount = amount,
                        )
                    },
                )
            }
        }
    }

    override suspend fun recordExpenditure(
        dateTime: ZonedDateTime,
        amount: Long,
        allocations: Map<UUID, Long>,
    ): Result<UUID> = EnvelogRepositoryException.catch {
        validateAllocations(amount, allocations)
        val id = transactions.create { id, _ ->
            Transaction(
                id = id,
                type = TransactionType.Expenditure,
                dateTime = dateTime,
                amount = amount,
            )
        }
        replaceAllocations(id, allocations, false)
        id
    }

    override suspend fun reallocate(transactionId: UUID, allocations: Map<UUID, Long>): Result<Unit> =
        reallocate(transactionId, null, allocations)

    override suspend fun reallocate(transactionId: UUID, amount: Long?, allocations: Map<UUID, Long>): Result<Unit> =
        EnvelogRepositoryException.catch {
            val realAmount = amount ?: transactions.getById(transactionId, NoSuch.Transaction()).amount
            validateAllocations(realAmount, allocations)
            if (amount != null)
                transactions.findIdAndApply(transactionId, NoSuch.Transaction()) {
                    this[it] = this[it].copy(amount = amount)
                }
            replaceAllocations(transactionId, allocations)
        }

    override suspend fun deleteTransaction(id: UUID): Result<Unit> = EnvelogRepositoryException.catch {
        replaceAllocations(id, emptyMap())
        transactions.findIdAndApply(id, NoSuch.Transaction()) { removeAt(it) }
    }

    private fun <T : HasId> MutableStateFlow<List<T>>.findIdAndApply(
        id: UUID,
        exception: NoSuch,
        function: MutableList<T>.(Int) -> Unit,
    ) {
        update {
            val index = it.indexOfFirst { item -> item.id == id }
            if (index == -1) throw exception
            it.toMutableList().apply { function(index) }
        }
    }

    private fun <T : HasId> StateFlow<List<T>>.getById(id: UUID, exception: NoSuch): T {
        val result = value.find { item -> item.id == id }
        if (result == null) throw exception
        return result
    }

    private fun <T : HasId> MutableStateFlow<List<T>>.create(function: (UUID, MutableList<T>) -> T): UUID {
        var id = UUID.randomUUID()
        while (value.any { it.id == id }) id = UUID.randomUUID()
        update { it.toMutableList().apply { add(function(id, this)) } }
        return id
    }
}