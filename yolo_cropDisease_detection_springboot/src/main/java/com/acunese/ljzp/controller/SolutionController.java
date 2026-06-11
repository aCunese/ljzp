package com.acunese.ljzp.controller;

import com.acunese.ljzp.common.Result;
import com.acunese.ljzp.dto.GenerateSolutionRequest;
import com.acunese.ljzp.dto.SolutionOption;
import com.acunese.ljzp.dto.SolutionRecommendation;
import com.acunese.ljzp.dto.WeatherData;
import com.acunese.ljzp.service.SolutionService;
import com.acunese.ljzp.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint for generating intelligent plant protection solutions.
 */
@RestController
@RequestMapping("/solution")
@RequiredArgsConstructor
public class SolutionController {

    private final SolutionService solutionService;
    private final WeatherService weatherService;

    @GetMapping("/catalog")
    public Result<List<SolutionOption>> catalog() {
        return Result.success(solutionService.listCatalog());
    }

    @PostMapping("/generate")
    public Result<SolutionRecommendation> generate(@RequestBody GenerateSolutionRequest request) {
        WeatherData weatherData = request.getOverrideWeather();
        if (weatherData == null && request.getLatitude() != null && request.getLongitude() != null) {
            weatherData = weatherService.getWeatherSnapshot(request.getLatitude(), request.getLongitude());
        } else if (weatherData == null) {
            weatherData = weatherService.getDefaultWeatherSnapshot();
        }

        SolutionRecommendation recommendation = solutionService.generateSolution(
                request.getDiseaseId(),
                request.getCropId(),
                weatherData
        );
        return Result.success(recommendation);
    }
}
