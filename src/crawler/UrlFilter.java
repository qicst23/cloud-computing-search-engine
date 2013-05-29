/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.PatternSyntaxException;
import storage.UrlContent;
import com.google.common.net.InternetDomainName;
import com.sleepycat.je.DatabaseNotFoundException;
import com.sleepycat.persist.EntityCursor;
import crawler.state.IPresource;
import crawler.state.toFilterStore;

/** Thread for processing links extracted from HTML and XML resources, removing
 * duplicates and building the URL frontier */
public class UrlFilter extends Thread {

	private static boolean primary = false;
	public String hostInProgress = null;

	private static final String CRLF_REGEX = "\\r?\\n";

	/** Default constructor for URL Filter thread */
	public UrlFilter(boolean isPrimary) {
		primary = isPrimary;
		if (isPrimary) {
			CrawlerDrone.addLog("URL Filter starting......");
			CrawlerDrone.robotsDisallow = new Hashtable<String, ArrayList<String>>();
			CrawlerDrone.robotsAllow = new Hashtable<String, ArrayList<String>>();
			CrawlerDrone.robotsRetrievalTime = new Hashtable<String, Long>();
			CrawlerDrone.crawlDelay = new Hashtable<String, Long>();
			CrawlerDrone.urlsToFilter = new LinkedBlockingQueue<IPresource>();
			CrawlerDrone.filterUrlIndex = CrawlerDrone.bdb.getPrimaryIndex(
					storage.BDBWrapper.DB_TO_FILTER_STORE, String.class,
					toFilterStore.class);
			if (CrawlerDrone.filterUrlIndex != null) {
				EntityCursor<toFilterStore> previousUnfiltered = CrawlerDrone.filterUrlIndex
						.entities();
				for (toFilterStore item : previousUnfiltered) {
					try {
						if (item.getIPresource() != null) {
							CrawlerDrone.urlsToFilter.put(item.getIPresource());
							CrawlerDrone.addLog("URL Filter: retrieving "
									+ item.getIPresource().getFullURL());
						}
					} catch (InterruptedException e) {
						CrawlerDrone
								.addLog("URL Filter interrupted while restoring state.");
					}
				}
				previousUnfiltered.close();
				try {
					CrawlerDrone.bdb.getEnvironment().truncateDatabase(null,
							storage.BDBWrapper.DB_TO_FILTER_STORE, false);
				} catch (DatabaseNotFoundException e) {
					// Nothing to restore
				}
			}
		}
		CrawlerDrone.addLog("URL Filter started.");
	}

	@Override
	public void run() {
		while (true) {
			try {
				if (CrawlerDrone.urlsToFilter != null) {
					IPresource toFilter = CrawlerDrone.urlsToFilter.take();
					String toFilterUrl = null;
					toFilterUrl = toFilter.getFullURL();
					InternetDomainName domain = null;
					try {
						domain = InternetDomainName.from(toFilter.getHost());
						if (domain != null) {
							InternetDomainName secondLevel = domain.topPrivateDomain();
							if (secondLevel != null) {
								toFilter.setHost(secondLevel.name());
							}
						}
					} catch (IllegalArgumentException e) {
						domain = null;
					} catch (IllegalStateException e) {
						domain = null;
					}

					if (primary) {
						while (toFilter != null
								&& CrawlerDrone.secondaryUrlFilter != null
								&& toFilter.getHost().equalsIgnoreCase(
										CrawlerDrone.secondaryUrlFilter.hostInProgress)) {
							CrawlerDrone.urlsToFilter.add(toFilter);
							toFilter = CrawlerDrone.urlsToFilter.take();
						}
						hostInProgress = toFilter.getHost();
					} else {
						while (toFilter != null
								&& CrawlerDrone.primaryUrlFilter != null
								&& toFilter.getHost().equalsIgnoreCase(
										CrawlerDrone.primaryUrlFilter.hostInProgress)) {
							CrawlerDrone.urlsToFilter.add(toFilter);
							toFilter = CrawlerDrone.urlsToFilter.take();
						}
						hostInProgress = toFilter.getHost();
					}
					CrawlerDrone.addLog("URL Filter: retrieved " + toFilterUrl);
					if (!toFilterUrl.endsWith("robots.txt")) {
						String host;
						host = toFilter.getHost();
						if (host == null) {
							continue;
						}

						if (!CrawlerDrone.robotsRetrievalTime.containsKey(host)) {
							CrawlerDrone
									.addLog("URL Filter: attempting to get robots.txt file for "
											+ host);
							getRobots(host);
						}
						boolean allowed = true;
						if (CrawlerDrone.robotsDisallow.get(host) != null) {
							if (toFilterUrl.indexOf(host) < 0) {
								CrawlerDrone.urlFrontier.addNew(toFilter);
								continue;
							}
							String path = toFilterUrl.substring(toFilterUrl.indexOf(host)
									+ host.length());
							try {
								for (String rule : CrawlerDrone.robotsDisallow.get(host)) {
									if (path.matches(rule)) {
										allowed = false;
										break;
									}
								}

								if (!allowed) {
									for (String rule : CrawlerDrone.robotsAllow.get(host)) {
										if (path.matches(rule)) {
											allowed = true;
											break;
										}
									}
								}
							} catch (PatternSyntaxException e) {
								allowed = true;
							}
						}
						if (allowed) {
							CrawlerDrone.urlFrontier.addNew(toFilter);
						} else {
							CrawlerDrone.addLog("URL Filter: dropped " + toFilterUrl
									+ " - in violation of robots.txt rules.");
						}
					} else {
						getRobots(toFilter.getHost());
					}
				} else {
					CrawlerDrone
							.addLog("URL Filter: data structures not initialized. Exiting.");
					return;
				}
			} catch (InterruptedException e) {
				CrawlerDrone.addLog("URL Filter: received shutdown signal. Exiting.");
				executeShutdown();
				return;
			}
		}
	}

	private UrlContent headRobots(String inputHost, Long lastRetrieved) {
		String host = inputHost;
		String path = "/robots.txt";
		UrlContent response = null;
		try {
			HttpClient headClient = new HttpClient(host, path);
			response = headClient.sendHead(lastRetrieved);
			headClient.close();
			if (response == null)
				return null;
			int responseCode = response.getStatus();
			if (responseCode == 304)
				return null;
		} catch (UnknownHostException e) {
			CrawlerDrone
					.addLog("URL Filter: Host exception encountered on resource retrival of: "
							+ path + " from: " + inputHost);
		} catch (IOException e) {
			CrawlerDrone
					.addLog("URL Filter: IO exception encountered on resource retrival of: "
							+ path + " from: " + inputHost);
		}
		return response;
	}

	private void getRobots(String inputHost) {
		String host = inputHost;
		String path = "/robots.txt";
		try {
			UrlContent response = headRobots(inputHost, null);
			if (response == null)
				return;
			if (CrawlerDrone.robotsRetrievalTime != null
					&& CrawlerDrone.robotsRetrievalTime.containsKey(inputHost)
					&& response.getLastModified() != -1L
					&& response.getLastModified() > CrawlerDrone.robotsRetrievalTime
							.get(inputHost))
				return;

			int responseCode = response.getStatus();
			if ((responseCode == 301 || responseCode == 302 || responseCode == 303
					|| responseCode == 307 || responseCode == 308)) {
				URL redirect = null;
				if (response.getRedirect() != null) {
					String redirectUrl = LinkExtractor.derelativize(response,
							response.getRedirect());
					try {
						redirect = new URL(redirectUrl);
					} catch (MalformedURLException e) {
						// Location could not be parsed into a valid URL
						return;
					}
					if (redirect != null) {
						host = redirect.getHost();
						path = redirect.getPath();
					}
				}
				HttpClient redirectedHeadClient = null;
				redirectedHeadClient = new HttpClient(host, path,
						response.requiresHSTS());
				response = redirectedHeadClient.sendHead();
				redirectedHeadClient.close();
			}
			if (response == null)
				return;
			if (response.getStatus() == 200) {
				HttpClient getClient = new HttpClient(host, path,
						response.requiresHSTS());
				response = getClient.sendGet(response.isKeepAlive());
				getClient.close();
				if (response == null)
					return;
				String[] rules = response.getContentString().split(CRLF_REGEX);
				ArrayList<String> disallowedPaths = new ArrayList<String>();
				ArrayList<String> allowedPaths = new ArrayList<String>();
				String userAgent = null;
				boolean otherUserAgent = false;
				Long delay = 0L;
				for (String line : rules) {
					if (line.indexOf("#") == 0) {
						// It's a comment line
						continue;
					}
					if (line.toLowerCase().indexOf("user-agent") >= 0) {
						if (line.split(":").length < 2) {
							continue;
						}
						String nextUA = line.split(":")[1].trim();
						if (nextUA.equalsIgnoreCase("cis455crawler")) {
							otherUserAgent = false;
							userAgent = nextUA;
							disallowedPaths = new ArrayList<String>();
							allowedPaths = new ArrayList<String>();
							delay = 0L;
						} else if (nextUA.equals("*")) {
							otherUserAgent = false;
						} else {
							otherUserAgent = true;
						}
					} else if (!otherUserAgent
							&& line.toLowerCase().trim().startsWith("disallow")
							&& (line.trim().length() > "Disallow:".length())) {
						if (line.split(":").length < 2) {
							continue;
						}
						String rule = line.split(":")[1].trim();
						rule = rule.replaceAll("\\.", "\\.");
						rule = rule.replaceAll("\\?", "\\?");
						rule = rule.replaceAll("\\*", ".*");
						rule = rule.replaceAll("//", "/.*/");
						if (rule.endsWith("?") || rule.endsWith("/")) {
							rule = rule + ".*";
						}
						disallowedPaths.add(rule);
						printDebug("URL Filter: getRobots: found disallowed path "
								+ line.split(":")[1].trim() + " with user-agent: " + userAgent
								+ " for host: " + host);
					} else if (!otherUserAgent
							&& line.toLowerCase().trim().startsWith("allow")
							&& (line.trim().length() > "allow:".length())) {
						if (line.split(":").length < 2) {
							continue;
						}
						String rule = line.split(":")[1].trim();
						rule = rule.replaceAll("\\.", "\\.");
						rule = rule.replaceAll("\\?", "\\?");
						rule = rule.replaceAll("\\*", ".*");
						rule = rule.replaceAll("//", "/.*/");
						if (rule.endsWith("?") || rule.endsWith("/")) {
							rule = rule + ".*";
						}
						if (rule.startsWith("?")) {
							rule = "\\" + rule;
						}
						allowedPaths.add(rule);
						printDebug("URL Filter: getRobots: found allowed path "
								+ line.split(":")[1].trim() + " with user-agent: " + userAgent
								+ " for host: " + host);
					} else if (!otherUserAgent
							&& line.toLowerCase().indexOf("crawl-delay") >= 0
							&& (line.trim().length() > "crawl-delay:".length())) {
						try {
							delay = 1000 * Long.parseLong(line.split(":")[1].trim());
						} catch (NumberFormatException e) {
							delay = 0L;
						}
					}
				}
				CrawlerDrone.robotsRetrievalTime.put(inputHost, new Date().getTime());
				CrawlerDrone.robotsDisallow.put(inputHost, disallowedPaths);
				CrawlerDrone.robotsAllow.put(inputHost, allowedPaths);
				CrawlerDrone.crawlDelay.put(inputHost, delay);
			} else {
				CrawlerDrone.robotsDisallow.put(inputHost, new ArrayList<String>());
				CrawlerDrone.robotsAllow.put(inputHost, new ArrayList<String>());
				CrawlerDrone.robotsRetrievalTime.put(inputHost, new Date().getTime());
			}
		} catch (UnknownHostException e) {
			CrawlerDrone
					.addLog("URL Filter: Host exception encountered on resource retrival of: "
							+ path + " from: " + inputHost);
			CrawlerDrone.robotsDisallow.put(inputHost, new ArrayList<String>());
			CrawlerDrone.robotsAllow.put(inputHost, new ArrayList<String>());
			CrawlerDrone.robotsRetrievalTime.put(inputHost, new Date().getTime());
		} catch (IOException e) {
			CrawlerDrone
					.addLog("URL Filter: IO exception encountered on resource retrival of: "
							+ path + " from: " + inputHost);
			CrawlerDrone.robotsDisallow.put(inputHost, new ArrayList<String>());
			CrawlerDrone.robotsAllow.put(inputHost, new ArrayList<String>());
			CrawlerDrone.robotsRetrievalTime.put(inputHost, new Date().getTime());
		} catch (NumberFormatException e) {
			CrawlerDrone
					.addLog("URL Filter: Unrecognizable HTTP response on requesting "
							+ path + " from: " + inputHost);
			CrawlerDrone.robotsDisallow.put(inputHost, new ArrayList<String>());
			CrawlerDrone.robotsAllow.put(inputHost, new ArrayList<String>());
			CrawlerDrone.robotsRetrievalTime.put(inputHost, new Date().getTime());
		}
	}

	private static void printDebug(String debugInfo) {
		if (CrawlerDrone.DEBUG) {
			System.out.println(debugInfo);
		}
	}

	private static void executeShutdown() {
		if (primary && CrawlerDrone.filterUrlIndex != null) {
			for (IPresource res : CrawlerDrone.urlsToFilter) {
				toFilterStore store = new toFilterStore();
				store.setPKey(res.getFullURL());
				store.setIPresource(res);
				CrawlerDrone.filterUrlIndex.put(store);
			}
		}
	}
}
