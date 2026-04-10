package iuh.fit.goat.service;

public interface ProfileRealtimeService {
    void emitUserProfileUpdated(String userPrincipal, String profileType, Object profileData);
}
