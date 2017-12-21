package org.smartregister.path.activity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.smartregister.immunization.domain.VaccineType;
import org.smartregister.path.R;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.repository.StockRepository;
import org.smartregister.path.tabfragments.CurrentStock;
import org.smartregister.path.tabfragments.PlanningStockFragment;
import org.smartregister.path.toolbar.LocationSwitcherToolbar;

public class StockControlActivity extends BaseActivity {

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    public VaccineType vaccineType;
    public PlanningStockFragment planningStockFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        vaccineType = (VaccineType) getIntent().getSerializableExtra("vaccine_type");

        setTitle("");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        LocationSwitcherToolbar toolbar = (LocationSwitcherToolbar) getToolbar();
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StockControlActivity.this, StockActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        ((TextView) toolbar.findViewById(R.id.title)).setText("Stock Control > " + vaccineType.getName());

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        ViewPager mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

    }

    @Override
    protected void onResume() {
        super.onResume();
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        LinearLayout stockregister = (LinearLayout) drawer.findViewById(R.id.stock_control);
        stockregister.setBackgroundColor(getResources().getColor(R.color.tintcolor));
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_stock_control;
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
        return StockActivity.class;
    }


    public int getcurrentVialNumber() {
        net.sqlcipher.database.SQLiteDatabase db = VaccinatorApplication.getInstance().getRepository().getReadableDatabase();
        Cursor c = db.rawQuery("Select sum(value) from Stocks where " + StockRepository.DATE_CREATED + " <= " + new DateTime(System.currentTimeMillis()).toDate().getTime() + " and " + StockRepository.VACCINE_TYPE_ID + " = " + vaccineType.getId(), null);
        String stockvalue = "0";
        if (c.getCount() > 0) {
            c.moveToFirst();
            if (c.getString(0) != null && !StringUtils.isBlank(c.getString(0))) {
                stockvalue = c.getString(0);
            }
            c.close(); 
        } else {
            c.close();
        }
        return Integer.parseInt(stockvalue);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_stock_control, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position) {
                case 0:
                    return CurrentStock.newInstance("", "");
                case 1:
                    planningStockFragment = PlanningStockFragment.newInstance("", "");
                    return planningStockFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Current Stock";
                case 1:
                    return "Stock Planning";
            }
            return null;
        }
    }
}
