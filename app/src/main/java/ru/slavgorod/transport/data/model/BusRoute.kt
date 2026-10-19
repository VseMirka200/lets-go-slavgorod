package ru.slavgorod.transport.data.model

import androidx.compose.runtime.Immutable
import ru.slavgorod.transport.domain.util.ValidationUtils

@Immutable
data class BusRoute(
    val id: String,
    val routeNumber: String,
    val name: String,
    val description: String,
    val stopNames: List<String> = emptyList(),
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
            ValidationCheck(ValidationUtils.hasMeaningfulText(id), "Некорректный идентификатор маршрута: '$id'"),
            ValidationCheck(
                ValidationUtils.hasMeaningfulText(routeNumber),
                "Некорректный номер маршрута: '$routeNumber'"
            ),
            ValidationCheck(
                ValidationUtils.hasTrimmedLengthAtLeast(name, 3),
                "Некорректное название маршрута: '$name'"
            ),
            ValidationCheck(
                ValidationUtils.hasTrimmedLengthAtLeast(description, 2),
                "Некорректное описание: '$description'"
            )
        )
    }
}
