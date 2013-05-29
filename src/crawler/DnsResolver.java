/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package crawler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import com.sleepycat.je.DatabaseNotFoundException;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.PrimaryIndex;
import crawler.state.IPresource;
import crawler.state.toResolveStore;

/** Thread for resolving hostnames to DNS */
public class DnsResolver extends Thread {

	protected static LinkedBlockingQueue<String> urlsToResolve = null;
	private static PrimaryIndex<String, toResolveStore> resolutionIndex;
	protected ConcurrentHashMap<String, InetAddress> dnsCache = null;

	/** Default constructor for the DNS resolver thread */
	public DnsResolver() {
		dnsCache = new ConcurrentHashMap<String, InetAddress>();
		urlsToResolve = new LinkedBlockingQueue<String>();
		CrawlerDrone.addLog("DNS Resolver starting......");
		resolutionIndex = CrawlerDrone.bdb.getPrimaryIndex(
				storage.BDBWrapper.DB_TO_RESOLVE_STORE, String.class,
				toResolveStore.class);
		if (resolutionIndex != null) {
			EntityCursor<toResolveStore> previousUnresolved = resolutionIndex
					.entities();
			for (toResolveStore item : previousUnresolved) {
				urlsToResolve.add(item.getPKey());
			}
			previousUnresolved.close();
			try {
				CrawlerDrone.bdb.getEnvironment().truncateDatabase(null,
						storage.BDBWrapper.DB_TO_RESOLVE_STORE, false);
			} catch (DatabaseNotFoundException e) {
				// Nothing to restore
			}
		}
		CrawlerDrone.addLog("DNS Resolver started.");
	}

	@Override
	public void run() {
		while (true) {
			try {
				if (urlsToResolve != null && CrawlerDrone.urlFrontier != null) {
					String toResolve = urlsToResolve.take();
					CrawlerDrone.addLog("DNS Resolver: retrieved " + toResolve);

					int protocolIndex = toResolve.indexOf("://");
					String path = null;
					if (protocolIndex != -1) {
						int pathIndex = toResolve.indexOf("/", protocolIndex + 3);
						if (pathIndex != -1) {
							path = toResolve.substring(pathIndex);
						} else {
							path = "/";
						}
					} else {
						int pathIndex = toResolve.indexOf("/");
						if (pathIndex != -1) {
							path = toResolve.substring(pathIndex);
						} else {
							path = "/";
						}
						protocolIndex = -3;
					}
					if (path == null) {
						continue;
					}

					String host = null;
					if (toResolve.indexOf(path) == -1) {
						host = toResolve;
					} else {
						if (toResolve.indexOf(path, protocolIndex + 3) != -1) {
							host = toResolve.substring(protocolIndex + 3,
									toResolve.indexOf(path, protocolIndex + 3));
						} else {
							host = toResolve.substring(protocolIndex + 3);
						}
					}

					InetAddress address = null;
					if (dnsCache.contains(host)) {
						address = dnsCache.get(host);
					} else {
						try {
							address = InetAddress.getByName(host);
							dnsCache.put(host, address);
						} catch (UnknownHostException e) {
							CrawlerDrone
									.addLog("DNS Resolver: unable to resolve IP address for "
											+ toResolve + ". Skipping.");
							continue;
						}
					}

					IPresource toAdd = new IPresource(address, path, null);
					Long lastVisited = null;
					if (CrawlerDrone.visited != null
							&& CrawlerDrone.visited.containsKey(toAdd.getFullURL())) {
						lastVisited = CrawlerDrone.visited.get(toAdd.getFullURL());
						toAdd.setLastVisited(lastVisited);
					}
					CrawlerDrone.urlsToFilter.add(toAdd);
				} else {
					CrawlerDrone
							.addLog("DNS Resolver: data structures not initialized. Exiting.");
					return;
				}
			} catch (InterruptedException e) {
				CrawlerDrone.addLog("DNS Resolver: received shutdown signal. Exiting.");
				executeShutdown();
				return;
			}
		}
	}

	private void executeShutdown() {
		if (resolutionIndex != null) {
			for (String link : urlsToResolve) {
				toResolveStore store = new toResolveStore();
				store.setPKey(link);
				resolutionIndex.put(store);
			}
		}
	}

}
