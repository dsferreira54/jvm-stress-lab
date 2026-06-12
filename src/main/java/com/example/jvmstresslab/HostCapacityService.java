package com.example.jvmstresslab;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sun.management.OperatingSystemMXBean;

@Service
public class HostCapacityService {

	private final OperatingSystemMXBean os;
	private final int availableProcessors;
	private final long totalPhysicalMemoryBytes;

	public HostCapacityService() {
		this.os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		this.availableProcessors = os.getAvailableProcessors();
		this.totalPhysicalMemoryBytes = os.getTotalMemorySize();
	}

	public Map<String, Object> snapshot(MemoryUsage heapUsage) {
		long heapMaxBytes = heapUsage.getMax();
		long heapUsedBytes = heapUsage.getUsed();
		int heapMbMax = toMb(heapMaxBytes);
		if (heapMbMax <= 0) {
			heapMbMax = 512;
		}

		int heapAllocatableMb = Math.max(1, toMb(heapMaxBytes - heapUsedBytes));
		int heapMbDefault = Math.min(heapMbMax, heapAllocatableMb);
		int heapStep = Math.max(16, heapMbMax / 32);

		int threadMax = threadCountMax();
		int threadStep = Math.max(1, threadMax / 30);

		Map<String, Object> capacity = new LinkedHashMap<>();
		capacity.put("availableProcessors", availableProcessors);
		capacity.put("totalMemoryBytes", totalPhysicalMemoryBytes);
		capacity.put("freeMemoryBytes", os.getFreeMemorySize());
		capacity.put("cpu", Map.of(
				"min", 1,
				"max", availableProcessors,
				"default", availableProcessors,
				"step", 1));
		capacity.put("heap", Map.of(
				"min", heapStep,
				"max", heapMbMax,
				"default", heapMbDefault,
				"step", heapStep));
		capacity.put("threads", Map.of(
				"min", threadStep,
				"max", threadMax,
				"default", threadMax,
				"step", threadStep));
		return capacity;
	}

	public int clampCpuThreads(int threads) {
		return Math.clamp(threads, 1, availableProcessors);
	}

	public int clampHeapMb(int targetMb, MemoryUsage heapUsage) {
		int maxMb = toMb(heapUsage.getMax());
		if (maxMb <= 0) {
			maxMb = 512;
		}
		return Math.clamp(targetMb, 1, maxMb);
	}

	public int clampThreadCount(int count) {
		return Math.clamp(count, 1, threadCountMax());
	}

	public int cpuDefault() {
		return availableProcessors;
	}

	public int heapDefault(MemoryUsage heapUsage) {
		long heapMaxBytes = heapUsage.getMax();
		int heapMbMax = toMb(heapMaxBytes);
		if (heapMbMax <= 0) {
			heapMbMax = 512;
		}
		int allocatable = Math.max(1, toMb(heapMaxBytes - heapUsage.getUsed()));
		return Math.min(heapMbMax, allocatable);
	}

	public int threadDefault() {
		return threadCountMax();
	}

	private int threadCountMax() {
		int byCpu = availableProcessors * 32;
		int byMemory = (int) Math.min(10_000, totalPhysicalMemoryBytes / (1024 * 1024));
		return Math.max(100, Math.min(byCpu, byMemory));
	}

	private int toMb(long bytes) {
		return (int) (bytes / (1024 * 1024));
	}

}
