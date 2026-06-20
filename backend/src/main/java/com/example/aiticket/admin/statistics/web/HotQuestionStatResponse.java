package com.example.aiticket.admin.statistics.web;

import com.example.aiticket.admin.statistics.domain.HotQuestionStat;

public record HotQuestionStatResponse(
        String question,
        long askCount
) {
    public static HotQuestionStatResponse from(HotQuestionStat stat) {
        return new HotQuestionStatResponse(stat.question(), stat.askCount());
    }
}
