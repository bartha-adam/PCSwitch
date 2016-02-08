package gr3go.pcswitch;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pcswitch.common.MACAddress;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

//import com.google.code.microlog4android.config.PropertyConfigurator;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import gr3go.pcswitch.R;


public class MainActivity extends ActionBarActivity
                          implements MyLogger, PCManagerListener, OnItemSelectedListener,
                                     DialogInterface.OnClickListener {
    Logger LOG = LoggerFactory.getLogger(MainActivity.class);

    private NetworkingUtils networkingUtils;
    private CommLink		commLink;
    private PCManager		pcManager;
    PC 				selectedRemotePC;
    AlertDialog 	shutdownOptionDialog;

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
        
        
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        networkingUtils = new NetworkingUtils(connManager, wifi);
        try {
            pcManager = new PCManager(networkingUtils, this, getApplicationContext());
            new Thread(pcManager).start();
        }
        catch (Exception e) {
            LOG.error("Failed to start application! PCManager construction failed. Ex=" + e.toString());
            LOG.error("Closing!");
            finish();
        }
    }
    
    @Override
    protected void onSaveInstanceState (Bundle outState)
    {
    	
    }
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public void onAttachedToWindow()
    {
    	if(!networkingUtils.IsWifiConnected()){
    		AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
    		builder1.setTitle(getResources().getString(R.string.warn));
    		builder1.setMessage(getResources().getString(R.string.warnWifiDisabled));
    		builder1.setCancelable(true);
    		builder1.setNeutralButton(android.R.string.ok,
    		        new DialogInterface.OnClickListener() {
    		    		public void onClick(DialogInterface dialog, int id) {
    		    			dialog.cancel();
    		    		}
    		});

    		AlertDialog alert = builder1.create();
    		alert.show();
            //TODO: Limit the functionality of the app
    	}
		Spinner pcSpinner = (Spinner) findViewById(R.id.spnPCSelector);
		if(pcSpinner != null){
			pcSpinner.setOnItemSelectedListener(this);
            Vector<PC> remotePCs = pcManager.GetRemotePCs();
			Iterator<PC> it = remotePCs.iterator();
			while(it.hasNext()) {
				PC remotePC = (PC)it.next();
				PCAdded(remotePC);
			}
		}
    	else {
            LOG.warn("onAttachedToWindow spinner is NULL!");
        }
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
    
    public void btnCheckWifiClicked(View view)
    {
    	addLogLine("Checking WIFI connection!");
    	if (networkingUtils.IsWifiConnected()) {
    		addLogLine("WIFI is connected!");
    		addLogLine("IP=" + networkingUtils.GetIPv4String());
    		addLogLine("Gateway=" + networkingUtils.GetGatewayIPv4String());
    		addLogLine("NetMask=" + networkingUtils.GetNetMaskString());
    	} else {
    		addLogLine("WIFI is not connected!");
    	}
    }

    public void btnDiscoverServersClicked(View view) {
    	pcManager.SendGetServerStatus_Broadcast();
    }


	@Override
	public void onClick(DialogInterface dialog, int item) {
        //TODO: rename this method
		dialog.dismiss();
        if(dialog == shutdownOptionDialog)
        {
    		switch(item)
	        {
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
        public void onTimeSet(TimePicker view, int hourOfDay, int minute){
            if(view.isShown()){
                // This method will return true only once
                String logLine = "Selected time:";
                logLine += hourOfDay;
                logLine += ":";
                logLine += minute;
                addLogLine(logLine);

                int secondsInMin = 60;
                int secondsInHour = 60 * secondsInMin;
                pcManager.SetShutdownDelayFor(selectedRemotePC, hourOfDay * secondsInHour + minute * secondsInMin);
            }
        }
    };

	public void showTimePicker()
	{
		int defaultMin = 30;
    	int defaultHour = 0;
    	if(selectedRemotePC != null && selectedRemotePC.IsShutdownConfigured()){
    		int configuredShutdownInSec = selectedRemotePC.GetShutdownDelay();
    		int secondsInMin = 60;
    		int secondsInHour = 60 * secondsInMin;
			int secondsInDay = 24 * secondsInHour;
			if(configuredShutdownInSec >= secondsInDay){
				defaultHour = 23;
				defaultMin = 59;
			} else {
				defaultHour = configuredShutdownInSec/secondsInHour;
				defaultMin = (configuredShutdownInSec % secondsInHour)/secondsInMin;
			}
    	}
    	TimePickerDialog timePicker = new TimePickerDialog(this, shutdownTimeSelectCallback,
    			defaultHour, defaultMin, true);
    	timePicker.setTitle(getResources().getString(R.string.chooseShutdownDelay));
    	timePicker.show();
	}
	
	public void txtShutDownInValueClicked(View view){
    	final CharSequence[] items = {" Now "," Delayed "};
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Shutdown");
        builder.setSingleChoiceItems(items, 0, this);
        shutdownOptionDialog = builder.create();
        shutdownOptionDialog.show();
    }
 
    public void btnCancelShutdownClicked(View view){
    	pcManager.SetShutdownDelayFor(selectedRemotePC, -1); //Cancel
    }
    
    public void btnWakeUpClicked(View view){
    	if(selectedRemotePC != null) {
    		if(selectedRemotePC.GetStatus() == PC.Status.OFF) {
    			InetAddress broadcastAddress = networkingUtils.GetBroadcastAddress();
    			if(broadcastAddress != null)
                    new WOLSender().execute(new WOLSenderParams(broadcastAddress,
                            selectedRemotePC.GetMACAddress()));
    		} else {
                LOG.warn("Wakeup skipped, pc status is not OFF! " + selectedRemotePC.toString());
            }
    	} else {
            LOG.warn("Wakeup skipped, selectedRemotePC is null");
        }
    }

    // TODO: check if this method is still needed
    public void addLogLine(String logLine) {
    	String timestamp = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
    }

    // TODO: check if this method is still needed
    public boolean pingHost(InetAddress hostAddress, int timeout /*ms*/) throws IOException {
    	boolean r = hostAddress.isReachable(timeout);
    	return hostAddress.isReachable(timeout);
    }
    
    //TODO: move to some utililty class
    private static byte[] getMacBytes(String macStr) throws IllegalArgumentException {
        byte[] bytes = new byte[6];
        String[] hex = macStr.split("(\\:|\\-)");
        if (hex.length != 6) {
            throw new IllegalArgumentException("Invalid MAC address.");
        }
        try {
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) Integer.parseInt(hex[i], 16);
            }
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex digit in MAC address.");
        }
        return bytes;
    }

    //TODO: move to some utililty class
    static public int invertBytes(int value) {
		int result = 0;
		result += value & 0xFF;
		result = result << 8;
		result += (value >>>  8)& 0xFF;
		result = result << 8;
		result += (value >>> 16)& 0xFF;
		result = result << 8;
		result += (value >>> 24)& 0xFF;
		
		return result;
	}
    
	@Override
	public void LogMessage(String msg) {
		LOG.info(msg);
	}

	@Override
	public void PCChanged(PC pc) {
		if(pc == selectedRemotePC) {
			MainActivity.this.runOnUiThread(new RunnableWithPCParam(pc) {
			    public void run() {
			    	UpdateUIFromModel(pc);
			    }
			});
		}
	}

	@Override
	public void PCAdded(PC pc) {
		final MainActivity main = this;
		MainActivity.this.runOnUiThread(new RunnableWithPCParam(pc) {
		    public void run() {
		    	Spinner spinner = (Spinner) findViewById(R.id.spnPCSelector);
		    	if(spinner == null){
		    		LOG.error("PCAdded spinner is null!");
		    		return;
		    	}
		    		
		    	List<String> list = new ArrayList<String>();
		    	Vector<PC> remotePCs = main.pcManager.GetRemotePCs();
		    	Iterator<PC> it = remotePCs.iterator();
			    while(it.hasNext()){
			    	PC entry = (PC)it.next();
			    	list.add(entry.GetAddress().getHostAddress());
			    }
		    	ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(main,
		    		android.R.layout.simple_spinner_item, list);
		    	dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		    	spinner.setAdapter(dataAdapter);
		    }
		});
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View arg1, int pos, long id) {
		String ip = parent.getItemAtPosition(pos).toString();
		addLogLine("Selection changed to " + ip);
		selectedRemotePC = pcManager.FindPC(ip);
		pcManager.SetSelectedPC(selectedRemotePC);
		UpdateUIFromModel(selectedRemotePC);
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
	}

	public void setTextViewClickable(TextView textView, Boolean flag) {
		if(flag) {
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
		if((view = findViewById(R.id.txtStatusValue)) != null) {
            txtStatusValue = (TextView) view;
        }
		
		TextView txtShutdownDelayValue = null;
		if((view = findViewById(R.id.txtShutDownInValue)) != null) {
            txtShutdownDelayValue = (TextView) view;
        }
		
		TextView txtMACValue = null;
		if((view = findViewById(R.id.txtMACValue)) != null) {
            txtMACValue = (TextView) view;
        }
		
		Button btnWakeUp = null;
		if((view = findViewById(R.id.btnWakeUp)) != null) {
            btnWakeUp = (Button) view;
        }
		
		Button btnCancelShutdown = null;
		if((view = findViewById(R.id.btnCancelShutdown)) != null) {
            btnCancelShutdown = (Button) view;
        }
		
		if(remotePC != null) {
			if(txtMACValue != null) {
				if(remotePC.GetMACAddress() != null) {
                    txtMACValue.setText(remotePC.GetMACAddress().toString());
                }
				else {
                    txtMACValue.setText(getResources().getString(R.string.notavailable));
                }
			}
			
			if(remotePC.GetStatus() == PC.Status.ON) {
				if(txtStatusValue != null) {
					txtStatusValue.setText(getResources().getString(R.string.on));
					txtStatusValue.setTextColor(Color.GREEN);
				}
				if(btnWakeUp != null) {
                    btnWakeUp.setVisibility(View.GONE);
                }
				if(txtShutdownDelayValue != null) {
                    setTextViewClickable(txtShutdownDelayValue, true);
                }
				
				if(remotePC.IsShutdownConfigured()) {
					if(btnCancelShutdown != null) {
                        btnCancelShutdown.setVisibility(View.VISIBLE);
                    }
					int shutdownDelay = remotePC.GetShutdownDelay();
					String shutdownDelayStr = new String();

					int secondsInMinute = 60;
					int secondsInHour = 60 * secondsInMinute;
					int secondsInDay = 24 * secondsInHour;

					if(shutdownDelay >= secondsInDay) {
					    // Days
						int numDays = shutdownDelay / secondsInDay;
						shutdownDelayStr += numDays + "d ";
						shutdownDelay = shutdownDelay % secondsInDay;
					}

					if(shutdownDelay >= secondsInHour) {
                        // Hours
						int numHours = shutdownDelay / secondsInHour;
						shutdownDelayStr += numHours + "h ";
						shutdownDelay = shutdownDelay % secondsInHour;
					}

					if(shutdownDelay > secondsInMinute) {
                        // Minutes
						int numMinutes = shutdownDelay / secondsInMinute;
						shutdownDelayStr += numMinutes + "m ";
						shutdownDelay = shutdownDelay % secondsInMinute;
					}

                    // Seconds shown only when delay is < 1min
					if(shutdownDelayStr.length() == 0) {
						shutdownDelayStr += shutdownDelay + "s";
					}
					if(txtShutdownDelayValue != null) {
                        txtShutdownDelayValue.setText(shutdownDelayStr);
                    }
				}
				else{
					if(btnCancelShutdown != null)
						btnCancelShutdown.setVisibility(View.GONE);
					if(txtShutdownDelayValue != null)
						txtShutdownDelayValue.setText(getResources().getString(R.string.notset));
				}
			}
			else if(remotePC.GetStatus() == PC.Status.OFF){
				if(txtStatusValue != null) {
					txtStatusValue.setText(getResources().getString(R.string.off));
					txtStatusValue.setTextColor(Color.RED);
				}
				if(txtShutdownDelayValue != null) {
                    txtShutdownDelayValue.setText(getResources().getString(R.string.notavailable));
                }
				if(btnWakeUp != null) {
                    btnWakeUp.setVisibility(View.VISIBLE);
                }
				if(txtShutdownDelayValue != null) {
                    setTextViewClickable(txtShutdownDelayValue, false);
                }
				if(btnCancelShutdown != null) {
                    btnCancelShutdown.setVisibility(View.GONE);
                }
				
			}
			else if(remotePC.GetStatus() == PC.Status.ShuttingDown){
				if(txtStatusValue != null){
					txtStatusValue.setText(getResources().getString(R.string.shutingdown));
					txtStatusValue.setTextColor(Color.parseColor("#ffa500"));
				}
				if(txtShutdownDelayValue != null) {
                    txtShutdownDelayValue.setText(getResources().getString(R.string.notavailable));
                }
				if(btnWakeUp != null) {
                    btnWakeUp.setVisibility(View.GONE);
                }
				if(txtShutdownDelayValue != null) {
                    setTextViewClickable(txtShutdownDelayValue, false);
                }
				if(btnCancelShutdown != null) {
                    btnCancelShutdown.setVisibility(View.GONE);
                }
			}
		}
		else {
			// Default case, probably should not get here
			if(txtStatusValue != null){
				txtStatusValue.setText(getResources().getString(R.string.notavailable));
				txtStatusValue.setTextColor(Color.BLACK);
			}
			if(txtShutdownDelayValue != null){
				txtShutdownDelayValue.setText(getResources().getString(R.string.notavailable));
				setTextViewClickable(txtShutdownDelayValue, false);
			}
		}		
	}
}
