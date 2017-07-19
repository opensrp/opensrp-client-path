package org.opensrp.path.servicemode;

import android.view.View;

import org.opensrp.view.activity.SecuredNativeSmartRegisterActivity;
import org.opensrp.view.contract.ANCSmartRegisterClient;
import org.opensrp.view.contract.ChildSmartRegisterClient;
import org.opensrp.view.contract.FPSmartRegisterClient;
import org.opensrp.view.contract.pnc.PNCSmartRegisterClient;
import org.opensrp.view.dialog.ServiceModeOption;
import org.opensrp.provider.SmartRegisterClientsProvider;
import org.opensrp.view.viewHolder.NativeANCSmartRegisterViewHolder;
import org.opensrp.view.viewHolder.NativeChildSmartRegisterViewHolder;
import org.opensrp.view.viewHolder.NativeFPSmartRegisterViewHolder;
import org.opensrp.view.viewHolder.NativePNCSmartRegisterViewHolder;

public class VaccinationServiceModeOption extends ServiceModeOption {

    private String name;
    private int[] headerTextResourceIds;
    private int[] columnWeights;

    public VaccinationServiceModeOption(SmartRegisterClientsProvider provider, String name, int[] headerTextResourceIds, int[] columnWeights){
        super(provider);
        this.name = name;
        this.headerTextResourceIds = headerTextResourceIds;
        this.columnWeights = columnWeights;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public SecuredNativeSmartRegisterActivity.ClientsHeaderProvider getHeaderProvider() {
        return new SecuredNativeSmartRegisterActivity.ClientsHeaderProvider() {
            @Override
            public int count() {
                return headerTextResourceIds.length;
            }

            @Override
            public int weightSum() {
                int sum = 0;
                for(int i = 0; i < columnWeights.length; i++){
                    sum += columnWeights[i];
                }
                return sum;
            }

            @Override
            public int[] weights() {
                return columnWeights;
            }

            @Override
            public int[] headerTextResourceIds() {
                return headerTextResourceIds;
            }
        };
    }

    @Override
    public void setupListView(ChildSmartRegisterClient client, NativeChildSmartRegisterViewHolder viewHolder, View.OnClickListener clientSectionClickListener) {

    }

    @Override
    public void setupListView(ANCSmartRegisterClient client, NativeANCSmartRegisterViewHolder viewHolder, View.OnClickListener clientSectionClickListener) {

    }

    @Override
    public void setupListView(FPSmartRegisterClient client, NativeFPSmartRegisterViewHolder viewHolder, View.OnClickListener clientSectionClickListener) {

    }

    @Override
    public void setupListView(PNCSmartRegisterClient client, NativePNCSmartRegisterViewHolder viewHolder, View.OnClickListener clientSectionClickListener) {

    }
}
