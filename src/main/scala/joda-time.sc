import org.joda.time.format._
import org.joda.time.{DateTime, DateTimeZone}

val fmt = new DateTimeFormatterBuilder()
  .appendLiteral("log-")
  .appendYear(4,4)
  .appendLiteral('.')
  .appendMonthOfYear(2)
  .appendLiteral('.')
  .appendDayOfMonth(2)
  .toFormatter
DateTime.now().toString(fmt)


DateTime.now()
DateTime.now(DateTimeZone.forID("Asia/Taipei"))
DateTime.now(DateTimeZone.forID("Asia/Tokyo"))
