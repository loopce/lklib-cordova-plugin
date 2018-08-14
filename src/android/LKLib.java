package br.com.loopkey.lklib.cordova;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LKLib extends CordovaPlugin
{
    public LKLib()
    {}

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext)
    {
        if (action.equals("hello")) {
            callbackContext.success("world!");
        }
        return false;
    }
}