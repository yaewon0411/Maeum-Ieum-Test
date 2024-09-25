package com.develokit.maeum_ieum.service.report;

import com.develokit.maeum_ieum.config.openAI.ThreadWebClient;
import com.develokit.maeum_ieum.domain.message.Message;
import com.develokit.maeum_ieum.domain.report.Report;
import com.develokit.maeum_ieum.domain.report.ReportRepository;
import com.develokit.maeum_ieum.domain.report.indicator.*;
import com.develokit.maeum_ieum.dto.openAi.run.ReqDto;
import com.develokit.maeum_ieum.ex.CustomApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.develokit.maeum_ieum.dto.openAi.run.ReqDto.*;

@Service
@RequiredArgsConstructor
public class MonthlyReportAnalysisService {

    private final WebClient webClient;
    private final ReportRepository reportRepository;
    private final String openAiAssistantId = "asst_4teudnZ5otWPmJW2mwNKCJ4P";
    private final String threadId = "thread_PP4Qc1s76Sredc5pTGgnclP9";
    private LocalDateTime threadCreatedDate;
    private final Logger log = LoggerFactory.getLogger(MonthlyReportAnalysisService.class);

    private static final Pattern PATTERN = Pattern.compile("\\*\\*(.*?)\\: \\s*(.*?)\\s*\\*\\*\\s*이유: (.*?)\\n(?=\\n|$)");

    private static final Pattern SUMMARY_PATTERN = Pattern.compile("\\*\\*종합 평가\\:\\*\\*\\s*(.*?)\\s*(?=\\n|$)", Pattern.DOTALL);



    @Transactional
    public Mono<Report> generateMonthlyReportAnalysis(Report report, List<Report>reportList) {
        String conversationContent = reportList.stream()
                .map(Report::getQuantitativeAnalysis)
                .collect(Collectors.joining("\n"));

        System.out.println("conversationContent = " + conversationContent);

        CreateRunReqDto createRunReqDto = new CreateRunReqDto(
                openAiAssistantId,
                true
        );

        return createRun(createRunReqDto)
                .flatMap(this::parseAnalysisResult)
                .publishOn(Schedulers.boundedElastic())
                .map(analysisResult -> {
                    // Analysis 결과에서 Summary 추출(지표 상수가 아님. 정성적 평가로 들어갈것)
                    IndicatorResult summaryResult = analysisResult.get("Summary");
                    System.out.println("summaryResult = " + summaryResult);
                    if (summaryResult != null) {
                        report.setQualitativeAnalysis(summaryResult.getReason()); // 종합 평가 저장
                        analysisResult.remove("Summary"); // 종합 평가 Map에서 삭제
                    }
                    updateReportWithAnalysis(report, analysisResult);
                    // 분석 결과 사용해서 보고서 업데이트
                    return reportRepository.save(report);
                })
                .doOnNext(savedReport -> log.info("월간 보고서 분석 완료 및 저장: {}", savedReport.getId()))
                .doOnError(e -> log.error("월간 보고서 분석 과정에서 오류 발생: ", e))
                .onErrorResume(e -> Mono.error(new CustomApiException("보고서 분석 과정에서 오류 발생", HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR)));
    }
    private Mono<String> createRun(CreateRunReqDto createRunReqDto) {
        return webClient.post()
                .uri("/threads/{threadId}/runs", threadId)
                .bodyValue(createRunReqDto)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .doOnSubscribe(subscription -> log.info("OPENAI에 월간 보고서 분석 요청 전송"))
                .doOnError(WebClientResponseException.class, e -> {
                    log.error("OPENAI 요청 중 오류 발생: {}", e.getMessage());
                    throw new CustomApiException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR);
                })
                .filter(event -> "thread.message.completed".equals(event.event()))
                .next()
                .flatMap(event -> {
                    String data = event.data();
                    try {
                        ObjectMapper om = new ObjectMapper();
                        JsonNode rootNode = om.readTree(data);
                        JsonNode contentArray = rootNode.path("content");
                        if (!contentArray.isEmpty()) {
                            JsonNode textNode = contentArray.get(0).path("text");
                            return Mono.just(textNode.path("value").asText());
                        }
                        log.warn("분석 결과에서 content를 찾을 수 없습니다");
                        return Mono.error(new CustomApiException("분석 결과 형식 오류", HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR));
                    } catch (JsonProcessingException e) {
                        log.error("JSON 파싱 오류: {}", e.getMessage());
                        return Mono.error(new CustomApiException("분석 결과 파싱 오류", HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR));
                    }
                })
                .timeout(Duration.ofSeconds(60))
                .doOnNext(answer -> log.debug("분석 완료: {}", answer));
    }

    //분석 결과 파싱
    private Mono<Map<String, IndicatorResult>> parseAnalysisResult(String analysisResult) {
        return Mono.fromCallable(() -> {
            Map<String, IndicatorResult> resultMap = new HashMap<>();

            // 각 섹션을 "**이유:**"를 기준으로 분리
            String[] sections = analysisResult.split("\\n\\n");

            Matcher matcher = PATTERN.matcher(analysisResult);

            // 각 지표와 이유 추출
            while (matcher.find()) {
                String indicator = matcher.group(1).trim();
                String value = matcher.group(2).trim();
                String reason = matcher.group(3).trim();
                // 지표명과 값이 제대로 분리된 상태로 저장
                resultMap.put(indicator, new IndicatorResult(value, reason));
            }

            // 종합 평가 추출
            Matcher summaryMatcher = SUMMARY_PATTERN.matcher(analysisResult);
            if (summaryMatcher.find()) {
                String summary = summaryMatcher.group(1).trim();
                resultMap.put("Summary", new IndicatorResult("종합 평가", summary));
            }

            return resultMap;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private void updateReportWithAnalysis(Report report, Map<String, IndicatorResult> analysisResult) {
        updateIndicator(report, ActivityLevelIndicator.class, analysisResult.get("ActivityLevelIndicator"));
        updateIndicator(report, HealthStatusIndicator.class, analysisResult.get("HealthStatusIndicator"));
        updateIndicator(report, CognitiveFunctionIndicator.class, analysisResult.get("CognitiveFunctionIndicator"));
        updateIndicator(report, LifeSatisfactionIndicator.class, analysisResult.get("LifeSatisfactionIndicator"));
        updateIndicator(report, PsychologicalStabilityIndicator.class, analysisResult.get("PsychologicalStabilityIndicator"));
        updateIndicator(report, SocialConnectivityIndicator.class, analysisResult.get("SocialConnectivityIndicator"));
        updateIndicator(report, SupportNeedsIndicator.class, analysisResult.get("SupportNeedsIndicator"));

        // 정성적 분석을 위한 문자열 생성
//        String qualitativeAnalysis = analysisResult.entrySet().stream()
//                .map(entry -> entry.getKey() + ": " + entry.getValue().getReason())
//                .collect(Collectors.joining("\n"));
//        report.setQualitativeAnalysis(qualitativeAnalysis);
    }

    private <T extends Enum<T> & ReportIndicator> void updateIndicator(Report report, Class<T> indicatorClass, IndicatorResult result) {
        if (result != null) {
            try {
                T indicator = Enum.valueOf(indicatorClass, result.getValue());
                report.setQuantitativeAnalysis(indicator, result.getReason());
            } catch (IllegalArgumentException e) {
                log.error("지표에 정의되지 않은 value가 반환 {}: {}", indicatorClass.getSimpleName(), result.getValue());
            } catch (JsonProcessingException e) {
                log.error("Json 파싱 중 오류 발생 {}: {}", indicatorClass.getSimpleName(), e.getMessage());
            } catch (IllegalStateException e) {
                log.error("존재하지 않는 지표가 반환 {}: {}", indicatorClass.getSimpleName(), e.getMessage());
            }
        } else {
            log.warn("분석 결과에 해당 지표가 빠졌습니다 {}", indicatorClass.getSimpleName());
        }
    }

    // 지표 상수 value와 이유
    public static class IndicatorResult {
        private final String value;
        private final String reason;

        public IndicatorResult(String value, String reason) {
            this.value = value;
            this.reason = reason;
        }

        public String getValue() {
            return value;
        }

        public String getReason() {
            return reason;
        }
    }

}
