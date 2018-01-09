package shared.customshadows;

import android.content.Context;
import android.support.annotation.NonNull;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import io.ona.kujaku.data.realm.RealmDatabase;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 09/01/2018.
 */
@Implements(RealmDatabase.class)
public class ShadowRealmDatabase {

    @Implementation
    public static RealmDatabase init(@NonNull Context context) {
        return null;
    }
}
