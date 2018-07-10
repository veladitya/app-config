package nitro.plc.map.schedule;

import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
public final class SchedulerMap<K, V> {
	private final ConcurrentMap<K, ExpiredObject<K, V>> map = new ConcurrentHashMap<K, ExpiredObject<K, V>>();
	private final Lock writeLock = new ReentrantLock();
	private Long defaultLifeTime = 3000L;
	private ScheduledThreadPoolExecutor scheduledExecutorService = (ScheduledThreadPoolExecutor) Executors
			.newScheduledThreadPool(20);

	private EventExpiredManager expiredEventManager;

	public SchedulerMap(EventExpiredManager expiredEventManager) {
		this(expiredEventManager, 300L);
	}

	public SchedulerMap(EventExpiredManager expiredEventManager, Long ttl) {
		defaultLifeTime = ttl;
		this.expiredEventManager = expiredEventManager;
		scheduledExecutorService.setRemoveOnCancelPolicy(true); // Force removal of all cancelled tasks
	}

	/**
	 * A wrapper for a underlying object that associates a {@link TimerTask}
	 * instance with the object.
	 * 
	 * @author 5288873
	 *
	 * @param <K>
	 *            The key type K
	 * @param <V>
	 *            The value type V
	 */
	@SuppressWarnings("hiding")
	class ExpiredObject<K, V> implements Callback<V> {
		private final V value;
		private final ScheduledFuture<?> task;
		private final long ttl;

		public ExpiredObject(K key, V value) {
			this(key, value, defaultLifeTime);
		}

		public ExpiredObject(K key, V value, long ttl) {
			this.value = value;
			this.ttl = ttl;
			this.task = scheduledExecutorService.schedule(() -> {
				// logic
				execute(this.value);
			}, this.ttl, TimeUnit.MILLISECONDS);
		}

		public ScheduledFuture<?> getTask() {
			return task;
		}

		public V getValue() {
			return value;
		}

		public long getTtl() {
			return ttl;
		}

		@Override
		public void execute(V value) {
			expiredEventManager.sendException(value);
		}
	}

	/**
	 * A visitor interface.
	 */
	public interface Callback<V> {
		public void execute(V v);
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
				object.getTask().cancel(false);
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
				obj.getTask().cancel(false);
			}
			map.clear();
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
				object.getTask().cancel(false);
		} finally {
			writeLock.unlock();
		}
		return (object == null ? null : object.getValue());
	}

	public int size() {
		return map.size();
	}
}