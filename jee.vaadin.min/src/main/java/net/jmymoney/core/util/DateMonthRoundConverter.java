package net.jmymoney.core.util;

import com.vaadin.data.util.converter.Converter;

import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;

public class DateMonthRoundConverter implements Converter<Date, Date> {

	@Override
	public Date convertToModel(Date value, Class<? extends Date> targetType, Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException {
		return Date.from((value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).withDayOfMonth(1).toInstant()));
	}

	@Override
	public Date convertToPresentation(Date value, Class<? extends Date> targetType, Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException {
		return Date.from((value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).withDayOfMonth(1).toInstant()));
	}

	@Override
	public Class<Date> getModelType() {
		return Date.class;
	}

	@Override
	public Class<Date> getPresentationType() {
		return Date.class;
	}

}
