var cordova = require('cordova');
var exec = require('cordova/exec');

var LKLib = function () {
    this._level = null;
    this._isPlugged = null;
    this.isScanning = false;
    // Create new event handlers on the window (returns a channel instance)
    this.channels = {
        lkreachabledevices: cordova.addWindowEventHandler('lkreachabledevices')
    };
    for (var key in this.channels) {
        this.channels[key].onHasSubscribersChange = LKLib._onHasSubscribersChange;
    }
};

LKLib._onHasSubscribersChange = function () {
    // If we just registered the first handler, make sure native listener is started.
    if (lklib.channels.lkreachabledevices.numHandlers === 1) {
        lklib.startScan();
    } else if (lklib.channels.lkreachabledevices.numHandlers === 0) {
        lklib.stopScan();
    }
};

LKLib.prototype.hello = function()
{
    console.log("hello called");
    exec(function(msg) {
        console.log(msg);
    }, function (err) {
        console.log(err);
    }, 'LKLib', 'hello', []);
}

LKLib.prototype._reachableCallback = function(obj)
{
    cordova.fireWindowEvent('lkreachabledevices', obj);
}

LKLib.prototype.startScan = function()
{
    if (!this.isScanning) {
        this.isScanning = true;
        exec(this._reachableCallback, null, 'LKLib', 'startDiscovery', []);
    }
}

LKLib.prototype.stopScan = function()
{
    if (this.isScanning) {
        this.isScanning = false;
        exec(null, null, 'LKLib', 'stopDiscovery', []);
    }
}

LKLib.prototype.communicateWithDevice = function(device, message, success, error)
{
    exec(success, error, 'LKLib', 'communicateWithDevice', [device, message]);
}

var lklib = new LKLib();

module.exports = lklib;