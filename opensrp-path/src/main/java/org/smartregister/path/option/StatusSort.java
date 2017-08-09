package org.smartregister.path.option;

import android.util.Log;

import org.smartregister.cursoradapter.CursorSortOption;
import org.smartregister.domain.Alert;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.view.contract.SmartRegisterClient;
import org.smartregister.view.contract.SmartRegisterClients;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatusSort implements CursorSortOption {
    private final String name;

    private final Comparator<SmartRegisterClient> commoncomparator = new Comparator<SmartRegisterClient>() {
        @Override
        public int compare(SmartRegisterClient oneClient, SmartRegisterClient anotherClient2) {
            Map<String, Integer> m = new HashMap<>();
            m.put("completed", 0);
            m.put("expired", 1);
            m.put("upcoming", 2);
            m.put("normal", 3);
            m.put("urgent", 4);

            List<Alert> alertlist_for_client1 = VaccinatorApplication.getInstance().context().alertService().findByEntityIdAndAlertNames(oneClient.entityId(), "TT 1", "TT 2", "TT 3", "TT 4", "TT 5", "tt1", "tt2", "tt3", "tt4", "tt5");
            List<Alert> alertlist_for_client2 = VaccinatorApplication.getInstance().context().alertService().findByEntityIdAndAlertNames(anotherClient2.entityId(), "TT 1", "TT 2", "TT 3", "TT 4", "TT 5", "tt1", "tt2", "tt3", "tt4", "tt5");

            Log.i(this.getClass().getName(), "ALERT 1 SIZE: " + alertlist_for_client1.toString());
            Log.i(this.getClass().getName(), "ALERT 2 SIZE: " + alertlist_for_client2.toString());

            int max1 = 0;
            for (int i = 0; i < alertlist_for_client1.size(); i++) {
                Alert a = alertlist_for_client1.get(i);
                Log.i(this.getClass().getName(), "Alert 1:" + a.toString());
                if (m.get(a.status().name()) > max1) {
                    max1 = m.get(a.status().name());
                }
            }

            int max2 = 0;
            for (int i = 0; i < alertlist_for_client2.size(); i++) {
                Alert a = alertlist_for_client2.get(i);
                Log.i(this.getClass().getName(), "Alert 2:" + a.toString());
                if (m.get(a.status().name()) > max2) {
                    max2 = m.get(a.status().name());
                }
            }

            if (max1 == max2) return 0;
            if (max1 > max2) return -1;
            else return 1;
        }
    };

    public StatusSort(String name) {
        this.name = name;
    }

    @Override
    public String sort() {
        return "";
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public SmartRegisterClients sort(SmartRegisterClients allClients) {
        Collections.sort(allClients, commoncomparator);
        return allClients;
    }
}
