package org.opensrp.path.controller;

import android.app.Activity;
import android.content.Intent;

import org.opensrp.path.activity.ChildSmartRegisterActivity;
import org.opensrp.path.activity.FieldMonitorSmartRegisterActivity;
import org.opensrp.path.activity.ProviderProfileActivity;
import org.opensrp.path.activity.VaccineReportActivity;
import org.opensrp.path.activity.WomanSmartRegisterActivity;

public class VaccinatorNavigationController extends org.opensrp.view.controller.NavigationController {

    private Activity activity;

    public VaccinatorNavigationController(Activity activity) {
        super(activity, null);
        this.activity = activity;
    }

    @Override
    public void startReports() {
        activity.startActivity(new Intent(activity, VaccineReportActivity.class));
    }

    public void startFieldMonitor() {
        activity.startActivity(new Intent(activity, FieldMonitorSmartRegisterActivity.class));
    }

    @Override
    public void startChildSmartRegistry() {
        activity.startActivity(new Intent(activity, ChildSmartRegisterActivity.class));
    }

    public void startWomanSmartRegister() {
        activity.startActivity(new Intent(activity, WomanSmartRegisterActivity.class));
    }

    public void startProviderProfile() {
        activity.startActivity(new Intent(activity, ProviderProfileActivity.class));
    }
}
