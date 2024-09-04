package com.develokit.maeum_ieum.service;

import com.develokit.maeum_ieum.domain.assistant.Assistant;
import com.develokit.maeum_ieum.domain.assistant.AssistantRepository;
import com.develokit.maeum_ieum.domain.user.Gender;
import com.develokit.maeum_ieum.domain.user.caregiver.CareGiverRepository;
import com.develokit.maeum_ieum.domain.user.caregiver.Caregiver;
import com.develokit.maeum_ieum.domain.user.elderly.Elderly;
import com.develokit.maeum_ieum.domain.user.elderly.ElderlyRepository;
import com.develokit.maeum_ieum.dto.assistant.ReqDto;
import com.develokit.maeum_ieum.dto.caregiver.RespDto;
import com.develokit.maeum_ieum.dto.caregiver.RespDto.JoinRespDto;
import com.develokit.maeum_ieum.dto.openAi.assistant.RespDto.CreateAssistantRespDto;
import com.develokit.maeum_ieum.ex.CustomApiException;
import com.develokit.maeum_ieum.util.CustomAccessCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import lombok.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import static com.develokit.maeum_ieum.dto.caregiver.ReqDto.*;
import static com.develokit.maeum_ieum.dto.caregiver.RespDto.*;
import static com.develokit.maeum_ieum.dto.openAi.assistant.ReqDto.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaregiverService {

    private final CareGiverRepository careGiverRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final S3Service s3Service;
    private final OpenAiService openAiService;
    private final ElderlyRepository elderlyRepository;
    private final AssistantRepository assistantRepository;
    private final CustomAccessCodeGenerator accessCodeGenerator;

    private final Logger log = LoggerFactory.getLogger(CaregiverService.class);

    @Transactional
    public JoinRespDto join(JoinReqDto joinReqDto){ //회원가입

        //아이디 중복 검사
        Optional<Caregiver> caregiverOP = careGiverRepository.findByUsername(joinReqDto.getUsername());
        if(caregiverOP.isPresent()) throw new CustomApiException("이미 존재하는 아이디 입니다", HttpStatus.CONFLICT.value(),HttpStatus.CONFLICT);

        //이미지 저장
        String imgUrl = null;
        if(joinReqDto.getImg() != null)
            imgUrl = s3Service.uploadImage(joinReqDto.getImg());

        //비밀번호 인코딩
        Caregiver caregiverPS = careGiverRepository.save(joinReqDto.toEntity(bCryptPasswordEncoder, imgUrl));

        //DTO 반환
        return new JoinRespDto(caregiverPS);
    }

    @Transactional //노인 사용자의 AI Assistant 생성
    public CreateAssistantRespDto attachAssistantToElderly(ReqDto.CreateAssistantReqDto createAssistantReqDto, Long elderlyId, Caregiver caregiver){

        //존재하는 사용자인지 검사
        Elderly elderlyPS = elderlyRepository.findById(elderlyId)
                .orElseThrow(() -> new CustomApiException("존재하지 않는 노인 사용자 입니다", HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND));


        //해당 사용자가 이미 어시스턴트가 붙어 있는지 검사
        if(elderlyPS.hasAssistant()) throw new CustomApiException("이미 AI Assistant가 생성되어 있습니다", HttpStatus.CONFLICT.value(),HttpStatus.CONFLICT);


        //필수 속성과 선택 속성(대화 주제, 금기어, 응답 형식, 성격) 설정하기
        String instructions = "";

        //필수 속성
        instructions += createAssistantReqDto.getDescription()+"\n";


        //선택 속성
        if(createAssistantReqDto.getConversationTopic() != null){ //대화 주제
            instructions += "당신은 나와 "+createAssistantReqDto.getConversationTopic()+"을 주제로 얘기합니다. ";
        }
        if(createAssistantReqDto.getForbiddenTopic() != null){ //금기어
            instructions += "당신은 '"+createAssistantReqDto.getForbiddenTopic()+"'에 관해서는 절대 언급해서는 안됩니다!!! ";
        }
        if(createAssistantReqDto.getResponseType() != null){ //응답 형식
            instructions += "나와 대화할 때 '"+createAssistantReqDto.getResponseType()+"'의 응답 형식을 지켜서 대화해주세요. ";
        }
        if(createAssistantReqDto.getPersonality() != null){ //성격
            instructions += "당신의 성격은 '"+createAssistantReqDto.getPersonality()+"' 합니다!";
        }


        //어시스턴트 생성 + instructions 설정
        String assistantId = openAiService.createAssistant(new OpenAiCreateAssistantReqDto(
                createAssistantReqDto.getDescription(),
                instructions,
                createAssistantReqDto.getName()
        ));

        //어시스턴트 저장
        Assistant assistantPS = assistantRepository.save(
                Assistant.builder()
                        .name(createAssistantReqDto.getName())
                        .openAiAssistantId(assistantId)
                        .caregiver(caregiver)
                        .elderly(elderlyPS)
                        .rule(createAssistantReqDto.getDescription())
                        .conversationTopic(createAssistantReqDto.getConversationTopic())
                        .responseType(createAssistantReqDto.getResponseType())
                        .personality(createAssistantReqDto.getPersonality())
                        .forbiddenTopic(createAssistantReqDto.getForbiddenTopic())
                        .accessCode(accessCodeGenerator.generateEncodedAccessCode())
                        .build()
        );

        //노인에 어시스턴트 주입
        elderlyPS.attachAssistant(assistantPS);

        return new CreateAssistantRespDto(assistantPS, instructions);
    }

    public MyInfoRespDto caregiverInfo(String username){//내 정보(이름, 이미지, 성별, 생년월일, 주거지, 소속기관, 연락처)
        Caregiver caregiverPS = careGiverRepository.findByUsername(username).orElseThrow(
                () -> new CustomApiException("등록되지 않은 요양사 사용자입니다", HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND)
        );
        return new MyInfoRespDto(caregiverPS);
    }

    //홈 화면 (요양사 이름, 이미지, 총 관리 인원, 기관, 노인 사용자(이름, 나이, 주소, 연락처,  마지막 대화(n시간 전)), AI 생성 여부
    public CaregiverMainRespDto getCaregiverMainInfo(String username){

        Caregiver caregiverPS = careGiverRepository.findByUsername(username).orElseThrow(
                () -> new CustomApiException("등록되지 않은 요양사 사용자입니다", HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND)
        );

        return new CaregiverMainRespDto(caregiverPS);
    }

    //마이페이지 -> 수정 (이미지 제외)
    @Transactional
    public CaregiverModifyRespDto modifyCaregiverInfo(String username, CaregiverModifyReqDto caregiverModifyReqDto){

        //요양사 가져오기
        Caregiver caregiverPS = careGiverRepository.findByUsername(username).orElseThrow(
                () -> new CustomApiException("등록되지 않은 요양사 사용자입니다", HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND)
        );

        caregiverPS.updateCaregiverInfo(caregiverModifyReqDto);
        return new CaregiverModifyRespDto(caregiverPS);
    }

    @Transactional
    public CaregiverImgModifyRespDto modifyCaregiverImg(String username, CaregiverImgModifyReqDto caregiverImgModifyReqDto){
        //요양사 가져오기
        Caregiver caregiverPS = careGiverRepository.findByUsername(username).orElseThrow(
                () -> new CustomApiException("등록되지 않은 요양사 사용자입니다", HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND)
        );

        if (caregiverImgModifyReqDto == null || caregiverImgModifyReqDto.getImg() == null) {
            return new CaregiverImgModifyRespDto(caregiverPS.getImgUrl());
        }

        String newImgUrl = null;
        String oldImgUrl = caregiverPS.getImgUrl();

        try {
            // 새 이미지 업로드
            newImgUrl = s3Service.uploadImage(caregiverImgModifyReqDto.getImg());

            caregiverPS.updateImg(newImgUrl);

            // 기존 이미지 삭제
            if (oldImgUrl != null && !oldImgUrl.isEmpty()) {
                s3Service.deleteImage(oldImgUrl);
            }
            return new CaregiverImgModifyRespDto(newImgUrl);
        } catch (Exception e) {
            // 업로드 실패 시 새로 업로드된 이미지 삭제 (있다면)
            if (newImgUrl != null) {
                try {
                    s3Service.deleteImage(newImgUrl);
                } catch (Exception ignored) {
                    log.error("S3에 올라간 업데이트된 이미지 삭제 작업 실패");
                }
            }
            log.error("이미지 업데이트 중 오류 발생");
            throw new CustomApiException("이미지 업데이트 중 오류 발생", HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }








}
