package shared;

import org.smartregister.path.domain.ServiceRecord;
import org.smartregister.path.domain.Vaccine;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.repository.RecurringServiceRecordRepository;
import org.smartregister.path.repository.RecurringServiceTypeRepository;
import org.smartregister.path.repository.VaccineRepository;
import org.smartregister.growthmonitoring.repository.WeightRepository;
import org.mockito.Mock;
import org.robolectric.TestLifecycleApplication;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.mockito.Matchers.anyString;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created by onadev on 15/06/2017.
 */
public class VaccinatorApplicationTestVersion extends VaccinatorApplication implements TestLifecycleApplication {
    @Mock
    VaccineRepository vaccineRepository;
    @Mock
    RecurringServiceRecordRepository recurringServiceRecordRepository;
    @Mock
    RecurringServiceTypeRepository recurringServiceTypeRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        initMocks(this);

        when(vaccineRepository.findByEntityId(anyString())).thenReturn(Collections.<Vaccine>emptyList());

        when(recurringServiceRecordRepository.findByEntityId(anyString())).thenReturn(Collections.<ServiceRecord>emptyList());

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
