package net.jmymoney.core.i18n;

public enum I18nResourceConstant {

    DASHBOARD("dashboard.dashboard"),
    DASHBOARD_TOP_CATEGORIES("dashboard.top_categories"),
    DASHBOARD_ACCOUNT_BALANCE("dashboard.account_balance"),
    DASHBOARD_ACCOUNT_BALANCE_HISTORY("dashboard.account_balance_history"),
    DASHBOARD_NET_WORTH("dashboard.net_worth");
    
    private final String key;

    private I18nResourceConstant(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
    
}
