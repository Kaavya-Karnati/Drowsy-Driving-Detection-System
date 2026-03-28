package com.example.drowsydrivingdetection.voice;

import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/*
  Class maps a speech-recognition transcript + confidence score to one of four outcomes

  Classification Types:

    1. ESCALATE: transcript contains a distress keyword OR transcript is null/empty (treated as "no response")
    2. ALERT_OK: transcript contains a positive keyword AND confidence ≥ threshold
    3. GIBBERISH: confidence below threshold OR text too short / non-words
    4. TOO_SHORT: text present but under minimum length (probably background noise)

  Callers can treat GIBBERISH and TOO_SHORT identically (re-prompt once, then
  escalate), but having them separate makes logging and unit-testing clearer
 */
public class ResponseAnalyzer {

    private static final String TAG = "ResponseAnalyzer";

    //min confidence from SpeechRecognizer to trust the result
    private static final float MIN_CONFIDENCE = 0.35f;

    //min word count to be considered a real response
    private static final int MIN_WORD_COUNT = 1;

    //min total characters (filters out noise like "uh")
    private static final int MIN_CHAR_LENGTH = 2;

    private static final Set<String> POSITIVE_KEYWORDS = new HashSet<>(Arrays.asList(
            "yes", "yeah", "yep", "yup", "sure", "okay", "ok", "fine",
            "alert", "awake", "aware", "good", "alright", "absolutely",
            "i'm fine", "i am fine", "im fine", "i'm good", "i am good",
            "i'm awake", "i am awake", "i'm okay", "i am okay", "ready",
            "no problem", "I'm alert"
    ));

    private static final Set<String> DISTRESS_KEYWORDS = new HashSet<>(Arrays.asList(
            "help", "get help", "need help", "emergency", "call 911",
            "call police", "sos", "danger", "hurt", "injured", "accident",
            "no", "not okay", "not fine", "not good", "can't", "cannot",
            "sleepy", "tired", "drowsy", "falling asleep", "can't drive"
    ));

    public enum Classification {
        //Driver gave a positive response
        ALERT_OK,
        //Driver signalled distress OR gave no response at all
        ESCALATE,
        //Response present but too short / low-confidence to interpret
        GIBBERISH,
        //Separate from ESCALATE so we can log/count silences before deciding to escalate
        NO_RESPONSE
    }

    public static class Result {
        /*
        Data type returned by the classify() to pass to other classes and keep
        it organized
         */
        public final Classification classification; //Type of response
        public final String transcript; //cleaned up and trimmed transcript
        public final float confidence;
        public final String reason; //human-readable for logs/debug

        public Result(Classification classification, String transcript,
                      float confidence, String reason) {
            this.classification = classification;
            this.transcript     = transcript;
            this.confidence     = confidence;
            this.reason         = reason;
        }

        @Override
        public String toString() {
            return "[" + classification + "] \"" + transcript
                    + "\" conf=" + confidence + " (" + reason + ")";
        } //for logging purposes
    }

    public static Result classify(String transcript, float confidence) {
        Log.d(TAG, "Classifying: \"" + transcript + "\" conf=" + confidence);

        //no response at all
        if (transcript == null || transcript.trim().isEmpty()) {
            return new Result(Classification.NO_RESPONSE, transcript, confidence,
                    "null or empty transcript");
        }

        String trimmed = transcript.trim();
        String lower   = trimmed.toLowerCase();

        //too short to be meaningful
        if (trimmed.length() < MIN_CHAR_LENGTH) {
            return new Result(Classification.GIBBERISH, trimmed, confidence,
                    "transcript too short: " + trimmed.length() + " chars");
        }

        //distress keywords take priority over everything else
        if (containsDistressKeyword(lower)) {
            return new Result(Classification.ESCALATE, trimmed, confidence,
                    "distress keyword detected: \"" + findMatchingKeyword(lower, DISTRESS_KEYWORDS) + "\"");
        }

        //confidence below threshold so can't trust the words
        if (confidence >= 0 && confidence < MIN_CONFIDENCE) {
            return new Result(Classification.GIBBERISH, trimmed, confidence,
                    "confidence too low: " + confidence);
        }

        //positive keyword present
        if (containsPositiveKeyword(lower)) {
            return new Result(Classification.ALERT_OK, trimmed, confidence,
                    "positive keyword detected: \"" + findMatchingKeyword(lower, POSITIVE_KEYWORDS) + "\"");
        }

        //response present but didn't match any keyword
        //treat as gibberish so the caller can re-prompt once before escalating
        //driver who is alert will be able to say "yes" on the retry
        return new Result(Classification.GIBBERISH, trimmed, confidence,
                "no matching keyword found in: \"" + trimmed + "\"");
    }

    //Helper methods for the classify() method, checks the HashSet for the keywords transcribed
    private static boolean containsDistressKeyword(String lower) {
        for (String keyword : DISTRESS_KEYWORDS) {
            if (lower.contains(keyword)) return true;
        }
        return false;
    }

    private static boolean containsPositiveKeyword(String lower) {
        for (String keyword : POSITIVE_KEYWORDS) {
            if (lower.contains(keyword)) return true;
        }
        return false;
    }

    private static String findMatchingKeyword(String lower, Set<String> keywords) {
        for (String kw : keywords) {
            if (lower.contains(kw)) return kw;
        }
        return "unknown";
    }
}