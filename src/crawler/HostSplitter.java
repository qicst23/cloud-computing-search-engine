/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package crawler;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.concurrent.LinkedBlockingQueue;
import com.sleepycat.je.DatabaseNotFoundException;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.PrimaryIndex;
import crawler.state.toSplitStore;

/** @author Michael Collis (mcollis@seas.upenn.edu) */
public class HostSplitter extends Thread {

	protected static LinkedBlockingQueue<String> linksToSplit = null;
	private static PrimaryIndex<String, toSplitStore> toSplitIndex;
	protected static HostSplitterHandler handler = null;

	/** Default constructor */
	public HostSplitter(String seedfile, int pastryPort,
			InetSocketAddress bootstrap) {
		linksToSplit = new LinkedBlockingQueue<String>();
		CrawlerDrone.addLog("Host Splitter starting.........");
		if (seedfile != null) {
			BufferedReader seeds = null;
			int numSeeded = 0;
			try {
				seeds = new BufferedReader(new InputStreamReader(new FileInputStream(
						seedfile), Charset.forName("UTF-8")));;
			} catch (FileNotFoundException e) {
				seeds = null;
			}
			if (seeds != null) {
				String line;
				try {
					while ((line = seeds.readLine()) != null) {
						linksToSplit.add(line);
						numSeeded++;
					}
				} catch (IOException e) {}
			}
			System.out.println("Seeded " + numSeeded + " URLs from input file.");
		}

		toSplitIndex = CrawlerDrone.bdb.getPrimaryIndex(
				storage.BDBWrapper.DB_TO_SPLIT_STORE, String.class, toSplitStore.class);
		if (toSplitIndex != null) {
			EntityCursor<toSplitStore> previousToProcess = toSplitIndex.entities();
			for (toSplitStore item : previousToProcess) {
				linksToSplit.add(item.getPKey());
				previousToProcess.delete();
			}
			previousToProcess.close();
			try {
				CrawlerDrone.bdb.getEnvironment().truncateDatabase(null,
						storage.BDBWrapper.DB_TO_SPLIT_STORE, false);
			} catch (DatabaseNotFoundException e) {
				// Nothing to restore
			}
		}
		handler = new HostSplitterHandler(pastryPort, bootstrap);
		CrawlerDrone.addLog("Host Splitter started.");
	}

	@Override
	public void run() {
		if (linksToSplit != null) {
			while (true) {
				try {
					String toSplit = HostSplitter.linksToSplit.take();
					String hostname;
					try {
						hostname = InetAddress.getByName(toSplit).getHostName();
					} catch (UnknownHostException e) {
						hostname = toSplit;
					}
					handler.sendMessage(handler.nodeFactory.getIdFromString(hostname),
							toSplit);
				} catch (InterruptedException e) {
					CrawlerDrone
							.addLog("Host Splitter: received shutdown signal. Exiting.");
					break;
				}
			}
		}
		executeShutdown();
	}

	private void executeShutdown() {
		if (toSplitIndex != null) {
			for (String link : linksToSplit) {
				toSplitStore store = new toSplitStore();
				store.setPKey(link);
				toSplitIndex.put(store);
			}
		}
		handler.closeNode();
	}
}
