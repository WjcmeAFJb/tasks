package org.tasks.repeats

import android.content.Context
import android.content.res.Resources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.tasks.R
import org.tasks.analytics.Firebase
import java.util.Locale

/**
 * Mutation-killing tests for [RepeatRuleToString].
 *
 * Targets SURVIVED PIT mutations: conditional boundary changes on interval,
 * removed conditionals on frequency/dayList checks, and void method call removals.
 */
class RepeatRuleToStringMutationTest {

    private lateinit var context: Context
    private lateinit var resources: Resources
    private lateinit var firebase: Firebase
    private lateinit var formatter: RepeatRuleToString

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        resources = mock(Resources::class.java)
        firebase = mock(Firebase::class.java)
        `when`(context.resources).thenReturn(resources)

        // --- single-frequency strings ---
        `when`(context.getString(R.string.repeats_minutely)).thenReturn("minutely")
        `when`(context.getString(R.string.repeats_hourly)).thenReturn("hourly")
        `when`(context.getString(R.string.repeats_daily)).thenReturn("daily")
        `when`(context.getString(R.string.repeats_weekly)).thenReturn("weekly")
        `when`(context.getString(R.string.repeats_monthly)).thenReturn("monthly")
        `when`(context.getString(R.string.repeats_yearly)).thenReturn("yearly")

        // --- "Repeats %s" ---
        `when`(context.getString(eq(R.string.repeats_single), anyString()))
            .thenAnswer { "Repeats ${it.arguments[1]}" }

        // --- "Repeats every %s" ---
        `when`(context.getString(eq(R.string.repeats_plural), anyString()))
            .thenAnswer { "Repeats every ${it.arguments[1]}" }

        // --- "Repeats %1$s on %2$s" ---
        `when`(context.getString(eq(R.string.repeats_single_on), anyString(), anyString()))
            .thenAnswer { "Repeats ${it.arguments[1]} on ${it.arguments[2]}" }

        // --- "Repeats every %1$s on %2$s" ---
        `when`(context.getString(eq(R.string.repeats_plural_on), anyString(), anyString()))
            .thenAnswer { "Repeats every ${it.arguments[1]} on ${it.arguments[2]}" }

        // --- "Repeats %1$s, occurs %2$d %3$s" ---
        `when`(context.getString(eq(R.string.repeats_single_number_of_times), anyString(), anyInt(), anyString()))
            .thenAnswer { "Repeats ${it.arguments[1]}, occurs ${it.arguments[2]} ${it.arguments[3]}" }

        // --- "Repeats every %1$s, occurs %2$d %3$s" ---
        `when`(context.getString(eq(R.string.repeats_plural_number_of_times), anyString(), anyInt(), anyString()))
            .thenAnswer { "Repeats every ${it.arguments[1]}, occurs ${it.arguments[2]} ${it.arguments[3]}" }

        // --- "Repeats %1$s on %2$s, occurs %3$d %4$s" ---
        `when`(context.getString(eq(R.string.repeats_single_on_number_of_times), anyString(), anyString(), anyInt(), anyString()))
            .thenAnswer { "Repeats ${it.arguments[1]} on ${it.arguments[2]}, occurs ${it.arguments[3]} ${it.arguments[4]}" }

        // --- "Repeats every %1$s on %2$s, occurs %3$d %4$s" ---
        `when`(context.getString(eq(R.string.repeats_plural_on_number_of_times), anyString(), anyString(), anyInt(), anyString()))
            .thenAnswer { "Repeats every ${it.arguments[1]} on ${it.arguments[2]}, occurs ${it.arguments[3]} ${it.arguments[4]}" }

        // --- "Repeats %1$s, ends on %2$s" ---
        `when`(context.getString(eq(R.string.repeats_single_until), anyString(), anyString()))
            .thenAnswer { "Repeats ${it.arguments[1]}, ends on ${it.arguments[2]}" }

        // --- "Repeats every %1$s, ends on %2$s" ---
        `when`(context.getString(eq(R.string.repeats_plural_until), anyString(), anyString()))
            .thenAnswer { "Repeats every ${it.arguments[1]}, ends on ${it.arguments[2]}" }

        // --- "Repeats %1$s on %2$s, ends on %3$s" ---
        `when`(context.getString(eq(R.string.repeats_single_on_until), anyString(), anyString(), anyString()))
            .thenAnswer { "Repeats ${it.arguments[1]} on ${it.arguments[2]}, ends on ${it.arguments[3]}" }

        // --- "Repeats every %1$s on %2$s, ends on %3$s" ---
        `when`(context.getString(eq(R.string.repeats_plural_on_until), anyString(), anyString(), anyString()))
            .thenAnswer { "Repeats every ${it.arguments[1]} on ${it.arguments[2]}, ends on ${it.arguments[3]}" }

        // --- list separator ---
        `when`(context.getString(R.string.list_separator_with_space)).thenReturn(", ")

        // --- plurals for frequency intervals ---
        `when`(resources.getQuantityString(eq(R.plurals.repeat_n_minutes), anyInt(), anyInt()))
            .thenAnswer { "${it.arguments[2]} minutes" }
        `when`(resources.getQuantityString(eq(R.plurals.repeat_n_hours), anyInt(), anyInt()))
            .thenAnswer { "${it.arguments[2]} hours" }
        `when`(resources.getQuantityString(eq(R.plurals.repeat_n_days), anyInt(), anyInt()))
            .thenAnswer { "${it.arguments[2]} days" }
        `when`(resources.getQuantityString(eq(R.plurals.repeat_n_weeks), anyInt(), anyInt()))
            .thenAnswer { "${it.arguments[2]} weeks" }
        `when`(resources.getQuantityString(eq(R.plurals.repeat_n_months), anyInt(), anyInt()))
            .thenAnswer { "${it.arguments[2]} months" }
        `when`(resources.getQuantityString(eq(R.plurals.repeat_n_years), anyInt(), anyInt()))
            .thenAnswer { "${it.arguments[2]} years" }

        // --- plurals for count ("time"/"times") ---
        `when`(resources.getQuantityString(eq(R.plurals.repeat_times), anyInt()))
            .thenAnswer { if (it.arguments[1] as Int == 1) "time" else "times" }

        // --- monthly week names ---
        `when`(context.getString(R.string.repeat_monthly_first_week)).thenReturn("first")
        `when`(context.getString(R.string.repeat_monthly_second_week)).thenReturn("second")
        `when`(context.getString(R.string.repeat_monthly_third_week)).thenReturn("third")
        `when`(context.getString(R.string.repeat_monthly_fourth_week)).thenReturn("fourth")
        `when`(context.getString(R.string.repeat_monthly_fifth_week)).thenReturn("fifth")
        `when`(context.getString(R.string.repeat_monthly_last_week)).thenReturn("last")

        // --- "every %1$s %2$s" for monthly day-of-nth-week ---
        `when`(context.getString(eq(R.string.repeat_monthly_every_day_of_nth_week), anyString(), anyString()))
            .thenAnswer { "every ${it.arguments[1]} ${it.arguments[2]}" }

        formatter = RepeatRuleToString(context, Locale.US, firebase)
    }

    // ================================================================
    // ConditionalsBoundaryMutator on interval <= 1 (line 46)
    // Mutation: changed `<= 1` to `< 1` — interval 1 takes plural path
    // ================================================================

    @Test
    fun intervalOneUsesSingleForm() {
        // Kills: ConditionalsBoundaryMutator at line 46 (interval <= 1 -> < 1)
        // Also kills: RemoveConditionalMutator_ORDER_IF at line 46
        // Also kills: VoidMethodCallMutator on checkNotNull at lines 46, 48
        // With interval=1, should use singular ("Repeats daily"), not plural ("Repeats every 1 days")
        val result = formatter.toString("RRULE:FREQ=DAILY;INTERVAL=1")
        assertEquals("Repeats daily", result)
    }

    @Test
    fun intervalZeroUsesSingleForm() {
        // Kills: boundary mutation — interval 0 (ical4j default) should also use single form
        // Also kills: VoidMethodCallMutator on checkNotNull at line 48
        val result = formatter.toString("RRULE:FREQ=DAILY")
        assertEquals("Repeats daily", result)
    }

    @Test
    fun intervalTwoUsesPluralForm() {
        // Kills: ConditionalsBoundaryMutator at line 46
        // interval=2 must use plural ("Repeats every 2 days"), not singular
        val result = formatter.toString("RRULE:FREQ=DAILY;INTERVAL=2")
        assertEquals("Repeats every 2 days", result)
    }

    @Test
    fun singleVsPluralAreDifferent() {
        // Kills: mutations that make interval=1 and interval=2 produce same output
        val single = formatter.toString("RRULE:FREQ=DAILY;INTERVAL=1")
        val plural = formatter.toString("RRULE:FREQ=DAILY;INTERVAL=2")
        assertNotEquals("Single and plural forms must differ", single, plural)
    }

    // ================================================================
    // RemoveConditionalMutator_EQUAL_IF on frequency == WEEKLY for dayList (line 49)
    // Mutation: replaced equality check with true — always enters the day branch
    // ================================================================

    @Test
    fun dailyFrequencyDoesNotShowDays() {
        // Kills: RemoveConditionalMutator_EQUAL_IF at line 49 (frequency==WEEKLY always true)
        // Daily frequency should NOT include "on ..." even if mutation makes it enter day branch
        val result = formatter.toString("RRULE:FREQ=DAILY;INTERVAL=1")
        assertEquals("Repeats daily", result)
        // Should NOT contain "on" since it's daily
        assert(!result!!.contains(" on ")) { "Daily should not have 'on' day string" }
    }

    @Test
    fun weeklyWithDaysShowsDayNames() {
        // Kills: RemoveConditionalMutator_EQUAL_ELSE at line 49
        // Weekly with BYDAY must show the day names
        val result = formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE")
        assertEquals("Repeats weekly on Mon, Wed", result)
    }

    @Test
    fun weeklyWithoutDaysDoesNotShowOnClause() {
        // Kills: RemoveConditionalMutator on !dayList.isEmpty() check
        val result = formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=1")
        assertEquals("Repeats weekly", result)
    }

    // ================================================================
    // ConditionalsBoundaryMutator on count > 0 (line 52)
    // Mutation: changed `> 0` to `>= 0` — count=0 would enter count branch
    // ================================================================

    @Test
    fun singleIntervalNoCountNoUntil() {
        // Kills: ConditionalsBoundaryMutator at line 52 (count > 0 -> >= 0)
        // count=0 (no count) should NOT trigger the count branch
        val result = formatter.toString("RRULE:FREQ=DAILY;INTERVAL=1")
        assertEquals("Repeats daily", result)
        // No "occurs" in the output
        assert(!result!!.contains("occurs")) { "No count should not have 'occurs'" }
    }

    @Test
    fun singleIntervalWithCountShowsOccurs() {
        // Kills: positive boundary for count > 0
        val result = formatter.toString("RRULE:FREQ=DAILY;INTERVAL=1;COUNT=3")
        assertEquals("Repeats daily, occurs 3 times", result)
    }

    // ================================================================
    // ConditionalsBoundaryMutator on count > 0 (line 68)
    // Same mutation but in the singular branch without days
    // ================================================================

    @Test
    fun singleIntervalWithCountNoDays() {
        // Kills: ConditionalsBoundaryMutator at line 68 (interval<=1, no days, count > 0)
        val result = formatter.toString("RRULE:FREQ=HOURLY;INTERVAL=1;COUNT=2")
        assertEquals("Repeats hourly, occurs 2 times", result)
    }

    // ================================================================
    // ConditionalsBoundaryMutator on count > 0 in plural branch (line 86)
    // Mutation: changed boundary in plural path
    // ================================================================

    @Test
    fun pluralIntervalWithCountShowsOccurs() {
        // Kills: ConditionalsBoundaryMutator at line 86 (count > 0 in plural path)
        val result = formatter.toString("RRULE:FREQ=DAILY;INTERVAL=3;COUNT=5")
        assertEquals("Repeats every 3 days, occurs 5 times", result)
    }

    @Test
    fun pluralIntervalNoCount() {
        // Kills: boundary — count=0 in plural path should NOT trigger count display
        val result = formatter.toString("RRULE:FREQ=DAILY;INTERVAL=3")
        assertEquals("Repeats every 3 days", result)
        assert(!result!!.contains("occurs")) { "No count should not show 'occurs'" }
    }

    // ================================================================
    // ConditionalsBoundaryMutator on count > 0 in plural+days branch (line 102)
    // ================================================================

    @Test
    fun pluralIntervalWithDaysAndCount() {
        // Kills: ConditionalsBoundaryMutator at line 102
        val result = formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,FR;COUNT=10")
        assertEquals("Repeats every 2 weeks on Mon, Fri, occurs 10 times", result)
    }

    @Test
    fun pluralIntervalWithDaysNoCount() {
        // Kills: boundary — plural+days without count
        val result = formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=2;BYDAY=TU,TH")
        assertEquals("Repeats every 2 weeks on Tue, Thu", result)
        assert(!result!!.contains("occurs")) { "No count in plural+days" }
    }

    // ================================================================
    // RemoveConditionalMutator_EQUAL_IF on frequency == MONTHLY (line 59)
    // ================================================================

    @Test
    fun monthlyFrequencyWithDaysShowsNthWeekday() {
        // Kills: RemoveConditionalMutator_EQUAL_IF at line 59 (frequency == MONTHLY check)
        val result = formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=2TU")
        assertEquals("Repeats monthly on every second Tuesday", result)
    }

    @Test
    fun yearlyFrequencyDoesNotShowDayString() {
        // Kills: mutation that makes frequency==WEEKLY/MONTHLY always true for yearly
        val result = formatter.toString("RRULE:FREQ=YEARLY;INTERVAL=1")
        assertEquals("Repeats yearly", result)
    }

    // ================================================================
    // RemoveConditionalMutator_EQUAL_ELSE on repeatUntil == null (line 49)
    // ================================================================

    @Test
    fun singleIntervalWithDaysNoUntilNoCount() {
        // Kills: RemoveConditionalMutator_EQUAL_ELSE at line 49 (repeatUntil null check)
        val result = formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=MO")
        assertEquals("Repeats weekly on Mon", result)
        assert(!result!!.contains("ends on")) { "No until should not show 'ends on'" }
    }

    // ================================================================
    // RemoveConditionalMutator_EQUAL_IF at line 71 (repeatUntil == null in singular no-day path)
    // ================================================================

    @Test
    fun singleIntervalNoUntilNoCountNoDays() {
        // Kills: RemoveConditionalMutator_EQUAL_IF at line 71
        val result = formatter.toString("RRULE:FREQ=DAILY;INTERVAL=1")
        assertEquals("Repeats daily", result)
    }

    // ================================================================
    // RemoveConditionalMutator_EQUAL_ELSE on repeatUntil == null in plural (line 83)
    // ================================================================

    @Test
    fun pluralIntervalNoUntilNoCountNoDays() {
        // Kills: RemoveConditionalMutator_EQUAL_ELSE at line 83
        val result = formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=3")
        assertEquals("Repeats every 3 weeks", result)
    }

    @Test
    fun pluralIntervalNoUntilNoDays() {
        // Kills: RemoveConditionalMutator_EQUAL_IF at line 83
        val result = formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=4")
        assertEquals("Repeats every 4 months", result)
    }

    // ================================================================
    // RemoveConditionalMutator_EQUAL_IF at line 93 (repeatUntil null in plural+day path)
    // ================================================================

    @Test
    fun pluralIntervalWithDaysNoUntilNoCount() {
        // Kills: RemoveConditionalMutator_EQUAL_IF at line 93
        val result = formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=2;BYDAY=SA")
        assertEquals("Repeats every 2 weeks on Sat", result)
        assert(!result!!.contains("ends on")) { "Should not have 'ends on'" }
    }

    // ================================================================
    // RemoveConditionalMutator_EQUAL_IF at line 105 (repeatUntil null in plural no-day path)
    // ================================================================

    @Test
    fun pluralIntervalNoDaysNoUntilNoCount() {
        // Kills: RemoveConditionalMutator_EQUAL_IF at line 105
        val result = formatter.toString("RRULE:FREQ=YEARLY;INTERVAL=2")
        assertEquals("Repeats every 2 years", result)
    }

    // ================================================================
    // getDayString — WEEKLY branch (lines 122-128)
    // Mutations: VoidMethodCallMutator on checkNotNullExpressionValue (lines 125, 128)
    //            RemoveConditionalMutator_EQUAL_IF on frequency==WEEKLY (line 129)
    // ================================================================

    @Test
    fun weeklyDayStringContainsCorrectDayNames() {
        // Kills: VoidMethodCallMutator on checkNotNullExpressionValue at lines 125, 128
        // Verifies that the short weekday names are correctly resolved
        val result = formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE,FR")
        assertNotNull(result)
        assert(result!!.contains("Mon")) { "Should contain Mon" }
        assert(result.contains("Wed")) { "Should contain Wed" }
        assert(result.contains("Fri")) { "Should contain Fri" }
    }

    @Test
    fun weeklyDayStringUsesListSeparator() {
        // Kills: mutations affecting the joinToString separator
        val result = formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=TU,TH,SA")
        assertNotNull(result)
        assert(result!!.contains("Tue, Thu, Sat")) { "Should use ', ' separator: $result" }
    }

    @Test
    fun weeklySingleDayNoSeparator() {
        // Kills: edge case for single day (no separator needed)
        val result = formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=SU")
        assertEquals("Repeats weekly on Sun", result)
    }

    // ================================================================
    // getDayString — MONTHLY branch (lines 129-146)
    // Mutations: RemoveConditionalMutator_EQUAL_IF on frequency==MONTHLY (line 129)
    //            VoidMethodCallMutator on checkNotNullExpressionValue (lines 134, 135)
    //            VoidMethodCallMutator on checkNotNull (lines 136, 141)
    // ================================================================

    @Test
    fun monthlyDayStringFirstWeekday() {
        // Kills: VoidMethodCallMutator on checkNotNullExpressionValue (lines 134, 135)
        // Also kills: VoidMethodCallMutator on checkNotNull (line 141)
        val result = formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=1MO")
        assertEquals("Repeats monthly on every first Monday", result)
    }

    @Test
    fun monthlyDayStringSecondWeekday() {
        // Kills: array index for NTH_WEEK[offset-1] with offset=2
        val result = formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=2TU")
        assertEquals("Repeats monthly on every second Tuesday", result)
    }

    @Test
    fun monthlyDayStringThirdWeekday() {
        // Kills: array index for NTH_WEEK[offset-1] with offset=3
        val result = formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=3WE")
        assertEquals("Repeats monthly on every third Wednesday", result)
    }

    @Test
    fun monthlyDayStringFourthWeekday() {
        // Kills: array index for NTH_WEEK[offset-1] with offset=4
        val result = formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=4TH")
        assertEquals("Repeats monthly on every fourth Thursday", result)
    }

    @Test
    fun monthlyDayStringFifthWeekday() {
        // Kills: array index for NTH_WEEK[offset-1] with offset=5
        val result = formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=5FR")
        assertEquals("Repeats monthly on every fifth Friday", result)
    }

    @Test
    fun monthlyDayStringLastWeekday() {
        // Kills: VoidMethodCallMutator on checkNotNull (line 136)
        // Also kills: RemoveConditionalMutator_EQUAL_IF at line 129
        val result = formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=-1SU")
        assertEquals("Repeats monthly on every last Sunday", result)
    }

    @Test
    fun monthlyLastVsFirstAreDifferent() {
        // Kills: mutation that confuses offset==-1 with positive offset
        val last = formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=-1MO")
        val first = formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=1MO")
        assertNotEquals("Last and first should be different", last, first)
    }

    // ================================================================
    // Monthly plural interval with BYDAY
    // ================================================================

    @Test
    fun monthlyPluralWithDay() {
        // Kills: plural path for monthly with day string
        val result = formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=3;BYDAY=2WE")
        assertEquals("Repeats every 3 months on every second Wednesday", result)
    }

    // ================================================================
    // COUNT with various combinations
    // ================================================================

    @Test
    fun singleWeeklyWithDaysAndCount() {
        // Kills: ConditionalsBoundaryMutator at line 52 for weekly+days+count
        val result = formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=MO;COUNT=4")
        assertEquals("Repeats weekly on Mon, occurs 4 times", result)
    }

    @Test
    fun singleMonthlyWithDayAndCount() {
        // Kills: count branch in singular+day path for monthly
        val result = formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=1TH;COUNT=6")
        assertEquals("Repeats monthly on every first Thursday, occurs 6 times", result)
    }

    @Test
    fun pluralMonthlyWithDayAndCount() {
        // Kills: ConditionalsBoundaryMutator at line 86 for monthly plural+day+count
        val result = formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=2;BYDAY=3FR;COUNT=12")
        assertEquals("Repeats every 2 months on every third Friday, occurs 12 times", result)
    }

    // ================================================================
    // Null and blank string handling (line 38-39)
    // Kills: RemoveConditionalMutator_EQUAL_IF on null check (line 44)
    // ================================================================

    @Test
    fun nullStringReturnsNull() {
        // Kills: RemoveConditionalMutator_EQUAL_IF at line 44
        assertNull(formatter.toString(null as String?))
    }

    @Test
    fun emptyStringReturnsNull() {
        // Kills: isNotBlank check
        assertNull(formatter.toString(""))
    }

    @Test
    fun blankStringReturnsNull() {
        assertNull(formatter.toString("   "))
    }

    // ================================================================
    // VoidMethodCallMutator on checkNotNull at line 111
    // ================================================================

    @Test
    fun toStringReturnsNonNullForValidRRule() {
        // Kills: VoidMethodCallMutator on checkNotNull at line 111
        val result = formatter.toString("RRULE:FREQ=MINUTELY;INTERVAL=5")
        assertNotNull(result)
        assertEquals("Repeats every 5 minutes", result)
    }

    // ================================================================
    // VoidMethodCallMutator on checkNotNull at lines 81, 82
    // ================================================================

    @Test
    fun pluralPathUsesQuantityString() {
        // Kills: VoidMethodCallMutator on checkNotNull at line 81
        // Also kills: VoidMethodCallMutator on checkNotNullExpressionValue at line 82
        val result = formatter.toString("RRULE:FREQ=HOURLY;INTERVAL=4")
        assertEquals("Repeats every 4 hours", result)
    }

    // ================================================================
    // All six frequency types produce distinct outputs for singular
    // ================================================================

    @Test
    fun allSixSingularFrequencies() {
        // Kills: mutations that swap frequency string lookups
        assertEquals("Repeats minutely", formatter.toString("RRULE:FREQ=MINUTELY"))
        assertEquals("Repeats hourly", formatter.toString("RRULE:FREQ=HOURLY"))
        assertEquals("Repeats daily", formatter.toString("RRULE:FREQ=DAILY"))
        assertEquals("Repeats weekly", formatter.toString("RRULE:FREQ=WEEKLY"))
        assertEquals("Repeats monthly", formatter.toString("RRULE:FREQ=MONTHLY"))
        assertEquals("Repeats yearly", formatter.toString("RRULE:FREQ=YEARLY"))
    }

    // ================================================================
    // All six frequency types for plural path
    // ================================================================

    @Test
    fun allSixPluralFrequencies() {
        // Kills: mutations in getFrequencyPlural that swap plural resource IDs
        assertEquals("Repeats every 2 minutes", formatter.toString("RRULE:FREQ=MINUTELY;INTERVAL=2"))
        assertEquals("Repeats every 2 hours", formatter.toString("RRULE:FREQ=HOURLY;INTERVAL=2"))
        assertEquals("Repeats every 2 days", formatter.toString("RRULE:FREQ=DAILY;INTERVAL=2"))
        assertEquals("Repeats every 2 weeks", formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=2"))
        assertEquals("Repeats every 2 months", formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=2"))
        assertEquals("Repeats every 2 years", formatter.toString("RRULE:FREQ=YEARLY;INTERVAL=2"))
    }

    // ================================================================
    // Weekly plural path with days (dayList not empty in plural branch)
    // ================================================================

    @Test
    fun pluralWeeklyWithMultipleDays() {
        // Kills: dayList.isEmpty() check in plural branch (line 83)
        val result = formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=4;BYDAY=MO,WE,FR")
        assertEquals("Repeats every 4 weeks on Mon, Wed, Fri", result)
    }

    // ================================================================
    // Edge case: monthly plural with last weekday
    // ================================================================

    @Test
    fun monthlyPluralWithLastWeekday() {
        // Kills: RemoveConditionalMutator on offset==-1 check in getDayString
        val result = formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=2;BYDAY=-1SA")
        assertEquals("Repeats every 2 months on every last Saturday", result)
    }
}
