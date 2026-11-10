package cn.osmrpighead.envelog.util

import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator
import java.util.UUID

object UUIDGenerator {
    val instance: TimeBasedEpochGenerator by lazy { Generators.timeBasedEpochGenerator() }
}

fun generateUUID(): UUID = UUIDGenerator.instance.generate()