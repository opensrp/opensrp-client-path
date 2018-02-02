package org.smartregister.path.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Spinner;

import java.lang.reflect.Field;

/**
 * Created by keyman on 2/2/18.
 */

@SuppressLint("AppCompatCustomView")
public class CustomHeightSpinner extends Spinner {
    private static final String TAG = CustomHeightSpinner.class.getName();

    public CustomHeightSpinner(Context context) {
        super(context);
    }

    public CustomHeightSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomHeightSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void updateHeight(final int height, final int itemCount) {
        try {
            final Field popup = Spinner.class.getDeclaredField("mPopup");
            popup.setAccessible(true);

            // Get private mPopup member variable and try cast to ListPopupWindow
            final android.widget.ListPopupWindow popupWindow = (android.widget.ListPopupWindow) popup.get(this);

            // Set popupWindow height to max - 40dp
            this.post(new Runnable() {
                @Override
                public void run() {
                    int heightToUse = height;
                    if (heightToUse == -1) {
                        heightToUse = CustomHeightSpinner.this.getHeight();
                    }
                    popupWindow.setHeight(heightToUse * itemCount);
                    popup.setAccessible(false);
                }
            });
        } catch (NoClassDefFoundError | ClassCastException | NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }
}