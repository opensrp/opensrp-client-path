package org.smartregister.path.servicemode;

import android.view.View;

import org.smartregister.Context;
import org.smartregister.path.R;
import org.smartregister.view.activity.SecuredNativeSmartRegisterActivity;
import org.smartregister.view.contract.ANCSmartRegisterClient;
import org.smartregister.view.contract.ChildSmartRegisterClient;
import org.smartregister.view.contract.FPSmartRegisterClient;
import org.smartregister.view.contract.pnc.PNCSmartRegisterClient;
import org.smartregister.view.dialog.ServiceModeOption;
import org.smartregister.provider.SmartRegisterClientsProvider;
import org.smartregister.view.viewholder.NativeANCSmartRegisterViewHolder;
import org.smartregister.view.viewholder.NativeChildSmartRegisterViewHolder;
import org.smartregister.view.viewholder.NativeFPSmartRegisterViewHolder;
import org.smartregister.view.viewholder.NativePNCSmartRegisterViewHolder;

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
