package org.smartregister.path.option;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.view.contract.SmartRegisterClient;
import org.smartregister.view.dialog.FilterOption;

public class ReportFilterOption implements FilterOption {
    private String filter;

    public ReportFilterOption(String filter){
        this.filter=filter;
    }

    // FIXME path_conflict
    //@Override
    public void setFilter(String filter) {
        this.filter = filter;
    }

    // FIXME path_conflict
    //@Override
    public String getCriteria() {
        if(StringUtils.isNotBlank(filter)){
            return " date LIKE '"+filter+"%'";
        }
        return "";
    }

    @Override
    public boolean filter(SmartRegisterClient client) {
        CommonPersonObjectClient currentclient = (CommonPersonObjectClient) client;
        if(currentclient.getColumnmaps().get("report").toLowerCase().contains(filter.toLowerCase())) {
            return true;
        }
        else if(currentclient.getColumnmaps().get("report").toLowerCase().contains(filter.toLowerCase())) {
            return true;
        }
        return false;
    }

    @Override
    public String name() {
        return "";
    }
}
