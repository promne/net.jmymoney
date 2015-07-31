package net.jmymoney.core.i18n;

public enum I18nResourceConstant {

    UNIVERSAL_USERNAME("universal.username"),
    UNIVERSAL_PASSWORD("universal.password"),
    UNIVERSAL_ACCOUNT("universal.account"),
    UNIVERSAL_ACCOUNTS("universal.accounts"),
    UNIVERSAL_CATEGORIES("universal.categories"),
    UNIVERSAL_PARTNERS("universal.partners"),
    UNIVERSAL_REPORTS("universal.reports"),
    UNIVERSAL_NAME("universal.name"),
    UNIVERSAL_DELETE("universal.delete"),
    UNIVERSAL_BALANCE("universal.balance"),
    
    ACCOUNT_NEW("account.new"),
    ACCOUNT_EDIT("account.edit"),
    ACCOUNT_DIALOG_NEW_TITLE("account.dialog_new.title"),
    ACCOUNT_DIALOG_NEW_DESCRIPTION("account.dialog_new.description"),
    ACCOUNT_DIALOG_EDIT_TITLE("account.dialog_edit.title"),
    ACCOUNT_DIALOG_EDIT_DESCRIPTION("account.dialog_edit.description"),
    ACCOUNT_MESSAGE_DELETED("account.message.deleted"),
    ACCOUNT_MESSAGE_UNABLE_TO_DELETE("account.message.unable_to_delete"),

    NAVIGATION_TRANSACTIONS("navigation.transactions"),
    
    LOGIN("login.login"),
    LOGIN_DO_LOGIN("login.do_login"),
    LOGIN_DO_REGISTER("login.do_register"),
    LOGIN_ERROR_INVALID_CREDENTIALS("login.error.invalid_credentials"),
    
    TRANSACTIONS_ACCOUNT("transactions.account"),
    TRANSACTIONS_TRANSACTIONS("transactions.transactions"),
    TRANSACTIONS_DETAIL("transactions.detail"),
    TRANSACTIONS_DEPOSIT("transactions.deposit"),
    TRANSACTIONS_WITHDRAWL("transactions.withdrawl"),
    TRANSACTIONS_DATE("transactions.date"),
    TRANSACTIONS_ACTION_ACCOUNT_SELECT("transactions.action.select_account"),
    TRANSACTIONS_ACTION_CREATE("transactions.action.create"),
    TRANSACTIONS_ACTION_EDIT("transactions.action.edit"),
    TRANSACTIONS_ACTION_SAVE("transactions.action.save"),
    TRANSACTIONS_ACTION_CANCEL("transactions.action.cancel"),
    TRANSACTIONS_ACTION_DELETE("transactions.action.delete"),
//    TRANSACTIONS_("transactions."),
    
    
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
