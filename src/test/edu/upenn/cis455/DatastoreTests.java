/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package test.edu.upenn.cis455;

import storage.ContentQueryHandler;
import junit.framework.TestCase;

/** Cache/Storage JUnit tests */

public class DatastoreTests extends TestCase {

	public static void main(String[] a) {
		junit.textui.TestRunner.run(DatastoreTests.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void test1() {
		ContentQueryHandler h1 = new ContentQueryHandler(
				"/Volumes/MacFiles/Xcode/BDB/1", 9001, 4, 0);
		ContentQueryHandler h2 = new ContentQueryHandler(
				"/Volumes/MacFiles/Xcode/BDB/2", 9002, 4, 1);
		ContentQueryHandler h3 = new ContentQueryHandler(
				"/Volumes/MacFiles/Xcode/BDB/3", 9003, 4, 2);

	}

}
