package org.tasks.repeats

import net.fortuna.ical4j.model.Recur
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RecurrenceUtilsExtraTest {

    // --- newRecur() no-arg ---

    @Test
    fun newRecurReturnsDaily() {
        val recur = RecurrenceUtils.newRecur()
        assertEquals(Recur.Frequency.DAILY, recur.frequency)
    }

    @Test
    fun newRecurHasNoInterval() {
        val recur = RecurrenceUtils.newRecur()
        // Default interval is -1 (not set) in ical4j
        assertEquals(-1, recur.interval)
    }

    @Test
    fun newRecurHasNoDayList() {
        val recur = RecurrenceUtils.newRecur()
        assertEquals(0, recur.dayList.size)
    }

    // --- newRecur(String) ---

    @Test
    fun newRecurFromString() {
        val recur = RecurrenceUtils.newRecur("FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,FR")
        assertEquals(Recur.Frequency.WEEKLY, recur.frequency)
        assertEquals(2, recur.interval)
        assertEquals(2, recur.dayList.size)
    }

    @Test
    fun newRecurFromStringWithCount() {
        val recur = RecurrenceUtils.newRecur("FREQ=DAILY;COUNT=5")
        assertEquals(Recur.Frequency.DAILY, recur.frequency)
        assertEquals(5, recur.count)
    }

    @Test
    fun newRecurFromStringWithUntil() {
        val recur = RecurrenceUtils.newRecur("FREQ=MONTHLY;UNTIL=20231231")
        assertEquals(Recur.Frequency.MONTHLY, recur.frequency)
        assertNotNull(recur.until)
    }

    @Test
    fun newRecurStripsLegacyRrulePrefix() {
        val recur = RecurrenceUtils.newRecur("RRULE:FREQ=DAILY;INTERVAL=3")
        assertEquals(Recur.Frequency.DAILY, recur.frequency)
        assertEquals(3, recur.interval)
    }

    @Test
    fun newRecurHandlesFreqOnly() {
        val recur = RecurrenceUtils.newRecur("FREQ=YEARLY")
        assertEquals(Recur.Frequency.YEARLY, recur.frequency)
    }

    @Test
    fun newRecurHandlesMinutely() {
        val recur = RecurrenceUtils.newRecur("FREQ=MINUTELY;INTERVAL=15")
        assertEquals(Recur.Frequency.MINUTELY, recur.frequency)
        assertEquals(15, recur.interval)
    }

    @Test
    fun newRecurHandlesHourly() {
        val recur = RecurrenceUtils.newRecur("FREQ=HOURLY;INTERVAL=4")
        assertEquals(Recur.Frequency.HOURLY, recur.frequency)
        assertEquals(4, recur.interval)
    }

    @Test
    fun newRecurSanitizesNegativeCount() {
        val recur = RecurrenceUtils.newRecur("FREQ=DAILY;COUNT=-1")
        // After sanitization, count=-1 should be removed
        assertEquals(-1, recur.count)
    }

    @Test
    fun newRecurSanitizesZeroCount() {
        val recur = RecurrenceUtils.newRecur("FREQ=DAILY;COUNT=0")
        assertEquals(-1, recur.count)
    }

    // --- newRRule ---

    @Test
    fun newRRuleReturnsValidRRule() {
        val rrule = RecurrenceUtils.newRRule("FREQ=WEEKLY")
        assertNotNull(rrule)
        assertEquals(Recur.Frequency.WEEKLY, rrule.recur.frequency)
    }

    @Test
    fun newRRuleStripsRrulePrefix() {
        val rrule = RecurrenceUtils.newRRule("RRULE:FREQ=MONTHLY;INTERVAL=2")
        assertEquals(Recur.Frequency.MONTHLY, rrule.recur.frequency)
        assertEquals(2, rrule.recur.interval)
    }

    @Test
    fun newRRuleWithAllDays() {
        val rrule = RecurrenceUtils.newRRule("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR,SA,SU")
        assertEquals(7, rrule.recur.dayList.size)
    }

    @Test
    fun newRecurWithByDayMonthly() {
        val recur = RecurrenceUtils.newRecur("FREQ=MONTHLY;BYDAY=2TU")
        assertEquals(Recur.Frequency.MONTHLY, recur.frequency)
        assertEquals(1, recur.dayList.size)
        assertEquals(2, recur.dayList[0].offset)
    }

    @Test
    fun newRecurWithByDayLastWeekday() {
        val recur = RecurrenceUtils.newRecur("FREQ=MONTHLY;BYDAY=-1FR")
        assertEquals(1, recur.dayList.size)
        assertEquals(-1, recur.dayList[0].offset)
    }
}
