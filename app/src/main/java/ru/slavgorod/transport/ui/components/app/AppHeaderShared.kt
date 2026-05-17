package ru.slavgorod.transport.ui.components.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.slavgorod.transport.R
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.ui.components.schedule.ScheduleResponsiveLayoutSpec

internal val AppHeaderContainerColor = androidx.compose.ui.graphics.Color(0xFF0D47A1)
internal val AppHeaderContentColor = androidx.compose.ui.graphics.Color.White
internal val AppHeaderSupportingTextColor = AppHeaderContentColor.copy(alpha = 0.8f)

private val HeaderDividerColor = AppHeaderContentColor.copy(alpha = 0.2f)
private val HeaderLabelColor = AppHeaderContentColor.copy(alpha = 0.7f)

@Composable
internal fun ScheduleHeaderDetailsContent(
    route: BusRoute,
    layout: ScheduleResponsiveLayoutSpec,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 0.dp,
    noteText: String? = null
) {
    val hasNoteText = !noteText.isNullOrBlank()
    val headerItemSpacing = layout.header.headerItemSpacing
    val bottomPadding = if (hasNoteText) 0.dp else 8.dp
    val headerScale = 1.12f
    val headerLabelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = layout.header.headerSubtitleFontSize * headerScale,
        lineHeight = layout.header.headerSubtitleFontSize * headerScale * 1.12f
    )
    val headerValueStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = layout.header.headerSubtitleFontSize * headerScale,
        lineHeight = layout.header.headerSubtitleFontSize * headerScale * 1.12f
    )
    val routeValueStyle = headerValueStyle.copy(
        fontSize = layout.header.headerSubtitleFontSize * headerScale,
        lineHeight = layout.header.headerSubtitleFontSize * headerScale * 1.12f
    )
    val priceText = buildPriceLabel(
        cityPrice = route.pricePrimary,
        intercityPrice = route.priceSecondary,
        citySuffix = stringResource(R.string.route_header_city_suffix),
        intercitySuffix = stringResource(R.string.route_header_intercity_suffix)
    )
    val travelTime = route.travelTime
    val travelTimeLabel = stringResource(R.string.schedule_header_travel_time)
    val priceLabel = stringResource(R.string.schedule_header_price)
    val paymentLabel = stringResource(R.string.schedule_header_payment_methods)
    val routeLabel = stringResource(R.string.schedule_header_route)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                top = 0.dp,
                end = horizontalPadding,
                bottom = bottomPadding
            ),
        verticalArrangement = Arrangement.spacedBy(headerItemSpacing)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(headerItemSpacing)
        ) {
            travelTime?.let { value ->
                ScheduleHeaderInfoItem(
                    label = travelTimeLabel,
                    value = value,
                    labelStyle = headerLabelStyle,
                    valueStyle = headerValueStyle
                )
            }

            priceText?.let { value ->
                ScheduleHeaderInfoItem(
                    label = priceLabel,
                    value = value,
                    labelStyle = headerLabelStyle,
                    valueStyle = headerValueStyle
                )
            }

            route.paymentMethods?.let { paymentMethods ->
                ScheduleHeaderInfoItem(
                    label = paymentLabel,
                    value = paymentMethods,
                    labelStyle = headerLabelStyle,
                    valueStyle = headerValueStyle
                )
            }

            ScheduleHeaderInfoItem(
                label = routeLabel,
                value = route.description,
                labelStyle = headerLabelStyle,
                valueStyle = routeValueStyle,
                allowWrapping = true
            )
        }

        if (hasNoteText) {
            HorizontalDivider(color = HeaderDividerColor)

            Text(
                text = noteText,
                style = headerValueStyle,
                color = AppHeaderSupportingTextColor,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
internal fun ScheduleHeaderInfoItem(
    label: String,
    value: String,
    labelStyle: TextStyle,
    valueStyle: TextStyle,
    modifier: Modifier = Modifier,
    allowWrapping: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = labelStyle,
            color = HeaderLabelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = valueStyle,
            color = AppHeaderContentColor,
            softWrap = allowWrapping
        )
    }
}

internal fun buildPriceLabel(
    cityPrice: String?,
    intercityPrice: String?,
    citySuffix: String,
    intercitySuffix: String
): String? {
    val city = normalizePriceSegment(cityPrice, citySuffix, intercitySuffix)
    val intercity = normalizePriceSegment(intercityPrice, citySuffix, intercitySuffix)
    val cityDisplay = city?.let { appendSuffixIfMissing(it, citySuffix) }
    val intercityDisplay = intercity?.let { appendSuffixIfMissing(it, intercitySuffix) }

    return when {
        cityDisplay != null && intercityDisplay != null -> "$cityDisplay / $intercityDisplay"
        cityDisplay != null -> cityDisplay
        intercityDisplay != null -> intercityDisplay
        else -> null
    }
}

private fun normalizePriceSegment(
    value: String?,
    citySuffix: String,
    intercitySuffix: String
): String? {
    val raw = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val lower = raw.lowercase()

    if (lower.contains(citySuffix.lowercase()) || lower.contains(intercitySuffix.lowercase())) {
        return raw
            .replace(
                Regex(
                    """\b(${Regex.escape(citySuffix)})\b(\s+\1\b)+""",
                    RegexOption.IGNORE_CASE
                ), citySuffix
            )
            .replace(
                Regex(
                    """\b(${Regex.escape(intercitySuffix)})\b(\s+\1\b)+""",
                    RegexOption.IGNORE_CASE
                ), intercitySuffix
            )
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    return raw
}

private fun appendSuffixIfMissing(value: String, suffix: String): String {
    return if (value.lowercase().contains(suffix.lowercase())) {
        value
    } else {
        "$value $suffix"
    }
}
