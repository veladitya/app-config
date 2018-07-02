package nitro.plc.map;

import java.util.Timer;
import java.util.TimerTask;

import nitro.plc.manager.EventExpiredManager;

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
	private final ExpiryTask<K,V> task;
	private final long ttl;	
	private  static Long defaultLifeTime = 3000L;
	private EventExpiredManager expiredEventManager;
	private final Timer timer = new Timer("TimedMap Timer", true);
	public ExpiredObject(K key, V value) {
		this(key, value, defaultLifeTime);
	}

	public ExpiredObject(K key, V value, long ttl) {
		this.value = value;
		this.task = new ExpiryTask<K, V>(key);
		this.ttl = ttl;
		timer.schedule(this.task, ttl);
	}

	public ExpiryTask<K, V> getTask() {
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