var exec = require('cordova/exec');

exports.init = function (arg0, success, error) {
    exec(success, error, 'RFIDReaderZebra', 'init', [arg0]);
};

exports.subscribe = function (arg0, success, error) {
    exec(success, error, 'RFIDReaderZebra', 'subscribe', [arg0]);
};

exports.unsubscribe = function (arg0, success, error) {
    exec(success, error, 'RFIDReaderZebra', 'unsubscribe', [arg0]);
};