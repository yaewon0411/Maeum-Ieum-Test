package com.develokit.maeum_ieum.domain.user.elderly;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Embeddable
@NoArgsConstructor
@Getter
public class EmergencyContactInfo {

    private String name;
    private String contact;
    private String relationship;

}
