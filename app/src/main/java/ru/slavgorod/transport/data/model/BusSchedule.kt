package ru.slavgorod.transport.data.model

import androidx.compose.runtime.Immutable
import ru.slavgorod.transport.domain.util.ValidationUtils

@Immutable
data class BusSchedule(
    val id: String,
    val routeId: String,
    val stopName: String,
    val departureTime: String,
    val dayOfWeek: Int,
    val isWeekend: Boolean = false,
    val dayType: String? = null,
    val serviceCalendar: ServiceCalendar = ServiceCalendar.Daily,
    val variant: String? = null,
    val platform: String? = null,
    val notes: String? = null,
    val remark: String? = null,
    val departurePoint: String
) {

    fun isValid(): Boolean {
        return validateModel(
            ValidationCheck(ValidationUtils.hasMeaningfulText(id), "Некорректный идентификатор рейса: '$id'"),
            ValidationCheck(
                ValidationUtils.hasMeaningfulText(routeId),
                "Некорректный идентификатор маршрута: '$routeId'"
            ),
            ValidationCheck(
                ValidationUtils.hasTrimmedLengthAtLeast(stopName, 2),
                "Некорректное название остановки: '$stopName'"
            ),
            ValidationCheck(
                ValidationUtils.isValidTime(departureTime),
                "Некорректное время отправления: '$departureTime'"
            ),
            ValidationCheck(
                ValidationUtils.isValidDayOfWeek(dayOfWeek),
                "Некорректный день недели: $dayOfWeek"
            ),
            ValidationCheck(
                ValidationUtils.hasTrimmedLengthAtLeast(departurePoint, 2),
                "Некорректный пункт отправления: '$departurePoint'"
            )
        )
    }
}
