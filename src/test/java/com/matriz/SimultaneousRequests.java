package com.matriz;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.test.web.servlet.ResultActions;

/** Sends requests at the same moment, to check simultaneous changes (ADR-006). */
final class SimultaneousRequests {

	private SimultaneousRequests() {
	}

	/** Runs the requests at once, each on its own thread, and returns their statuses in order. */
	@SafeVarargs
	static List<Integer> simultaneously(Callable<Integer>... requests) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(requests.length);
		try {
			CountDownLatch ready = new CountDownLatch(requests.length);
			CountDownLatch go = new CountDownLatch(1);
			List<Future<Integer>> results = new ArrayList<>();
			for (Callable<Integer> request : requests) {
				results.add(pool.submit(() -> {
					ready.countDown();
					go.await();
					return request.call();
				}));
			}
			ready.await();
			go.countDown();
			List<Integer> statuses = new ArrayList<>();
			for (Future<Integer> result : results) {
				statuses.add(result.get());
			}
			return statuses;
		}
		finally {
			pool.shutdownNow();
		}
	}

	static int statusOf(ResultActions actions) {
		return actions.andReturn().getResponse().getStatus();
	}

}
