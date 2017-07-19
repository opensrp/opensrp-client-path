package org.opensrp.path.sync;

import org.opensrp.domain.FetchStatus;
import org.opensrp.sync.AfterFetchListener;

import static org.opensrp.event.Event.ON_DATA_FETCHED;

public class PathAfterFetchListener implements AfterFetchListener {

    @Override
    public void afterFetch(FetchStatus fetchStatus) {
    }

    void partialFetch(FetchStatus fetchStatus) {
        ON_DATA_FETCHED.notifyListeners(fetchStatus);
    }
}
