/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package crawler.state;

import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import com.google.common.net.InternetDomainName;

/** @author Michael Collis (mcollis@seas.upenn.edu) */
public class UrlFrontier extends ConcurrentSkipListMap<String, HostFrontier> {

	private static final long serialVersionUID = 1L;
	private static Random chaos = new Random(new Date().getTime());

	public UrlFrontier() {
		super();
	}

	protected static SortedSet<Map.Entry<String, HostFrontier>> hostFrontiersSorted(
			Map<String, HostFrontier> hostList) {
		SortedSet<Map.Entry<String, HostFrontier>> sortedEntries = new TreeSet<Map.Entry<String, HostFrontier>>(
				new Comparator<Map.Entry<String, HostFrontier>>() {
					@Override
					public int compare(Map.Entry<String, HostFrontier> e1,
							Map.Entry<String, HostFrontier> e2) {

						if (e1.getValue().getLastAction() != null) {
							if (e2.getValue().getLastAction() != null)
								return e1.getValue().getLastAction() < e2.getValue()
										.getLastAction() ? -1 : 1;
							else
								return 1;
						} else {
							if (e2.getValue().getLastAction() != null)
								return -1;
							else
								return chaos.nextInt(3) - 1;
						}
					}
				});
		sortedEntries.addAll(hostList.entrySet());
		return sortedEntries;
	}

	private HostFrontier getNextFrontier() {
		while (this.size() == 0) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}
		SortedSet<Map.Entry<String, HostFrontier>> sorted = hostFrontiersSorted(this);
		return sorted.first().getValue();
	}

	public synchronized IPresource getNext() throws InterruptedException {
		HostFrontier next = getNextFrontier();
		while (next.size() == 0) {
			next = getNextFrontier();
		}

		IPresource result = next.take();
		put(next.getHost(), next);
		return result;
	}

	public void addNew(IPresource toAdd) {
		if (toAdd != null) {
			if (toAdd.getHost() != null) {
				String hostname = toAdd.getHost();

				InternetDomainName domain = null;
				try {
					domain = InternetDomainName.from(hostname);
					if (domain != null) {
						InternetDomainName secondLevel = domain.topPrivateDomain();
						if (secondLevel != null) {
							hostname = secondLevel.name();
							toAdd.setHost(hostname);
						}
					}
				} catch (IllegalArgumentException e) {
					hostname = toAdd.getHost();
				} catch (IllegalStateException e) {
					hostname = toAdd.getHost();
				}

				if (this.containsKey(hostname)) {
					HostFrontier matching = this.get(hostname);
					matching.add(toAdd);
					this.put(hostname, matching);
				} else {
					HostFrontier frontierToAdd = new HostFrontier(hostname);
					frontierToAdd.put(toAdd);
					this.put(hostname, frontierToAdd);
				}
			}
		}
	}

	public void replace(IPresource toReplace) {
		if (toReplace != null) {
			String hostname = toReplace.getHost();
			if (hostname != null) {
				InternetDomainName domain = null;
				try {
					domain = InternetDomainName.from(hostname);
					if (domain != null) {
						InternetDomainName secondLevel = domain.topPrivateDomain();
						if (secondLevel != null) {
							hostname = secondLevel.name();
							toReplace.setHost(hostname);
						}
					}
				} catch (IllegalArgumentException e) {
					hostname = toReplace.getHost();
				} catch (IllegalStateException e) {
					hostname = toReplace.getHost();
				}

				if (this.containsKey(hostname)) {
					HostFrontier matching = this.get(hostname);
					matching.replace(toReplace);
					this.put(hostname, matching);
				} else {
					HostFrontier frontierToAdd = new HostFrontier(hostname);
					frontierToAdd.replace(toReplace);
					this.put(hostname, frontierToAdd);
				}

			}
		}
	}

	@SuppressWarnings("unused")
	private int getSize() {
		int size = 0;
		for (HostFrontier host : this.values()) {
			size += host.size();
		}
		return size;
	}

	/** For debugging purposes only */
	public void printHosts() {
		System.out
				.println("Hosts currently in URL Frontier (" + this.size() + "):");
		for (HostFrontier hostQueue : this.values()) {
			System.out.println(hostQueue.getHost() + " : " + hostQueue.size() + "\t"
					+ hostQueue.getLastAction());
		}
	}

}
