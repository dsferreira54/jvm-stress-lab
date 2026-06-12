package com.example.jvmstresslab;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sun.management.OperatingSystemMXBean;

@Service
public class MetricsService {

	private final StressService stressService;
	private final long startedAt = System.currentTimeMillis();
	private final String hostname = resolveHostname();

	public MetricsService(StressService stressService) {
		this.stressService = stressService;
	}

	public Map<String, Object> snapshot() {
		MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
		ThreadMXBean threads = ManagementFactory.getThreadMXBean();
		OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

		Map<String, Object> metrics = new LinkedHashMap<>();
		metrics.put("timestamp", System.currentTimeMillis());
		metrics.put("hostname", hostname);
		metrics.put("uptimeMs", System.currentTimeMillis() - startedAt);
		metrics.put("cpu", cpuMetrics(os));
		metrics.put("heap", memoryMetrics(memory.getHeapMemoryUsage()));
		metrics.put("nonHeap", memoryMetrics(memory.getNonHeapMemoryUsage()));
		metrics.put("threads", threadMetrics(threads));
		metrics.put("gc", gcMetrics());
		metrics.put("stress", stressService.status());
		return metrics;
	}

	private Map<String, Object> cpuMetrics(OperatingSystemMXBean os) {
		double processLoad = os.getProcessCpuLoad();
		double systemLoad = os.getCpuLoad();
		return Map.of(
				"processLoad", processLoad < 0 ? 0.0 : round(processLoad * 100),
				"systemLoad", systemLoad < 0 ? 0.0 : round(systemLoad * 100),
				"availableProcessors", os.getAvailableProcessors());
	}

	private Map<String, Object> memoryMetrics(MemoryUsage usage) {
		long used = usage.getUsed();
		long max = usage.getMax();
		double percent = max > 0 ? (used * 100.0) / max : 0.0;
		return Map.of(
				"usedBytes", used,
				"maxBytes", max,
				"committedBytes", usage.getCommitted(),
				"usedPercent", round(percent));
	}

	private Map<String, Object> threadMetrics(ThreadMXBean threads) {
		return Map.of(
				"live", threads.getThreadCount(),
				"peak", threads.getPeakThreadCount(),
				"daemon", threads.getDaemonThreadCount(),
				"started", threads.getTotalStartedThreadCount());
	}

	private List<Map<String, Object>> gcMetrics() {
		return ManagementFactory.getGarbageCollectorMXBeans().stream()
				.map(this::gcBeanMetrics)
				.toList();
	}

	private Map<String, Object> gcBeanMetrics(GarbageCollectorMXBean gc) {
		return Map.of(
				"name", gc.getName(),
				"collections", gc.getCollectionCount(),
				"timeMs", gc.getCollectionTime());
	}

	private double round(double value) {
		return Math.round(value * 10.0) / 10.0;
	}

	private String resolveHostname() {
		String env = System.getenv("HOSTNAME");
		if (env != null && !env.isBlank()) {
			return env;
		}
		try {
			return InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException ex) {
			return "unknown";
		}
	}

}
