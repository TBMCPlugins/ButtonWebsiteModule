package buttondevteam.website.io;

import com.sun.net.httpserver.HttpExchange;

import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class Cookies extends HashMap<String, Cookie> {
	private static final long serialVersionUID = -328053564170765287L;

	private String expiretime;

	public Cookies(int addyears) {
		super();
		this.expiretime = ZonedDateTime.now(ZoneId.of("GMT")).plus(Period.of(addyears, 0, 0))
				.format(DateTimeFormatter.RFC_1123_DATE_TIME);
	}

	public Cookies(String expiretime) {
		super();
		this.expiretime = expiretime;
	}

	public Cookies() {
		super();
		this.expiretime = ZonedDateTime.now(ZoneId.of("GMT")).format(DateTimeFormatter.RFC_1123_DATE_TIME);
	}

	public void AddHeaders(HttpExchange exchange) {
		for (Entry<String, Cookie> item : entrySet())
			exchange.getResponseHeaders().add("Set-Cookie",
                    item.getKey() + "=" + item.getValue().getValue() + "; expires=" + expiretime + "; Secure; HttpOnly; Domain=figytuna.com"); //Allow for frontend
        exchange.getResponseHeaders().add("Set-Cookie", "expiretime=" + expiretime + "; expires=" + expiretime + "; Secure; HttpOnly; Domain=figytuna.com");
	}

	public Cookies add(Cookie cookie) {
		this.put(cookie.getName(), cookie);
		return this;
	}

	public String getExpireTime() {
		return expiretime;
	}

	public ZonedDateTime getExpireTimeParsed() {
		return ZonedDateTime.parse(expiretime, DateTimeFormatter.RFC_1123_DATE_TIME);
	}

	public void setExpireTime(LocalDateTime expiretime) {
		this.expiretime = expiretime.format(DateTimeFormatter.RFC_1123_DATE_TIME);
	}

	@Override
	public String toString() {
		return "Cookies [expiretime=" + expiretime + ", " + super.toString() + "]";
	}
}
