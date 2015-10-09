package net.jmymoney.core.component.transaction;

import net.jmymoney.core.entity.Category;

public class CategoryCaptionGenerator {

    public static String getCaption(Category category) {
        StringBuilder sb = new StringBuilder();
        buildFullName(sb, category);
        return sb.toString();
    }

    private static void buildFullName(StringBuilder sb, Category category) {
        if (category.getParent() != null) {
            buildFullName(sb, category.getParent());
            sb.append(" > ");
        }
        sb.append(category.getName());
    }
    
}
