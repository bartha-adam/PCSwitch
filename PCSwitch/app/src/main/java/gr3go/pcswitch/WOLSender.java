package gr3go.pcswitch;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import android.os.AsyncTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by gr3go on 2/8/2016.
 */
public class WOLSender extends AsyncTask<WOLSenderParams, Integer, Boolean> {

    Logger LOG = LoggerFactory.getLogger(CommLink.class);

    protected Boolean doInBackground(WOLSenderParams... params) {
        int count = params.length;
        long totalSize = 0;
        Boolean result = Boolean.TRUE;
        for (int pi = 0; pi < count; pi++) {
            try
            {
                WOLSenderParams param = params[pi];
                byte[] macBytes = param.GetDestinationMAC().bytes;

                byte[] bytes = new byte[6 + 16 * macBytes.length];
                for (int i = 0; i < 6; i++) {
                    bytes[i] = (byte) 0xff;
                }
                for (int i = 6; i < bytes.length; i += macBytes.length) {
                    System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
                }

                DatagramPacket packet_port7 = new DatagramPacket(bytes, bytes.length,
                        param.GetDestinationAddress(), 7);
                DatagramPacket packet_port9 = new DatagramPacket(bytes, bytes.length,
                        param.GetDestinationAddress(), 9);
                DatagramPacket packet_port40000 = new DatagramPacket(bytes, bytes.length,
                        param.GetDestinationAddress(), 40000);
                DatagramSocket socket = new DatagramSocket();
                //Send an all three ports: 7,9,40000
                //Send packets multiple times
                for(int i = 0; i< 10 ; ++i) {
                    socket.send(packet_port7);
                    socket.send(packet_port9);
                    socket.send(packet_port40000);
                }
                socket.close();
                LOG.info("Wake-Up " + param.GetDestinationAddress().getHostName()
                        + " " + param.GetDestinationMAC().toString());
            }
            catch (Exception e) {
                LOG.error("Failed to send Wake-on-LAN packet: + e");
                result = Boolean.FALSE;
            }
            publishProgress((int) ((pi / (float) count) * 100));
            // Escape early if cancel() is called
            if (isCancelled()) break;
        }
        return result;
    }

    protected void onProgressUpdate(Integer... progress) {
        //Do nothing
    }

    protected void onPostExecute(Boolean result) {
        if(result) {
            LOG.error("Successfully sent Wake-on-LAN packets.");
        }
        else {
            LOG.error("Failed to send Wake-on-LAN packets.");
        }
        //TODO handle error
    }
}
