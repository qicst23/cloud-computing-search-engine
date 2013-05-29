/**
 * 
 */
package ranker;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import storage.ContentClient;

/** @author cis455 */
public class SendPRFeedbackScore {

	protected static ContentClient contentClient = null;


	public SendPRFeedbackScore(ContentClient c) {
		
		contentClient = c;
		
	}//constructor ends

	
	
	public void putScore(String url, double factor)
	{
		contentClient.sendPRFeedbackScore(url,factor); //factor would usually be +1.000 or -1.000 

	}


}
