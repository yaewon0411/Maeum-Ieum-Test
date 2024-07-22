package com.develokit.maeum_ieum.domain.user;

import com.develokit.maeum_ieum.domain.Gender;
import com.develokit.maeum_ieum.domain.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Generated;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED) //조인 전략으로 사용
@DiscriminatorColumn(name = "user_type")
@Getter
public abstract class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;//이름
    private String contact; //연락처
    @Enumerated(value = EnumType.STRING)
    private Gender gender; //성별

    private String imgUrl; //이미지
    private LocalDate birthDate; //생년 월일
    private String organization; //소속 기관

    @Builder
    public User(String name, String contact, Gender gender, String imgUrl, LocalDate birthDate, String organization) {
        this.name = name;
        this.contact = contact;
        this.gender = gender;
        this.imgUrl = imgUrl;
        this.birthDate = birthDate;
        this.organization = organization;
    }
}
