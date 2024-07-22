package com.develokit.maeum_ieum.domain.user.elderly;

import com.develokit.maeum_ieum.domain.assistant.Assistant;
import com.develokit.maeum_ieum.domain.user.User;
import com.develokit.maeum_ieum.domain.user.caregiver.Caregiver;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("ELDERLY")
@NoArgsConstructor
@Getter
public class Elderly extends User {

    private String healthInfo; // 특이 사항
    private String address; //주거지

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caregiver_id")
    private Caregiver caregiver;

    @OneToOne(mappedBy = "elderly")
    private Assistant assistant;
}
