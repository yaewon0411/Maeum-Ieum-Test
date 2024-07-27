package com.develokit.maeum_ieum.dto.caregiver;

import com.develokit.maeum_ieum.domain.user.Gender;
import com.develokit.maeum_ieum.domain.user.caregiver.Caregiver;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public class ReqDto {
    @Getter
    @NoArgsConstructor
    @Setter
    public static class JoinReqDto{
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,12}$", message = "영문자와 숫자를 포함하여 6자에서 12자 사이여야 합니다")
        private String username; //영문자+숫자 조합 최소 6~12자
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,12}$", message = "영문자와 숫자를 포함하여 6자에서 12자 사이여야 합니다")
        private String password; //영문자+숫자 조합 최소 6~12자
        @Pattern(regexp = "^(010-\\d{4}-\\d{4})|(02-\\d{3,4}-\\d{4})|(0[3-9]\\d-\\d{3,4}-\\d{4})$", message = "유효한 전화번호 형식이 아닙니다.")
        private String contact; // '-'를 포함하는 전화번호
        private Gender gender;
        private LocalDate birthDate;
        private String organization;
        private MultipartFile img;
        @NotNull
        private String name;

        public Caregiver toEntity(BCryptPasswordEncoder bCryptPasswordEncoder, String imgUrl){
            return Caregiver.builder()
                    .username(username)
                    .password(bCryptPasswordEncoder.encode(password))
                    .organization(organization)
                    .birthDate(birthDate)
                    .gender(gender)
                    .contact(contact)
                    .name(name)
                    .imgUrl(imgUrl)
                    .build();
        }

    }
}
