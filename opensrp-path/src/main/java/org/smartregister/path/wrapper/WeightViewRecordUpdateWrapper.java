package org.smartregister.path.wrapper;

import org.smartregister.growthmonitoring.domain.Weight;

/**
 * Created by onaio on 14/09/2017.
 */

public class WeightViewRecordUpdateWrapper extends BaseViewRecordUpdateWrapper {

    private Weight weight;

    public Weight getWeight() {
        return weight;
    }

    public void setWeight(Weight weight) {
        this.weight = weight;
    }
}
