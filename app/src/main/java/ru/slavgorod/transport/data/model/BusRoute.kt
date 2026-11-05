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

    /** Проверяет обязательные поля и записывает причины отказа в журнал. */
    fun isValid(): Boolean {
        return validateModel(
            ValidationCheck(ValidationUtils.isValidRouteId(id), "Некорректный идентификатор маршрута: '$id'"),
            ValidationCheck(
                ValidationUtils.isValidRouteNumber(routeNumber),
                "Некорректный номер маршрута: '$routeNumber'"
            ),
            ValidationCheck(
                ValidationUtils.isValidRouteName(name),
                "Некорректное название маршрута: '$name'"
            ),
            ValidationCheck(
                ValidationUtils.hasTrimmedLengthAtLeast(description, 2),
                "Некорректное описание: '$description'"
            )
        )
    }
}
