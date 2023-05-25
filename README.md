# CORDOVA RFID SCANNER PLUGIN FOR RFID HH DEVICE  
#### Installation

`cordova plugin add https://github.com/Egalite-Dev-Ops/cordova-rfid-reader-zebra.git`

#### Methods
For start scanning and subscribe to device use:
* `cordova.plugins.RFIDReaderZebra.subscribe(data, successCallback, errorCallback)` - subscribe to device and listen RFID tags
* `cordova.plugins.RFIDReaderZebra.unsubscribe()` - unsubscribe from device