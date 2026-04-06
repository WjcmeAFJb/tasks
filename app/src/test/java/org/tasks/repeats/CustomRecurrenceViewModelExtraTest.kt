package org.tasks.repeats

import androidx.lifecycle.SavedStateHandle
import net.fortuna.ical4j.model.Recur.Frequency.DAILY
import net.fortuna.ical4j.model.Recur.Frequency.HOURLY
import net.fortuna.ical4j.model.Recur.Frequency.MINUTELY
import net.fortuna.ical4j.model.Recur.Frequency.MONTHLY
import net.fortuna.ical4j.model.Recur.Frequency.WEEKLY
import net.fortuna.ical4j.model.Recur.Frequency.YEARLY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.repeats.CustomRecurrenceActivity.Companion.EXTRA_ACCOUNT_TYPE
import org.tasks.repeats.CustomRecurrenceActivity.Companion.EXTRA_DATE
import org.tasks.repeats.CustomRecurrenceActivity.Companion.EXTRA_RRULE
import org.tasks.time.DateTime
import java.time.DayOfWeek
import java.util.Locale

class CustomRecurrenceViewModelExtraTest {

    // --- ViewState computed properties ---

    @Test
    fun dueDayOfWeekReturnsCorrectDay() {
        // 2023-07-27 is a Thursday
        val state = newVM(dueDate = DateTime(2023, 7, 27)).state.value
        assertEquals(DayOfWeek.THURSDAY, state.dueDayOfWeek)
    }

    @Test
    fun dueDayOfWeekForMonday() {
        // 2023-07-24 is a Monday
        val state = newVM(dueDate = DateTime(2023, 7, 24)).state.value
        assertEquals(DayOfWeek.MONDAY, state.dueDayOfWeek)
    }

    @Test
    fun dueDayOfWeekForSunday() {
        // 2023-07-30 is a Sunday
        val state = newVM(dueDate = DateTime(2023, 7, 30)).state.value
        assertEquals(DayOfWeek.SUNDAY, state.dueDayOfWeek)
    }

    @Test
    fun dueDayOfMonthReturnsCorrectDay() {
        val state = newVM(dueDate = DateTime(2023, 7, 15)).state.value
        assertEquals(15, state.dueDayOfMonth)
    }

    @Test
    fun dueDayOfMonthForFirstDay() {
        val state = newVM(dueDate = DateTime(2023, 7, 1)).state.value
        assertEquals(1, state.dueDayOfMonth)
    }

    @Test
    fun dueDayOfMonthForLastDay() {
        val state = newVM(dueDate = DateTime(2023, 7, 31)).state.value
        assertEquals(31, state.dueDayOfMonth)
    }

    @Test
    fun nthWeekReturnsCorrectValue() {
        // 2023-07-27 is the 4th Thursday
        val state = newVM(dueDate = DateTime(2023, 7, 27)).state.value
        assertEquals(4, state.nthWeek)
    }

    @Test
    fun nthWeekForFirstWeek() {
        // 2023-07-03 is the 1st Monday
        val state = newVM(dueDate = DateTime(2023, 7, 3)).state.value
        assertEquals(1, state.nthWeek)
    }

    @Test
    fun lastWeekDayOfMonthTrueForLastWeek() {
        // 2023-07-31 is the last Monday (and also the 5th)
        val state = newVM(dueDate = DateTime(2023, 7, 31)).state.value
        assertTrue(state.lastWeekDayOfMonth)
    }

    @Test
    fun lastWeekDayOfMonthFalseForNonLastWeek() {
        // 2023-07-10 is the 2nd Monday
        val state = newVM(dueDate = DateTime(2023, 7, 10)).state.value
        assertFalse(state.lastWeekDayOfMonth)
    }

    // --- getRecur with various combinations ---

    @Test
    fun getRecurWeeklyWithDays() {
        val result = newVM {
            setFrequency(WEEKLY)
            toggleDay(DayOfWeek.MONDAY)
            toggleDay(DayOfWeek.FRIDAY)
        }.getRecur()
        assertEquals("FREQ=WEEKLY;BYDAY=MO,FR", result)
    }

    @Test
    fun getRecurMonthlyWithMonthDay() {
        val result = newVM(dueDate = DateTime(2023, 7, 27)) {
            setFrequency(MONTHLY)
            setMonthSelection(1)
        }.getRecur()
        assertEquals("FREQ=MONTHLY;BYDAY=4TH", result)
    }

    @Test
    fun getRecurMonthlyWithLastDay() {
        val result = newVM(dueDate = DateTime(2023, 7, 27)) {
            setFrequency(MONTHLY)
            setMonthSelection(2)
        }.getRecur()
        assertEquals("FREQ=MONTHLY;BYDAY=-1TH", result)
    }

    @Test
    fun getRecurMonthlyDayOfMonth() {
        val result = newVM(dueDate = DateTime(2023, 7, 27)) {
            setFrequency(MONTHLY)
            setMonthSelection(0)
        }.getRecur()
        assertEquals("FREQ=MONTHLY", result)
    }

    @Test
    fun getRecurWithEndDateAndCount() {
        // Count should be used when endSelection is 2
        val result = newVM {
            setEndType(2)
            setOccurrences(5)
        }.getRecur()
        assertEquals("FREQ=WEEKLY;COUNT=5", result)
    }

    @Test
    fun getRecurCountCoercedToAtLeastOne() {
        val result = newVM {
            setEndType(2)
            setOccurrences(0)
        }.getRecur()
        assertEquals("FREQ=WEEKLY;COUNT=1", result)
    }

    @Test
    fun getRecurWithIntervalOne() {
        // interval=1 should NOT appear in the rrule output
        val result = newVM { setInterval(1) }.getRecur()
        assertFalse(result.contains("INTERVAL"))
    }

    @Test
    fun getRecurWithIntervalTwo() {
        val result = newVM { setInterval(2) }.getRecur()
        assertTrue(result.contains("INTERVAL=2"))
    }

    @Test
    fun getRecurDailyNoExtras() {
        val result = newVM { setFrequency(DAILY) }.getRecur()
        assertEquals("FREQ=DAILY", result)
    }

    @Test
    fun getRecurYearlyNoExtras() {
        val result = newVM { setFrequency(YEARLY) }.getRecur()
        assertEquals("FREQ=YEARLY", result)
    }

    @Test
    fun getRecurMinutelyWithInterval() {
        val result = newVM {
            setFrequency(MINUTELY)
            setInterval(30)
        }.getRecur()
        assertEquals("FREQ=MINUTELY;INTERVAL=30", result)
    }

    @Test
    fun getRecurHourlyWithCount() {
        val result = newVM {
            setFrequency(HOURLY)
            setEndType(2)
            setOccurrences(10)
        }.getRecur()
        assertEquals("FREQ=HOURLY;COUNT=10", result)
    }

    // --- init block parsing ---

    @Test
    fun initParsesWeeklyWithDaysAndInterval() {
        val state = newVM("FREQ=WEEKLY;INTERVAL=3;BYDAY=MO,WE,FR").state.value
        assertEquals(3, state.interval)
        assertEquals(WEEKLY, state.frequency)
        assertEquals(3, state.selectedDays.size)
        assertTrue(state.selectedDays.contains(DayOfWeek.MONDAY))
        assertTrue(state.selectedDays.contains(DayOfWeek.WEDNESDAY))
        assertTrue(state.selectedDays.contains(DayOfWeek.FRIDAY))
    }

    @Test
    fun initParsesMonthlyWithByday() {
        val state = newVM(
            recur = "FREQ=MONTHLY;BYDAY=3WE",
            dueDate = DateTime(2023, 7, 19)
        ).state.value
        assertEquals(MONTHLY, state.frequency)
        assertNotNull(state.monthDay)
        assertEquals(3, state.monthDay!!.offset)
    }

    @Test
    fun initParsesEndUntil() {
        val state = newVM("FREQ=DAILY;UNTIL=20231231").state.value
        assertEquals(1, state.endSelection)
    }

    @Test
    fun initParsesEndCount() {
        val state = newVM("FREQ=DAILY;COUNT=10").state.value
        assertEquals(2, state.endSelection)
        assertEquals(10, state.endCount)
    }

    @Test
    fun initParsesNoEnd() {
        val state = newVM("FREQ=DAILY").state.value
        assertEquals(0, state.endSelection)
    }

    // --- setFrequency ---

    @Test
    fun setFrequencyUpdatesState() {
        val vm = newVM()
        vm.setFrequency(DAILY)
        assertEquals(DAILY, vm.state.value.frequency)
        vm.setFrequency(MONTHLY)
        assertEquals(MONTHLY, vm.state.value.frequency)
    }

    // --- setEndDate ---

    @Test
    fun setEndDateUpdatesState() {
        val vm = newVM()
        val date = DateTime(2024, 1, 1).millis
        vm.setEndDate(date)
        assertEquals(date, vm.state.value.endDate)
    }

    // --- toggleDay ---

    @Test
    fun toggleDayAddsAndRemoves() {
        val vm = newVM()
        vm.toggleDay(DayOfWeek.TUESDAY)
        assertTrue(vm.state.value.selectedDays.contains(DayOfWeek.TUESDAY))
        vm.toggleDay(DayOfWeek.TUESDAY)
        assertFalse(vm.state.value.selectedDays.contains(DayOfWeek.TUESDAY))
    }

    @Test
    fun toggleMultipleDays() {
        val vm = newVM()
        vm.toggleDay(DayOfWeek.MONDAY)
        vm.toggleDay(DayOfWeek.WEDNESDAY)
        vm.toggleDay(DayOfWeek.FRIDAY)
        assertEquals(3, vm.state.value.selectedDays.size)
    }

    // --- setMonthSelection ---

    @Test
    fun setMonthSelectionZeroClearsMonthDay() {
        val vm = newVM(dueDate = DateTime(2023, 7, 27)) {
            setFrequency(MONTHLY)
            setMonthSelection(1) // set it first
        }
        assertNotNull(vm.state.value.monthDay)
        vm.setMonthSelection(0)
        assertNull(vm.state.value.monthDay)
    }

    @Test(expected = IllegalArgumentException::class)
    fun setMonthSelectionInvalidThrows() {
        newVM(dueDate = DateTime(2023, 7, 27)) {
            setFrequency(MONTHLY)
            setMonthSelection(3) // invalid
        }
    }

    // --- Microsoft task constraints ---

    @Test
    fun microsoftEndSelectionAlwaysZeroEvenWithUntil() {
        val state = newVM(
            recur = "FREQ=DAILY;UNTIL=20231231",
            isMicrosoft = true
        ).state.value
        assertEquals(0, state.endSelection)
    }

    @Test
    fun microsoftFrequencyOptionsExcludeMinutelyAndHourly() {
        val state = newVM(isMicrosoft = true).state.value
        assertFalse(state.frequencyOptions.contains(MINUTELY))
        assertFalse(state.frequencyOptions.contains(HOURLY))
        assertTrue(state.frequencyOptions.contains(DAILY))
        assertTrue(state.frequencyOptions.contains(WEEKLY))
        assertTrue(state.frequencyOptions.contains(MONTHLY))
        assertTrue(state.frequencyOptions.contains(YEARLY))
    }

    @Test
    fun microsoftMonthDayAlwaysNull() {
        val state = newVM(
            recur = "FREQ=MONTHLY;BYDAY=2TU",
            dueDate = DateTime(2023, 7, 11),
            isMicrosoft = true
        ).state.value
        assertNull(state.monthDay)
    }

    @Test
    fun microsoftInvalidFrequencyDefaultsToWeekly() {
        val state = newVM(
            recur = "FREQ=MINUTELY;INTERVAL=5",
            isMicrosoft = true
        ).state.value
        assertEquals(WEEKLY, state.frequency)
    }

    // --- FREQ_ALL and FREQ_MICROSOFT lists ---

    @Test
    fun freqAllContainsAllSixFrequencies() {
        assertEquals(6, CustomRecurrenceViewModel.FREQ_ALL.size)
        assertTrue(CustomRecurrenceViewModel.FREQ_ALL.contains(MINUTELY))
        assertTrue(CustomRecurrenceViewModel.FREQ_ALL.contains(HOURLY))
        assertTrue(CustomRecurrenceViewModel.FREQ_ALL.contains(DAILY))
        assertTrue(CustomRecurrenceViewModel.FREQ_ALL.contains(WEEKLY))
        assertTrue(CustomRecurrenceViewModel.FREQ_ALL.contains(MONTHLY))
        assertTrue(CustomRecurrenceViewModel.FREQ_ALL.contains(YEARLY))
    }

    @Test
    fun freqMicrosoftContainsFourFrequencies() {
        assertEquals(4, CustomRecurrenceViewModel.FREQ_MICROSOFT.size)
        assertTrue(CustomRecurrenceViewModel.FREQ_MICROSOFT.contains(DAILY))
        assertTrue(CustomRecurrenceViewModel.FREQ_MICROSOFT.contains(WEEKLY))
        assertTrue(CustomRecurrenceViewModel.FREQ_MICROSOFT.contains(MONTHLY))
        assertTrue(CustomRecurrenceViewModel.FREQ_MICROSOFT.contains(YEARLY))
    }

    // --- days of week ordering ---

    @Test
    fun daysOfWeekHasSevenEntries() {
        val state = newVM().state.value
        assertEquals(7, state.daysOfWeek.size)
    }

    @Test
    fun selectedDaysEmptyByDefault() {
        val state = newVM().state.value
        assertEquals(0, state.selectedDays.size)
    }

    // --- getRecur weekday ordering ---

    @Test
    fun getRecurSortsDays() {
        val result = newVM {
            toggleDay(DayOfWeek.FRIDAY)
            toggleDay(DayOfWeek.MONDAY)
            toggleDay(DayOfWeek.WEDNESDAY)
        }.getRecur()
        assertEquals("FREQ=WEEKLY;BYDAY=MO,WE,FR", result)
    }

    @Test
    fun getRecurAllDays() {
        val result = newVM {
            toggleDay(DayOfWeek.SUNDAY)
            toggleDay(DayOfWeek.MONDAY)
            toggleDay(DayOfWeek.TUESDAY)
            toggleDay(DayOfWeek.WEDNESDAY)
            toggleDay(DayOfWeek.THURSDAY)
            toggleDay(DayOfWeek.FRIDAY)
            toggleDay(DayOfWeek.SATURDAY)
        }.getRecur()
        assertEquals("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR,SA,SU", result)
    }

    private fun newVM(
        recur: String? = null,
        dueDate: DateTime = DateTime(0),
        isMicrosoft: Boolean = false,
        block: CustomRecurrenceViewModel.() -> Unit = {}
    ) =
        CustomRecurrenceViewModel(
            savedStateHandle = SavedStateHandle(
                buildMap {
                    put(EXTRA_RRULE, recur)
                    put(EXTRA_DATE, dueDate.millis)
                    if (isMicrosoft) put(
                        EXTRA_ACCOUNT_TYPE,
                        org.tasks.data.entity.CaldavAccount.TYPE_MICROSOFT
                    )
                }
            ),
            locale = Locale.US
        ).also(block)
}
