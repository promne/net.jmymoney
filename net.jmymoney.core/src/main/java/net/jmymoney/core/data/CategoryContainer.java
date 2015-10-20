package net.jmymoney.core.data;

import com.vaadin.data.Container.Hierarchical;
import com.vaadin.data.util.BeanItemContainer;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import net.jmymoney.core.entity.Category;

public class CategoryContainer extends BeanItemContainer<Category> implements Hierarchical {

    private static final long serialVersionUID = 6184993447849780969L;

    public CategoryContainer() {
        super(Category.class);
    }

    @Override
    public Collection<?> getChildren(Object itemId) {
        Long categoryId = getItem(itemId).getBean().getId();
        return getAllItemIds().stream().filter(c -> c.getParent()!=null && categoryId.equals(c.getParent().getId())).collect(Collectors.toList());
    }

    @Override
    public Object getParent(Object itemId) {
        return getItem(itemId).getBean().getParent();
    }

    @Override
    public Collection<?> rootItemIds() {
        return getAllItemIds().stream().filter(c -> c.getParent()==null).collect(Collectors.toList());
    }

    @Override
    public boolean setParent(Object itemId, Object newParentId) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean areChildrenAllowed(Object itemId) {
        return getItem(itemId)!=null;
    }

    @Override
    public boolean setChildrenAllowed(Object itemId, boolean areChildrenAllowed) throws UnsupportedOperationException {
        return false;
    }

    @Override
    public boolean isRoot(Object itemId) {
        return getItem(itemId).getBean().getParent()==null;
    }

    @Override
    public boolean hasChildren(Object itemId) {
        Long categoryId = getItem(itemId).getBean().getId();
        return getAllItemIds().stream().anyMatch(c -> categoryId.equals(c.getId()));
    }
    
    public Optional<Category> getCategory(Long categoryId) {
        return getAllItemIds().stream().filter(c -> categoryId.equals(c.getId())).findFirst();
    }
}
