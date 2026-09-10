package com.example.gastofacil;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.util.Locale;

public class LocaleHelper {
    public static Context applyLocale(Context context, String language) {
        if (language == null) language = "es";
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);
        
        // Guardar en SharedPreferences para acceso rápido
        context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
                .edit()
                .putString("My_Lang", language)
                .apply();

        return context.createConfigurationContext(configuration);
    }

    public static Context onAttach(Context context) {
        String lang = getLanguage(context);
        return applyLocale(context, lang);
    }

    public static String getLanguage(Context context) {
        return context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
                .getString("My_Lang", "es");
    }
}
