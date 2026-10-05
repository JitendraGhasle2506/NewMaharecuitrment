package com.maharecruitment.gov.in.web.service.security;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpSession;

@Service
public class LoginCaptchaService {

    private static final String SESSION_ATTRIBUTE = LoginCaptchaService.class.getName() + ".challenges";
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private static final int MAX_ACTIVE_CHALLENGES = 5;
    private static final int CAPTCHA_LENGTH = 6;
    private static final char[] LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final char[] DIGITS = "23456789".toCharArray();
    private static final char[] ALPHANUMERIC = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final SecureRandom secureRandom = new SecureRandom();

    public LoginCaptchaChallenge createChallenge(HttpSession session) {
        return createChallenge(session, generateAnswer());
    }

    public LoginCaptchaChallenge replaceChallenge(HttpSession session, String previousChallengeId) {
        if (StringUtils.hasText(previousChallengeId)) {
            synchronized (session) {
                Map<String, StoredChallenge> challenges = getChallenges(session, false);
                if (challenges != null) {
                    challenges.remove(previousChallengeId);
                    removeExpired(challenges, Instant.now());
                }
            }
        }
        return createChallenge(session);
    }

    LoginCaptchaChallenge createChallenge(HttpSession session, String answer) {
        if (answer == null || !answer.matches("[A-Z0-9]{6}")) {
            throw new IllegalArgumentException("CAPTCHA answer must contain six uppercase letters or digits");
        }
        String id = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(CHALLENGE_TTL);
        StoredChallenge stored = new StoredChallenge(hash(id, answer), createImage(answer), expiresAt);

        synchronized (session) {
            Map<String, StoredChallenge> challenges = getChallenges(session, true);
            removeExpired(challenges, Instant.now());
            while (challenges.size() >= MAX_ACTIVE_CHALLENGES) {
                Iterator<String> iterator = challenges.keySet().iterator();
                iterator.next();
                iterator.remove();
            }
            challenges.put(id, stored);
        }

        return new LoginCaptchaChallenge(id, Math.toIntExact(CHALLENGE_TTL.toSeconds()));
    }

    public Optional<byte[]> getChallengeImage(HttpSession session, String id) {
        if (session == null || !StringUtils.hasText(id)) {
            return Optional.empty();
        }
        synchronized (session) {
            Map<String, StoredChallenge> challenges = getChallenges(session, false);
            if (challenges == null) {
                return Optional.empty();
            }
            removeExpired(challenges, Instant.now());
            StoredChallenge challenge = challenges.get(id);
            return challenge == null ? Optional.empty() : Optional.of(challenge.image().clone());
        }
    }

    public boolean validateAndConsume(HttpSession session, String id, String answer) {
        if (session == null || !StringUtils.hasText(id) || !StringUtils.hasText(answer)) {
            return false;
        }

        StoredChallenge challenge;
        synchronized (session) {
            Map<String, StoredChallenge> challenges = getChallenges(session, false);
            if (challenges == null) {
                return false;
            }
            challenge = challenges.remove(id);
            removeExpired(challenges, Instant.now());
        }

        if (challenge == null || !challenge.expiresAt().isAfter(Instant.now())) {
            return false;
        }
        return MessageDigest.isEqual(
                challenge.answerHash(),
                hash(id, answer.trim().toUpperCase(Locale.ROOT)));
    }

    @SuppressWarnings("unchecked")
    private Map<String, StoredChallenge> getChallenges(HttpSession session, boolean create) {
        Object existing = session.getAttribute(SESSION_ATTRIBUTE);
        if (existing instanceof Map<?, ?>) {
            return (Map<String, StoredChallenge>) existing;
        }
        if (!create) {
            return null;
        }
        Map<String, StoredChallenge> challenges = new LinkedHashMap<>();
        session.setAttribute(SESSION_ATTRIBUTE, challenges);
        return challenges;
    }

    private void removeExpired(Map<String, StoredChallenge> challenges, Instant now) {
        challenges.values().removeIf(challenge -> !challenge.expiresAt().isAfter(now));
    }

    private byte[] hash(String id, String answer) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest((id + ':' + answer).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    String generateAnswer() {
        char[] answer = new char[CAPTCHA_LENGTH];
        answer[0] = randomCharacter(LETTERS);
        answer[1] = randomCharacter(DIGITS);
        for (int index = 2; index < answer.length; index++) {
            answer[index] = randomCharacter(ALPHANUMERIC);
        }
        for (int index = answer.length - 1; index > 0; index--) {
            int swapIndex = secureRandom.nextInt(index + 1);
            char value = answer[index];
            answer[index] = answer[swapIndex];
            answer[swapIndex] = value;
        }
        return new String(answer);
    }

    private char randomCharacter(char[] characters) {
        return characters[secureRandom.nextInt(characters.length)];
    }

    private byte[] createImage(String answer) {
        BufferedImage image = new BufferedImage(220, 70, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(241, 245, 249));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

            for (int index = 0; index < 10; index++) {
                graphics.setColor(new Color(
                        80 + secureRandom.nextInt(100),
                        80 + secureRandom.nextInt(100),
                        80 + secureRandom.nextInt(100),
                        130));
                graphics.drawLine(
                        secureRandom.nextInt(image.getWidth()), secureRandom.nextInt(image.getHeight()),
                        secureRandom.nextInt(image.getWidth()), secureRandom.nextInt(image.getHeight()));
            }

            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 38));
            for (int index = 0; index < answer.length(); index++) {
                AffineTransform originalTransform = graphics.getTransform();
                double rotation = Math.toRadians(secureRandom.nextInt(25) - 12);
                int x = 20 + (index * 31);
                int y = 48 + secureRandom.nextInt(9) - 4;
                graphics.rotate(rotation, x + 12, y - 15);
                graphics.setColor(new Color(
                        15 + secureRandom.nextInt(55),
                        45 + secureRandom.nextInt(65),
                        90 + secureRandom.nextInt(75)));
                graphics.drawString(String.valueOf(answer.charAt(index)), x, y);
                graphics.setTransform(originalTransform);
            }
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to generate CAPTCHA image", ex);
        }
    }

    public record LoginCaptchaChallenge(String id, int expiresInSeconds) {
    }

    private record StoredChallenge(byte[] answerHash, byte[] image, Instant expiresAt) {
    }
}
