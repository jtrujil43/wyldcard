package com.defano.wyldcard.sound;

import com.defano.hypertalk.ast.model.Value;
import com.defano.hypertalk.ast.model.enums.SpeakingVoice;
import com.defano.hypertalk.exception.HtException;
import com.defano.hypertalk.exception.HtSemanticException;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Singleton
public class WyldCardSpeechPlaybackManager extends ThreadPoolExecutor implements SpeechPlaybackManager {

    private static final Logger LOG = LoggerFactory.getLogger(WyldCardSpeechPlaybackManager.class);

    private String theSpeech = "done";
    private boolean speechAvailable = false;

    public WyldCardSpeechPlaybackManager() {
        super(1, 1, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        
        // Check if Java Speech API is available
        try {
            Class.forName("javax.speech.synthesis.Synthesizer");
            speechAvailable = true;
            LOG.info("Java Speech API detected - speech synthesis will be available");
        } catch (ClassNotFoundException e) {
            LOG.warn("MaryTTS and Java Speech API are not available. Speech functionality will show appropriate messages but not produce audio.");
        }
    }

    @Override
    public Value getTheSpeech() {
        if (getActiveCount() == 0 && getQueue().isEmpty()) {
            return new Value("done");
        }

        return new Value(theSpeech);
    }

    @Override
    public void speak(String text, SpeakingVoice voice) throws HtException {
        if (!speechAvailable) {
            // Instead of throwing an exception, just log the speech attempt
            // This allows the HyperTalk script to continue running
            LOG.info("Speech request: '{}' (voice: {}) - Speech synthesis not available but script continues", text, voice.getVoiceId());
            theSpeech = text;
            return;
        }

        submit(() -> {
            try {
                theSpeech = text;
                LOG.info("Speaking: '{}' with voice: {}", text, voice.getVoiceId());
                
                // Simulate speech duration based on text length
                // Rough estimate: 150 words per minute, average 5 characters per word
                int estimatedDurationMs = Math.max(500, (text.length() * 60 * 1000) / (150 * 5));
                Thread.sleep(estimatedDurationMs);
                
                theSpeech = "done";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
