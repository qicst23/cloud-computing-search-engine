/**
 * 
 */
package indexer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.tartarus.snowball.SnowballStemmer;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.KeepEverythingExtractor;

import storage.ContentClient;
import storage.RIEntry;
import storage.RIInfo;
import storage.UrlContent;

/**
 * @author cis455
 *
 */
public class Indexer {
	protected static ContentClient contentClient = null;
	protected static Charset utf8 = Charset.forName("UTF-8");
	protected static int numDocUrls; //number of docurls to get before terminating
	protected static String dfResultFolder = "dfCounterResult";
	protected static String indexerResultFolder = "indexerResult";
	protected static String tfResultFolder = "tfCounterResult";
	protected static String dvlResultFolder = "dvlResult";
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
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	static File unstemmedDir;
	static File stemmedDir;
	static File outputDir;
	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException{
		Configuration conf = new Configuration();
		String[] otherArgs = null;
		try {
			otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
		if (otherArgs.length <3) {
			System.err.println("Usage: Indexer <folder> <numToParse> <pastry addrs>");
			System.exit(2);
		}
		numDocUrls = Integer.valueOf(otherArgs[1]);

		File inputDir = new File(otherArgs[0]);
		if(!inputDir.exists()){
			inputDir.mkdirs();
		}
		unstemmedDir = new File(inputDir, "unstemmed");
		unstemmedDir.mkdir();
		stemmedDir = new File(inputDir, "stemmed");
		stemmedDir.mkdir();
		outputDir = new File(inputDir, "outputs");
		outputDir.mkdir();
		printDebug("sanity check");
		ArrayList<InetAddress> iNets = new ArrayList<InetAddress>();
		for(int i=2; i<otherArgs.length; i++){
			try {
				printDebug("adding inet: " + otherArgs[i]);
				iNets.add(InetAddress.getByName(otherArgs[i]));
			} catch (UnknownHostException e) {
				System.err.println("Unknown Host: " + otherArgs[i]);
				e.printStackTrace();
			}
		}
		printDebug("finished adding Inets");
		contentClient = new ContentClient(iNets);
		printDebug("content Client initialized");
		int count = writeInputFiles();
		printDebug("finished writing input files");
		
		Job indexerJob = indexerMain(args);
		Job dfCounterJob = dfCounterMain(args);
		Job tfCounterJob = tfCounterMain(args);
		Job dvlJob = dvlMain(args);
		printDebug("starting jobs");
		indexerJob.waitForCompletion(true);
		printDebug("indexer finished");
		dfCounterJob.waitForCompletion(true);
		printDebug("dfCounter finished");
		tfCounterJob.waitForCompletion(true);
		printDebug("tfCounter finished");
		dvlJob.waitForCompletion(true);
		printDebug("dvl finished");

		File indexerResult = new File(outputDir, indexerResultFolder + "/part-r-00000");
		File dfResult = new File(outputDir, dfResultFolder + "/part-r-00000");
		File dvlResult = new File(outputDir, dvlResultFolder + "/part-r-00000");
		if(!indexerResult.exists() || !dfResult.exists()){
			printError("mapreduce failed to output");
			System.exit(1);
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
		printDebug("put all RIEntries, read dfOutput");
		//if this needs to much memory, need to use a local berkeley db
		HashMap<String, Double> docVenLengths = new HashMap<String, Double>();
		BufferedReader dvlIn = new BufferedReader(new FileReader(dvlResult));
		line = dvlIn.readLine();
		while(line!=null){
			String[] split = line.split("\t");
			docVenLengths.put(split[0], Double.valueOf(split[1]));
			line = dvlIn.readLine();
		}
		dvlIn.close();
		printDebug("made DVLs, read dflOutput");
		BufferedReader indIn = new BufferedReader(new FileReader(indexerResult));
		line = indIn.readLine();
		while(line!=null){
			String[] split = line.split("\t"); //word hash\tpos word pos word pos word
			String[] keysplit = split[0].split(" ");
			String[] valuesplit = split[1].split(" ");
			printDebug(split[1]);
			printDebug("end sanity check");
			RIInfo info = new RIInfo();
			for(int i=0; i<valuesplit.length; i+=2){
				try{
					info.addHit(Integer.valueOf(valuesplit[i]), valuesplit[i+1]);
				}
				catch(ArrayIndexOutOfBoundsException e){
					String d ="";
					for(int j=0; j<valuesplit.length; j++){
						d += valuesplit[j] + ", ";
					}
					printDebug("array indexes " + i + ", " + (i+1) + " failed for array" + d);
				}
				catch(NumberFormatException e){
					printDebug("NumberFormatException with: " + valuesplit[i] + ", " + valuesplit[i+1]);
				}
			}
			printDebug("processingHash");
			String hash = keysplit[1];
			if(docVenLengths.containsKey(hash)){
				info.setDocVecLen(docVenLengths.get(hash));
			}
			else{
				printDebug("docVenLengths didn't contain hash");
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
		printDebug("putRIInfos, read indOutput");
		//look up all urls for each doc hash 
		//docVeclen in RIInfo
		//hits list -> tf from length of list
	}

	//returns number of files written
	public static int writeInputFiles(){
		HashSet<String> docHashes = contentClient.sendGetAllDocHash();
		printDebug("all docHashes: " + docHashes);
		//be able to deal with docHash not existing (be prepared for content to return null
		int count = 0;
		for(String docHash: docHashes){
			printDebug("Asking for url: " + docHash);
			UrlContent uC = contentClient.sendGetContent(docHash);
			if(uC!=null){
				String textContent; //extract text from file type
				if (uC.getContentType().toLowerCase().indexOf("xml") != -1) {
					printDebug("xml doc");
					/*// processXML(content);
					DocumentBuilder d = null;
					try {
						d = DocumentBuilderFactory.newInstance().newDocumentBuilder();
					} catch (ParserConfigurationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						System.exit(1);
					}
					try {
						Indexer.extractTextFromXml(d.parse(new InputSource(new StringReader(uC.getContentString()))));
					} catch (SAXException e) {
						// TODO xmls not currently parsed
						e.printStackTrace();
						continue;
					} catch (IOException e) {
						// TODO xmls not currently parsed
						e.printStackTrace();
						continue;
					}
					textContent = uC.getContentString();*/
					continue;

				} else if (uC.getContentType().toLowerCase().indexOf("html") != -1) {
					printDebug("html doc");
					try {
						textContent = extractTextFromHtml(uC.getContentString());
					} catch (BoilerpipeProcessingException e) {
						e.printStackTrace();
						continue;
					}

				} else if (uC.getContentType().toLowerCase().indexOf("pdf") != -1) {
					printDebug("pdf doc");
					try {
						textContent = extractTextFromPdf(uC.getContentBytes());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						continue;
					} //TODO is this in utf-8?

				}
				else {
					textContent = uC.getContentString();
				}

				@SuppressWarnings("rawtypes")
				Class stemClass;
				SnowballStemmer stemmer = null;
				try {
					stemClass = Class.forName("org.tartarus.snowball.ext." + getLanguage(textContent) + "Stemmer");
					stemmer = (SnowballStemmer) stemClass.newInstance();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.exit(1);
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.exit(1);
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.exit(1);
				}
				printDebug("stemmer initialized");
				//stem each word in text
				File stemmed = new File(stemmedDir, docHash);
				File unstemmed = new File(unstemmedDir, docHash);
				try {
					stemmed.createNewFile();
					unstemmed.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					continue;
				}
				FileOutputStream stemmedOut = null;
				FileOutputStream unstemmedOut = null;
				try {
					stemmedOut = new FileOutputStream(stemmed);
					unstemmedOut = new FileOutputStream(unstemmed);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String[] splitted = textContent.split("[\\s\\.,?)!}>\\]'\"”]*\\s+[\\s{'\"\\[(“<]*");
				String stemmedContent = "";
				for(int i=0; i<splitted.length; i++){
					stemmer.setCurrent(splitted[i].trim());
					stemmer.stem();
					stemmedContent += stemmer.getCurrent().toLowerCase() + " ";
				}
				try {
					unstemmedOut.write(textContent.getBytes(utf8));
					stemmedOut.write(stemmedContent.getBytes(utf8));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					continue;
				}
				try {
					unstemmedOut.close();
					stemmedOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					continue;
				}
				count++;
				if(numDocUrls!=-1 && count==numDocUrls){
					printDebug("read max num of docHashes");
					break;
				}
			}
		}
		return count;
	}


	public static String extractTextFromPdf(byte[] input) throws IOException{
		ByteArrayInputStream in = new ByteArrayInputStream(input);
		PDDocument doc = PDDocument.load(in);
		PDFTextStripper text = new PDFTextStripper();
		text.setSortByPosition(true);
		String res = text.getText(doc);
		doc.close();
		return res;
	}

	public static String extractTextFromHtml(String input) throws BoilerpipeProcessingException{
		return KeepEverythingExtractor.INSTANCE.getText(input);
	}

	//not very sophisticated right now
	public static String extractTextFromXml(Document d){
		//printDebug(d.getDocumentElement().getTextContent());
		return d.getDocumentElement().getTextContent();
	}

	public static String getLanguage(String input){
		return "english";
	}
	public static Job dfCounterMain(String[] args) throws IOException{
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		if (otherArgs.length <2) {
			System.err.println("Usage: wordcount <in> <out>");
			System.exit(2);
		}
		Job job = new Job(conf, "dfCounter");
		job.setJarByClass(Indexer.class);
		job.setMapperClass(DfCounterMapper.class);
		job.setReducerClass(DfCounterReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);
		FileInputFormat.addInputPath(job, new Path(stemmedDir.getPath()));
		FileOutputFormat.setOutputPath(job, new Path(outputDir.getPath(), dfResultFolder));
		return job;
	}

	public static Job indexerMain(String[] args) throws IOException{
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		if (otherArgs.length < 2) {
			System.err.println("Usage: wordcount <in> <out>");
			System.exit(2);
		}
		Job job = new Job(conf, "indexer");
		job.setInputFormatClass(MyFileInputFormat.class);
		job.setJarByClass(Indexer.class);
		job.setMapperClass(IndexerMapper.class);
		job.setReducerClass(IndexerReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path(unstemmedDir.getPath()));
		FileOutputFormat.setOutputPath(job, new Path(outputDir.getPath(), indexerResultFolder));
		return job;
	}

	public static Job tfCounterMain(String[] args) throws IOException {
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		if (otherArgs.length < 2) {
			System.err.println("Usage: wordcount <in> <out>");
			System.exit(2);
		}
		Job job = new Job(conf, "indexer");
		job.setJarByClass(TfCounterMain.class);
		job.setMapperClass(TfCounterMapper.class);
		job.setCombinerClass(TfCounterReducer.class);
		job.setReducerClass(TfCounterReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);
		FileInputFormat.addInputPath(job, new Path(stemmedDir.getPath()));
		FileOutputFormat.setOutputPath(job, new Path(outputDir.getPath(), tfResultFolder));
		return job;
	}

	public static Job dvlMain(String[] args) throws IOException{
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		if (otherArgs.length < 2) {
			System.err.println("Usage: wordcount <in> <out>");
			System.exit(2);
		}
		Job job = new Job(conf, "indexer");
		job.setJarByClass(DVLMain.class);
		job.setInputFormatClass(KeyValueTextInputFormat.class);
		job.setMapperClass(DVLMapper.class);
		job.setReducerClass(DVLReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(DoubleWritable.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);
		FileInputFormat.addInputPath(job, new Path(outputDir.getPath(), tfResultFolder));
		FileOutputFormat.setOutputPath(job, new Path(outputDir.getPath(), dvlResultFolder));
		return job;
	}
}
