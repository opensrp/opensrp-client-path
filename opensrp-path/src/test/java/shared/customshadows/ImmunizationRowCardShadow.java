package shared.customshadows;

import android.content.Context;
import android.widget.Button;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowLinearLayout;
import org.smartregister.domain.Alert;
import org.smartregister.immunization.domain.State;
import org.smartregister.immunization.domain.VaccineWrapper;
import org.smartregister.immunization.view.ImmunizationRowCard;

import java.util.Date;

import shared.VaccinatorApplicationTestVersion;

/**
 * Created by coder on 06/07/2017.
 */

@Implements(ImmunizationRowCard.class)
public class ImmunizationRowCardShadow extends ShadowLinearLayout {

    @Implementation
    private void init(Context context) {
    }

    @Implementation
    public void setVaccineWrapper(VaccineWrapper vaccineWrapper) {
    }

    @Implementation
    public VaccineWrapper getVaccineWrapper() {
        return null;
    }

    @Implementation
    public void updateState() {
    }

    @Implementation
    public void setOnVaccineStateChangeListener(OnVaccineStateChangeListener onVaccineStateChangeListener) {
    }

    @Implementation
    public State getState() {
        return State.DUE;
    }

    @Implementation
    private void updateStateUi() {
    }

    @Implementation
    private String getVaccineName() {
        return null;
    }

    @Implementation
    private Date getDateDue() {
        return null;
    }

    @Implementation
    private Date getDateDone() {
        return null;
    }

    @Implementation
    private boolean isSynced() {
        return false;
    }

    @Implementation
    private Alert getAlert() {
        return null;
    }

    @Implementation
    private String getStatus() {
        return null;
    }


    @Implementation
    public Button getUndoB() {
        return new Button(VaccinatorApplicationTestVersion.getInstance().getApplicationContext());
    }

    public interface OnVaccineStateChangeListener {
        void onStateChanged(State newState);
    }

}
