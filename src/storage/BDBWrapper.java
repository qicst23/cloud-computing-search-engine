/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package storage;

import java.io.File;
import java.util.ArrayList;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;
import com.sleepycat.persist.StoreConfig;

/** Wrapper class encapsulating initiation, connection to and manipulation of a
 * BDB instance
 * 
 * @author Michael Collis (mcollis@seas.upenn.edu)
 * @version 20130409 */
public class BDBWrapper {

	/** String constants for names of Berkeley DB EntityStores */
	public static final String DB_CONTENT_STORE = "crawledContent";
	public static final String DB_FRONTIER_STORE = "frontier";
	public static final String DB_VISITED_STORE = "visited";
	public static final String DB_CONTENT_SEEN = "contentSeen";
	public static final String DB_TO_EXTRACT_STORE = "toExtract";
	public static final String DB_TO_RESOLVE_STORE = "toResolve";
	public static final String DB_TO_SPLIT_STORE = "toSplit";
	public static final String DB_TO_FILTER_STORE = "toFilter";
	public static final String DB_RIENTRY_STORE = "rientries";
	public static final String DB_URLLINKS_STORE = "urllinks";
	public static final String DB_URL_TO_HASH_STORE = "urlToHash";
	public static final String DB_QUERY_CACHE = "queryCache";

	/** Environment for the database */
	private Environment dbEnvironment = null;

	/** List of all the EntityStores currently open in the database */
	private ArrayList<EntityStore> tables = null;

	/** Default constructor */
	public BDBWrapper() {}

	/** Sets up the DB instance and initializes the empty list of entity stores
	 * 
	 * @param pathToDB Absolute path to the DB folder (throws an exception on an
	 *          error or if the folder could not be created)
	 * @return True if DB setup succeeds; false otherwise */
	public boolean setupDB(String pathToDB) {
		try {
			EnvironmentConfig envConfig = new EnvironmentConfig();
			envConfig.setAllowCreate(true);
			File DBdir = new File(pathToDB);
			if (!DBdir.exists()) {
				if (!DBdir.mkdirs()) {
					System.err.println("Unable to create DB folder. Setup failed.");
					return false;
				}
			}
			dbEnvironment = new Environment(new File(pathToDB), envConfig);
		} catch (DatabaseException dbe) {
			System.err.println("Database exception occurred on wrapper initiation.");
			return false;
		}
		tables = new ArrayList<EntityStore>();
		return true;
	}

	/** Retrieves the DB Environment
	 * 
	 * @return Environment in which the database is operating */

	public Environment getEnvironment() {
		return dbEnvironment;
	}

	/** Shuts down the DB instance and closes all open EntityStores
	 * 
	 * @return True if all the EntityStores were closed correctly and the
	 *         Environment was closed without error; false otherwise */
	public boolean closeDB() {
		try {
			if (tables != null) {
				for (EntityStore ent : tables) {
					ent.close();
				}
			}
		} catch (DatabaseException dbe) {
			System.err
					.println("Database exception occurred on closing EntityStores.");
			return false;
		}

		try {
			if (dbEnvironment != null) {
				// dbEnvironment.cleanLog();
				dbEnvironment.close();
			}
		} catch (DatabaseException dbe) {
			System.err.println("Database exception occurred on wrapper close call.");
			return false;
		}
		return true;
	}

	/** Opens the EntityStore with the input name. If no such EntityStore exists,
	 * it will be created
	 * 
	 * @param name Name of EntityStore to open
	 * @return True if EntityStore is already open, if one with the input name was
	 *         found and opened successfully, or if a new EntityStore with that
	 *         name was created successfully; false otherwise */
	public boolean setupStore(String name) {
		if (tables != null && dbEnvironment != null) {
			for (EntityStore ent : tables) {
				if (ent.getStoreName() != null && ent.getStoreName().equals(name))
					return true;
			}
			try {
				StoreConfig config = new StoreConfig();
				config.setAllowCreate(true);
				config.setExclusiveCreate(false);
				tables.add(new EntityStore(dbEnvironment, name, config));
				return true;
			} catch (DatabaseException dbe) {
				System.err.println("Unable to create DB entity store with the name: "
						+ name);
				dbe.printStackTrace();
			}
		}
		return false;
	}

	/** Retrieves the EntityStore with the input name
	 * 
	 * @param name Name of EntityStore to retrieve
	 * @return If it exists, the EntityStore with the input name is retrieved;
	 *         else, returns null */
	public EntityStore getEntityStore(String name) {
		if (tables != null && dbEnvironment != null) {
			for (EntityStore ent : tables) {
				if (ent.getStoreName().equals(name))
					return ent;
			}
		}
		return null;
	}

	/** Retrieves the PrimaryIndex for a given EntityStore
	 * 
	 * @param name Name of the EntityStore for which to retrieve the PrimaryIndex
	 * @param pKey Class of the PKey of the PrimaryIndex being retrieved
	 * @param value Class of the value of the PrimaryIndex being retrieved
	 * @return If an EntityStore of the input name exists, has matching PKey and
	 *         value Class templates, and is open, its PrimaryIndex is returned;
	 *         else, returns null */
	public <T, K> PrimaryIndex<T, K> getPrimaryIndex(String name, Class<T> pKey,
			Class<K> value) {
		EntityStore store = getEntityStore(name);
		if (store != null)
			return store.getPrimaryIndex(pKey, value);
		return null;
	}

	public <SK, PK, E> SecondaryIndex<SK, PK, E> getSecondaryIndex(
			String storeName, PrimaryIndex<PK, E> pIndex, Class<SK> keyClass,
			String keyName) {
		EntityStore store = getEntityStore(storeName);
		if (store != null)
			return store.getSecondaryIndex(pIndex, keyClass, keyName);
		return null;
	}

}
