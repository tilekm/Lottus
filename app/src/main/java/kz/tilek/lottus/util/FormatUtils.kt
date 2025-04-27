// ./app/src/main/java/kz/tilek/lottus/util/FormatUtils.kt
package kz.tilek.lottus.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object FormatUtils {

    // Форматтер для целых чисел с разделителями групп (напр., 15 500)
    // Используем Locale("ru", "RU") для правильных разделителей
    private val integerFormatter = (NumberFormat.getNumberInstance(Locale("ru", "RU")) as DecimalFormat).apply {
        maximumFractionDigits = 0 // Без дробной части
    }

    // Форматтер для десятичных чисел с двумя знаками после запятой и разделителями групп (напр., 15 500,50)
    private val decimalFormatter = (NumberFormat.getNumberInstance(Locale("ru", "RU")) as DecimalFormat).apply {
        minimumFractionDigits = 2 // Всегда 2 знака
        maximumFractionDigits = 2 // Всегда 2 знака
    }

    /**
     * Форматирует цену BigDecimal.
     * Если число целое (например, 15500.00), отображает как целое ("15 500 ₸").
     * Иначе отображает с двумя знаками после запятой ("15 500,50 ₸").
     *
     * @param price Цена для форматирования (может быть null).
     * @return Отформатированная строка цены или "N/A ₸", если price is null.
     */
    fun formatPrice(price: BigDecimal?): String {
        if (price == null) return "N/A ₸" // Или другое значение по умолчанию

        return try {
            // Проверяем, есть ли значащая дробная часть
            // stripTrailingZeros() убирает нули в конце (15500.00 -> 15500)
            // Если scale() <= 0, значит, дробной части нет или она нулевая
            if (price.stripTrailingZeros().scale() <= 0) {
                // Форматируем как целое
                integerFormatter.format(price) + " ₸"
            } else {
                // Форматируем как десятичное с двумя знаками
                // setScale используется для гарантии двух знаков, даже если пришло 15500.5
                decimalFormatter.format(price.setScale(2, RoundingMode.HALF_UP)) + " ₸"
            }
        } catch (e: Exception) {
            // В случае ошибки форматирования, возвращаем просто строку
            price.toPlainString() + " ₸"
        }
    }
}
