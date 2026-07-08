package com.example.trekkingapp.trekkerprofile;

import com.example.trekkingapp.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class TrekkerProfileService {

    private static final Set<String> SUPPORTED_EXPERIENCES = Set.of("NEWBIE", "OCCASIONAL", "FREQUENT");

    private final TrekkerProfileRepository trekkerProfileRepository;

    public TrekkerProfileService(TrekkerProfileRepository trekkerProfileRepository) {
        this.trekkerProfileRepository = trekkerProfileRepository;
    }

    @Transactional
    public TrekkerProfile createProfile(User user, String trekkingExperience, String citizenIdImageUrl) {
        String normalizedExperience = normalizeExperience(trekkingExperience);

        TrekkerProfile profile = new TrekkerProfile();
        profile.setUser(user);
        profile.setTrekkingExperience(normalizedExperience);
        profile.setCitizenIdImageUrl(citizenIdImageUrl);
        return trekkerProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public TrekkerProfileResponse toResponse(TrekkerProfile profile) {
        return new TrekkerProfileResponse(
                profile.getProfileId(),
                profile.getUser().getUserId(),
                profile.getTrekkingExperience(),
                profile.getCitizenIdImageUrl(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    public String normalizeExperience(String trekkingExperience) {
        String normalizedExperience = trekkingExperience == null ? "" : trekkingExperience.trim().toUpperCase();
        if (!SUPPORTED_EXPERIENCES.contains(normalizedExperience)) {
            throw new IllegalArgumentException("Unsupported trekking experience");
        }
        return normalizedExperience;
    }
}
