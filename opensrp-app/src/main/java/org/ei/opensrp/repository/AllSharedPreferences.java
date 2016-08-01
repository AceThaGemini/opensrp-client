package org.ei.opensrp.repository;

import android.content.SharedPreferences;

import static org.ei.opensrp.AllConstants.*;

public class AllSharedPreferences {
    public static final String ANM_IDENTIFIER_PREFERENCE_KEY = "anmIdentifier";
    private static final String DRISHTI_BASE_URL = "DRISHTI_BASE_URL";
    private static final String HOST = "HOST";
    private static final String PORT = "PORT";
    private SharedPreferences preferences;

    public AllSharedPreferences(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public void updateANMUserName(String userName) {
        preferences.edit().putString(ANM_IDENTIFIER_PREFERENCE_KEY, userName).commit();
    }

    public String fetchRegisteredANM() {
        return preferences.getString(ANM_IDENTIFIER_PREFERENCE_KEY, "").trim();
    }

    public String fetchLanguagePreference() {
        return preferences.getString(LANGUAGE_PREFERENCE_KEY, DEFAULT_LOCALE).trim();
    }

    public void saveLanguagePreference(String languagePreference) {
        preferences.edit().putString(LANGUAGE_PREFERENCE_KEY, languagePreference).commit();
    }

    public Boolean fetchIsSyncInProgress() {
        return preferences.getBoolean(IS_SYNC_IN_PROGRESS_PREFERENCE_KEY, false);
    }

    public void saveIsSyncInProgress(Boolean isSyncInProgress) {
        preferences.edit().putBoolean(IS_SYNC_IN_PROGRESS_PREFERENCE_KEY, isSyncInProgress).commit();
    }

    public String fetchBaseURL(String baseurl){

      return   preferences.getString(DRISHTI_BASE_URL,baseurl);
    }

    public Integer fetchPort(Integer port){


                return  Integer.parseInt( preferences.getString(PORT,""+port));
          //      return  Integer.parseInt(preferences.getString(PORT, "" + port));
           }

    public String getPreference(String key) {
        return preferences.getString(key, "").trim();
           }
    public void setPreference(String key, String value) {
               preferences.edit().putString(key, value).commit();
    }
}

