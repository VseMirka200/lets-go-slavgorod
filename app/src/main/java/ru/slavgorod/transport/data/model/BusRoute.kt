package ru.slavgorod.transport.data.model

import androidx.compose.runtime.Immutable
import ru.slavgorod.transport.domain.util.ValidationUtils

/**
 * Сведения о маршруте, отображаемые в списке и на экране расписания.
 *
 * [id] связывает маршрут с рейсами и должен быть устойчивым между обновлениями;
 * [routeNumber] предназначен для отображения и может отличаться от идентификатора.
 * Цены, время в пути и способы оплаты являются необязательным текстом источника,
 * поэтому их нельзя использовать как числовые значения без дополнительного разбора.
 */
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
