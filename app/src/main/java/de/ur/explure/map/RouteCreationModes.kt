package de.ur.explure.map

// empty interface, used only for polymorphism reasons so the enums can be used more exchangeable
interface RouteCreationMode

enum class ManualRouteCreationModes : RouteCreationMode {
    MODE_ADD,
    // MODE_EDIT,
    MODE_DELETE
}

enum class RouteDrawModes : RouteCreationMode {
    MODE_DRAW,
    MODE_MOVE,
    MODE_DELETE
}
