package cn.osmrpighead.envelog.data.db.dao

import android.app.Application
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import cn.osmrpighead.envelog.testing.RoomTestData
import cn.osmrpighead.envelog.testing.seed
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class SeedDaoTest {
    private lateinit var database: EnvelogDatabase
    private lateinit var dao: SeedDao

    @Before
    fun setUp() {
        database = RoomTestData.createDatabase()
        dao = database.seedDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun envelopeCount_returnsZeroForEmptyDatabase() = runTest {
        assertEquals(0, dao.envelopeCount())
    }

    @Test
    fun envelopeCount_countsInsertedEnvelopes() = runTest {
        database.seed(
            envelopes = listOf(
                RoomTestData.envelope(RoomTestData.envelopeA, name = "餐饮"),
                RoomTestData.envelope(RoomTestData.envelopeB, name = "购物", isBudgeted = false),
            ),
        )

        assertEquals(2, dao.envelopeCount())
    }
}
