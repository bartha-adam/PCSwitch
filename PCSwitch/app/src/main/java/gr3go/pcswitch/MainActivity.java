package gr3go.pcswitch;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.IntentFilter;
import android.content.Context;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import pcswitch.common.MACAddress;


public class MainActivity extends ActionBarActivity
        implements MyLogger,
        PCManagerListener,
        OnItemSelectedListener,
        DialogInterface.OnClickListener,
        NetworkingUtils.WifiStateObserver {
    Logger LOG = LoggerFactory.getLogger(MainActivity.class);

    private NetworkingUtils networkingUtils;
    private CommLink commLink;
    private PCManager pcManager;
    private Thread pcManagerThread;
    PC selectedRemotePC;
    AlertDialog shutdownOptionDialog;
    private boolean isWifiConnectionAvailable;

    private enum Layouts {
        Layout_Loading,
        Layout_NoWifi,
        Layout_Normal,
        Layout_EmptyModel
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOG.info("Starting application!");
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        Initialize();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(networkingUtils);
        StopFunctionality();
    }

    @Override
    public void onStart() {
        //UI controls are created
        super.onStart();

        UpdateLayouts(Layouts.Layout_Loading);

        Spinner pcSpinner = (Spinner) findViewById(R.id.spnPCSelector);
        pcSpinner.setOnItemSelectedListener(this);

        isWifiConnectionAvailable = networkingUtils.IsWifiConnected();
        if (isWifiConnectionAvailable) {
            OnWifiConnected();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

    public void btnDiscoverServersClicked(View view) {
        pcManager.SendGetServerStatus_Broadcast();
    }

    @Override
    public void onClick(DialogInterface dialog, int item) {
        //TODO: rename this method
        dialog.dismiss();
        if (dialog == shutdownOptionDialog) {
            switch (item) {
                case 0: //Now
                    pcManager.SetShutdownDelayFor(selectedRemotePC, 0);
                    break;
                case 1:
                    showTimePicker();
                    break;
            }
        }
    }

    OnTimeSetListener shutdownTimeSelectCallback = new OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            if (view.isShown()) {
                // This method will return true only once
                String logLine = "Selected time:";
                logLine += hourOfDay;
                logLine += ":";
                logLine += minute;
                LOG.debug(logLine);

                int secondsInMin = 60;
                int secondsInHour = 60 * secondsInMin;
                pcManager.SetShutdownDelayFor(selectedRemotePC, hourOfDay * secondsInHour + minute * secondsInMin);
            }
        }
    };

    public void showTimePicker() {
        int defaultMin = 30;
        int defaultHour = 0;
        if (selectedRemotePC != null && selectedRemotePC.IsShutdownConfigured()) {
            int configuredShutdownInSec = selectedRemotePC.GetShutdownDelay();
            int secondsInMin = 60;
            int secondsInHour = 60 * secondsInMin;
            int secondsInDay = 24 * secondsInHour;
            if (configuredShutdownInSec >= secondsInDay) {
                defaultHour = 23;
                defaultMin = 59;
            } else {
                defaultHour = configuredShutdownInSec / secondsInHour;
                defaultMin = (configuredShutdownInSec % secondsInHour) / secondsInMin;
            }
        }
        TimePickerDialog timePicker = new TimePickerDialog(this, shutdownTimeSelectCallback,
                defaultHour, defaultMin, true);
        timePicker.setTitle(getResources().getString(R.string.chooseShutdownDelay));
        timePicker.show();
    }

    public void txtShutDownInValueClicked(View view) {
        final CharSequence[] items = {" Now ", " Delayed "};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Shutdown");
        builder.setSingleChoiceItems(items, 0, this);
        shutdownOptionDialog = builder.create();
        shutdownOptionDialog.show();
    }

    public void btnCancelShutdownClicked(View view) {
        pcManager.SetShutdownDelayFor(selectedRemotePC, -1); //Cancel
    }

    public void btnWakeUpClicked(View view) {
        if (selectedRemotePC != null) {
            if (selectedRemotePC.GetStatus() == PC.Status.OFF) {
                InetAddress broadcastAddress = networkingUtils.GetBroadcastAddress();
                if (broadcastAddress != null)
                    new WOLSender().execute(new WOLSenderParams(broadcastAddress,
                            selectedRemotePC.GetMACAddress()));
            } else {
                LOG.warn("Wakeup skipped, pc status is not OFF! " + selectedRemotePC.toString());
            }
        } else {
            LOG.warn("Wakeup skipped, selectedRemotePC is null");
        }
    }

    @Override
    public void LogMessage(String msg) {
        LOG.info(msg);
    }

    @Override
    public void PCChanged(PC pc) {
        RunnableWithPCParam changeAction = new RunnableWithPCParam(pc) {
            public void run() {
                UpdateUIFromModel(pc);
            }
        };
        if (pc == selectedRemotePC) {
            if (IsUIThread()){
                changeAction.run();
            } else {
                MainActivity.this.runOnUiThread(changeAction);
            }
        }
    }

    @Override
    public void PCAdded(PC pc) {
        final MainActivity main = this;
        RunnableWithPCParam addAction = new RunnableWithPCParam(pc) {
            public void run() {
                Spinner spinner = (Spinner) findViewById(R.id.spnPCSelector);
                if (spinner == null) {
                    LOG.error("PCAdded spinner is null!");
                    return;
                }

                if (pcManager == null) {
                    LOG.error("PCAdded pcManager is null!");
                    return;
                }

                List<String> list = new ArrayList<String>();
                Vector<PC> remotePCs = pcManager.GetRemotePCs();
                Iterator<PC> it = remotePCs.iterator();
                while (it.hasNext()) {
                    PC entry = (PC) it.next();
                    //list.add(entry.GetAddress().getHostAddress());
                    //list.add(entry.GetMACAddress().toString());
                    list.add(entry.GetName());
                }
                ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(main,
                        android.R.layout.simple_spinner_item, list);
                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(dataAdapter);

                if (remotePCs.isEmpty())
                    UpdateLayouts(Layouts.Layout_EmptyModel);
                else
                    UpdateLayouts(Layouts.Layout_Normal);
            }
        };
        if (IsUIThread()) {
            addAction.run();
        } else {
            MainActivity.this.runOnUiThread(addAction);
        }
    }

    private void ReloadPCList() {
        // Update PC list
        final MainActivity main = this;
        Runnable addAction = new Runnable() {
            public void run() {
                Spinner spinner = (Spinner) findViewById(R.id.spnPCSelector);
                if (spinner == null) {
                    LOG.error("PCAdded spinner is null!");
                    return;
                }

                if (pcManager == null) {
                    LOG.error("PCAdded pcManager is null!");
                    return;
                }

                List<String> list = new ArrayList<String>();
                Vector<PC> remotePCs = pcManager.GetRemotePCs();
                Iterator<PC> it = remotePCs.iterator();
                while (it.hasNext()) {
                    PC entry = (PC) it.next();
                    //list.add(entry.GetAddress().getHostAddress());
                    //list.add(entry.GetMACAddress().toString());
                    list.add(entry.GetName());
                }
                ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(main,
                        android.R.layout.simple_spinner_item, list);
                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(dataAdapter);

                // Need to update the values here since, the onItemSelected
                // method will be called with a delay and flicker occurs
                if (selectedRemotePC == null && !remotePCs.isEmpty()) {
                    selectedRemotePC = remotePCs.elementAt(0);
                    UpdateUIFromModel(selectedRemotePC);
                }

                if (remotePCs.isEmpty())
                    UpdateLayouts(Layouts.Layout_EmptyModel);
                else
                    UpdateLayouts(Layouts.Layout_Normal);
            }
        };
        if (IsUIThread()) {
            addAction.run();
        } else {
            MainActivity.this.runOnUiThread(addAction);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View arg1, int pos, long id) {
        String name = parent.getItemAtPosition(pos).toString();
        //Suppose we are not sorting the DataSource
        //selectedRemotePC = pcManager.FindPC(new MACAddress(mac));
        selectedRemotePC = pcManager.GetRemotePCs().elementAt(pos);
        LOG.debug("Selection changed to " + selectedRemotePC.GetName());
        pcManager.SetSelectedPC(selectedRemotePC);
        UpdateUIFromModel(selectedRemotePC);
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

    public void setTextViewClickable(TextView textView, Boolean flag) {
        if (flag) {
            textView.setPaintFlags(textView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        } else {
            textView.setPaintFlags(textView.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
        }
        textView.setClickable(flag);
    }

    // TODO: remove hardcoded strings from this method
    public void UpdateUIFromModel(PC remotePC) {
        View view;

        TextView txtStatusValue = null;
        if ((view = findViewById(R.id.txtStatusValue)) != null) {
            txtStatusValue = (TextView) view;
        }

        TextView txtShutdownDelayValue = null;
        if ((view = findViewById(R.id.txtShutDownInValue)) != null) {
            txtShutdownDelayValue = (TextView) view;
        }

        TextView txtMACValue = null;
        if ((view = findViewById(R.id.txtMACValue)) != null) {
            txtMACValue = (TextView) view;
        }

        TextView txtIPValue = null;
        if ((view = findViewById(R.id.txtIPValue)) != null) {
            txtIPValue = (TextView) view;
        }

        Button btnWakeUp = null;
        if ((view = findViewById(R.id.btnWakeUp)) != null) {
            btnWakeUp = (Button) view;
        }

        Button btnCancelShutdown = null;
        if ((view = findViewById(R.id.btnCancelShutdown)) != null) {
            btnCancelShutdown = (Button) view;
        }

        if (remotePC != null) {
            if (txtMACValue != null) {
                if (remotePC.GetMACAddress() != null) {
                    txtMACValue.setText(remotePC.GetMACAddress().toString());
                } else {
                    txtMACValue.setText(getResources().getString(R.string.notavailable));
                }
            }
            if (txtIPValue != null) {
                if (remotePC.GetAddressStr() != null) {
                    txtIPValue.setText(remotePC.GetAddressStr());
                } else {
                    txtIPValue.setText(getResources().getString(R.string.notavailable));
                }
            }


            if (remotePC.GetStatus() == PC.Status.ON) {
                if (txtStatusValue != null) {
                    txtStatusValue.setText(getResources().getString(R.string.on));
                    txtStatusValue.setTextColor(Color.GREEN);
                }
                if (btnWakeUp != null) {
                    btnWakeUp.setVisibility(View.GONE);
                }
                if (txtShutdownDelayValue != null) {
                    setTextViewClickable(txtShutdownDelayValue, true);
                }

                if (remotePC.IsShutdownConfigured()) {
                    if (btnCancelShutdown != null) {
                        btnCancelShutdown.setVisibility(View.VISIBLE);
                    }
                    int shutdownDelay = remotePC.GetShutdownDelay();
                    String shutdownDelayStr = new String();

                    int secondsInMinute = 60;
                    int secondsInHour = 60 * secondsInMinute;
                    int secondsInDay = 24 * secondsInHour;

                    if (shutdownDelay >= secondsInDay) {
                        // Days
                        int numDays = shutdownDelay / secondsInDay;
                        shutdownDelayStr += numDays + "d ";
                        shutdownDelay = shutdownDelay % secondsInDay;
                    }

                    if (shutdownDelay >= secondsInHour) {
                        // Hours
                        int numHours = shutdownDelay / secondsInHour;
                        shutdownDelayStr += numHours + "h ";
                        shutdownDelay = shutdownDelay % secondsInHour;
                    }

                    if (shutdownDelay > secondsInMinute) {
                        // Minutes
                        int numMinutes = shutdownDelay / secondsInMinute;
                        shutdownDelayStr += numMinutes + "m ";
                        shutdownDelay = shutdownDelay % secondsInMinute;
                    }

                    // Seconds shown only when delay is < 1min
                    if (shutdownDelayStr.length() == 0) {
                        shutdownDelayStr += shutdownDelay + "s";
                    }
                    if (txtShutdownDelayValue != null) {
                        txtShutdownDelayValue.setText(shutdownDelayStr);
                    }
                } else {
                    if (btnCancelShutdown != null)
                        btnCancelShutdown.setVisibility(View.GONE);
                    if (txtShutdownDelayValue != null)
                        txtShutdownDelayValue.setText(getResources().getString(R.string.notset));
                }
            } else if (remotePC.GetStatus() == PC.Status.OFF) {
                if (txtStatusValue != null) {
                    txtStatusValue.setText(getResources().getString(R.string.off));
                    txtStatusValue.setTextColor(Color.RED);
                }
                if (txtShutdownDelayValue != null) {
                    txtShutdownDelayValue.setText(getResources().getString(R.string.notavailable));
                }
                if (btnWakeUp != null) {
                    btnWakeUp.setVisibility(View.VISIBLE);
                }
                if (txtShutdownDelayValue != null) {
                    setTextViewClickable(txtShutdownDelayValue, false);
                }
                if (btnCancelShutdown != null) {
                    btnCancelShutdown.setVisibility(View.GONE);
                }

            } else if (remotePC.GetStatus() == PC.Status.ShuttingDown) {
                if (txtStatusValue != null) {
                    txtStatusValue.setText(getResources().getString(R.string.shutingdown));
                    txtStatusValue.setTextColor(Color.parseColor("#ffa500"));
                }
                if (txtShutdownDelayValue != null) {
                    txtShutdownDelayValue.setText(getResources().getString(R.string.notavailable));
                }
                if (btnWakeUp != null) {
                    btnWakeUp.setVisibility(View.GONE);
                }
                if (txtShutdownDelayValue != null) {
                    setTextViewClickable(txtShutdownDelayValue, false);
                }
                if (btnCancelShutdown != null) {
                    btnCancelShutdown.setVisibility(View.GONE);
                }
            }
        } else {
            // Default case, probably should not get here
            if (txtStatusValue != null) {
                txtStatusValue.setText(getResources().getString(R.string.notavailable));
                txtStatusValue.setTextColor(Color.BLACK);
            }
            if (txtShutdownDelayValue != null) {
                txtShutdownDelayValue.setText(getResources().getString(R.string.notavailable));
                setTextViewClickable(txtShutdownDelayValue, false);
            }
        }
    }

    public void UpdateLayouts(Layouts layout) {
        View view = null;
        RelativeLayout topLayout = null;
        if ((view = findViewById(R.id.topLayout)) != null) {
            topLayout = (RelativeLayout) view;
        }

        RelativeLayout middleLayout = null;
        if ((view = findViewById(R.id.middleLayout)) != null) {
            middleLayout = (RelativeLayout) view;
        }

        RelativeLayout noWifiLayout = null;
        if ((view = findViewById(R.id.noWifiLayout)) != null) {
            noWifiLayout = (RelativeLayout) view;
        }

        RelativeLayout emptyModelLayout = null;
        if ((view = findViewById(R.id.emptyModelLayout)) != null) {
            emptyModelLayout = (RelativeLayout) view;
        }

        switch (layout) {
            case Layout_NoWifi:
                if (topLayout != null)
                    topLayout.setVisibility(View.GONE);
                if (middleLayout != null)
                    middleLayout.setVisibility(View.GONE);
                if (emptyModelLayout != null)
                    emptyModelLayout.setVisibility(View.GONE);
                if (noWifiLayout != null)
                    noWifiLayout.setVisibility(View.VISIBLE);
                break;
            case Layout_Loading:
                if (topLayout != null)
                    topLayout.setVisibility(View.GONE);
                if (middleLayout != null)
                    middleLayout.setVisibility(View.GONE);
                if (emptyModelLayout != null)
                    emptyModelLayout.setVisibility(View.GONE);
                if (noWifiLayout != null)
                    noWifiLayout.setVisibility(View.GONE);
                break;
            case Layout_Normal:
                if (topLayout != null)
                    topLayout.setVisibility(View.VISIBLE);
                if (middleLayout != null)
                    middleLayout.setVisibility(View.VISIBLE);
                if (emptyModelLayout != null)
                    emptyModelLayout.setVisibility(View.GONE);
                if (noWifiLayout != null)
                    noWifiLayout.setVisibility(View.GONE);
                break;
            case Layout_EmptyModel:
                if (topLayout != null)
                    topLayout.setVisibility(View.GONE);
                if (middleLayout != null)
                    middleLayout.setVisibility(View.GONE);
                if (emptyModelLayout != null)
                    emptyModelLayout.setVisibility(View.VISIBLE);
                if (noWifiLayout != null)
                    noWifiLayout.setVisibility(View.GONE);
                break;
        }
        if (noWifiLayout != null) {
            if (isWifiConnectionAvailable)
                noWifiLayout.setVisibility(View.GONE);
            else
                noWifiLayout.setVisibility(View.VISIBLE);
        }
    }

    private void Initialize() {
        if (networkingUtils == null) {
            ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            networkingUtils = new NetworkingUtils(connManager, wifi, this);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        registerReceiver(networkingUtils, intentFilter);
    }

    private void StartFunctionality() {
        try {
            if (pcManager == null) {
                pcManager = new PCManager(networkingUtils, this, getApplicationContext());
                pcManagerThread = new Thread(pcManager);

                ReloadPCList();

                // start will send start discovery and send update,
                // so PC spinner needs to be populated
                pcManagerThread.start();
            }
        } catch (Exception e) {
            LOG.error("Failed to start application! PCManager construction failed. Ex=" + e.toString());
            LOG.error("Closing!");
            finish();
        }
    }

    private void StopFunctionality() {
        if (pcManager != null)
            pcManager.terminate();
        try {
            if (pcManagerThread != null) {
                pcManagerThread.join();
                //TODO why not destroy pcManager?
            }
        } catch (InterruptedException ex) {
            LOG.info("Failed to stop pc manager thread");
        }
        networkingUtils = null;
    }

    public void OnWifiConnected() {
        LOG.debug("Connected to Wifi");
        isWifiConnectionAvailable = true;
        ReloadPCList();
        StartFunctionality();
        if (pcManager.GetRemotePCs().isEmpty()) {
            UpdateLayouts(Layouts.Layout_EmptyModel);
        } else {
            UpdateLayouts(Layouts.Layout_Normal);
        }
    }

    public void OnWifiDisconnected() {
        LOG.debug("Disconnected from Wifi");
        isWifiConnectionAvailable = false;
        StopFunctionality();
        UpdateLayouts(Layouts.Layout_NoWifi);
    }

    private boolean IsUIThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }
}
