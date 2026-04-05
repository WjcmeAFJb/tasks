package org.tasks.repeats

import androidx.lifecycle.SavedStateHandle
import net.fortuna.ical4j.model.Recur.Frequency.DAILY
import net.fortuna.ical4j.model.Recur.Frequency.HOURLY
import net.fortuna.ical4j.model.Recur.Frequency.MINUTELY
import net.fortuna.ical4j.model.Recur.Frequency.MONTHLY
import net.fortuna.ical4j.model.Recur.Frequency.SECONDLY
import net.fortuna.ical4j.model.Recur.Frequency.YEARLY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.repeats.CustomRecurrenceActivity.Companion.EXTRA_ACCOUNT_TYPE
import org.tasks.repeats.CustomRecurrenceActivity.Companion.EXTRA_DATE
import org.tasks.repeats.CustomRecurrenceActivity.Companion.EXTRA_RRULE
import org.tasks.time.DateTime
import java.time.DayOfWeek
import java.util.Locale

class CustomRecurrenceViewModelTest {
    @Test
    fun defaultStateValue() {
        val state = newVM().state.value
        assertEquals(CustomRecurrenceViewModel.ViewState(), state)
    }

    @Test
    fun setFrequencies() {
        assertEquals("FREQ=SECONDLY", newVM { setFrequency(SECONDLY) }.getRecur())
        assertEquals("FREQ=MINUTELY", newVM { setFrequency(MINUTELY) }.getRecur())
        assertEquals("FREQ=HOURLY", newVM { setFrequency(HOURLY) }.getRecur())
        assertEquals("FREQ=DAILY", newVM { setFrequency(DAILY) }.getRecur())
        assertEquals("FREQ=WEEKLY", newVM().getRecur())
        assertEquals("FREQ=MONTHLY", newVM { setFrequency(MONTHLY) }.getRecur())
        assertEquals("FREQ=YEARLY", newVM { setFrequency(YEARLY) }.getRecur())
    }

    @Test
    fun setInterval() {
        assertEquals("FREQ=WEEKLY;INTERVAL=4", newVM { setInterval(4) }.getRecur())
    }

    @Test
    fun ignoreCountWhenChangingToNever() {
        assertEquals(
            "FREQ=WEEKLY",
            newVM("FREQ=WEEKLY;COUNT=2") { setEndType(0) }.getRecur()
        )
    }

    @Test
    fun setEndDate() {
        assertEquals(
            "FREQ=WEEKLY;UNTIL=20230726",
            newVM {
                setEndDate(DateTime(2023, 7, 26).millis)
                setEndType(1)
            }.getRecur()
        )
    }

    @Test
    fun ignoreEndDateWhenChangingToNever() {
        assertEquals(
            "FREQ=WEEKLY",
            newVM("FREQ=WEEKLY;UNTIL=20230726") { setEndType(0) }.getRecur()
        )
    }

    @Test
    fun setDaysInOrder() {
        assertEquals(
            "FREQ=WEEKLY;BYDAY=MO,TU,WE",
            newVM {
                toggleDay(DayOfWeek.MONDAY)
                toggleDay(DayOfWeek.WEDNESDAY)
                toggleDay(DayOfWeek.TUESDAY)
            }
                .getRecur()
        )
    }

    @Test
    fun ignoreDaysForNonWeekly() {
        assertEquals(
            "FREQ=MONTHLY",
            newVM {
                setFrequency(MONTHLY)
                toggleDay(DayOfWeek.MONDAY)
            }
                .getRecur()
        )
    }

    @Test
    fun setCount() {
        assertEquals(
            "FREQ=WEEKLY;COUNT=3",
            newVM {
                setEndType(2)
                setOccurrences(3)
            }
                .getRecur()
        )
    }

    @Test
    fun toggleDayOff() {
        assertEquals(
            "FREQ=WEEKLY;BYDAY=MO",
            newVM("FREQ=WEEKLY;BYDAY=MO,TU") { toggleDay(DayOfWeek.TUESDAY) }.getRecur()
        )
    }

    @Test
    fun nthDayOfMonth() {
        assertEquals(
            "FREQ=MONTHLY;BYDAY=4TH",
            newVM(dueDate = DateTime(2023, 7, 27)) {
                setFrequency(MONTHLY)
                setMonthSelection(1)
            }.getRecur()
        )
    }

    @Test
    fun lastDayOfMonth() {
        assertEquals(
            "FREQ=MONTHLY;BYDAY=-1TH",
            newVM(dueDate = DateTime(2023, 7, 27)) {
                setFrequency(MONTHLY)
                setMonthSelection(2)
            }.getRecur()
        )
    }

    @Test
    fun restoreMonthDay() {
        assertEquals(
            "FREQ=MONTHLY;BYDAY=-1TH",
            newVM(
                recur = "FREQ=MONTHLY;BYDAY=-1TH",
                dueDate = DateTime(2023, 7, 27)
            ).getRecur()
        )
    }

    @Test
    fun changeMonthDay() {
        assertEquals(
            "FREQ=MONTHLY;BYDAY=4TH",
            newVM(
                recur = "FREQ=MONTHLY;BYDAY=-1TH",
                dueDate = DateTime(2023, 7, 27)
            ) {
                setMonthSelection(1)
            }.getRecur()
        )
    }

    // --- Mutation-killing tests for init block boundary conditions ---

    @Test
    fun zeroIntervalDefaultsToOne() {
        val state = newVM("FREQ=DAILY;INTERVAL=0").state.value
        assertEquals(1, state.interval)
    }

    @Test
    fun negativeIntervalDefaultsToOne() {
        val state = newVM("FREQ=DAILY;INTERVAL=-5").state.value
        assertEquals(1, state.interval)
    }

    @Test
    fun positiveIntervalPreserved() {
        val state = newVM("FREQ=DAILY;INTERVAL=3").state.value
        assertEquals(3, state.interval)
    }

    @Test
    fun nullRruleGivesDefaultState() {
        val state = newVM().state.value
        assertEquals(1, state.interval)
        assertEquals(net.fortuna.ical4j.model.Recur.Frequency.WEEKLY, state.frequency)
    }

    @Test
    fun blankRruleGivesDefaultState() {
        val state = newVM("").state.value
        assertEquals(net.fortuna.ical4j.model.Recur.Frequency.WEEKLY, state.frequency)
    }

    @Test
    fun countEndSelectionParsed() {
        val state = newVM("FREQ=WEEKLY;COUNT=5").state.value
        assertEquals(2, state.endSelection) // 2 = count
        assertEquals(5, state.endCount)
    }

    @Test
    fun untilEndSelectionParsed() {
        val state = newVM("FREQ=WEEKLY;UNTIL=20230726").state.value
        assertEquals(1, state.endSelection) // 1 = until
    }

    @Test
    fun noEndSelectionParsed() {
        val state = newVM("FREQ=DAILY").state.value
        assertEquals(0, state.endSelection) // 0 = never
    }

    @Test
    fun weeklyDaysParsed() {
        val state = newVM("FREQ=WEEKLY;BYDAY=MO,WE,FR").state.value
        assertEquals(3, state.selectedDays.size)
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), state.selectedDays)
    }

    @Test
    fun nonWeeklyDaysIgnored() {
        val state = newVM("FREQ=DAILY;BYDAY=MO,WE").state.value
        assertEquals(0, state.selectedDays.size)
    }

    @Test
    fun dueDateZeroUsesCurrentTime() {
        val state = newVM(dueDate = DateTime(0)).state.value
        // dueDate should be > 0 (set to startOfDay of current time)
        assert(state.dueDate > 0)
    }

    @Test
    fun dueDatePreserved() {
        val date = DateTime(2023, 8, 15)
        val state = newVM(dueDate = date).state.value
        assertEquals(date.millis, state.dueDate)
    }

    @Test
    fun endCountDefaultsToOneWithNoCount() {
        val state = newVM("FREQ=DAILY").state.value
        assertEquals(1, state.endCount)
    }

    @Test
    fun setOccurrencesUpdatesEndCount() {
        val vm = newVM { setOccurrences(10) }
        assertEquals(10, vm.state.value.endCount)
    }

    @Test
    fun setEndTypeUpdatesSelection() {
        val vm = newVM { setEndType(2) }
        assertEquals(2, vm.state.value.endSelection)
    }

    @Test
    fun setIntervalUpdatesState() {
        val vm = newVM { setInterval(7) }
        assertEquals(7, vm.state.value.interval)
    }

    @Test
    fun toggleDayOnAndOff() {
        val vm = newVM {
            toggleDay(DayOfWeek.FRIDAY)
            toggleDay(DayOfWeek.FRIDAY)
        }
        assertEquals(0, vm.state.value.selectedDays.size)
    }

    @Test
    fun monthDayParsedForMonthly() {
        val state = newVM(
            recur = "FREQ=MONTHLY;BYDAY=2MO",
            dueDate = DateTime(2023, 7, 10)
        ).state.value
        assert(state.monthDay != null)
    }

    @Test
    fun monthDayNullForNonMonthly() {
        val state = newVM("FREQ=DAILY;BYDAY=MO").state.value
        assertEquals(null, state.monthDay)
    }

    @Test
    fun freqAllIncludesMinutely() {
        assert(CustomRecurrenceViewModel.FREQ_ALL.contains(MINUTELY))
    }

    @Test
    fun freqMicrosoftExcludesMinutely() {
        assert(!CustomRecurrenceViewModel.FREQ_MICROSOFT.contains(MINUTELY))
    }

    @Test
    fun freqMicrosoftExcludesHourly() {
        assert(!CustomRecurrenceViewModel.FREQ_MICROSOFT.contains(HOURLY))
    }

    // --- Microsoft task tests (isMicrosoftTask branch) ---

    @Test
    fun microsoftTaskUsesFreqMicrosoft() {
        val state = newVM(recur = "FREQ=DAILY", isMicrosoft = true).state.value
        assertEquals(CustomRecurrenceViewModel.FREQ_MICROSOFT, state.frequencyOptions)
    }

    @Test
    fun microsoftTaskEndSelectionAlwaysZero() {
        val state = newVM(recur = "FREQ=WEEKLY;COUNT=5", isMicrosoft = true).state.value
        assertEquals(0, state.endSelection)
    }

    @Test
    fun microsoftTaskMonthDayNull() {
        val state = newVM(
            recur = "FREQ=MONTHLY;BYDAY=2MO",
            dueDate = DateTime(2023, 7, 10),
            isMicrosoft = true
        ).state.value
        assertEquals(null, state.monthDay)
    }

    @Test
    fun nonMicrosoftTaskEndSelectionFromCount() {
        val state = newVM(recur = "FREQ=WEEKLY;COUNT=5").state.value
        assertEquals(2, state.endSelection)
    }

    @Test
    fun intervalExactlyOnePreserved() {
        val state = newVM("FREQ=DAILY;INTERVAL=1").state.value
        assertEquals(1, state.interval)
    }

    @Test
    fun countZeroGivesEndSelection2() {
        val state = newVM("FREQ=DAILY;COUNT=0").state.value
        assertEquals(2, state.endSelection)
        assertEquals(0, state.endCount)
    }

    @Test
    fun frequencyNotInListDefaultsToWeekly() {
        // SECONDLY is not in FREQ_MICROSOFT
        val state = newVM(recur = "FREQ=SECONDLY", isMicrosoft = true).state.value
        assertEquals(net.fortuna.ical4j.model.Recur.Frequency.WEEKLY, state.frequency)
    }

    @Test
    fun microsoftTaskIsFlaggedTrue() {
        assertTrue(newVM(isMicrosoft = true).state.value.isMicrosoftTask)
    }

    @Test
    fun nonMicrosoftTaskIsFlaggedFalse() {
        assertFalse(newVM().state.value.isMicrosoftTask)
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
                        CustomRecurrenceActivity.EXTRA_ACCOUNT_TYPE,
                        org.tasks.data.entity.CaldavAccount.TYPE_MICROSOFT
                    )
                }
            ),
            locale = Locale.getDefault()
        ).also(block)
}
