import org.joda.time.format._
import org.joda.time.DateTime

val fmt = new DateTimeFormatterBuilder()
  .appendLiteral("log-")
  .appendYear(4,4)
  .appendLiteral('.')
  .appendMonthOfYear(2)
  .appendLiteral('.')
  .appendDayOfMonth(2)
  .toFormatter
DateTime.now().toString(fmt)