package ru.slavgorod.transport.data.local

internal object RoutesJsonContract {
    const val ROOT_ROUTES = "routes"

    const val ROUTE_ID = "id"
    const val ROUTE_NUMBER = "routeNumber"
    const val ROUTE_NAME = "name"
    const val ROUTE_DESCRIPTION = "description"
    const val ROUTE_NOTES = "notes"
    const val ROUTE_COLOR = "color"
    const val ROUTE_TRAVEL_TIME = "travelTime"
    const val ROUTE_PRICE_PRIMARY = "pricePrimary"
    const val ROUTE_PRICE_SECONDARY = "priceSecondary"
    const val ROUTE_PAYMENT_METHODS = "paymentMethods"
    const val ROUTE_REMARK = "remark"
    const val ROUTE_SCHEDULES = "schedules"

    const val SCHEDULE_ID = "id"
    const val SCHEDULE_DEPARTURE_POINT = "departurePoint"
    const val SCHEDULE_DEPARTURE_TIME = "departureTime"
    const val SCHEDULE_DAY_TYPE = "dayType"
    const val SCHEDULE_VARIANT = "variant"
    const val SCHEDULE_PLATFORM = "platform"
    const val SCHEDULE_NOTES = "notes"
    const val SCHEDULE_REMARK = "remark"

}
