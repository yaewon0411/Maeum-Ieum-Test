package com.develokit.maeum_ieum.dto.elderly;

import com.develokit.maeum_ieum.domain.user.Gender;
import com.develokit.maeum_ieum.domain.user.caregiver.Caregiver;
import com.develokit.maeum_ieum.domain.user.elderly.Elderly;
import com.develokit.maeum_ieum.domain.user.elderly.EmergencyContactInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public class ReqDto {

    /*
        - 이름
        - 성별
        - 생년월일
        -주거지
        -긴급 연락처(이름, 전화번호, 노인과의 관계)
        - 노인 연락처
        - 노인 특이사항
     */

    @Getter
    @NoArgsConstructor
    public static class ElderlyCreateReqDto{
        private String name; //이름
        private Gender gender; //성별
        private LocalDate birthDate; //생년월일
        private String homeAddress; //주거지
        private EmergencyContactInfo emergencyContactInfo; //긴급 연락처
        private String contact; //연락처
        private String healthInfo; //특이 사항
        private MultipartFile imgFile;

        public Elderly toEntity(Caregiver caregiver, String imgUrl){
            return Elderly.builder()
                    .name(name)
                    .homeAddress(homeAddress)
                    .emergencyContactInfo(emergencyContactInfo)
                    .caregiver(caregiver)
                    .birthDate(birthDate)
                    .healthInfo(healthInfo)
                    .imgUrl(imgUrl)
                    .build();
        }
    }
}
