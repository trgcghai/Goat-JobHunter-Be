package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.message.MessageTranslationResponse;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.AiService;
import iuh.fit.goat.service.helper.MessageHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageTranslateTest {

    @Mock private MessageHelper messageHelper;
    @Mock private AiService aiService;

    @InjectMocks private MessageServiceImpl messageService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void translateMessage_success_basic() throws Exception {
        when(messageHelper.normalizeMessageContent("Hello")).thenReturn("Hello");
        when(aiService.translateText("Hello", "vi")).thenReturn("Xin chao");

        MessageTranslationResponse resp = messageService.translateMessage("Hello", "vi");

        assertThat(resp.getSourceText()).isEqualTo("Hello");
        assertThat(resp.getTranslatedText()).isEqualTo("Xin chao");
        assertThat(resp.getTargetLang()).isEqualTo("vi");
    }

    @Test
    void translateMessage_targetLangNull_returnsNullTargetLang() throws Exception {
        when(messageHelper.normalizeMessageContent("Hi")).thenReturn("Hi");
        when(aiService.translateText("Hi", null)).thenReturn("Translated");

        MessageTranslationResponse resp = messageService.translateMessage("Hi", null);

        assertThat(resp.getSourceText()).isEqualTo("Hi");
        assertThat(resp.getTranslatedText()).isEqualTo("Translated");
        assertThat(resp.getTargetLang()).isNull();
    }

    @Test
    void translateMessage_targetLangWithSpaces_isTrimmed() throws Exception {
        when(messageHelper.normalizeMessageContent("Hi there")).thenReturn("Hi there");
        when(aiService.translateText("Hi there", " en ")).thenReturn("Hello there");

        MessageTranslationResponse resp = messageService.translateMessage("Hi there", " en ");

        assertThat(resp.getTargetLang()).isEqualTo("en");
    }

    @Test
    void translateMessage_normalizeReturnsNull_throwsInvalid() {
        when(messageHelper.normalizeMessageContent("   ")).thenReturn(null);

        assertThrows(InvalidException.class, () -> messageService.translateMessage("   ", "vi"));
    }

    @Test
    void translateMessage_normalizeReturnsBlank_throwsInvalid() {
        when(messageHelper.normalizeMessageContent("")).thenReturn("");

        assertThrows(InvalidException.class, () -> messageService.translateMessage("", "vi"));
    }

    @Test
    void translateMessage_aiServiceThrows_propagatesRuntime() throws Exception {
        when(messageHelper.normalizeMessageContent("Hello"))
                .thenReturn("Hello");
        when(aiService.translateText("Hello", "xx")).thenThrow(new RuntimeException("ai down"));

        assertThrows(RuntimeException.class, () -> messageService.translateMessage("Hello", "xx"));
    }

    @Test
    void translateMessage_aiReturnsEmptyString_allowed() throws Exception {
        when(messageHelper.normalizeMessageContent("Hello"))
                .thenReturn("Hello");
        when(aiService.translateText("Hello", "fr")).thenReturn("");

        MessageTranslationResponse resp = messageService.translateMessage("Hello", "fr");
        assertThat(resp.getTranslatedText()).isEqualTo("");
    }

    @Test
    void translateMessage_sourceContainsExtraSpaces_normalizedBeforeTranslate() throws Exception {
        when(messageHelper.normalizeMessageContent("  Hello  ")).thenReturn("Hello");
        when(aiService.translateText("Hello", "de")).thenReturn("Hallo");

        MessageTranslationResponse resp = messageService.translateMessage("  Hello  ", "de");
        assertThat(resp.getSourceText()).isEqualTo("Hello");
        assertThat(resp.getTranslatedText()).isEqualTo("Hallo");
    }

    @Test
    void translateMessage_longContent_handlesSuccessfully() throws Exception {
        String longContent = "a".repeat(2000);
        when(messageHelper.normalizeMessageContent(longContent)).thenReturn(longContent);
        when(aiService.translateText(longContent, "es")).thenReturn("translated-long");

        MessageTranslationResponse resp = messageService.translateMessage(longContent, "es");
        assertThat(resp.getSourceText()).isEqualTo(longContent);
        assertThat(resp.getTranslatedText()).isEqualTo("translated-long");
    }

    @Test
    void translateMessage_verifyAiServiceCalledWithNormalizedContent() throws Exception {
        when(messageHelper.normalizeMessageContent("Hello world")).thenReturn("Hello world");
        when(aiService.translateText("Hello world", "it")).thenReturn("Ciao mondo");

        MessageTranslationResponse resp = messageService.translateMessage("Hello world", "it");

        assertThat(resp.getSourceText()).isEqualTo("Hello world");
        assertThat(resp.getTranslatedText()).isEqualTo("Ciao mondo");
    }
}