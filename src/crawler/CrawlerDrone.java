/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package crawler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;
import storage.BDBWrapper;
import storage.ContentClient;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.PrimaryIndex;
import crawler.state.IPresource;
import crawler.state.UrlFrontier;
import crawler.state.frontierStore;
import crawler.state.toFilterStore;
import crawler.state.visitedStore;

public class CrawlerDrone {

	public static final boolean DEBUG = false;

	private static ArrayList<String> crawlLog = null;
	protected static ConcurrentHashMap<String, Long> visited = null;
	protected static BDBWrapper bdb = null;
	protected static ContentClient contentClient = null;
	protected static int MAX_PAGES_TO_CRAWL = 1000;
	protected static int MAX_LOG_SIZE = 10000;
	protected static int pagesCrawled = 0;
	protected static OutputStreamWriter logFile = null;
	private static boolean signalSent = false;
	private final static SimpleDateFormat logDateFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss.SSS");
	private static final boolean LOGGING_ENABLED = false;
	private static boolean isShutdown = false;
	protected static ConcurrentSkipListMap<String, Integer> topHosts = null;
	protected static UrlFrontier urlFrontier = null;
	protected static PrimaryIndex<String, frontierStore> frontierIndex;
	protected static Hashtable<String, Long> hostLastAction = null;
	protected static PrimaryIndex<String, visitedStore> visitedIndex;
	protected static PrimaryIndex<String, toFilterStore> filterUrlIndex;
	protected static LinkedBlockingQueue<IPresource> urlsToFilter = null;
	protected static Hashtable<String, Long> robotsRetrievalTime = null;
	protected static Hashtable<String, ArrayList<String>> robotsDisallow = null;
	protected static Hashtable<String, ArrayList<String>> robotsAllow = null;
	protected static Hashtable<String, Long> crawlDelay = null;
	protected static UrlFilter primaryUrlFilter;
	protected static UrlFilter secondaryUrlFilter;

	public static void main(String args[]) {
		if (args.length == 0) {
			System.out.println("Michael Collis (mcollis@seas.upenn.edu)");
			return;
		}

		if (args.length < 10) {
			printUsage();
			return;
		}

		try {
			File createLog = new File(args[8]);
			if (!createLog.exists()) {
				createLog.createNewFile();
			}
			logFile = new OutputStreamWriter(new FileOutputStream(new File(args[8])));
		} catch (FileNotFoundException e) {
			System.out.println("Error creating log file, defaulting to System.out");
			logFile = new OutputStreamWriter(System.out);
		} catch (IOException e) {
			System.out.println("Error creating log file, defaulting to System.out");
			logFile = new OutputStreamWriter(System.out);
		}

		crawlLog = new ArrayList<String>();
		topHosts = new ConcurrentSkipListMap<String, Integer>();

		try {
			MAX_PAGES_TO_CRAWL = Integer.parseInt(args[3]);
		} catch (NumberFormatException e) {
			System.out
					.println("Invalid input for number of files to crawl. Please check usage and try again.");
			printUsage();
			return;
		}

		long maxSize;
		try {
			maxSize = Long.parseLong(args[2]);
		} catch (NumberFormatException e) {
			System.out
					.println("Invalid input for maximum size of document to retrieve. Please check usage and try again.");
			printUsage();
			return;
		}

		crawlLog = new ArrayList<String>();
		addLog("Crawler started.");

		bdb = new BDBWrapper();
		try {
			bdb.setupDB(args[1]);
			bdb.setupStore(storage.BDBWrapper.DB_CONTENT_STORE);
			bdb.setupStore(storage.BDBWrapper.DB_FRONTIER_STORE);
			bdb.setupStore(storage.BDBWrapper.DB_VISITED_STORE);
			bdb.setupStore(storage.BDBWrapper.DB_CONTENT_SEEN);
			bdb.setupStore(storage.BDBWrapper.DB_TO_EXTRACT_STORE);
			bdb.setupStore(storage.BDBWrapper.DB_TO_RESOLVE_STORE);
			bdb.setupStore(storage.BDBWrapper.DB_TO_SPLIT_STORE);
			bdb.setupStore(storage.BDBWrapper.DB_TO_FILTER_STORE);

			visited = new ConcurrentHashMap<String, Long>();

			ArrayList<InetAddress> dhtGateways = new ArrayList<InetAddress>();
			for (int i = 9; i < args.length; i++) {
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
				System.out
						.println("Unable to resolve to any input DHT gateway nodes. Please check and try again.");
				return;
			}
			System.out.println("Globus Web Spider started.");
			System.out.println("Initializing crawler worker modules...");
			contentClient = new ContentClient(dhtGateways);
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				@Override
				public void run() {
					executeShutdown();
				}
			}));
			HttpProtocolModule httpGetter1 = new HttpProtocolModule(true, maxSize);
			HttpProtocolModule httpGetter2 = new HttpProtocolModule(false, maxSize);
			// HttpProtocolModule httpGetter3 = new HttpProtocolModule(false,
			// maxSize);
			// HttpProtocolModule httpGetter4 = new HttpProtocolModule(false,
			// maxSize);
			// HttpProtocolModule httpGetter5 = new HttpProtocolModule(false,
			// maxSize);
			// HttpProtocolModule httpGetter6 = new HttpProtocolModule(false,
			// maxSize);
			// HttpProtocolModule httpGetter7 = new HttpProtocolModule(false,
			// maxSize);
			// HttpProtocolModule httpGetter8 = new HttpProtocolModule(false,
			// maxSize);
			// HttpProtocolModule httpGetter9 = new HttpProtocolModule(false,
			// maxSize);
			// HttpProtocolModule httpGetter10 = new HttpProtocolModule(false,
			// maxSize);
			// HttpProtocolModule httpGetter11 = new HttpProtocolModule(false,
			// maxSize);
			// HttpProtocolModule httpGetter12 = new HttpProtocolModule(false,
			// maxSize);
			// HttpProtocolModule httpGetter13 = new HttpProtocolModule(false,
			// maxSize);
			// HttpProtocolModule httpGetter14 = new HttpProtocolModule(false,
			// maxSize);
			// HttpProtocolModule httpGetter15 = new HttpProtocolModule(false,
			// maxSize);
			// HttpProtocolModule httpGetter16 = new HttpProtocolModule(false,
			// maxSize);
			// HttpProtocolModule httpGetter17 = new HttpProtocolModule(false,
			// maxSize);
			// HttpProtocolModule httpGetter18 = new HttpProtocolModule(false,
			// maxSize);
			// HttpProtocolModule httpGetter19 = new HttpProtocolModule(false,
			// maxSize);
			// HttpProtocolModule httpGetter20 = new HttpProtocolModule(false,
			// maxSize);
			int crawlerPastryBootstrapPort = 120001, crawlerPastryLocalPort = 12002;
			InetSocketAddress crawlerPastryBootstrapNode = null;

			try {
				crawlerPastryBootstrapPort = Integer.parseInt(args[6]);
				crawlerPastryLocalPort = Integer.parseInt(args[4]);
				crawlerPastryBootstrapNode = new InetSocketAddress(args[5],
						crawlerPastryBootstrapPort);
			} catch (NumberFormatException e2) {
				System.out
						.println("Error parsing inputs, please check your arguments and try again.");
				printUsage();
				return;
			} catch (IllegalArgumentException e1) {}

			if (crawlerPastryBootstrapNode.isUnresolved()) {
				crawlerPastryBootstrapNode = null;
			}

			HostSplitter hostSplitter = new HostSplitter(args[0],
					crawlerPastryLocalPort, crawlerPastryBootstrapNode);
			LinkExtractor linkExt = new LinkExtractor(args[7]);
			primaryUrlFilter = new UrlFilter(true);
			secondaryUrlFilter = new UrlFilter(false);
			DnsResolver resolver = new DnsResolver();
			System.err
					.println("Crawler subsystems activated. Beginning crawling procedure.\n");
			httpGetter1.start();
			httpGetter2.start();
			// httpGetter3.start();
			// httpGetter4.start();
			// httpGetter5.start();
			// httpGetter6.start();
			// httpGetter7.start();
			// httpGetter8.start();
			// httpGetter9.start();
			// httpGetter10.start();
			// httpGetter11.start();
			// httpGetter12.start();
			// httpGetter13.start();
			// httpGetter14.start();
			// httpGetter15.start();
			// httpGetter16.start();
			// httpGetter17.start();
			// httpGetter18.start();
			// httpGetter19.start();
			// httpGetter20.start();
			hostSplitter.start();
			linkExt.start();
			primaryUrlFilter.start();
			secondaryUrlFilter.start();
			resolver.start();

			try {
				while (((httpGetter1.getState() != Thread.State.WAITING && httpGetter1
						.getState() != Thread.State.TERMINATED)
						|| (httpGetter2.getState() != Thread.State.WAITING && httpGetter2
								.getState() != Thread.State.TERMINATED)
						// || (httpGetter3.getState() != Thread.State.WAITING && httpGetter3
						// .getState() != Thread.State.TERMINATED)
						// || (httpGetter4.getState() != Thread.State.WAITING && httpGetter4
						// .getState() != Thread.State.TERMINATED)
						// || (httpGetter5.getState() != Thread.State.WAITING && httpGetter5
						// .getState() != Thread.State.TERMINATED)
						// || (httpGetter6.getState() != Thread.State.WAITING && httpGetter6
						// .getState() != Thread.State.TERMINATED)
						// || (httpGetter7.getState() != Thread.State.WAITING && httpGetter7
						// .getState() != Thread.State.TERMINATED)
						// || (httpGetter8.getState() != Thread.State.WAITING && httpGetter8
						// .getState() != Thread.State.TERMINATED)
						// || (httpGetter9.getState() != Thread.State.WAITING && httpGetter9
						// .getState() != Thread.State.TERMINATED)
						// || (httpGetter10.getState() != Thread.State.WAITING &&
						// httpGetter10
						// .getState() != Thread.State.TERMINATED)
						// || (httpGetter11.getState() != Thread.State.WAITING &&
						// httpGetter11
						// .getState() != Thread.State.TERMINATED)
						// || (httpGetter12.getState() != Thread.State.WAITING &&
						// httpGetter12
						// .getState() != Thread.State.TERMINATED)
						// || (httpGetter13.getState() != Thread.State.WAITING &&
						// httpGetter13
						// .getState() != Thread.State.TERMINATED)
						// || (httpGetter14.getState() != Thread.State.WAITING &&
						// httpGetter14
						// .getState() != Thread.State.TERMINATED)
						// || (httpGetter15.getState() != Thread.State.WAITING &&
						// httpGetter15
						// .getState() != Thread.State.TERMINATED)
						// || (httpGetter16.getState() != Thread.State.WAITING &&
						// httpGetter16
						// .getState() != Thread.State.TERMINATED)
						// || (httpGetter17.getState() != Thread.State.WAITING &&
						// httpGetter17
						// .getState() != Thread.State.TERMINATED)
						// || (httpGetter18.getState() != Thread.State.WAITING &&
						// httpGetter18
						// .getState() != Thread.State.TERMINATED)
						// || (httpGetter19.getState() != Thread.State.WAITING &&
						// httpGetter19
						// .getState() != Thread.State.TERMINATED)
						// || (httpGetter20.getState() != Thread.State.WAITING &&
						// httpGetter20
						// .getState() != Thread.State.TERMINATED)
						|| (linkExt.getState() != Thread.State.WAITING && linkExt
								.getState() != Thread.State.TERMINATED)
						|| (resolver.getState() != Thread.State.WAITING && resolver
								.getState() != Thread.State.TERMINATED)
						|| (primaryUrlFilter.getState() != Thread.State.WAITING && primaryUrlFilter
								.getState() != Thread.State.TERMINATED)
						|| (secondaryUrlFilter.getState() != Thread.State.WAITING && secondaryUrlFilter
								.getState() != Thread.State.TERMINATED) || (hostSplitter
						.getState() != Thread.State.WAITING && hostSplitter.getState() != Thread.State.TERMINATED))
						&& !signalSent) {
					Thread.sleep(2000);
				}

			} catch (InterruptedException e) {
				addLog("Crawler received signal to shutdown.");
			}

		} catch (DatabaseException dbe) {
			addLog("Crawler: Problem setting up DB");
		}
		System.out.println("\nExecuting Crawler Drone shutdown.");
		executeShutdown();
	}

	protected static SortedSet<Map.Entry<String, Integer>> hostsSortedByCount(
			Map<String, Integer> hostList) {
		SortedSet<Map.Entry<String, Integer>> sortedEntries = new TreeSet<Map.Entry<String, Integer>>(
				new Comparator<Map.Entry<String, Integer>>() {
					@Override
					public int compare(Map.Entry<String, Integer> e1,
							Map.Entry<String, Integer> e2) {
						int result = -(e1.getValue().intValue() - e2.getValue().intValue());
						if (result != 0)
							return result;
						else
							return e1.getKey().compareTo(e2.getKey());
					}
				});
		sortedEntries.addAll(hostList.entrySet());
		return sortedEntries;
	}

	protected static synchronized void addLog(String event) {
		// if (event.indexOf("HTTP Module") > -1) {
		// System.out.println(event);
		// }
		if (LOGGING_ENABLED) {
			String log = logDateFormat.format(new Date().getTime()) + ": " + event;
			crawlLog.add(log);
			if (crawlLog.size() >= MAX_LOG_SIZE) {
				printLog(logFile);
				crawlLog = new ArrayList<String>();
			}
		}
	}

	@SuppressWarnings("unused")
	private static synchronized void printLog(OutputStreamWriter out) {
		if (LOGGING_ENABLED && crawlLog != null && out != null) {
			try {
				for (String log : crawlLog) {
					out.write(log + "\n", 0, log.length() + 1);
				}
				out.flush();
			} catch (IOException e) {
				addLog("ERROR: WRITING LOG TO OUTPUT FAILED.");
				return;
			}
		}
	}

	private static void printUsage() {
		System.out
				.println("Usage: ./CrawlerDrone [URL seed file location]  [Dir to local BerkleyDB storage] "
						+ " [Max document size (MB) to be retrieved]  [# files to retrieve before halting] "
						+ " [Crawler pastry local port]  [IP for crawler pastry bootstrap node] "
						+ " [Crawler pastry bootstrap port]  [Hostname blacklist file path] "
						+ " [log file location]  [Array of IPs for distributed storage gateway nodes] ... ");
	}

	protected static void sendSignal() {
		signalSent = true;
	}

	private static void executeShutdown() {
		if (!isShutdown) {
			isShutdown = true;
			Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
			for (Thread thread : threadSet) {
				if (!thread.isDaemon()) {
					if (thread instanceof HostSplitter || thread instanceof DnsResolver
							|| thread instanceof HttpProtocolModule
							|| thread instanceof LinkExtractor || thread instanceof UrlFilter) {
						thread.interrupt();
					}
				}
			}
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {}

			System.out.println("\nConsidered: " + visited.keySet().size()
					+ " urls and crawled: " + pagesCrawled);
			printLog(logFile);
			bdb.closeDB();
			try {
				if (logFile != null) {
					logFile.close();
				}
			} catch (IOException e) {
				System.out.println("Error closing drone log file.");
			}
			System.out.println("CrawlerDrone shutdown complete");
		}
		try {
			Runtime.getRuntime().exec(
					"sudo s3cmd put ~/LinkMap* s3://pagerank/cis555/crawlerinput/");
		} catch (IOException e) {}
	}
}
