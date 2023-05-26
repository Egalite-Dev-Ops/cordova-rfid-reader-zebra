package rfidReader;

import android.os.AsyncTask;
import android.util.Log;

import com.zebra.rfid.api3.ACCESS_OPERATION_CODE;
import com.zebra.rfid.api3.ACCESS_OPERATION_STATUS;
import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.ENUM_TRANSPORT;
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE;
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE;
import com.zebra.rfid.api3.INVENTORY_STATE;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.Readers;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.SESSION;
import com.zebra.rfid.api3.SL_FLAG;
import com.zebra.rfid.api3.START_TRIGGER_TYPE;
import com.zebra.rfid.api3.STATUS_EVENT_TYPE;
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE;
import com.zebra.rfid.api3.TagData;
import com.zebra.rfid.api3.TriggerInfo;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;

public class RFIDReaderZebra extends CordovaPlugin implements Readers.RFIDReaderEventHandler {
  // a bunch of globals

  private CallbackContext keepCallbackContext = null;
  //
  private volatile HashSet<String> tagDataSet = new HashSet<String>();
  private volatile JSONObject data = null;
  //
  private static Readers readers;
  //private static ArrayList<ReaderDevice> availableRFIDReaderList;
  private static ReaderDevice readerDevice;
  private static RFIDReader reader;
  private static String TAG = "EGA";
  private EventHandler eventHandler;
  //private RFIDHandler rfidScannerHandler;
  private int MAX_POWER = 300;

    
  private void initDataCache() {
    try {
      this.tagDataSet.clear();
      this.data = new JSONObject();
      this.data.put("tags", new JSONArray());
      this.data.put("events", new JSONArray());
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  private synchronized JSONObject dataSwapOrAdd(boolean swap, String event, TagData[] tags) {
    if (swap) {
      JSONObject l_data = this.data;
      initDataCache();
      return l_data;
    } else {
      try {
        if (event != null) {
          this.data.getJSONArray("events").put(event);
        }
        if (tags != null && tags.length != 0) {
          for (int index = 0; index < tags.length; index++) {
            String l_tag = tags[index].getTagID();
            /*
                        Log.d(TAG, "Tag ID " + myTags[index].getTagID());
                        if (myTags[index].getOpCode() ==
                                ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ &&
                                myTags[index].getOpStatus() ==
                                        ACCESS_OPERATION_STATUS.ACCESS_SUCCESS) {
                            if (myTags[index].getMemoryBankData().length() > 0) {
                                Log.d(TAG, " Mem Bank Data " + myTags[index].getMemoryBankData());
                            }
                        }
             */
            if (!tagDataSet.contains(l_tag)) {
              tagDataSet.add(l_tag);
              this.data.getJSONArray("tags").put(l_tag);
            }
          }
        }
      } catch (JSONException e) {
        e.printStackTrace();
      }
      return null;
    }
  }

  private void ConfigureReader() {
    Log.d(TAG, "ConfigureReader " + reader.getHostName());
    if (reader.isConnected()) {
      TriggerInfo triggerInfo = new TriggerInfo();
      triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
      triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);
      try {
        // receive events from reader
        if (eventHandler == null) {
          eventHandler = new EventHandler();
        }
        reader.Events.addEventsListener(eventHandler);
        // HH event
        reader.Events.setHandheldEvent(true);
        // tag event with tag data
        reader.Events.setTagReadEvent(true);
        reader.Events.setAttachTagDataWithReadEvent(false);
        // set trigger mode as rfid so scanner beam will not come
        reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true);
        // set start and stop triggers
        reader.Config.setStartTrigger(triggerInfo.StartTrigger);
        reader.Config.setStopTrigger(triggerInfo.StopTrigger);
        // power levels are index based so maximum power supported get the last one
        MAX_POWER = reader.ReaderCapabilities.getTransmitPowerLevelValues().length - 1;
        // set antenna configurations
        Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
        config.setTransmitPowerIndex(MAX_POWER);
        config.setrfModeTableIndex(0);
        config.setTari(0);
        reader.Config.Antennas.setAntennaRfConfig(1, config);
        // Set the singulation control
        Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
        s1_singulationControl.setSession(SESSION.SESSION_S0);
        s1_singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
        s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
        reader.Config.Antennas.setSingulationControl(1, s1_singulationControl);
        // delete any prefilters
        reader.Actions.PreFilters.deleteAll();
        //
      } catch (InvalidUsageException | OperationFailureException e) {
        e.printStackTrace();
      }
    }
  }
  
  /*private class CreateInstanceTask extends AsyncTask<Void, Void, Void> {

    @Override
    protected Void doInBackground(Void... voids) {
      Log.d(TAG, "CreateInstanceTask");
      // Based on support available on host device choose the reader type
      InvalidUsageException invalidUsageException = null;
      readers = new Readers(webView.getContext(), ENUM_TRANSPORT.ALL);
      try {
        availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
      } catch (InvalidUsageException e) {
        e.printStackTrace();
      }
      if (invalidUsageException != null) {
        readers.Dispose();
        readers = null;
        if (readers == null) {
          readers = new Readers(webView.getContext(), ENUM_TRANSPORT.BLUETOOTH);
        }
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      super.onPostExecute(aVoid);
      connectReader();
    }
  }*/
  
  private class ConnectionTask extends AsyncTask<Void, Void, Boolean> {

    @Override
    protected Boolean doInBackground(Void... voids) {
      try {
        if (readers != null) {
          if (readers.GetAvailableRFIDReaderList() != null) {
            ArrayList<ReaderDevice> availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
            if (availableRFIDReaderList.size() != 0) {
              // get first reader from list
              readerDevice = availableRFIDReaderList.get(0);
              reader = readerDevice.getRFIDReader();
              if (!reader.isConnected()) {
                // Establish connection to the RFID Reader
                reader.connect();
                ConfigureReader();
                return true;
              }
            }
          }
        }
      } catch (InvalidUsageException e) {
        e.printStackTrace();
        dataSwapOrAdd(false, e.getLocalizedMessage(), null);
      } catch (OperationFailureException e) {
        e.printStackTrace();
        Log.d(TAG, "OperationFailureException " + e.getVendorMessage());
        dataSwapOrAdd(false, e.getLocalizedMessage(), null);
      }
      return false;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
      super.onPostExecute(aBoolean);
      if (aBoolean) {
        dataSwapOrAdd(false, "Reader Connected", null);
        //textView.setText("Reader connected");
      }
    }
  }

  // at the initialize function, we can configure the tools we want to use later, like the sensors
  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);

    initDataCache();

    // RFID Handler
    //rfidScannerHandler = new RFIDHandler();
    //rfidScannerHandler = DataProvider.newRFIDHandlerInstance();
    //rfidScannerHandler.onCreate(this);
// SDK
    if (readers == null) {
      readers = new Readers(webView.getContext(), ENUM_TRANSPORT.ALL);
      readers.attach(this);
    }
    new ConnectionTask().execute();
  }

  private void sendPluginResult(boolean keepAlive) {
    JSONObject l_data = dataSwapOrAdd(true, null, null);
    PluginResult result = new PluginResult(PluginResult.Status.OK, l_data);
    if (keepAlive) {
      result.setKeepCallback(true);
    }
    keepCallbackContext.sendPluginResult(result);
    if (!keepAlive) {
      keepCallbackContext = null;
    }
  }
  private synchronized String connect() {
    if (reader != null) {
      Log.d(TAG, "connect " + reader.getHostName());
      try {
        if (!reader.isConnected()) {
          // Establish connection to the RFID Reader
          reader.connect();
          ConfigureReader();
          if (reader.isConnected()) {
            return "Connected: " + reader.getHostName();
          }
        }
      } catch (InvalidUsageException e) {
        e.printStackTrace();
      } catch (OperationFailureException e) {
        e.printStackTrace();
        Log.d(TAG, "OperationFailureException " + e.getVendorMessage());
        String des = e.getResults().toString();
        return "Connection failed" + e.getVendorMessage() + " " + des;
      }
    }
    return "";
  }
  
  private synchronized void disconnect() {
    Log.d(TAG, "disconnect " + reader);
    try {
      if (reader != null) {
        reader.Events.removeEventsListener(eventHandler);
        reader.disconnect();
        dataSwapOrAdd(false, "Disconnecting reader", null);
        reader = null;
      }
    } catch (InvalidUsageException e) {
      e.printStackTrace();
    } catch (OperationFailureException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // safety unregistering from the events if the application stops somehow
  @Override
  public void onDestroy() {
    super.onDestroy();
    if (reader != null) {
      disconnect();
      readers.Dispose();
      readers = null;
    }
  }

  // this is the main part of the plugin, we have to handle all of the actions sent from the js
  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    this.keepCallbackContext = callbackContext;
    if ("start".equals(action)) {
      // here we call a start action to register our event listener to watch the data coming from the accelerometer
      //mSensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI);
      if (reader != null) {
        try {
          reader.Actions.Inventory.perform();
          dataSwapOrAdd(false, "Manual start scanning", null);
        } catch (InvalidUsageException e) {
          dataSwapOrAdd(false, e.getLocalizedMessage(), null);
          e.printStackTrace();
        } catch (OperationFailureException e) {
          dataSwapOrAdd(false, e.getLocalizedMessage(), null);
          e.printStackTrace();
        }
        sendPluginResult(true);
        return true;
      }
    } else if ("stop".equals(action)) {
      // manually stop receiving data.
      //mSensorManager.unregisterListener(listener);
      if (reader != null) {
        try {
          reader.Actions.Inventory.stop();
          dataSwapOrAdd(false, "Manual stop scanning", null);
        } catch (InvalidUsageException e) {
          dataSwapOrAdd(false, e.getLocalizedMessage(), null);
          e.printStackTrace();
        } catch (OperationFailureException e) {
          dataSwapOrAdd(false, e.getLocalizedMessage(), null);
          e.printStackTrace();
        }
        sendPluginResult(true);
        return true;
      }
    } else if ("subscribe".equals(action)) {
      // we send back the latest saved data from the event listener to the success callback
      sendPluginResult(true);
      return true;
    } else if ("unsubscribe".equals(action)) {
      // we send back the latest saved data from the event listener to the success callback
      sendPluginResult(false);
      return true;
    }
    return false;  // Returning false results in a "MethodNotFound" error.
  }

  // this is our event listener which will handle the received data and save it to a global variable
  // Read/Status Notify handler
// Implement the RfidEventsLister class to receive event notifications
  public class EventHandler implements RfidEventsListener {
    // Read Event Notification

    public void eventReadNotify(RfidReadEvents e) {
// Recommended to use new method getReadTagsEx for better performance in case of large tag population
      TagData[] myTags = reader.Actions.getReadTags(100);
      if (keepCallbackContext != null && myTags != null) {
        dataSwapOrAdd(false, null, myTags);
        //dataSwapOrAdd(true, null, null);
        sendPluginResult(true);
      } else {
        Log.d(TAG, "TagData Array Null");
      }
    }
    // Status Event Notification

    public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {
      Log.d(TAG, "Status Notification: "
              + rfidStatusEvents.StatusEventData.getStatusEventType());
      if (rfidStatusEvents.StatusEventData.getStatusEventType()
              == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
        if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent()
                == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED) {
          new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
              try {
                reader.Actions.Inventory.perform();
              } catch (InvalidUsageException e) {
                e.printStackTrace();
              } catch (OperationFailureException e) {
                e.printStackTrace();
              }
              return null;
            }
          }.execute();
        }

        if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent()
                == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED) {
          new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
              try {
                reader.Actions.Inventory.stop();
              } catch (InvalidUsageException e) {
                e.printStackTrace();
              } catch (OperationFailureException e) {
                e.printStackTrace();
              }
              return null;
            }
          }.execute();
        }
      }
    }
  }

  // handler for receiving reader appearance events
  @Override
  public void RFIDReaderAppeared(ReaderDevice readerDevice) {
    Log.d(TAG, "RFIDReaderAppeared " + readerDevice.getName());
    new ConnectionTask().execute();
  }

  @Override
  public void RFIDReaderDisappeared(ReaderDevice readerDevice) {
    Log.d(TAG, "RFIDReaderDisappeared " + readerDevice.getName());
    if (readerDevice.getName().equals(reader.getHostName())) {
      disconnect();
    }
  }

}
