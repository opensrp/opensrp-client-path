package shared;

import android.util.Log;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.TestLifecycleApplication;
import org.smartregister.Context;
import org.smartregister.domain.Alert;
import org.smartregister.growthmonitoring.repository.WeightRepository;
import org.smartregister.immunization.domain.ServiceRecord;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.repository.RecurringServiceRecordRepository;
import org.smartregister.immunization.repository.RecurringServiceTypeRepository;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.service.AlertService;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;

import static org.mockito.Matchers.anyString;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created by onadev on 15/06/2017.
 */

public class VaccinatorApplicationTestVersion extends VaccinatorApplication implements TestLifecycleApplication {
    @Mock
    private VaccineRepository vaccineRepository;
    @Mock
    private RecurringServiceRecordRepository recurringServiceRecordRepository;
    @Mock
    private RecurringServiceTypeRepository recurringServiceTypeRepository;
    @Mock
    private AlertService alertService;
    public static final String TAG = VaccinatorApplicationTestVersion.class.getCanonicalName();

    @Override
    public void onCreate() {
        super.onCreate();
        initMocks(this);

        when(vaccineRepository.findByEntityId(anyString())).thenReturn(Collections.<Vaccine>emptyList());

        when(recurringServiceRecordRepository.findByEntityId(anyString())).thenReturn(Collections.<ServiceRecord>emptyList());

        try {
            Field field = Context.class.getDeclaredField("alertService");
            field.setAccessible(true);
            field.set(context, alertService);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        when(alertService.findByEntityIdAndAlertNames(Mockito.anyString(), Mockito.any(String[].class))).thenReturn(new ArrayList<Alert>());


        mInstance = this;

    }

    @Override
    public void beforeTest(Method method) {
    }

    @Override
    public void prepareTest(Object test) {

    }

    @Override
    public void afterTest(Method method) {
    }

    @Override
    public VaccineRepository vaccineRepository() {
        return vaccineRepository;
    }

    @Override
    public WeightRepository weightRepository() {
        return weightRepository;
    }

    @Override
    public RecurringServiceRecordRepository recurringServiceRecordRepository() {
        return recurringServiceRecordRepository;
    }

    public static synchronized VaccinatorApplication getInstance() {
        return (VaccinatorApplication) mInstance;
    }

    @Mock
    WeightRepository weightRepository;

    @Override
    public RecurringServiceTypeRepository recurringServiceTypeRepository() {
        return recurringServiceTypeRepository;

    }

    @Override
    public void applyUserLanguagePreference() {

    }

    @Override
    public void cleanUpSyncState() {
    }

}
