package org.smartregister.path.sync;

import org.smartregister.domain.FetchStatus;
import org.smartregister.sync.AfterFetchListener;

import static org.smartregister.event.Event.ON_DATA_FETCHED;

public class PathAfterFetchListener implements AfterFetchListener {

    @Override
    public void afterFetch(FetchStatus fetchStatus) {
    }

    public void partialFetch(FetchStatus fetchStatus) {
        ON_DATA_FETCHED.notifyListeners(fetchStatus);
    }
}
