/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package crawler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDActionLaunch;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl;
import storage.UrlContent;

/** Thread for extracting links from crawled HTML and XML content stored in the
 * BerkleyDB */
public class LinkExtractor extends Thread {

	protected static LinkedBlockingQueue<UrlContent> pagesToExtractLinksFrom = null;
	private static final String EXTENSION_REGEX = ".+\\.[a-zA-Z0-9]+";
	private static final String LINKMAPDELIM = "#&&#";
	protected static HashSet<String> hostnameBlacklist = null;
	private static PrintWriter linkMap = null;

	/** Default constructor for Link Extractor thread */
	public LinkExtractor(String blacklistPath) {
		pagesToExtractLinksFrom = new LinkedBlockingQueue<UrlContent>();
		hostnameBlacklist = new HashSet<String>();
		int blacklistCount = 0;
		CrawlerDrone.addLog("Link Extractor starting.......");
		File blacklistFile = new File(blacklistPath);
		if (blacklistFile.exists()) {
			BufferedReader blacklistReader = null;
			try {
				blacklistReader = new BufferedReader(new FileReader(blacklistFile));
				String blockedHost = blacklistReader.readLine();
				while (blockedHost != null) {
					hostnameBlacklist.add(blockedHost);
					blacklistCount++;
					blockedHost = blacklistReader.readLine();
				}
			} catch (FileNotFoundException e) {} catch (IOException e) {} finally {
				if (blacklistReader != null) {
					try {
						blacklistReader.close();
					} catch (IOException e) {}
				}
			}
		}
		System.out.println("Added " + blacklistCount
				+ " hosts to crawler blackist.");
		CrawlerDrone.addLog("Link Extractor started.");
	}

	@Override
	public void run() {
		String linkMapFilename = "LinkMap_"
				+ HostSplitter.handler.node.getId().toString();
		File linkMapFile = new File(linkMapFilename);
		if (!linkMapFile.exists()) {
			try {
				linkMapFile.createNewFile();
			} catch (IOException e) {
				System.err
						.println("Unable to create file to store link map. Setup failed.");
				return;
			}
		}
		try {
			linkMap = new PrintWriter(new BufferedWriter(new FileWriter(linkMapFile,
					true)));
		} catch (IOException e) {
			System.err
					.println("Unable to open file to store link map. Setup failed.");
			return;
		}

		while (true) {
			UrlContent extractFrom = null;
			try {
				if (pagesToExtractLinksFrom != null) {

					extractFrom = pagesToExtractLinksFrom.take();
					extractLinks(extractFrom);
				} else {
					CrawlerDrone
							.addLog("Link Extractor: data structures not initialized. Exiting.");
					return;
				}
			} catch (InterruptedException e) {
				CrawlerDrone
						.addLog("Link Extractor: received shutdown signal. Exiting.");
				executeShutdown();
				return;
			}
		}
	}

	private void extractLinks(UrlContent page) {
		if (CrawlerDrone.contentClient != null) {
			if (page != null) {
				CrawlerDrone.addLog("Link Extractor: retrieved " + page.getUrl());
				if (page.getContentType() != null
						&& (hostnameBlacklist == null || !hostnameBlacklist.contains(page
								.getHost()))) {
					if (page.getContentType().toLowerCase().indexOf("xml") != -1) {
						// processXML(content);
						if (CrawlerDrone.contentClient != null) {
							CrawlerDrone.contentClient.sendPutContent(page);
							CrawlerDrone.contentClient.sendPutPageRankScore(page.getUrl(),
									0.15);
						}
					} else if (page.getContentType().toLowerCase().indexOf("html") != -1) {
						processHTML(page);
					} else if (page.getContentType().toLowerCase().indexOf("pdf") != -1) {
						// processPDF(page);
						if (CrawlerDrone.contentClient != null) {
							CrawlerDrone.contentClient.sendPutContent(page);
							CrawlerDrone.contentClient.sendPutPageRankScore(page.getUrl(),
									0.15);
						}
					}
				} else {
					// No content type found
					CrawlerDrone
							.addLog("Link Extractor: "
									+ page.getUrl()
									+ " is not a known content type or is on a blacklisted host. Links not extracted.");
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private void processXML(UrlContent content) {
		Tidy tidy = new Tidy();
		tidy.setXmlTags(true);
		tidy.setQuiet(true);
		tidy.setShowErrors(0);
		Document doc = tidy.parseDOM(new ByteArrayInputStream(content
				.getContentString().getBytes()), null);
		NodeList elements = doc.getChildNodes();
		ArrayList<String> results = new ArrayList<String>();
		for (int i = 0; i < elements.getLength(); i++) {
			NodeList children = elements.item(i).getChildNodes();
		}

	}

	private void processHTML(final UrlContent content) {
		final StringBuilder linkMapBuilder = new StringBuilder(content.getUrl()
				+ "\t" + "1.0000\t");
		final HashSet<String> outlinks = new HashSet<String>();
		try {
			SAXParserImpl.newInstance(null).parse(
					new ByteArrayInputStream(content.getContentString().getBytes()),
					new DefaultHandler() {
						/** Anonymous inner class Processes the start of an element in the
						 * HTML DOM */
						StringBuilder titleBuilder = null;

						@Override
						public void startElement(String uri, String localName,
								String qName, Attributes attributes) throws SAXException {
							String linkToAdd = null;
							if (qName.equalsIgnoreCase("a") || qName.equalsIgnoreCase("area")) {
								linkToAdd = attributes.getValue("href");
								if (linkToAdd == null) {
									linkToAdd = attributes.getValue("src");
								}
							} else if (qName.equalsIgnoreCase("link")
									|| qName.equalsIgnoreCase("iframe")) {
								linkToAdd = attributes.getValue("src");
								if (linkToAdd == null) {
									linkToAdd = attributes.getValue("href");
								}
							} else if (qName.equalsIgnoreCase("title")) {
								titleBuilder = new StringBuilder();
							} else if (qName.equalsIgnoreCase("meta")) {
								String type = attributes.getValue("name");
								if (type != null && type.equalsIgnoreCase("robots")) {
									String directive = attributes.getValue("content");
									if (directive != null
											&& directive.equalsIgnoreCase("noindex")
											&& CrawlerDrone.robotsDisallow != null) {
										ArrayList<String> rules = new ArrayList<String>();
										if (CrawlerDrone.robotsDisallow.containsKey(content
												.getHost())) {
											rules = CrawlerDrone.robotsDisallow.get(content.getHost());
										}
										rules.add(content.getUrl());
										CrawlerDrone.robotsDisallow.put(content.getHost(), rules);

									}
								}
							}
							if (linkToAdd != null) {
								String cleanedLink = derelativize(content, linkToAdd);
								if (cleanedLink != null) {
									if (!cleanedLink.equals(content.getUrl())) {
										HostSplitter.linksToSplit.add(cleanedLink);
										outlinks.add(cleanedLink);
										CrawlerDrone
												.addLog("Link Extractor: Adding link to queue ["
														+ cleanedLink + "]");
									}
								}
							}
						}

						@Override
						public void endElement(String uri, String localName, String name) {
							if (name.equalsIgnoreCase("title")) {
								content.setTitle(titleBuilder.toString());
								titleBuilder = null;
							}
						}

						@Override
						public void characters(char[] ch, int start, int length) {
							if (titleBuilder != null) {
								titleBuilder.append(new String(ch, start, length));
							}
						}

					});

		} catch (SAXException e) {
			CrawlerDrone.addLog("Link Extractor: SAX Error parsing "
					+ content.getUrl());
		} catch (IOException e) {
			CrawlerDrone.addLog("Link Extractor: IO Error parsing "
					+ content.getUrl());
		}
		if (CrawlerDrone.contentClient != null) {
			CrawlerDrone.contentClient.sendPutContent(content);
		}
		if (outlinks != null && outlinks.size() > 0) {
			if (linkMap != null) {
				for (String link : outlinks) {
					linkMapBuilder.append(link + LINKMAPDELIM);
				}
				linkMap.println(linkMapBuilder);
			}
		}
		CrawlerDrone.contentClient.sendPutPageRankScore(content.getUrl(), 0.15);
	}

	@SuppressWarnings("unused")
	private void processPDF(UrlContent content) {
		PDDocument doc = null;
		try {
			doc = PDDocument
					.load(new ByteArrayInputStream(content.getContentBytes()));
			if (doc.isEncrypted()) {
				doc.close();
				return;
			}
			List<?> docPages = doc.getDocumentCatalog().getAllPages();
			System.out.println("1 pages retrieved");
			for (Object page : docPages) {
				List<PDAnnotation> annotations = ((PDPage) page).getAnnotations();
				System.out.println("2 annotations retrieved");
				for (PDAnnotation annotation : annotations) {
					if (annotation instanceof PDAnnotationLink) {
						System.out.println("3 link found");
						PDActionLaunch annotationAction = (PDActionLaunch) ((PDAnnotationLink) annotation)
								.getAction();
						System.out.println("4 action retrieved");
						String linkToAdd = derelativize(content, annotationAction.getF());
						HostSplitter.linksToSplit.add(linkToAdd);
						System.out.println("5 link added");
					}
				}
			}
		} catch (IOException e) {
			CrawlerDrone.addLog("Link Extractor: PDFBox Error parsing "
					+ content.getUrl());
		} finally {
			if (doc != null) {
				try {
					doc.close();
				} catch (IOException e) {
					CrawlerDrone.addLog("Link Extractor: PDFBox Error closing "
							+ content.getUrl());
				}
			}
		}
	}

	public static String derelativize(UrlContent content, String link) {
		URL check = null;
		if (link.startsWith("#"))
			return null;
		try {
			check = new URL(link.trim());
		} catch (MalformedURLException e) {
			check = null;
		}
		if (check != null)
			return check.getHost() + check.getPath();

		String build = "http://";
		if (link.startsWith("/")) {
			// Relative to top level
			if (content.getHost().endsWith("/")) {
				build += content.getHost() + link.substring(1);
				// } else if (content.getUrl().matches(EXTENSION_REGEX)) {
				// build += content.getUrl().substring(0,
				// content.getUrl().lastIndexOf("/"))
				// + link;
			} else {
				build += content.getUrl() + link;
			}
		} else {
			// Relative to current path
			if (content.getUrl().endsWith("/")) {
				build += content.getUrl() + link;
			} else if (content.getUrl().matches(EXTENSION_REGEX)) {
				build += content.getUrl().substring(0,
						content.getUrl().lastIndexOf("/") + 1)
						+ link;
			} else {
				build += content.getUrl() + "/" + link;
			}
		}

		// Use URL class to eliminate queries and anchors
		URL builder = null;
		try {
			builder = new URL(build).toURI().normalize().toURL();
		} catch (MalformedURLException e) {
			return null;
		} catch (URISyntaxException e) {
			return null;
		}

		return builder.getProtocol() + "://" + builder.getHost()
				+ builder.getPath();

	}

	@SuppressWarnings("unused")
	private static void printDebug(String debugInfo) {
		if (CrawlerDrone.DEBUG) {
			System.out.println(debugInfo);
		}
	}

	private static void executeShutdown() {
		if (linkMap != null) {
			linkMap.close();
		}
		/** if (extractLinksIndex != null) { for (String link :
		 * pagesToExtractLinksFrom) { toExtractStore store = new toExtractStore();
		 * store.setPKey(link); extractLinksIndex.put(store); } } **/
	}
}
