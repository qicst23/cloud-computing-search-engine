/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package test.edu.upenn.cis455;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class RunAllTests extends TestCase {
	public static Test suite() {
		try {
			@SuppressWarnings("rawtypes")
			Class[] testClasses = { Class
					.forName("test.edu.upenn.cis455.DatastoreTests"),
					Class.forName("test.edu.upenn.cis455.IndexerTest"),
					Class.forName("test.edu.upenn.cis455.IndexerCalculatorTests")
					};

			return new TestSuite(testClasses);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}
