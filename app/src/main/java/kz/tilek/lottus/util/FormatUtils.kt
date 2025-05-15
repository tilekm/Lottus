package kz.tilek.lottus.util
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
object FormatUtils {
    private val integerFormatter = (NumberFormat.getNumberInstance(Locale("ru", "RU")) as DecimalFormat).apply {
        maximumFractionDigits = 0 
    }
    private val decimalFormatter = (NumberFormat.getNumberInstance(Locale("ru", "RU")) as DecimalFormat).apply {
        minimumFractionDigits = 2 
        maximumFractionDigits = 2 
    }
    fun formatPrice(price: BigDecimal?): String {
        if (price == null) return "N/A ₸" 
        return try {
            if (price.stripTrailingZeros().scale() <= 0) {
                integerFormatter.format(price) + " ₸"
            } else {
                decimalFormatter.format(price.setScale(2, RoundingMode.HALF_UP)) + " ₸"
            }
        } catch (e: Exception) {
            price.toPlainString() + " ₸"
        }
    }
}
