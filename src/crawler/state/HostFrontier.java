/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package crawler.state;

import java.util.Comparator;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.PriorityBlockingQueue;

/** @author Michael Collis (mcollis@seas.upenn.edu) */
public class HostFrontier extends PriorityBlockingQueue<IPresource> {

	private static final long serialVersionUID = 1L;
	private String hostname = null;
	private Long lastAction = null;
	private static Random chaos = new Random(new Date().getTime());

	/** Default constructor */
	public HostFrontier(String host) {
		super(20, new Comparator<IPresource>() {
			@Override
			public int compare(IPresource arg0, IPresource arg1) {
				if (arg0.equals(arg1))
					return 0;
				if (arg0.getLastVisited() != null) {
					if (arg1.getLastVisited() != null)
						return arg0.getLastVisited() < arg1.getLastVisited() ? -1 : 1;
					else
						return 1;
				} else {
					if (arg1.getLastVisited() != null)
						return -1;
					else
						return chaos.nextInt(3) - 1;
				}
			}
		});
		hostname = host;
	}

	@Override
	public IPresource take() {
		lastAction = new Date().getTime();
		try {
			return super.take();
		} catch (InterruptedException e) {
			return null;
		}
	}

	public void replace(IPresource item) {
		if (item != null) {
			lastAction = new Date().getTime();
			put(item);
		}
	}

	public String getHost() {
		return hostname;
	}

	public Long getLastAction() {
		return lastAction;
	}

	public void setLastAction() {
		lastAction = new Date().getTime();
	}

}
