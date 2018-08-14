var cordova = require('cordova');
var exec = require('cordova/exec');

var LKLib = function() {
    this.hello = function() {
        exec(function(msg) {
            alert(msg);
        }, function (err) {
            console.log(err);
        }, 'LKLib', 'hello', []);
    }
}

module.exports = new LKLib();