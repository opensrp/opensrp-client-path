package org.smartregister.path.view;

import android.graphics.Color;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.View;

import org.smartregister.path.R;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 18/12/2017.
 */

public class StatusSnackbar {

    public static Snackbar showSuccess(@NonNull View rootView, @NonNull String messageText, int snackbarLength) {
        Snackbar successSnackbar = Snackbar.make(rootView, messageText, snackbarLength);

        //Change the background color to green
        setBackgroundColor(successSnackbar, getColor(rootView, R.color.snackbar_success_bg));

        // Add a tick -- Maybe later
        successSnackbar.show();
        return successSnackbar;
    }

    public static Snackbar showError(@NonNull View rootView, @NonNull String messageText, int snackbarLength) {
        Snackbar errorSnackbar = Snackbar.make(rootView, messageText, snackbarLength);

        //Change the background color to green
        setBackgroundColor(errorSnackbar, getColor(rootView, R.color.snackbar_error_bg));

        // Add an X
        errorSnackbar.show();
        return errorSnackbar;
    }

    private static void setBackgroundColor(Snackbar snackbar, @ColorInt int color) {
        snackbar.getView()
                .setBackgroundColor(color);
    }

    private static int getColor(View rootView, int colorId) {
        @ColorInt int color = Color.CYAN;

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            color = rootView.getContext()
                    .getColor(colorId);
        } else {
            color = rootView.getContext()
                    .getResources()
                    .getColor(colorId);
        }

        return color;
    }
}
