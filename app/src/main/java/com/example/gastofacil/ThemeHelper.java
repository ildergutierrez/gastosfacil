package com.example.gastofacil;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import java.io.InputStream;

public class ThemeHelper {

    public static boolean isDarkMode(Context context) {
        if (context == null) return false;
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    public static String getSplashVideoName(Context context) {
        return isDarkMode(context) ? "logo_animado.mp4" : "screen_blanco.mp4";
    }

    public static void applyBackground(Activity activity, View viewRoot, boolean isFondo1) {
        if (activity == null) return;

        // Obtener la vista raíz real del layout inflado
        ViewGroup contentParent = activity.findViewById(android.R.id.content);
        if (contentParent == null || contentParent.getChildCount() == 0) return;
        View actualRoot = contentParent.getChildAt(0);

        boolean darkMode = isDarkMode(activity);

        if (darkMode) {
            String[] darkAssets = {"fondo_black.jpeg", "fondo_black.jpg", "fondo_black.png", "f1_black.png"};
            for (String assetName : darkAssets) {
                try {
                    InputStream is = activity.getAssets().open(assetName);
                    Drawable drawable = Drawable.createFromStream(is, null);
                    is.close();
                    if (drawable != null) {
                        actualRoot.setBackground(drawable);
                        return;
                    }
                } catch (Exception ignored) {
                }
            }
            // Si el archivo en assets no existe aún, se usa un fondo oscuro (#121212)
            actualRoot.setBackgroundColor(Color.parseColor("#121212"));
        } else {
            String lightAsset = isFondo1 ? "fondo_1.png" : "fondo_2.png";
            try {
                InputStream is = activity.getAssets().open(lightAsset);
                Drawable drawable = Drawable.createFromStream(is, null);
                is.close();
                if (drawable != null) {
                    actualRoot.setBackground(drawable);
                    return;
                }
            } catch (Exception ignored) {
            }
            int fallbackRes = isFondo1 ? R.drawable.fondo_1 : R.drawable.fondo_2;
            actualRoot.setBackgroundResource(fallbackRes);
        }
    }
}
