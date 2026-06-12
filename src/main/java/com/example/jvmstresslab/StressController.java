package com.example.jvmstresslab;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StressController {

	private final StressService stressService;
	private final MetricsService metricsService;
	private final HostCapacityService hostCapacityService;
	private final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();

	public StressController(
			StressService stressService,
			MetricsService metricsService,
			HostCapacityService hostCapacityService) {
		this.stressService = stressService;
		this.metricsService = metricsService;
		this.hostCapacityService = hostCapacityService;
	}

	@GetMapping("/api/metrics")
	public Map<String, Object> metrics() {
		return metricsService.snapshot();
	}

	@GetMapping("/api/stress/status")
	public Map<String, Object> status() {
		return stressService.status();
	}

	@PostMapping("/api/stress/cpu/start")
	public Map<String, Object> startCpu(@RequestParam(required = false) Integer threads) {
		int count = threads != null ? threads : hostCapacityService.cpuDefault();
		stressService.startCpu(count);
		return metricsService.snapshot();
	}

	@PostMapping("/api/stress/cpu/stop")
	public Map<String, Object> stopCpu() {
		stressService.stopCpu();
		return metricsService.snapshot();
	}

	@PostMapping("/api/stress/heap/start")
	public Map<String, Object> startHeap(@RequestParam(required = false) Integer targetMb) {
		int mb = targetMb != null ? targetMb : hostCapacityService.heapDefault(memory.getHeapMemoryUsage());
		stressService.startHeap(mb);
		return metricsService.snapshot();
	}

	@PostMapping("/api/stress/heap/stop")
	public Map<String, Object> stopHeap() {
		stressService.stopHeap();
		return metricsService.snapshot();
	}

	@PostMapping("/api/stress/threads/start")
	public Map<String, Object> startThreads(@RequestParam(required = false) Integer count) {
		int threads = count != null ? count : hostCapacityService.threadDefault();
		stressService.startThreads(threads);
		return metricsService.snapshot();
	}

	@PostMapping("/api/stress/threads/stop")
	public Map<String, Object> stopThreads() {
		stressService.stopThreads();
		return metricsService.snapshot();
	}

	@PostMapping("/api/stress/stop-all")
	public Map<String, Object> stopAll() {
		stressService.stopAll();
		return metricsService.snapshot();
	}

}
