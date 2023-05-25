var exec = require('cordova/exec');

exports.subscribe = function (arg0, success, error) {
    exec(success, error, 'RFIDReaderZebra', 'subscribe', [arg0]);
};

exports.unsubscribe = function (arg0, success, error) {
    exec(success, error, 'RFIDReaderZebra', 'unsubscribe', [arg0]);
};