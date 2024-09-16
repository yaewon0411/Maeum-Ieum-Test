package com.develokit.maeum_ieum.service.report;

import com.develokit.maeum_ieum.domain.report.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import static com.develokit.maeum_ieum.service.report.ReportProcessor.*;


@Component
@RequiredArgsConstructor
public class ReportWriter implements ItemWriter<ReportProcessResult> {

    private final ReportRepository reportRepository;

    @Override
    public void write(Chunk<? extends ReportProcessResult> chunk) throws Exception {
        for (ReportProcessResult result : chunk.getItems()) {
            reportRepository.save(result.getCompletedReport());
        }
    }
}
