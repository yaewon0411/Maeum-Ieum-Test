package com.develokit.maeum_ieum.domain.user.caregiver;

import com.develokit.maeum_ieum.domain.assistant.Assistant;
import com.develokit.maeum_ieum.domain.user.elderly.Elderly;
import com.develokit.maeum_ieum.domain.user.User;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@DiscriminatorValue("CAREGIVER")
@Getter
public class Caregiver extends User {

    @OneToMany(mappedBy = "caregiver")
    private List<Assistant> assistantList = new ArrayList<>();

    @OneToMany(mappedBy = "caregiver")
    private List<Elderly> elderlyList = new ArrayList<>();

}
