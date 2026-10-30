package cn.osmrpighead.envelog.data.repository

import cn.osmrpighead.envelog.data.entity.BudgetEntity
import cn.osmrpighead.envelog.data.entity.EnvelopeEntity
import cn.osmrpighead.envelog.data.entity.ExpenditureAllocationEntity
import cn.osmrpighead.envelog.data.entity.TransactionEntity
import cn.osmrpighead.envelog.data.entity.TransactionType
import cn.osmrpighead.envelog.data.query.EnvelopeResult
import cn.osmrpighead.envelog.data.repository.EnvelogRepositoryException.*
import cn.osmrpighead.envelog.data.repository.EnvelogRepositoryException.ArgumentException.NoSuch as NoSuch
import cn.osmrpighead.envelog.data.entity.HasId
import cn.osmrpighead.envelog.data.query.AllocationResult
import cn.osmrpighead.envelog.data.query.DateRange
import cn.osmrpighead.envelog.data.query.judge
import cn.osmrpighead.envelog.data.query.contains
import cn.osmrpighead.envelog.util.UUIDGenerator
import cn.osmrpighead.envelog.core.time.DatePeriod
import cn.osmrpighead.envelog.util.mapEach
import cn.osmrpighead.envelog.core.time.untilExclusive
import cn.osmrpighead.envelog.util.trimAndCheck
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.collections.filter

class InMemoryEnvelogRepository(
    envelopes: List<EnvelopeEntity>,
    budgets: List<BudgetEntity>,
    transactions: List<TransactionEntity>,
    allocations: List<ExpenditureAllocationEntity>,
) : EnvelogRepository {
    private val envelopes: MutableStateFlow<List<EnvelopeEntity>> = MutableStateFlow(envelopes)
    private val budgets: MutableStateFlow<List<BudgetEntity>> = MutableStateFlow(budgets)
    private val transactions: MutableStateFlow<List<TransactionEntity>> = MutableStateFlow(transactions)
    private val allocations: MutableStateFlow<List<ExpenditureAllocationEntity>> = MutableStateFlow(allocations)

    override fun observeActiveEnvelopes(): Flow<List<EnvelopeEntity>> =
        envelopes.map { envelopes -> envelopes.filter { !it.archived } }

    override fun observeActiveEnvelopesWithExtra(range: DateRange): Flow<List<EnvelopeResult>> =
        observeActiveEnvelopes().mapEach {
            combine(
                if (it.isBudgeted) observeBudgets(it.id, range) else flowOf(emptyList()),
                observeExpenditureEnvelopeAllocations(it.id, range),
            ) { budgets, allocations -> EnvelopeResult(it, budgets, allocations) }
        }

    override fun observeBudgets(envelopeId: UUID, range: DateRange): Flow<List<BudgetEntity>> =
        budgets.map { budgets ->
            budgets.filter { range.judge(it.startDate untilExclusive it.endDate) && it.envelopeId == envelopeId }
        }

    override fun observeExpenditureEnvelopeAllocations(
        envelopeId: UUID,
        range: DateRange,
    ): Flow<List<AllocationResult>> =
        allocations.map { allocations ->
            allocations.mapNotNull {
                val transaction = transactions.value.find { transaction -> it.transactionId == transaction.id }
                if (transaction != null) AllocationResult(transaction = transaction, allocation = it)
                else null
            }.filter { (transaction, allocation) ->
                transaction.dateTime.toLocalDate() in range && allocation.envelopeId == envelopeId
            }
        }

    override fun observeTransactions(range: DateRange): Flow<List<TransactionEntity>> =
        transactions.map { transactions ->
            transactions.filter { it.dateTime.toLocalDate() in range }
        }

    override fun observeTransactions(type: TransactionType, range: DateRange): Flow<List<TransactionEntity>> =
        transactions.map { transactions ->
            transactions.filter { it.dateTime.toLocalDate() in range && it.type == type }
        }

    override suspend fun getEnvelope(envelopeId: UUID): EnvelopeEntity {
        val envelope = envelopes.value.find { it.id == envelopeId }
        if (envelope == null) throw NoSuch.Envelope()
        return envelope
    }

    /*
    TODO: observeEnvelopeWithExtra 是 suspend 且返回 Flow，其中 getEnvelope 的检查在“订阅时”之前同步完成，
     因此信封被删除或归档后，已建立的 Flow 不会重新校验信封存在性，只会继续发出旧的 Envelope 快照；如果后续需要
     “信封删除后 Flow 结束或报错”的行为，现有签名要改成在 flow { } 内获取信封，需要补充测试用例
     */
    override suspend fun observeEnvelopeWithExtra(envelopeId: UUID, range: DateRange): Flow<EnvelopeResult> {
        val envelope = getEnvelope(envelopeId)
        return combine(
            if (envelope.isBudgeted) observeBudgets(envelope.id, range) else flowOf(emptyList()),
            observeExpenditureEnvelopeAllocations(envelope.id, range),
        ) { budgets, allocations -> EnvelopeResult(envelope, budgets, allocations) }
    }

    override suspend fun createEnvelope(name: String, isBudgeted: Boolean): Result<UUID> =
        EnvelogRepositoryException.catch {
            envelopes.create { id, _ ->
                EnvelopeEntity(
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

    override suspend fun clearBudget(id: UUID): Result<BudgetEntity> = EnvelogRepositoryException.catch {
        var budget: BudgetEntity? = null
        budgets.findIdAndApply(id, NoSuch.Budget()) {
            budget = this[it]
            removeAt(it)
        }
        budget!!
    }

    private fun clearBudget(
        range: DateRange,
        function: (BudgetEntity) -> Boolean,
    ): Result<List<BudgetEntity>> {
        var results = emptyList<BudgetEntity>()
        budgets.update { budgets ->
            results = budgets.filter { function(it) && range.judge(it.startDate untilExclusive it.endDate) }
            budgets.toMutableList().apply { removeAll(results) }
        }
        return Result.success(results)
    }

    override suspend fun clearBudget(envelopeId: UUID, range: DateRange): Result<List<BudgetEntity>> =
        clearBudget(range) { it.envelopeId == envelopeId }

    override suspend fun clearBudget(range: DateRange): Result<List<BudgetEntity>> =
        clearBudget(range) { true }

    override suspend fun setBudget(
        envelopeId: UUID,
        period: DatePeriod,
        amount: Long,
    ): Result<UUID> = EnvelogRepositoryException.catch {
        budgets.create { id, budgets ->
            if (budgets.any { budget ->
                    budget.envelopeId == envelopeId && (budget.startDate untilExclusive budget.endDate) overlap period
                }) throw RuleViolation.BudgetOverlap()
            if (!envelopes.value.any { envelope -> envelope.id == envelopeId }) throw NoSuch.Envelope()
            BudgetEntity(
                id = id,
                envelopeId = envelopeId,
                startDate = period.start,
                endDate = period.endExclusive,
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
                        ExpenditureAllocationEntity(
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
            TransactionEntity(
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
        val id = UUIDGenerator.instance.generate()
        update { it.toMutableList().apply { add(function(id, this)) } }
        return id
    }
}