package net.jmymoney.core.component.tools;

import com.vaadin.ui.Table;

import java.util.Iterator;

public class TableHelper {

    public static void putItemInViewport(Object itemId, Table table) {
        Object currentPageFirstItemId = table.getCurrentPageFirstItemId();
        Iterator<?> visibleIds = table.getVisibleItemIds().iterator();

        while (visibleIds.hasNext() && !currentPageFirstItemId.equals(visibleIds.next())) {}
        
        int pageSize = table.getPageLength();
        boolean found = false;
        while (visibleIds.hasNext() && !found && pageSize>0) {
            found = visibleIds.next().equals(currentPageFirstItemId);
            pageSize--;
        };
        
        if (!found) {
            table.setCurrentPageFirstItemId(itemId);
        }
    }
    
}
