package ru.slavgorod.transport.data.local

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.data.validation.JsonValidator
import timber.log.Timber

/**
 * Преобразует корневой JSON-файл в модели маршрутов.
 *
 * Подсказка разработчику: не скрывайте CancellationException в общем обработчике,
 * иначе отменённая загрузка будет ошибочно выглядеть как пустой ответ.
 */
class JsonDataSource {

    /**
     * Разбирает маршруты и отбрасывает записи без обязательных полей.
     * При ошибке разбора или отсутствии корректных маршрутов возвращает пустой список;
     * отмена корутины передаётся вызывающему коду.
     */
    suspend fun parseRoutesFromJson(jsonString: String): List<BusRoute> =
        withContext(Dispatchers.IO) {
            try {
                val jsonObject = JsonParser.parseString(jsonString).asJsonObject
                parseRoutesFromObject(jsonObject)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "Ошибка разбора JSON маршрутов: %s", exception.message)
                emptyList()
            }
        }

    internal fun parseRoutesFromObject(jsonObject: JsonObject): List<BusRoute> {
        val routesArray = jsonObject.getAsJsonArray(RoutesJsonContract.ROOT_ROUTES) ?: return emptyList()
        val routes = routesArray.mapNotNull { element ->
            element.asJsonObjectOrNull()?.let(::buildRoute)
        }
        return JsonValidator.validateRoutes(routes)
    }

    private fun buildRoute(routeJson: JsonObject): BusRoute? {
        val id = routeJson.requiredText(RoutesJsonContract.ROUTE_ID) ?: return null
        val routeNumber = routeJson.requiredText(RoutesJsonContract.ROUTE_NUMBER) ?: return null
        val name = routeJson.requiredText(RoutesJsonContract.ROUTE_NAME) ?: return null
        val notes = routeJson.requiredText(RoutesJsonContract.ROUTE_NOTES)
        val description = notes
            ?: routeJson.optionalText(RoutesJsonContract.ROUTE_DESCRIPTION)
            ?: return null
        val color = routeJson.requiredText(RoutesJsonContract.ROUTE_COLOR) ?: return null
        val declaredStops = routeJson.optionalTextList(RoutesJsonContract.ROUTE_STOPS).orEmpty()
        val scheduleStops = routeJson.getAsJsonArray(RoutesJsonContract.ROUTE_SCHEDULES)
            ?.mapNotNull { scheduleElement ->
                scheduleElement.asJsonObjectOrNull()
                    ?.requiredText(RoutesJsonContract.SCHEDULE_DEPARTURE_POINT)
            }
            .orEmpty()

        return BusRoute(
            id = id,
            routeNumber = routeNumber,
            name = name,
            description = description,
            stopNames = (declaredStops.ifEmpty { scheduleStops }).distinct(),
            color = color,
            remark = routeJson.optionalText(RoutesJsonContract.ROUTE_REMARK),
            notes = notes,
            travelTime = routeJson.optionalText(RoutesJsonContract.ROUTE_TRAVEL_TIME),
            pricePrimary = routeJson.optionalText(RoutesJsonContract.ROUTE_PRICE_PRIMARY),
            priceSecondary = routeJson.optionalText(RoutesJsonContract.ROUTE_PRICE_SECONDARY),
            paymentMethods = routeJson.optionalText(RoutesJsonContract.ROUTE_PAYMENT_METHODS)
        )
    }

}
