package org.smartregister.path.listener;

import android.app.Activity;
import android.content.Intent;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;

import org.smartregister.path.R;
import org.smartregister.path.activity.HIA2ReportsActivity;
import org.smartregister.path.activity.PathStockActivity;
import org.smartregister.path.service.intent.SyncService;
import org.smartregister.path.toolbar.BaseToolbar;
import org.smartregister.view.activity.SecuredActivity;

import util.ServiceTools;

import static org.smartregister.path.activity.PathStockActivity.CURRENT_LOCATION;
import static util.PathConstants.JSONFORM.CHILD_ENROLLMENT;
import static util.PathConstants.JSONFORM.OUT_OF_CATCHMENT;

/**
 * Created by samuelgithengi on 2/14/18.
 */

public class NavigationItemListener extends BaseListener implements NavigationView.OnNavigationItemSelectedListener {

    public NavigationItemListener(Activity context) {
        super(context);
    }

    public NavigationItemListener(Activity context, BaseToolbar toolbar) {
        super(context, toolbar);
    }

    public NavigationItemListener(Activity context, String  location) {
        super(context, location);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        // Handle navigation view item clicks here.

        int id = menuItem.getItemId();

        if (id == R.id.nav_register) {
            if (context instanceof SecuredActivity)
                ((SecuredActivity) context).startFormActivity(CHILD_ENROLLMENT, null, null);
            else
                startJsonForm(CHILD_ENROLLMENT, null);
        } else if (id == R.id.nav_record_vaccination_out_catchment) {
            if (context instanceof SecuredActivity)
                ((SecuredActivity) context).startFormActivity(OUT_OF_CATCHMENT, null, null);
            else
                startJsonForm(OUT_OF_CATCHMENT, null);
        } else if (id == R.id.stock) {
            Intent intent = new Intent(context, PathStockActivity.class);
            intent.putExtra(CURRENT_LOCATION, getCurrentLocation());
            context.startActivity(intent);
        } else if (id == R.id.nav_sync) {
            ServiceTools.startService(context.getApplicationContext(), SyncService.class);
        } else if (id == R.id.nav_hia2) {
            Intent intent = new Intent(context, HIA2ReportsActivity.class);
            context.startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) context.findViewById(R.id.drawer_layout);

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
