package meteordevelopment.meteorclient.utils.network;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class MeteorExecutor {
	public static ExecutorService executor;

	private MeteorExecutor() {
	}

	public static synchronized void init() {
		if (executor != null && !executor.isShutdown()) {
			return;
		}

		AtomicInteger threadNumber = new AtomicInteger(1);
		executor = Executors.newCachedThreadPool(task -> {
			Thread thread = new Thread(task);
			thread.setDaemon(true);
			thread.setName("Kuzays-Secrets-Executor-" + threadNumber.getAndIncrement());
			return thread;
		});
	}

	public static void execute(Runnable task) {
		if (executor == null || executor.isShutdown()) {
			init();
		}

		executor.execute(task);
	}
}
