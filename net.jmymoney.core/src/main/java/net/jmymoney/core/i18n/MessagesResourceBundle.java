package net.jmymoney.core.i18n;

import com.vaadin.ui.UI;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.ejb.Stateless;

@Stateless
public class MessagesResourceBundle {

    public Locale getLocale() {
        return UI.getCurrent().getLocale();
    }
    
    public String getString(I18nResourceConstant key, Object... params) {
        return getString(getLocale(), key, params);
    }

    public static String getString(Locale locale, I18nResourceConstant key, Object... params) {
        String string = ResourceBundle.getBundle("locale.messages", locale).getString(key.getKey());
        if (params.length>0) {
            string = String.format(locale, string, params);
        }
        return string;
    }
    
}
