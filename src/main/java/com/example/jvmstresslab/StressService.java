package com.example.jvmstresslab;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

@Service
public class StressService {

	private final HostCapacityService hostCapacityService;
	private final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();

	private final AtomicBoolean cpuActive = new AtomicBoolean(false);
	private final List<Thread> cpuWorkers = new ArrayList<>();

	private final AtomicBoolean heapActive = new AtomicBoolean(false);
	private final List<byte[]> heapChunks = new ArrayList<>();
	private volatile int heapTargetMb = 0;
	private Thread heapAllocator;

	private final AtomicBoolean threadStressActive = new AtomicBoolean(false);
	private final List<Thread> blockingThreads = new ArrayList<>();

	public StressService(HostCapacityService hostCapacityService) {
		this.hostCapacityService = hostCapacityService;
	}

	public synchronized void startCpu(int threads) {
		stopCpu();
		cpuActive.set(true);

		int count = hostCapacityService.clampCpuThreads(threads);
		for (int i = 0; i < count; i++) {
			Thread worker = new Thread(this::burnCpu, "cpu-stress-" + i);
			worker.setDaemon(true);
			worker.start();
			cpuWorkers.add(worker);
		}
	}

	public synchronized void stopCpu() {
		cpuActive.set(false);
		cpuWorkers.clear();
	}

	public synchronized void startHeap(int targetMb) {
		stopHeap();
		heapActive.set(true);
		heapTargetMb = hostCapacityService.clampHeapMb(targetMb, memory.getHeapMemoryUsage());

		heapAllocator = new Thread(this::allocateHeap, "heap-stress");
		heapAllocator.setDaemon(true);
		heapAllocator.start();
	}

	public synchronized void stopHeap() {
		heapActive.set(false);
		heapChunks.clear();
		heapTargetMb = 0;
		heapAllocator = null;
	}

	public synchronized void startThreads(int count) {
		stopThreads();
		threadStressActive.set(true);

		int threadCount = hostCapacityService.clampThreadCount(count);
		for (int i = 0; i < threadCount; i++) {
			Thread worker = new Thread(this::blockThread, "thread-stress-" + i);
			worker.setDaemon(true);
			worker.start();
			blockingThreads.add(worker);
		}
	}

	public synchronized void stopThreads() {
		threadStressActive.set(false);
		blockingThreads.clear();
	}

	public synchronized void stopAll() {
		stopCpu();
		stopHeap();
		stopThreads();
	}

	public Map<String, Object> status() {
		Map<String, Object> status = new LinkedHashMap<>();
		status.put("cpu", Map.of(
				"active", cpuActive.get(),
				"threads", cpuWorkers.size()));
		status.put("heap", Map.of(
				"active", heapActive.get(),
				"allocatedMb", heapChunks.size(),
				"targetMb", heapTargetMb));
		status.put("threads", Map.of(
				"active", threadStressActive.get(),
				"count", blockingThreads.size()));
		status.put("anyActive", cpuActive.get() || heapActive.get() || threadStressActive.get());
		return status;
	}

	private void burnCpu() {
		while (cpuActive.get()) {
			double x = 0;
			for (int i = 0; i < 500_000; i++) {
				x += Math.sqrt(i * 1.0);
			}
			if (x < 0) {
				Thread.yield();
			}
		}
	}

	private void allocateHeap() {
		int chunkSize = 1024 * 1024;
		while (heapActive.get() && heapChunks.size() < heapTargetMb) {
			try {
				heapChunks.add(new byte[chunkSize]);
				Thread.sleep(30);
			}
			catch (OutOfMemoryError ex) {
				break;
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	private void blockThread() {
		while (threadStressActive.get()) {
			try {
				Thread.sleep(1_000);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

}
