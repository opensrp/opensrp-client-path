package org.opensrp.path.servicemode;

import android.view.View;

import org.opensrp.Context;
import org.opensrp.path.R;
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

/**
 * Created by muhammad.ahmed@ihsinformatics.com on 12-Nov-15.
 */
public class StockMonthlyServiceModeOption extends ServiceModeOption {

    public StockMonthlyServiceModeOption(SmartRegisterClientsProvider clientsProvider) {
        super(clientsProvider);
    }

    @Override
    public SecuredNativeSmartRegisterActivity.ClientsHeaderProvider getHeaderProvider() {
        return new SecuredNativeSmartRegisterActivity.ClientsHeaderProvider() {
            @Override
            public int count() {
                return 6;
            }

            @Override
            public int weightSum() {
                return 6;
            }

            @Override
            public int[] weights() {
                return new int[]{1,1,1,1,1,1};
            }

            @Override
            public int[] headerTextResourceIds() {
                return new int[]{ R.string.month, R.string.month_target, R.string.month_received,
                        R.string.month_used, R.string.month_wasted, R.string.month_inhand
                };
            }
        };
    }

    @Override
    public String name() {
        return Context.getInstance().getStringResource(R.string.stock_register_monthly_view);
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
