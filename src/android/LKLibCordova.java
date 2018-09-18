package br.com.loopkey.lklib.cordova;

import br.com.loopkey.indigo.lklib.LKLib;
import br.com.loopkey.indigo.lklib.LKMainThreadImpl;
import br.com.loopkey.indigo.lklib.core.LKReachableDeviceListener;
import br.com.loopkey.indigo.lklib.core.LKReachableDevice;
import br.com.loopkey.indigo.lklib.core.LKReachableDeviceImpl;
import br.com.loopkey.indigo.lklib.entity.LKCommDevice;
import br.com.loopkey.indigo.lklib.entity.LKEntity;
import br.com.loopkey.indigo.lklib.entity.LKSerialData;
import br.com.loopkey.indigo.lklib.commands.LKCommand;
import br.com.loopkey.indigo.lklib.commands.LKCommandRepository;
import br.com.loopkey.indigo.lklib.commands.implementations.LKUnlockCommand;
import br.com.loopkey.indigo.lklib.commands.LKDeviceCommunicator;

import java.security.InvalidParameterException;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import android.util.Base64;

import java.util.Arrays;
import java.util.List;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.security.InvalidParameterException;

public class LKLibCordova extends CordovaPlugin implements LKReachableDeviceListener
{
    private String _test = "nope";

    private LKReachableDevice _lkReachableDevice;

    private CallbackContext _reachableCallbackContext = null;

    private CallbackContext _commCallbackContext = null;

    private LKDeviceCommunicator _communicator = null;

    public LKLibCordova()
    {}

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext)
    {
        LOG.d("LKLIB", "action: " + action);
        if (action.equals("hello")) {
            callbackContext.success("world!" + this._test);
            return true;
        } else if (action.equals("startDiscovery")) {
            _startDiscovery(callbackContext);
            return true;
        } else if (action.equals("stopDiscovery")) {
            _stopDiscovery();
            return true;
        } else if (action.equals("communicateWithDevice")) {
            _communicateWithDevice(args, callbackContext);
            return true;
        }
        return false;
    }

    private void _communicateWithDevice(JSONArray args, CallbackContext callbackContext)
    {
        if (_commCallbackContext != null) {
            callbackContext.error("CommunicationBusy");
            return;
        }

        String command = null;
        JSONObject deviceObj = null;
        try {
            deviceObj = args.getJSONObject(0);
            command = args.getString(1);
            if (deviceObj == null || command == null) {
                callbackContext.error("InvalidParameters");
                return;
            }
        } catch (JSONException _) {
            callbackContext.error("InvalidParameters");
            return;
        }

        _commCallbackContext = callbackContext;

        if (command.equals("unlock")) {
            try {
                LKSerialData serialData = new LKSerialData(deviceObj.getString("serial"));
                LKCommDevice commDevice = new LKCommDevice(serialData.getSerialCode());
                String deviceKey = deviceObj.getString("key");
                commDevice.key = Base64.decode(deviceKey, 0);

                LKCommand lkCommand = LKCommandRepository.createUnlockCommand(new LKUnlockCommand.Listener() {
                    @Override
                    public void onResponse(LKUnlockCommand.Response response) {
                        _commCallbackContext.success(response.toString());
                        _commCallbackContext = null;
                        _communicator = null;
                    }
        
                    @Override
                    public void onError(LKCommand.Error error) {
                        _commCallbackContext.error(error.toString());
                        _commCallbackContext = null;
                        _communicator = null;
                    }
                });

                lkCommand.setDevice(commDevice);
                lkCommand.setUserId(0);

                _communicator = _lkReachableDevice.deviceCommunicatorForDevice(commDevice, 0);

                if (_communicator == null) {
                    _commCallbackContext.error("DeviceNotReachable");
                    _commCallbackContext = null;
                    return;
                }

                _communicator.execute(lkCommand);
            } catch (InvalidParameterException _) {
                _commCallbackContext.error("InvalidSerial");
                _commCallbackContext = null;
            } catch (IllegalArgumentException _) {
                _commCallbackContext.error("InvalidBase64Data");
                _commCallbackContext = null;
            } catch (JSONException _) {
                _commCallbackContext.error("InvalidParameters");
                _commCallbackContext = null;
            }
        } else {
            _commCallbackContext.error("InvalidCommand");
            _commCallbackContext = null;
        }
    }

    private void _startDiscovery(CallbackContext callbackContext)
    {
        LOG.d("LKLIB", "Discovery starting...");
        _reachableCallbackContext = callbackContext;
        _lkReachableDevice.subscribe(this);
    }

    private void _stopDiscovery()
    {
        _lkReachableDevice.unsubscribe(this);
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView)
    {
        super.initialize(cordova, webView);
        Context context = cordova.getActivity().getApplicationContext();
        LKLib.install(context, new LKMainThreadImpl());
        _lkReachableDevice = LKReachableDeviceImpl.newInstance();
        this._test = "YEAH!";
    }

    @Override
    public void onReachableDevices(List<LKEntity> lkEntityList)
    {
        // Here, we obtain iterate through all the currently reachable devices.
        JSONObject obj = new JSONObject();
        JSONArray arr = new JSONArray();
        for (LKEntity device : lkEntityList) {
            LOG.d("LKLIB", device.getName());
            LOG.d("LKLIB", Base64.encodeToString(device.getSerialCode(), 0));
            LOG.d("LKLIB", "Device Roll Count (from broadcast): " +
                    Base64.encodeToString(device.getRollCount(), 0));
            JSONObject entityObj = _entityToObject(device);
            if (entityObj != null) {
                arr.put(entityObj);
            }
        }
        try {
            obj.put("devices", arr);
        } catch(Exception _) {}
        PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
        result.setKeepCallback(true);
        _reachableCallbackContext.sendPluginResult(result);
    }

    private JSONObject _entityToObject(LKEntity entity)
    {
        try {
            JSONObject obj = new JSONObject();
            obj.put("serial", entity.getSerialData().toString());
            obj.put("name", entity.getName());
            obj.put("rollCountData", Base64.encodeToString(entity.getRollCount(), 0).trim());
            obj.put("rssi", entity.getRSSI());
            obj.put("flags", entity.getFlags());
            return obj;
        } catch(Exception _) {
            return null;
        }
    }
}