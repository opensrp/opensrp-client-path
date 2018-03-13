package org.smartregister.path.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import org.smartregister.path.R;
import org.smartregister.path.toolbar.LocationSwitcherToolbar;
import org.smartregister.stock.StockLibrary;
import org.smartregister.stock.activity.OrderListActivity;
import org.smartregister.stock.activity.StockActivity;
import org.smartregister.stock.activity.StockControlActivity;
import org.smartregister.stock.adapter.StockGridAdapter;
import org.smartregister.stock.domain.StockType;

import java.util.List;

/**
 * Created by samuelgithengi on 2/14/18.
 */

public class PathStockActivity extends BaseActivity {

    private GridView stockGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toolbar toolbar = (Toolbar) findViewById(getToolbarId());
        toolbar.setTitle(R.string.stock_title);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(getDrawerLayoutId());
        TextView nameInitials = (TextView) findViewById(R.id.name_inits);
        nameInitials.setText(getLoggedInUserInitials());
        nameInitials.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });
        stockGrid = (GridView) findViewById(R.id.stockgrid);


        // Enable Orders page to be called
        Button ordersBtn = (Button) findViewById(R.id.btn_stock_orders);
        ordersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PathStockActivity.this, OrderListActivity.class));
            }
        });
    }

    private void refreshAdapter() {
        List<StockType> allStockTypes = StockLibrary.getInstance().getStockTypeRepository().getAllStockTypes(null);
        StockType[] stockTypes = allStockTypes.toArray(new StockType[allStockTypes.size()]);
        StockGridAdapter adapter = new StockGridAdapter(this, stockTypes, StockControlActivity.class);
        stockGrid.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAdapter();
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_stock;
    }

    @Override
    protected int getDrawerLayoutId() {
        return R.id.drawer_layout;
    }

    @Override
    protected int getToolbarId() {
        return LocationSwitcherToolbar.TOOLBAR_ID;
    }

    @Override
    protected Class onBackActivity() {
        return null;
    }

}
