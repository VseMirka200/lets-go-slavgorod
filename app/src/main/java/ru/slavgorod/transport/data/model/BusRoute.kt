package ru.slavgorod.transport.data.model

import androidx.compose.runtime.Immutable
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.AppText
import ru.slavgorod.transport.domain.util.ValidationUtils

@Immutable
data class BusRoute(
    val id: String,
    val routeNumber: String,
    val name: String,
    val description: String,
    val isActive: Boolean = true,
    val color: String = "#1976D2",
    val pricePrimary: String? = null,
    val priceSecondary: String? = null,
    val directionDetails: String? = null,
    val remark: String? = null,
    val notes: String? = null,
    val travelTime: String?,
    val paymentMethods: String?
) {

    fun isValid(): Boolean {
        return validateModel(
            exceptionMessage = AppText.get(R.string.validation_bus_route_failed),
            ValidationCheck(ValidationUtils.hasMeaningfulText(id), "Invalid route ID: '$id'"),
            ValidationCheck(
                ValidationUtils.hasMeaningfulText(routeNumber),
                "Invalid route number: '$routeNumber'"
            ),
            ValidationCheck(
                ValidationUtils.hasTrimmedLengthAtLeast(name, 3),
                "Invalid route name: '$name'"
            ),
            ValidationCheck(
                ValidationUtils.hasTrimmedLengthAtLeast(description, 2),
                "Invalid description: '$description'"
            )
        )
    }
}
