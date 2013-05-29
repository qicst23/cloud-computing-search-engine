/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package storage;

import java.io.Serializable;
import java.util.ArrayList;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
public class ResultsCache implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public ResultsCache() {}

	@PrimaryKey
	private String query;

	private ArrayList<Result> results;

	public void setQuery(String query) {
		this.query = query;
	}

	public String getQuery() {
		return query;
	}

	public void setResults(ArrayList<Result> input) {
		results = input;
	}

	public ArrayList<Result> getResults() {
		return results;
	}

}
