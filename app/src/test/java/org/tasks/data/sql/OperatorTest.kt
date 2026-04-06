package org.tasks.data.sql

import org.junit.Assert.assertEquals
import org.junit.Test

class OperatorTest {
    @Test fun eq() = assertEquals("=", Operator.eq.toString())
    @Test fun isNull() = assertEquals("IS NULL", Operator.isNull.toString())
    @Test fun isNotNull() = assertEquals("IS NOT NULL", Operator.isNotNull.toString())
    @Test fun and() = assertEquals("AND", Operator.and.toString())
    @Test fun or() = assertEquals("OR", Operator.or.toString())
    @Test fun not() = assertEquals("NOT", Operator.not.toString())
    @Test fun like() = assertEquals("LIKE", Operator.like.toString())
    @Test fun inOp() = assertEquals("IN", Operator.`in`.toString())
    @Test fun exists() = assertEquals("EXISTS", Operator.exists.toString())
    @Test fun gt() = assertEquals(">", Operator.gt.toString())
    @Test fun lt() = assertEquals("<", Operator.lt.toString())
    @Test fun lte() = assertEquals("<=", Operator.lte.toString())
}
