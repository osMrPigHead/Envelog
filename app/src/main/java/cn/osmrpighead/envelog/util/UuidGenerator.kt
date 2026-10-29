package cn.osmrpighead.envelog.util

import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator

object UuidGenerator {
    val instance: TimeBasedEpochGenerator by lazy { Generators.timeBasedEpochGenerator() }
}