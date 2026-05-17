package ru.slavgorod.transport.data.model

import androidx.compose.runtime.Immutable
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.AppText
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
    val variant: String? = null,
    val platform: String? = null,
    val notes: String? = null,
    val remark: String? = null,
    val departurePoint: String
) {

    fun isValid(): Boolean {
        return validateModel(
            exceptionMessage = AppText.get(R.string.validation_bus_schedule_failed),
            ValidationCheck(ValidationUtils.hasMeaningfulText(id), "Invalid schedule ID: '$id'"),
            ValidationCheck(
                ValidationUtils.hasMeaningfulText(routeId),
                "Invalid route ID: '$routeId'"
            ),
            ValidationCheck(
                ValidationUtils.hasTrimmedLengthAtLeast(stopName, 2),
                "Invalid stop name: '$stopName'"
            ),
            ValidationCheck(
                ValidationUtils.isValidTime(departureTime),
                "Invalid departure time: '$departureTime'"
            ),
            ValidationCheck(
                ValidationUtils.isValidDayOfWeek(dayOfWeek),
                "Invalid day of week: $dayOfWeek"
            ),
            ValidationCheck(
                ValidationUtils.hasTrimmedLengthAtLeast(departurePoint, 2),
                "Invalid departure point: '$departurePoint'"
            )
        )
    }
}
