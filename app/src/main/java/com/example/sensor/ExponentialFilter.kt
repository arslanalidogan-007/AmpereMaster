package com.example.sensor

/**
 * Filter zur Glättung stark schwankender Sensor- und Kernel-Messwerte (z. B. BATTERY_PROPERTY_CURRENT_NOW).
 * Verwendet einen Exponential Moving Average (EMA) Filter mit adaptiver Rücksetzung bei Zustandswechseln.
 */
class ExponentialFilter(
    private val alpha: Double = 0.25
) {
    private var filteredValue: Double? = null

    /**
     * Führt eine neue Rohmessung in den Filter ein und liefert den geglätteten Wert zurück.
     * @param rawValue Der neu gemessene Wert.
     * @param resetIfDrastic Wenn true, wird der Filter zurückgesetzt (z.B. wenn das Ladekabel eingesteckt wird).
     */
    fun filter(rawValue: Double, resetIfDrastic: Boolean = false): Double {
        val current = filteredValue
        if (current == null || resetIfDrastic) {
            filteredValue = rawValue
            return rawValue
        }

        // Bei abrupten Sprüngen von mehr als 500% (z.B. Schnellladung startet) schneller anpassen
        val diff = Math.abs(rawValue - current)
        val effectiveAlpha = if (diff > 1500.0) 0.6 else alpha

        val newValue = current + effectiveAlpha * (rawValue - current)
        filteredValue = newValue
        return newValue
    }

    /**
     * Setzt den internen Filterzustand zurück.
     */
    fun reset() {
        filteredValue = null
    }

    /**
     * Gibt den aktuellen geglätteten Wert zurück oder null, falls noch keine Messung vorliegt.
     */
    fun get(): Double? = filteredValue
}
