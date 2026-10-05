package com.maharecruitment.gov.in.web.service.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import com.maharecruitment.gov.in.web.service.security.LoginCaptchaService.LoginCaptchaChallenge;

class LoginCaptchaServiceTest {

    private final LoginCaptchaService service = new LoginCaptchaService();

    @Test
    void validAnswerCanBeUsedOnlyOnce() {
        MockHttpSession session = new MockHttpSession();
        LoginCaptchaChallenge challenge = service.createChallenge(session, "A2B3C4");

        assertThat(service.validateAndConsume(session, challenge.id(), "a2b3c4")).isTrue();
        assertThat(service.validateAndConsume(session, challenge.id(), "A2B3C4")).isFalse();
    }

    @Test
    void invalidAnswerConsumesChallenge() {
        MockHttpSession session = new MockHttpSession();
        LoginCaptchaChallenge challenge = service.createChallenge(session, "D5E6F7");

        assertThat(service.validateAndConsume(session, challenge.id(), "WRONG1")).isFalse();
        assertThat(service.validateAndConsume(session, challenge.id(), "D5E6F7")).isFalse();
    }

    @Test
    void challengesFromMultipleLoginTabsRemainUsable() {
        MockHttpSession session = new MockHttpSession();
        LoginCaptchaChallenge first = service.createChallenge(session, "G8H9J2");
        LoginCaptchaChallenge second = service.createChallenge(session, "K3M4N5");

        assertThat(service.validateAndConsume(session, first.id(), "G8H9J2")).isTrue();
        assertThat(service.validateAndConsume(session, second.id(), "K3M4N5")).isTrue();
    }

    @Test
    void challengeProvidesPngImageWithoutExposingAnswer() {
        MockHttpSession session = new MockHttpSession();
        LoginCaptchaChallenge challenge = service.createChallenge(session, "P6Q7R8");

        byte[] image = service.getChallengeImage(session, challenge.id()).orElseThrow();

        assertThat(image).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47);
        assertThat(challenge.toString()).doesNotContain("P6Q7R8");
    }

    @Test
    void generatedAnswersAlwaysContainLettersAndNumbers() {
        for (int index = 0; index < 100; index++) {
            assertThat(service.generateAnswer()).matches("(?=.*[A-Z])(?=.*[0-9])[A-Z0-9]{6}");
        }
    }

    @Test
    void refreshReplacesOnlyCurrentTabsChallenge() {
        MockHttpSession session = new MockHttpSession();
        LoginCaptchaChallenge current = service.createChallenge(session, "A2B3C4");
        LoginCaptchaChallenge anotherTab = service.createChallenge(session, "D5E6F7");

        LoginCaptchaChallenge replacement = service.replaceChallenge(session, current.id());

        assertThat(service.getChallengeImage(session, current.id())).isEmpty();
        assertThat(service.getChallengeImage(session, anotherTab.id())).isPresent();
        assertThat(service.getChallengeImage(session, replacement.id())).isPresent();
    }
}
