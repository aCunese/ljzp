package com.acunese.ljzp.service;

import com.acunese.ljzp.entity.TaskKnowledgeRelation;

import java.util.List;

public interface KnowledgeGraphService {

    /**
     * Build or refresh the knowledge relation entry for the specified task.
     */
    void buildRelation(Long taskId);

    /**
     * Query related entities for a disease.
     */
    List<TaskKnowledgeRelation> queryRelatedEntities(Long diseaseId);
}

