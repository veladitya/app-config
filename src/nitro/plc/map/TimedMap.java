package nitro.plc.map;

import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import nitro.plc.manager.EventExpiredManager;

/**
 * A wrapper around a {@link ConcurrentMap} that expires entries from the
 * underlying map after a specific period (the TTL value) has expired.
 * 
 * @author 5288873
 *
 * @param <K>
 * @param <V>
 */
public final class TimedMap<K, V> {
	private final ConcurrentMap<K, ExpiredObject<K, V>> map = new ConcurrentHashMap<K, ExpiredObject<K, V>>();
	private final Lock writeLock = new ReentrantLock();
	private Long defaultLifeTime = 3000L;
	private final Timer timer = new Timer("TimedMap Timer", true);

	private EventExpiredManager expiredEventManager;

	public TimedMap(EventExpiredManager expiredEventManager) {
		this(expiredEventManager, 300L);
	}

	public TimedMap(EventExpiredManager expiredEventManager, Long ttl) {
		defaultLifeTime = ttl;
		this.expiredEventManager = expiredEventManager;
	}

	/**
	 * Insert an entry into the underlying map, specifying a time-to-live for the
	 * element to be inserted.
	 * <p/>
	 * 
	 * @param key
	 *            The key of the element to be inserted.
	 * @param value
	 *            The item to be inserted.
	 * @param expiry
	 *            A time-to-live value (specified in milliseconds).
	 */
	public void put(K key, V value, long expiry) {
		try {
			writeLock.lock();
			if (expiry < 0) {
				expiry = defaultLifeTime;
			}
			final ExpiredObject<K, V> object = map.putIfAbsent(key, new ExpiredObject<K, V>(key, value, expiry));
			/* Was there an existing entry for this key? If so, cancel the existing timer */
			if (object != null)
				object.getTask().cancel();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Insert an entry into the map with a default time-to-live value.
	 * 
	 * @param key
	 *            The key of the element to be inserted.
	 * @param value
	 *            The item to be inserted.
	 */
	public void put(K key, V value) {
		put(key, value, defaultLifeTime);
	}

	/**
	 * Retrieve the entry identified by <code>key</code>, or <code>null</code> if it
	 * does not exist.
	 * 
	 * @param key
	 *            The key identifying the entry to retrieve
	 * @return The entry corresponding to <code>key</code>, or <code>null</code>.
	 */
	public V get(K key) {
		return (V) (map.containsKey(key) ? map.get(key).getValue() : null);
	}

	/**
	 * Clear the underlying map and cancel any outstanding expiry timers.
	 */
	public void clear() {
		try {
			writeLock.lock();
			for (ExpiredObject<K, V> obj : map.values()) {
				obj.getTask().cancel();
			}
			map.clear();
			timer.purge(); // Force removal of all cancelled tasks
		} finally {
			writeLock.unlock();
		}
	}

	public boolean containsKey(K key) {
		return map.containsKey(key);
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * Remov the element identified by <code>key</code> from the map, returning
	 * <code>null</code> if it does not exist.
	 * 
	 * @param key
	 *            The key identifying the element to remove
	 * @return The removed element, or null if it was not present.
	 */
	public V remove(K key) {
		final ExpiredObject<K, V> object;
		try {
			writeLock.lock();
			// System.out.println("Removing element with key:" + key);
			object = map.remove(key);
			if (object != null)
				object.getTask().cancel();
		} finally {
			writeLock.unlock();
		}
		return (object == null ? null : object.getValue());
	}

	public int size() {
		return map.size();
	}
}