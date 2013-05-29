/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package crawler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.TimeZone;
import storage.UrlContent;
import com.sleepycat.je.DatabaseNotFoundException;
import com.sleepycat.persist.EntityCursor;
import crawler.state.HostFrontier;
import crawler.state.IPresource;
import crawler.state.UrlFrontier;
import crawler.state.frontierStore;
import crawler.state.visitedStore;

/** Thread for retrieving resources using the HTTP protocol */
public class HttpProtocolModule extends Thread {

	public boolean primary = false;
	private static long maxFileSizeBytes = 5242880; // Defaults to 5MB
	private static final long MBtoBytes = 1024 * 1024;
	private static final long millisecondsInHour = 3600000;
	private final SimpleDateFormat dateFormat = new SimpleDateFormat(
			"EEE, d MMM yyyy HH:mm:ss z");

	/** Default constructor for HTTP protocol module */
	public HttpProtocolModule(boolean primary) {
		CrawlerDrone.addLog("HTTP Module starting.....");
		if (primary) {
			this.primary = primary;
			CrawlerDrone.hostLastAction = new Hashtable<String, Long>();
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			CrawlerDrone.visitedIndex = CrawlerDrone.bdb
					.getPrimaryIndex(storage.BDBWrapper.DB_VISITED_STORE, String.class,
							visitedStore.class);

			if (CrawlerDrone.visitedIndex != null) {
				EntityCursor<visitedStore> previouslyVisited = CrawlerDrone.visitedIndex
						.entities();
				for (visitedStore item : previouslyVisited) {
					try {
						CrawlerDrone.visited.put(item.getPKey(), item.getTimestamp());
					} catch (Exception e) {
						previouslyVisited.close();
						return;
					}
					printDebug("HTTP Module: Restoring previously visited list with: "
							+ item.getPKey());
				}
				previouslyVisited.close();

				try {
					CrawlerDrone.bdb.getEnvironment().truncateDatabase(null,
							storage.BDBWrapper.DB_VISITED_STORE, false);
				} catch (DatabaseNotFoundException e) {
					// Nothing to restore
				}
			}

			CrawlerDrone.urlFrontier = new UrlFrontier();
			CrawlerDrone.frontierIndex = CrawlerDrone.bdb.getPrimaryIndex(
					storage.BDBWrapper.DB_FRONTIER_STORE, String.class,
					frontierStore.class);
			if (CrawlerDrone.frontierIndex != null) {
				EntityCursor<frontierStore> previousFrontier = CrawlerDrone.frontierIndex
						.entities();
				int restoredCount = 0;
				for (frontierStore item : previousFrontier) {
					IPresource inputUrl;
					try {
						inputUrl = new IPresource(InetAddress.getByName(item.getHost()),
								item.getResource(), null);
						if (LinkExtractor.hostnameBlacklist == null
								|| inputUrl.getHost() == null
								|| !LinkExtractor.hostnameBlacklist
										.contains(inputUrl.getHost())) {
							CrawlerDrone.urlFrontier.addNew(inputUrl);
							restoredCount++;
						}
					} catch (UnknownHostException e) {
						CrawlerDrone
								.addLog("Crawler: Restoring URL frontier: Unable to resolve DNS for "
										+ item.getHost());
						continue;
					}
					printDebug("Crawler: Restoring URL frontier with: " + item.getPKey());
				}
				System.out.println("Restored " + restoredCount
						+ " URLs to the frontier from previous crawl.");
				try {
					previousFrontier.close();
					CrawlerDrone.bdb.getEnvironment().truncateDatabase(null,
							storage.BDBWrapper.DB_FRONTIER_STORE, false);
				} catch (DatabaseNotFoundException e) {
					// Nothing to restore
				}
			}
			CrawlerDrone.addLog("Primary HTTP Module started.");
		} else {
			CrawlerDrone.addLog("Secondary HTTP Module started.");
		}
	}

	/** Default constructor for HTTP protocol module */
	public HttpProtocolModule(boolean primary, long maxSize) {
		this(primary);
		if (maxSize * MBtoBytes > 0) {
			maxFileSizeBytes = maxSize * MBtoBytes;
		}
	}

	@Override
	public void run() {
		if (CrawlerDrone.contentClient == null) {
			CrawlerDrone
					.addLog("HTTP Module: DB content index could not be retrieved. Exiting.");
			return;
		}
		while (true) {
			if (CrawlerDrone.pagesCrawled >= CrawlerDrone.MAX_PAGES_TO_CRAWL) {
				CrawlerDrone
						.addLog("HTTP Module: Crawler has reached specified maximum number of pages to gather. Exiting.");
				executeShutdown(primary);
				return;
			}
			try {
				Long timestamp = new Date().getTime();
				String host = null, path = null, fullUrl = null;
				if (CrawlerDrone.urlFrontier != null && CrawlerDrone.visited != null) {
					IPresource toCrawl = CrawlerDrone.urlFrontier.getNext();
					if (toCrawl == null) {
						// On the off chance the URL frontier is empty
						Thread.sleep(1000);
						continue;
					}

					CrawlerDrone.addLog("HTTP Module: examining " + toCrawl);

					host = toCrawl.getIp().getHostName();
					path = toCrawl.getResource();
					if (host != null && CrawlerDrone.hostLastAction.containsKey(host)
							&& CrawlerDrone.crawlDelay.containsKey(host)) {
						if (timestamp - CrawlerDrone.hostLastAction.get(host) < CrawlerDrone.crawlDelay
								.get(host)) {
							toCrawl.setLastVisited(timestamp);
							CrawlerDrone.urlFrontier.replace(toCrawl);
							CrawlerDrone
									.addLog("HTTP Module: "
											+ host
											+ " was accessed too recently, moving URL to back of the frontier.");
							// Thread.sleep(200);
							continue;
						}
					}
					fullUrl = toCrawl.getFullURL();
					if (CrawlerDrone.visited.containsKey(fullUrl)
							&& timestamp - CrawlerDrone.visited.get(fullUrl) < (millisecondsInHour * 12)) {
						toCrawl.setLastVisited(timestamp);
						// urlFrontier.replace(toCrawl);
						continue;
					}
					try {
						HttpClient headClient = new HttpClient(host, path);
						UrlContent response = headClient.sendHead(null);
						headClient.close();
						printDebug("HTTP Module: To Crawl Full URL: "
								+ toCrawl.getFullURL());
						if (response == null) {
							continue;
						}
						long lastModified = response.getLastModified();
						if (CrawlerDrone.visited.containsKey(fullUrl)
								&& lastModified > CrawlerDrone.visited.get(fullUrl)) {
							CrawlerDrone.addLog("HTTP Module: " + fullUrl
									+ " not modified since last crawl.");
							String hash = CrawlerDrone.contentClient
									.sendGetHashForUrl(fullUrl);
							if (hash != null) {
								UrlContent previousCrawl = CrawlerDrone.contentClient
										.sendGetContent(hash);
								if (previousCrawl != null) {
									previousCrawl.setCrawlTime(timestamp);
									LinkExtractor.pagesToExtractLinksFrom.add(previousCrawl);
								}
							}
							continue;
						}

						int responseCode = response.getStatus();
						if ((responseCode == 301 || responseCode == 302
								|| responseCode == 303 || responseCode == 307 || responseCode == 308)) {
							URL redirect = null;
							if (response.getRedirect() != null) {
								String redirectUrl = LinkExtractor.derelativize(response,
										response.getRedirect());
								try {
									redirect = new URL(redirectUrl);
								} catch (MalformedURLException e) {
									// Location could not be parsed into a valid URL
									continue;
								}
								host = redirect.getHost();
								path = redirect.getPath();
								fullUrl = host + path;
							}
							CrawlerDrone
									.addLog("HTTP Module: Received redirect to resource: "
											+ fullUrl);
							HttpClient redirectedHeadClient = null;
							redirectedHeadClient = new HttpClient(host, path,
									response.requiresHSTS());
							response = redirectedHeadClient.sendHead();
							redirectedHeadClient.close();
							printDebug("HTTP Module: To Crawl Full URL: "
									+ toCrawl.getFullURL());
						}

						CrawlerDrone.visited.put(fullUrl, timestamp);
						CrawlerDrone.hostLastAction.put(host, timestamp);
						if (response == null) {
							continue;
						}
						if (response.getStatus() == 200 && response.conformingMimeType()
								&& response.getContentLength() <= maxFileSizeBytes) {
							CrawlerDrone
									.addLog("HTTP Module: 200 OK received, content length acceptable and MIME type matches parameters.");
							HttpClient getClient = new HttpClient(host, path,
									response.requiresHSTS());
							response = getClient.sendGet(response.isKeepAlive());
							getClient.close();
							if (response == null) {
								continue;
							}
							if (response.getStatus() != 200) {
								continue;
							}
							response.setHost(host);
							CrawlerDrone.contentClient.sendPutUrlToHash(fullUrl,
									response.getPKey());
							if (!seenBefore(response.getContentString())) {
								CrawlerDrone.pagesCrawled++;
								System.out.println(CrawlerDrone.pagesCrawled + ": " + fullUrl
										+ ": Downloading");
								response.setCrawlTime(timestamp);
								if (response.getHost() != null) {
									if (CrawlerDrone.topHosts != null) {
										if (CrawlerDrone.topHosts.containsKey(response.getHost())) {
											CrawlerDrone.topHosts.put(response.getHost(),
													CrawlerDrone.topHosts.get(response.getHost()) + 1);
										} else {
											CrawlerDrone.topHosts.put(response.getHost(), 1);
										}
									}
								}
								if (CrawlerDrone.pagesCrawled % 100 == 0) {
									printTopHosts();
								}
							} else {
								printDebug("HTTP Module: Encountered duplicate content: "
										+ response.getContentString().length() + "  "
										+ response.getPKey());
								CrawlerDrone.contentClient.sendUpdateContentCrawlTime(
										response.getPKey(), timestamp);
							}
							LinkExtractor.pagesToExtractLinksFrom.add(response);
						} else {
							printDebug("Dropping url: " + response.getPKey()
									+ " with length: " + response.getContentLength());
							continue;
						}
					} catch (UnknownHostException e) {
						CrawlerDrone
								.addLog("HTTP Module: Host exception encountered on resource retrival of: "
										+ path + " from: " + host);
					} catch (IOException e) {
						CrawlerDrone
								.addLog("HTTP Module: IO exception encountered on resource retrival of: "
										+ path + " from: " + host);
					}
				} else {
					CrawlerDrone
							.addLog("HTTP Module: data structures not initialized. Exiting.");
					return;
				}
			} catch (InterruptedException e) {
				executeShutdown(primary);
				CrawlerDrone.addLog("HTTP Module: received shutdown signal. Exiting.");
				return;
			}
		}
	}

	private void printTopHosts() {
		System.err.println("\n---PROGRESS REPORT---");
		int count = 0;
		for (Entry<String, Integer> entry : CrawlerDrone
				.hostsSortedByCount(CrawlerDrone.topHosts)) {
			if (count >= 10) {
				break;
			}
			System.err.println(entry.getKey() + ": " + entry.getValue());
			count++;
		}
		System.err.println("-----END-----\n");
	}

	private boolean seenBefore(String content) {
		if (content.length() == 0)
			return false;
		try {
			MessageDigest cryptHash = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = cryptHash.digest(content.getBytes());
			StringBuilder hashed = new StringBuilder();
			for (int k = 0; k < hashBytes.length; k++) {
				hashed.append(String.format("%02x", hashBytes[k]));
			}
			printDebug("HTTP Module: Content hash: " + hashed.toString());
			if (CrawlerDrone.contentClient.sendContainsContent(hashed.toString())) {
				CrawlerDrone.addLog("HTTP Module: Encountered duplicate content.");
				return true;
			} else
				return false;
		} catch (NoSuchAlgorithmException e) {
			CrawlerDrone.addLog("HTTP Module: Content hashing failed.");
		}
		return false;
	}

	@SuppressWarnings("unused")
	private Date getDateFromString(String datestamp) {
		try {
			return dateFormat.parse(datestamp);
		} catch (ParseException e) {
			return null;
		}
	}

	private static void printDebug(String debugInfo) {
		if (CrawlerDrone.DEBUG) {
			System.out.println(debugInfo);
		}
	}

	private static void executeShutdown(boolean primary) {
		if (primary) {
			if (CrawlerDrone.visitedIndex != null) {
				for (String site : CrawlerDrone.visited.keySet()) {
					visitedStore store = new visitedStore();
					store.setPKey(site);
					store.setTimestamp(CrawlerDrone.visited.get(site));
					CrawlerDrone.visitedIndex.put(store);
				}
			}
			if (CrawlerDrone.frontierIndex != null) {
				for (String host : CrawlerDrone.urlFrontier.keySet()) {
					HostFrontier hostFrontier = CrawlerDrone.urlFrontier.get(host);
					for (IPresource item : hostFrontier) {
						frontierStore store = new frontierStore();
						store.setPKey(item.getFullURL());
						store.setHost(item.getIp().getHostName());
						store.setResource(item.getResource());
						CrawlerDrone.frontierIndex.put(store);
					}
				}
			}
		}
		CrawlerDrone.sendSignal();
	}

}
