package org.smartregister.path.listener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;

import org.smartregister.path.R;
import org.smartregister.path.activity.BaseRegisterActivity;
import org.smartregister.path.activity.ChildSmartRegisterActivity;
import org.smartregister.path.activity.CoverageReportsActivity;
import org.smartregister.path.activity.DropoutReportsActivity;
import org.smartregister.path.activity.HIA2ReportsActivity;
import org.smartregister.path.activity.PathStockActivity;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.service.intent.SyncIntentService;
import org.smartregister.path.toolbar.BaseToolbar;
import org.smartregister.view.activity.DrishtiApplication;
import org.smartregister.view.activity.SecuredActivity;

import static util.PathConstants.JSONFORM.CHILD_ENROLLMENT;
import static util.PathConstants.JSONFORM.OUT_OF_CATCHMENT;

/**
 * Created by samuelgithengi on 2/14/18.
 */

public class CustomNavigationBarListener extends BaseListener implements View.OnClickListener {

    public CustomNavigationBarListener(Activity context) {
        super(context);
    }

    public CustomNavigationBarListener(Activity context, BaseToolbar toolbar) {
        super(context, toolbar);
    }

    @Override
    public void onClick(View v) {
        final DrawerLayout drawer = (DrawerLayout) context.findViewById(R.id.drawer_layout);
        Intent intent;
        switch (v.getId()) {
            case R.id.nav_sync:
                intent = new Intent(context, SyncIntentService.class);
                startService(intent);
                drawer.closeDrawer(GravityCompat.START);
                break;
            case R.id.nav_register:
                if (context instanceof SecuredActivity)
                    ((SecuredActivity) context).startFormActivity(CHILD_ENROLLMENT, null, null);
                else
                    startJsonForm(CHILD_ENROLLMENT, null);
                drawer.closeDrawer(GravityCompat.START);
                break;
            case R.id.nav_record_vaccination_out_catchment:
                if (context instanceof SecuredActivity)
                    ((SecuredActivity) context).startFormActivity(OUT_OF_CATCHMENT, null, null);
                else
                    startJsonForm(OUT_OF_CATCHMENT, null);
                drawer.closeDrawer(GravityCompat.START);
                break;
            case R.id.stock_control:
                intent = new Intent(getApplicationContext(), PathStockActivity.class);
                startActivity(intent);
                drawer.closeDrawer(GravityCompat.START);
                break;
            case R.id.child_register:
                VaccinatorApplication.setCrashlyticsUser(VaccinatorApplication.getInstance().context());
                intent = new Intent(getApplicationContext(), ChildSmartRegisterActivity.class);
                intent.putExtra(BaseRegisterActivity.IS_REMOTE_LOGIN, false);
                startActivity(intent);
                context.finish();
                drawer.closeDrawer(GravityCompat.START);
                break;
            case R.id.hia2_reports:
                intent = new Intent(getApplicationContext(), HIA2ReportsActivity.class);
                startActivity(intent);
                drawer.closeDrawer(GravityCompat.START);
                break;
            case R.id.coverage_reports:
                intent = new Intent(getApplicationContext(), CoverageReportsActivity.class);
                startActivity(intent);
                drawer.closeDrawer(GravityCompat.START);
                break;
            case R.id.dropout_reports:
                intent = new Intent(getApplicationContext(), DropoutReportsActivity.class);
                startActivity(intent);
                drawer.closeDrawer(GravityCompat.START);
                break;
            case R.id.logout_b:
                DrishtiApplication application = (DrishtiApplication) context.getApplication();
                application.logoutCurrentUser();
                context.finish();
                break;
            default:
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                }
                break;
        }
    }

    private void startActivity(Intent intent) {
        context.startActivity(intent);
    }

    private void startService(Intent intent) {
        context.startService(intent);
    }

    private Context getApplicationContext() {
        return context.getApplicationContext();
    }

}
