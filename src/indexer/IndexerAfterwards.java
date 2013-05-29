/**
 * 
 */
package indexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.GenericOptionsParser;

import storage.ContentClient;
import storage.RIEntry;
import storage.RIInfo;

/**
 * @author cis455
 *
 */
public class IndexerAfterwards {
	protected static ContentClient contentClient = null;
	protected static Charset utf8 = Charset.forName("UTF-8");
	protected static int numDocUrls; //number of docurls to get before terminating
	protected static String dfResultFolder = "dfCounterResult";
	protected static String indexerResultFolder = "indexerResult";
	protected static String tfResultFolder = "tfCounterResult";
	protected static String dvlResultFolder = "dvlResult";
	protected static boolean debugMode = true;
	protected static boolean printErrors = true;
	static File outputDir;
	
	protected static void printDebug(String str){
		if(debugMode){
			System.out.println(str);
		}
	}
	
	protected static void printError(String str){
		if(printErrors){
			System.err.println(str);
		}
	}
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		if (args.length <2) {
			System.err.println("Usage: Indexer <folder> <count of docs> <IP address>");
			System.exit(2);
		}
		File inputDir = new File(args[0]);
		outputDir = new File(inputDir, "outputs");
		
		int count = Integer.valueOf(args[1]);
		ArrayList<InetAddress> iNets = new ArrayList<InetAddress>();
		for(int i=2; i<args.length; i++){
			try {
				printDebug("adding inet: " + args[i]);
				iNets.add(InetAddress.getByName(args[i]));
			} catch (UnknownHostException e) {
				System.err.println("Unknown Host: " + args[i]);
				e.printStackTrace();
			}
		}
		printDebug("finished adding Inets");
		contentClient = new ContentClient(iNets);
		printDebug("content Client initialized");
		File[] indexerResults = new File(outputDir, indexerResultFolder).listFiles();
		File[] dfResults = new File(outputDir, dfResultFolder).listFiles();
		File[] dvlResults = new File(outputDir, dvlResultFolder).listFiles();
		for(File dfResult : dfResults){
			if(dfResult.getName().startsWith(".")){
				continue;
			}
			BufferedReader dfIn = new BufferedReader(new FileReader(dfResult));
			String line = dfIn.readLine();
			while(line!=null){
				String[] split = line.split("\t"); //word, number
				RIEntry entry = new RIEntry(split[0]);
				double df = Double.valueOf(split[1]);
				entry.setNdf(((double)count)/df);//calculate and store ndf as double in RIEntry, n = count
				if(!contentClient.sendPutRIEntry(entry)){
					printError("put RI Entry failed for word: " + split[0]);
				}
				line = dfIn.readLine();
			}
			dfIn.close();
		}
		printDebug("put all RIEntries, read dfOutput");
		//if this needs to much memory, need to use a local berkeley db
		HashMap<String, Double> docVenLengths = new HashMap<String, Double>();
		for(File dvlResult : dvlResults){
			if(dvlResult.getName().startsWith(".")){
				continue;
			}
			BufferedReader dvlIn = new BufferedReader(new FileReader(dvlResult));
			String line = dvlIn.readLine();
			while(line!=null){
				String[] split = line.split("\t");
				printDebug(line);
				docVenLengths.put(split[0].trim(), Double.valueOf(split[1]));
				line = dvlIn.readLine();
			}
			dvlIn.close();
		}
		printDebug("made DVLs, read dflOutput");
		
		for(File indexerResult: indexerResults){
			if(indexerResult.getName().startsWith(".")){
				continue;
			}
			BufferedReader indIn = new BufferedReader(new FileReader(indexerResult));
			String line = indIn.readLine();
			while(line!=null){
				String[] split = line.split("\t"); //word hash\tpos word pos word pos word
				String[] keysplit = split[0].split(" ");
				String[] valuesplit = split[1].split(" ");
				printDebug(split[1].trim());
				printDebug("end sanity check");
				RIInfo info = new RIInfo();
				for(int i=0; i+1<valuesplit.length; i+=2){
					try{
						info.addHit(Integer.valueOf(valuesplit[i]), valuesplit[i+1]);
					}
					catch(ArrayIndexOutOfBoundsException e){
						String d ="";
						for(int j=0; j<valuesplit.length; j++){
							d += valuesplit[j] + ", ";
						}
						printDebug("array indexes " + i + ", " + (i+1) + " failed for array" + d);
						printDebug("array length: " + valuesplit.length);
					}
					catch(NumberFormatException e){
						printDebug("NumberFormatException with: " + valuesplit[i] + ", " + valuesplit[i+1]);
					}
				}
				printDebug("processingHash");
				String hash = keysplit[1].trim();
				if(docVenLengths.containsKey(hash)){
					info.setDocVecLen(docVenLengths.get(hash));
				}
				else{
					printDebug(docVenLengths.keySet().toString());
					printDebug("docVenLengths didn't contain hash: " + hash);
					line = indIn.readLine();
					continue;
				}
				HashSet<String> urls = contentClient.sendGetUrlsFromHash(hash);
				printDebug("got hashes back");
				for(String url: urls){
					String trimmedUrl = url.trim();
					if(contentClient.sendAddRIInfo(keysplit[0], trimmedUrl, info)){
						printDebug("successfully added RIInfo for: " + trimmedUrl);
					}
					else{
						printError("failed to add RIInfo: " + url);
					}
				}
				line = indIn.readLine();
			}
			indIn.close();
		}
		printDebug("putRIInfos, read indOutput");
	}

}
