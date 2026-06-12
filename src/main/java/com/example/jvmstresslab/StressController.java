package com.example.jvmstresslab;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StressController {

	private final StressService stressService;
	private final MetricsService metricsService;

	public StressController(StressService stressService, MetricsService metricsService) {
		this.stressService = stressService;
		this.metricsService = metricsService;
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
	public Map<String, Object> startCpu(@RequestParam(defaultValue = "4") int threads) {
		stressService.startCpu(threads);
		return metricsService.snapshot();
	}

	@PostMapping("/api/stress/cpu/stop")
	public Map<String, Object> stopCpu() {
		stressService.stopCpu();
		return metricsService.snapshot();
	}

	@PostMapping("/api/stress/heap/start")
	public Map<String, Object> startHeap(@RequestParam(defaultValue = "128") int targetMb) {
		stressService.startHeap(targetMb);
		return metricsService.snapshot();
	}

	@PostMapping("/api/stress/heap/stop")
	public Map<String, Object> stopHeap() {
		stressService.stopHeap();
		return metricsService.snapshot();
	}

	@PostMapping("/api/stress/threads/start")
	public Map<String, Object> startThreads(@RequestParam(defaultValue = "50") int count) {
		stressService.startThreads(count);
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
