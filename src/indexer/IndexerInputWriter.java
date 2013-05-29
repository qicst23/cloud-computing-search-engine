/**
 * 
 */
package indexer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.tartarus.snowball.SnowballStemmer;
import org.w3c.dom.Document;


import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.KeepEverythingExtractor;
import storage.ContentClient;
import storage.UrlContent;

/** @author cis455 */
public class IndexerInputWriter {
	protected static ContentClient contentClient = null;
	protected static Charset utf8 = Charset.forName("UTF-8");
	protected static int numDocUrls; // number of docurls to get before
																		// terminating
	protected static String dfResultFolder = "dfCounterResult";
	protected static String indexerResultFolder = "indexerResult";
	protected static String tfResultFolder = "tfCounterResult";
	protected static String dvlResultFolder = "dvlResult";
	protected static boolean debugMode = true;
	protected static boolean printErrors = true;
	static File unstemmedDir;
	static File stemmedDir;
	static File outputDir;

	protected static void printDebug(String str) {
		if (debugMode) {
			System.out.println(str);
		}
	}

	protected static void printError(String str) {
		if (printErrors) {
			System.err.println(str);
		}
	}

	/** @param args
	 * @throws IOException */
	public static void main(String[] args) throws IOException {

		if (args.length < 4) {
			System.err.println("Usage: Indexer <folder> <numToParse> <hashtableFolder> <pastry addrs>");
			System.exit(2);
		}
		numDocUrls = Integer.valueOf(args[1]);

		File inputDir = new File(args[0]);
		if (!inputDir.exists()) {
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
		for (int i = 3; i < args.length; i++) {
			try {
				printDebug("adding inet: " + args[i]);
				iNets.add(InetAddress.getByName(args[i]));
			} catch (UnknownHostException e) {
				System.err.println("Unknown Host: " + args[i]);
				e.printStackTrace();
			}
		}
		printDebug("finished adding Inets");
		printDebug(iNets.toString());
		contentClient = new ContentClient(iNets);
		printDebug("content Client initialized");
		
		int count = writeInputFiles(args[2]);
		System.out.println("num Files = " + count);
		File file = new File(inputDir, "count");
		file.createNewFile();
		BufferedWriter s = new BufferedWriter(new FileWriter(file));
		s.write(("" + count));
		s.close();
		printDebug("finished writing input files");
	}

	// returns number of files written
	public static int writeInputFiles(String hashDir) throws IOException {
		HashSet<String> docHashes = new HashSet<String>();
		File hashTableFile = new File(hashDir);
		
		for(File f: hashTableFile.listFiles()){
			BufferedReader reader = new BufferedReader(new FileReader(f));
			String line = reader.readLine();
			while(line!=null){
				docHashes.add(line.trim());
				System.out.println(line.trim());
				line = reader.readLine();
			}
			reader.close();
		}
		
		printDebug("# of hashes: " + docHashes.size());
		printDebug("all docHashes: " + docHashes);
		// be able to deal with docHash not existing (be prepared for content to
		// return null
		int count = 0;
		for (String docHash : docHashes) {
			printDebug("Asking for url: " + docHash);
			UrlContent uC = contentClient.sendGetContent(docHash);
			if (uC != null) {
				String textContent; // extract text from file type
				if (uC.getContentType().toLowerCase().indexOf("xml") != -1) {
					printDebug("xml doc");
					/*
					 * // processXML(content); DocumentBuilder d = null; try { d =
					 * DocumentBuilderFactory.newInstance().newDocumentBuilder(); } catch
					 * (ParserConfigurationException e) { // TODO Auto-generated catch
					 * block e.printStackTrace(); System.exit(1); } try {
					 * Indexer.extractTextFromXml(d.parse(new InputSource(new
					 * StringReader(uC.getContentString())))); } catch (SAXException e) {
					 * // TODO xmls not currently parsed e.printStackTrace(); continue; }
					 * catch (IOException e) { // TODO xmls not currently parsed
					 * e.printStackTrace(); continue; } textContent =
					 * uC.getContentString();
					 */
					continue;

				} else if (uC.getContentType().toLowerCase().indexOf("html") != -1) {
					printDebug("html doc");
					try {
						textContent = extractTextFromHtml(uC.getContentString());
					} catch (Exception e) {
						e.printStackTrace();
						continue;
					}

				} else if (uC.getContentType().toLowerCase().indexOf("pdf") != -1) {
					printDebug("pdf doc");
					try {
						textContent = extractTextFromPdf(uC.getContentBytes());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						continue;
					} // TODO is this in utf-8?

				} else {
					textContent = uC.getContentString();
				}

				@SuppressWarnings("rawtypes")
				Class stemClass;
				SnowballStemmer stemmer = null;
				try {
					stemClass = Class.forName("org.tartarus.snowball.ext."
							+ getLanguage(textContent) + "Stemmer");
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
				// stem each word in text
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
				String[] splitted = textContent
						.split("[\\s\\.,?)!}>\\]'\"���]*\\s+[\\s{'\"\\[(���<]*");
				String stemmedContent = "";
				for (int i = 0; i < splitted.length; i++) {
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
				if (numDocUrls != -1 && count == numDocUrls) {
					printDebug("read max num of docHashes");
					break;
				}
			}
		}
		return count;
	}

	public static String extractTextFromPdf(byte[] input) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(input);
		PDDocument doc = PDDocument.load(in);
		PDFTextStripper text = new PDFTextStripper();
		text.setSortByPosition(true);
		String res = text.getText(doc);
		doc.close();
		return res;
	}

	public static String extractTextFromHtml(String input)
			throws BoilerpipeProcessingException {
		return KeepEverythingExtractor.INSTANCE.getText(input);
	}

	// not very sophisticated right now
	public static String extractTextFromXml(Document d) {
		// printDebug(d.getDocumentElement().getTextContent());
		return d.getDocumentElement().getTextContent();
	}

	public static String getLanguage(String input) {
		return "english";
	}
}
