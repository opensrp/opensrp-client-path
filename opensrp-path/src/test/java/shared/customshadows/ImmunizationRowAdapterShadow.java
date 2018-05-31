package shared.customshadows;

import android.view.View;
import android.view.ViewGroup;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowBaseAdapter;
import org.smartregister.immunization.adapter.ImmunizationRowAdapter;
import org.smartregister.immunization.domain.VaccineWrapper;

import java.util.ArrayList;

/**
 * Created by coder on 06/07/2017.
 */

@Implements(ImmunizationRowAdapter.class)
public class ImmunizationRowAdapterShadow extends ShadowBaseAdapter {

    @Implementation
    public void update(ArrayList<VaccineWrapper> vaccinesToUpdate) {
    }

    @Implementation
    public ArrayList<VaccineWrapper> getDueVaccines() {
        return new ArrayList<>();
    }

    @Implementation
    private void updateWrapper(VaccineWrapper tag) {

    }

    @Implementation
    public int getCount() {
        return 0;
    }

    @Implementation
    public Object getItem(int position) {
        return 0;
    }

    @Implementation
    public long getItemId(int position) {
        return 231231 + position;
    }

    @Implementation
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }

}
