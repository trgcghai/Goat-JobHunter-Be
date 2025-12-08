package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.message.MessageCreateRequest;
import iuh.fit.goat.dto.response.message.MessageResponse;
import iuh.fit.goat.entity.Conversation;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.repository.ConversationRepository;
import iuh.fit.goat.repository.MessageRepository;
import iuh.fit.goat.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Override
    @Transactional
    public void handleCreateMessage(MessageCreateRequest request) {
        Conversation conversation = this.conversationRepository.findById(request.getConversationId()).orElse(null);

        Message message = new Message();
        message.setRole(request.getRole());
        message.setContent(request.getContent());
        message.setConversation(conversation);

        this.messageRepository.save(message);
    }

    @Override
    public MessageResponse convertMessageResponse(Message message) {
        MessageResponse response = new MessageResponse();
        response.setMessageId(message.getMessageId());
        response.setRole(message.getRole());
        response.setContent(message.getContent());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }
}
