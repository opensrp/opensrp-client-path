package org.smartregister.path.activity.mocks;

import android.os.Bundle;

import org.smartregister.domain.Photo;
import org.smartregister.path.R;
import org.smartregister.path.activity.ChildDetailTabbedActivity;


/**
 * Created by onadev on 29/06/2017.
 */
public class ChildDetailTabbedActivityTestVersion extends ChildDetailTabbedActivity {


    @Override
    public void onCreate(Bundle bundle) {
        setTheme(R.style.AppTheme); //we need this here
        super.onCreate(bundle);
    }

    @Override
    protected Photo getProfilePhotoByClient() {
        return new Photo();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }


    @Override
    protected void onRestart() {
        super.onRestart();
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
