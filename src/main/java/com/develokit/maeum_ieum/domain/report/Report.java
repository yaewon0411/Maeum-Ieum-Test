package com.develokit.maeum_ieum.domain.report;

import com.develokit.maeum_ieum.domain.base.BaseEntity;
import com.develokit.maeum_ieum.domain.report.indicator.*;
import com.develokit.maeum_ieum.domain.user.elderly.Elderly;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
public class Report extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elderly_id", nullable = false)
    private Elderly elderly;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType; //월간 보고서/주간 보고서

    private LocalDateTime startDate; // 보고서 기록 시작 일
    private LocalDateTime endDate;// 보고서 기록 종료 일

    @Column(length = 2048)
    private String quantitativeAnalysis; //정량적 분석 결과

    @Column(length = 2048)
    private String qualitativeAnalysis; //정성적 분석
    @Column(length = 512)
    private String memo; //메모

    @Enumerated(EnumType.STRING)
    private ReportStatus reportStatus;

    @Enumerated(EnumType.STRING)
    private DayOfWeek reportDay;


    //================================보고서 지표================================

    @Enumerated(EnumType.STRING)
    private HealthStatusIndicator healthStatusIndicator; //건강 상태 지표

    @Enumerated(EnumType.STRING)
    private ActivityLevelIndicator activityLevelIndicator; //활동 수준 지표

    @Enumerated(EnumType.STRING)
    private CognitiveFunctionIndicator cognitiveFunctionIndicator; //인지 기능 지표

    @Enumerated(EnumType.STRING)
    private LifeSatisfactionIndicator lifeSatisfactionIndicator;//생활 만족도 지표

    @Enumerated(EnumType.STRING)
    private PsychologicalStabilityIndicator psychologicalStabilityIndicator; //심리적 안정 지표

    @Enumerated(EnumType.STRING)
    private SocialConnectivityIndicator socialConnectivityIndicator; //사회적 연결성 지표

    @Enumerated(EnumType.STRING)
    private SupportNeedsIndicator supportNeedsIndicator; //필요 지원 지표

    @Builder
    public Report(Elderly elderly, ReportType reportType, LocalDateTime startDate, LocalDateTime endDate, String quantitativeAnalysis, String qualitativeAnalysis,  String memo, ReportStatus reportStatus, DayOfWeek reportDay) {
        this.elderly = elderly;
        this.reportType = reportType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.quantitativeAnalysis = quantitativeAnalysis;
        this.qualitativeAnalysis = qualitativeAnalysis;
        this.memo = memo;
        this.reportStatus = reportStatus;
        this.reportDay = reportDay;
    }
    public void modifyStartDateAndReportDay(LocalDateTime localDateTime, DayOfWeek dayOfWeek){
        this.startDate = localDateTime;
        this.reportDay = dayOfWeek;
    }
    public void updateReportDay(DayOfWeek reportDay){this.reportDay = reportDay;}

    public void updateReportStatus(ReportStatus reportStatus){
        this.reportStatus = reportStatus;
    }

    public void updateEndDate(LocalDateTime localDateTime){
        this.endDate = localDateTime;
    }

    public void setQuantitativeAnalysis(String quantitativeAnalysis) {
        this.quantitativeAnalysis = quantitativeAnalysis;
    }

    public void setQualitativeAnalysis(String qualitativeAnalysis) {
        this.qualitativeAnalysis = qualitativeAnalysis;
    }

    public void setMemo(String memo){
        this.memo = memo;
    }

    public Report(Elderly elderly, LocalDateTime startDate, LocalDateTime endDate, ReportType reportType){
        this.elderly = elderly;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reportType = reportType;
    }

}
