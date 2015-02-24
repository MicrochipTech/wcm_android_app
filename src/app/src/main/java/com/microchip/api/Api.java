package com.microchip.api;

import android.content.Context;
import android.content.Intent;

import com.microchip.api.model.Status.Status;
import com.microchip.utils.GGson;

/**
 * Created by: jossayjacobo
 * Date: 11/18/14
 * Time: 3:46 PM.
 */
public class Api {

    public static void getStatus(Context context) {
        Intent intent = new Intent(context, ApiService.class);
        intent.putExtra(ApiService.API_TYPE, ApiService.TYPE_GET_STATUS);
        context.startService(intent);
    }

    public static void postStatus(Context context, Status status) {
        Intent intent = new Intent(context, ApiService.class);
        intent.putExtra(ApiService.API_TYPE, ApiService.TYPE_POST_STATUS);
        intent.putExtra(ApiService.STATUS, GGson.toJson(status));
        context.startService(intent);
    }

}
