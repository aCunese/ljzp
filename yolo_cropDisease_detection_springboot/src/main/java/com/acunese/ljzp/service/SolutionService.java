package com.acunese.ljzp.service;

import com.acunese.ljzp.dto.SolutionOption;
import com.acunese.ljzp.dto.SolutionRecommendation;
import com.acunese.ljzp.dto.WeatherData;
import com.acunese.ljzp.dto.TaskCreationDTO;

import java.util.List;

public interface SolutionService {

    /**
     * Generate an agronomic solution for the given disease/crop combination.
     *
     * @param diseaseId  detected disease identifier
     * @param cropId     optional crop identifier (may be null)
     * @param weatherData contextual weather data, may be null for cached defaults
     * @return generated recommendation payload
     */
    SolutionRecommendation generateSolution(Long diseaseId, Long cropId, WeatherData weatherData);

    /**
     * Enumerate available knowledge base plans for UI selection.
     */
    List<SolutionOption> listCatalog();

    /**
     * Apply a solution and create a corresponding task.
     *
     * @return created task id
     */
    Long applySolution(Long solutionId, Long recordId, TaskCreationDTO payload);
}
