package gr3go.pcswitch;

public class Utils {

	static String byteArrayToHex(byte[] a, int size) {
	   StringBuilder sb = new StringBuilder();
	   for (int i=0; i<size; i++) {
           sb.append(String.format("%02x", a[i] & 0xff));
       }
	   return sb.toString();
	}
}
