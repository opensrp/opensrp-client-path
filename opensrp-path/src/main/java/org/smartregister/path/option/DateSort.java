package org.smartregister.path.option;

import org.smartregister.cursoradapter.CursorSortOption;
import org.smartregister.view.contract.SmartRegisterClients;

public class DateSort implements CursorSortOption {
    private final String name;
    private final String sort;

    @Override
    public String sort() {
        return sort;
    }

    public DateSort(String name, String sort) {
        this.sort = sort;
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public SmartRegisterClients sort(SmartRegisterClients allClients) {
        throw new UnsupportedOperationException();
    }
}
