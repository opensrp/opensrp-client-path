package org.opensrp.path.option;

import org.opensrp.cursoradapter.CursorSortOption;
import org.opensrp.view.contract.SmartRegisterClients;

public class DateSort implements CursorSortOption {
    private String name;
    private String sort;

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
