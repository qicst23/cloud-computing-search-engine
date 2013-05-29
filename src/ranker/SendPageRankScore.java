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
public class SendPageRankScore {

	protected static ContentClient contentClient = null;

	public static void main(String args[]) {
		if (args.length < 2) {
			System.err
					.println("Usage: sendpagerankscore <output folder> <inetaddress>");
			System.exit(2);
		}
		
		//Normalize PageRanks
		double max = 0.15;
		File f1 = new File(args[0]);
		File lf1[] = f1.listFiles();
		FileInputStream fs = null;
		DataInputStream din = null;
		BufferedReader br = null;
		String line;
		double d;
		for (File file : lf1) {
			if (file.getName().contains(".crc")) {
				continue;
			}

			
			try {
				fs = new FileInputStream(file.getAbsolutePath());
			} catch (FileNotFoundException e) {

				e.printStackTrace();
			}
			din = new DataInputStream(fs);
			br = new BufferedReader(new InputStreamReader(din));
			
			try {
				while ((line = br.readLine()) != null) {
					if(line.split("\t").length>=2)
					{
						d = Double.parseDouble(line.split("\t")[1]);
						if(d > max)
							max = d;
					}
				}
				
			} catch (IOException e) {

				e.printStackTrace();
			}

			try {
				br.close();
				din.close();
				fs.close();
			} catch (IOException e) {

				e.printStackTrace();

			}
		}// for ends

		System.out.println("maximum value for normalization obtained:"+max);
		
		// setting contentclient
		ArrayList<InetAddress> dhtGateways = new ArrayList<InetAddress>();
		for (int i = 1; i < args.length; i++) {
			InetAddress gateway = null;
			try {
				gateway = InetAddress.getByName(args[i]);
			} catch (UnknownHostException e1) {
				gateway = null;
			}
			if (gateway != null) {
				dhtGateways.add(gateway);
			}
		}
		if (dhtGateways.size() == 0) {
			System.out.println("Unable to resolve to any input DHT gateway nodes. Please check and try again.");
			return;
		}
		contentClient = new ContentClient(dhtGateways);

		// ********Updating ranks in the database*********
		File f = new File(args[0]);
		File lf[] = f.listFiles();
		//FileInputStream fs = null;
		//DataInputStream din = null;
		//BufferedReader br = null;
		for (File file : lf) {
			if (file.getName().contains(".crc")) {
				continue;
			}

			
			try {
				fs = new FileInputStream(file.getAbsolutePath());
			} catch (FileNotFoundException e) {

				e.printStackTrace();
			}
			din = new DataInputStream(fs);
			br = new BufferedReader(new InputStreamReader(din));
			//String line;
			try {
				while ((line = br.readLine()) != null) {
					if(line.split("\t").length>=2)
					{contentClient.sendPutPageRankScore(line.split("\t")[0],Double.parseDouble(line.split("\t")[1])/max);
				
					/*contentClient.sendPRFeedbackScore(line.split("\t")[0],d);
					double[] scores = new double[2];
					scores = contentClient.sendGetPageRankScore(line.split("\t")[0]);
					System.out.println("url:"+line.split("\t")[0]+" prscore:"+scores[0]+" fbscore:"+scores[1]);
					*/
					}
				}
			} catch (IOException e) {

				e.printStackTrace();
			}

			try {
				br.close();
				din.close();
				fs.close();
			} catch (IOException e) {

				e.printStackTrace();

			}
		}// for ends

	}

}
