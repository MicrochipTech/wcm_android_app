package com.microchip;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by: jossayjacobo
 * Date: 11/18/14
 * Time: 2:49 PM.
 */
public class DataStore {

    private static final String UUID = "uuid";
    private static final String AWS_AMI = "aws_ami";
    private static final String IGNORE_SSL_ERRORS = "ignore_ssl_errors";

    private static EncryptedSharedPreferences getDataStore(Context context) {
        return new EncryptedSharedPreferences(context,
                PreferenceManager.getDefaultSharedPreferences(context));
    }

    private static SharedPreferences.Editor getEditor(Context context) {
        return getDataStore(context).edit();
    }

    private static SharedPreferences getPrefs(Context context) {
        return getDataStore(context);
    }

    public static String getUuid(Context context) {
        return getPrefs(context).getString(UUID, "");
    }

    public static void persistUuid(Context context, String uuid) {
        getEditor(context).putString(UUID, uuid).commit();
    }

    public static String getAwsAmi(Context context) {
        return getPrefs(context).getString(AWS_AMI, "");
    }

    public static void persistAwsAmi(Context context, String awsAmi) {
        getEditor(context).putString(AWS_AMI, awsAmi).commit();
    }

    public static boolean getIgnoreSllErrors(Context context) {
        return getPrefs(context).getBoolean(IGNORE_SSL_ERRORS, false);
    }

    public static void persistIgnoreSllErrors(Context context, boolean ignoreSllErrors) {
        getEditor(context).putBoolean(IGNORE_SSL_ERRORS, ignoreSllErrors).commit();
    }

}
