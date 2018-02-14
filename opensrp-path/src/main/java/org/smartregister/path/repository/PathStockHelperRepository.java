package org.smartregister.path.repository;

import org.smartregister.repository.Repository;
import org.smartregister.stock.domain.ActiveChildrenStats;
import org.smartregister.stock.repository.StockExternalRepository;

/**
 * Created by samuelgithengi on 2/14/18.
 */

public class PathStockHelperRepository extends StockExternalRepository {

    public PathStockHelperRepository(Repository repository) {
        super(repository);
    }

    @Override
    public int getVaccinesUsedUntilDate(Long aLong, String s) {
        return 12;
    }

    @Override
    public int getVaccinesUsedToday(Long aLong, String s) {
        return 31;
    }

    @Override
    public ActiveChildrenStats getActiveChildrenStat() {
        ActiveChildrenStats childrenStats = new ActiveChildrenStats();
        childrenStats.setChildrenLastMonthZeroToEleven(12l);
        childrenStats.setChildrenLastMonthtwelveTofiftyNine(59l);
        childrenStats.setChildrenThisMonthZeroToEleven(14l);
        childrenStats.setChildrenThisMonthtwelveTofiftyNine(61l);
        return childrenStats;
    }
}
