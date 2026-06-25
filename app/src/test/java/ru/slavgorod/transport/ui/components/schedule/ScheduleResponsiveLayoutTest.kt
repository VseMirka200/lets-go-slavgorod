package ru.slavgorod.transport.ui.components.schedule

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleResponsiveLayoutTest {

    @Test
    fun `resolveScheduleResponsiveLayout returns tight spec for narrow containers`() {
        val spec = resolveScheduleResponsiveLayout(160.dp)

        assertEquals(ScheduleResponsiveLayoutMode.TIGHT, spec.mode)
        assertTrue(spec.card.stackCardContent)
        assertEquals(1.dp, spec.section.sectionColumnSpacing)
        assertEquals(8.dp, spec.card.cardHorizontalPadding)
        assertEquals(8.dp, spec.card.cardVerticalPadding)
        assertEquals(52.dp, spec.card.departureTimeColumnWidth)
        assertEquals(19f, spec.card.departureTimeFontSize.value, 0.001f)
        assertEquals(13f, spec.card.supportingTextFontSize.value, 0.001f)
        assertEquals(16f, spec.header.headerTitleFontSize.value, 0.001f)
        assertEquals(11f, spec.header.headerSubtitleFontSize.value, 0.001f)
        assertEquals(20.dp, spec.upcoming.upcomingMinRowHeight)
        assertEquals(8.dp, spec.upcoming.upcomingCardContentPadding)
        assertEquals(4.dp, spec.upcoming.upcomingCardTopPadding)
        assertEquals(8.dp, spec.upcoming.upcomingCardBottomPadding)
        assertEquals(4.dp, spec.upcoming.upcomingBottomSpacing)
    }

    @Test
    fun `resolveScheduleResponsiveLayout returns compact spec for medium containers`() {
        val spec = resolveScheduleResponsiveLayout(320.dp)

        assertEquals(ScheduleResponsiveLayoutMode.COMPACT, spec.mode)
        assertFalse(spec.card.stackCardContent)
        assertEquals(10.181818f, spec.card.cardHorizontalPadding.value, 0.001f)
        assertEquals(7.4545455f, spec.section.sectionVerticalSpacing.value, 0.001f)
        assertEquals(2.8181818f, spec.section.sectionColumnSpacing.value, 0.001f)
        assertEquals(57.81818f, spec.card.departureTimeColumnWidth.value, 0.001f)
        assertEquals(20.818182f, spec.card.departureTimeFontSize.value, 0.001f)
        assertEquals(14.090909f, spec.card.supportingTextFontSize.value, 0.001f)
        assertEquals(17.09091f, spec.header.headerTitleFontSize.value, 0.001f)
        assertEquals(12.090909f, spec.header.headerSubtitleFontSize.value, 0.001f)
        assertEquals(9.454545f, spec.upcoming.upcomingCardContentPadding.value, 0.001f)
        assertEquals(5.4545455f, spec.upcoming.upcomingCardTopPadding.value, 0.001f)
        assertEquals(9.454545f, spec.upcoming.upcomingCardBottomPadding.value, 0.001f)
        assertEquals(5.4545455f, spec.upcoming.upcomingBottomSpacing.value, 0.001f)
    }

    @Test
    fun `resolveScheduleResponsiveLayout returns regular spec for wide containers`() {
        val spec = resolveScheduleResponsiveLayout(600.dp)

        assertEquals(ScheduleResponsiveLayoutMode.REGULAR, spec.mode)
        assertFalse(spec.card.stackCardContent)
        assertEquals(16.dp, spec.section.sectionHorizontalPadding)
        assertEquals(6.dp, spec.section.sectionColumnSpacing)
        assertEquals(14.dp, spec.card.cardHorizontalPadding)
        assertEquals(12.dp, spec.card.cardVerticalPadding)
        assertEquals(68.dp, spec.card.departureTimeColumnWidth)
        assertEquals(24f, spec.card.departureTimeFontSize.value, 0.001f)
        assertEquals(16f, spec.card.supportingTextFontSize.value, 0.001f)
        assertEquals(19f, spec.header.headerTitleFontSize.value, 0.001f)
        assertEquals(14f, spec.header.headerSubtitleFontSize.value, 0.001f)
        assertEquals(12.dp, spec.upcoming.upcomingCardContentPadding)
        assertEquals(8.dp, spec.upcoming.upcomingCardTopPadding)
        assertEquals(12.dp, spec.upcoming.upcomingCardBottomPadding)
        assertEquals(30.dp, spec.upcoming.upcomingMinRowHeight)
        assertEquals(8.dp, spec.upcoming.upcomingBottomSpacing)
        assertEquals(520.dp, spec.section.twoColumnLayoutMinWidth)
    }

    @Test
    fun `resolveScheduleResponsiveLayout grows measurements when font scale increases`() {
        val regular = resolveScheduleResponsiveLayout(600.dp)
        val scaled = resolveScheduleResponsiveLayout(600.dp, fontScale = 1.4f)

        assertTrue(scaled.card.cardHorizontalPadding.value > regular.card.cardHorizontalPadding.value)
        assertTrue(scaled.card.departureTimeFontSize.value > regular.card.departureTimeFontSize.value)
        assertTrue(scaled.header.headerTitleFontSize.value > regular.header.headerTitleFontSize.value)
        assertTrue(scaled.upcoming.upcomingMinRowHeight > regular.upcoming.upcomingMinRowHeight)
    }
}
