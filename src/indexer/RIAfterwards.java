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

import storage.ContentClient;
import storage.RIEntry;
import storage.RIInfo;

/**
 * @author Yayang Tian
 * http://cis.upenn.edu/~yaytian
 */
public class RIAfterwards {
	protected static ContentClient contentClient = null;
	protected static Charset utf8 = Charset.forName("UTF-8");
	protected static boolean debugMode = true;
	protected static boolean printErrors = true;
	
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
			System.err.println("Usage: Indexer <HadoopOutputDir> <count of docs> <IP address>");
			System.exit(2);
		}
		File inputDir = new File(args[0]);;
		
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
		HashMap<String, Double> docVecLengths = new HashMap<String, Double>();
		HashMap<String, HashMap<String, Integer>> wordDocTf = new HashMap<String, HashMap<String, Integer>>();
		HashMap<String, Integer> wordDf = new HashMap<String, Integer>();
		File[] results = inputDir.listFiles();
		for(File result : results){
			if(result.getName().startsWith(".")){
				continue;
			}
			BufferedReader in = new BufferedReader(new FileReader(result));
			String line = in.readLine();
			while(line!=null){
				String[] keyVal = line.split("\t");
				if(keyVal[0].matches("\\s*")){
					line = in.readLine();
					continue; //should get rid of white space or empty string
				}
				if(keyVal[0].split(" ").length==2){
					String[] split = keyVal[0].split(" ");//tf
					String word = split[0].trim();
					String docHash = split[1].trim();
					if(word.matches("\\s*")||docHash.matches("\\s*")){
						line = in.readLine();
						continue; //should get rid of white space or empty string
					}
					
					Integer tf;
					try{
						tf = Integer.valueOf(split[1]);
					}
					catch(NumberFormatException e){
						e.printStackTrace();
						line = in.readLine();
						continue;
					}
					
					if(wordDocTf.containsKey(word)){
						wordDocTf.get(word).put(docHash, tf);
					} else{
						HashMap<String, Integer>x = new HashMap<String, Integer>();
						x.put(docHash, tf);
						wordDocTf.put(word, x);
					}
				}
				else if(keyVal[0].startsWith("\"\"\"\"")){
					String docHash = keyVal[0].substring(4);
					Double dvl;
					try{
						dvl = Double.valueOf(keyVal[1]);
					}
					catch(NumberFormatException e){
						e.printStackTrace();
						line = in.readLine();
						continue;
					}
					docVecLengths.put(docHash, dvl);//dvl
				}
				else{
					String word = keyVal[0].trim();//df
					if(word.matches("\\s*")){
						line = in.readLine();
						continue;
					}
					Integer df;
					try{
						df = Integer.valueOf(keyVal[1]);
					}
					catch(NumberFormatException e){
						e.printStackTrace();
						line = in.readLine();
						continue;
					}
					wordDf.put(word, df);
					
				}
				line = in.readLine();
			}
			in.close();
			for(String word: wordDf.keySet()){
				RIEntry entry = new RIEntry(word);
				entry.setNdf((double)count/(double)wordDf.get(word));
				if(wordDocTf.containsKey(word)){
					HashMap<String, Integer> docTfs = wordDocTf.get(word);
					for(String docHash: docTfs.keySet()){
						for(String url: contentClient.sendGetUrlsFromHash(docHash)){
							if(!url.matches("\\s*")){
								entry.addHit(url, 1, "a");
							}
						}
					}
				}
				contentClient.sendPutRIEntry(entry);
			}
		}
		
	}
}
