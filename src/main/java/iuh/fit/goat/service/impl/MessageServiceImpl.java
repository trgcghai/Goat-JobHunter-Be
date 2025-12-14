package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.message.MessageCreateRequest;
import iuh.fit.goat.dto.response.message.MessageResponse;
import iuh.fit.goat.repository.MessageRepository;
import iuh.fit.goat.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
//    private final ConversationRepository conversationRepository;
//    private final MessageRepository messageRepository;
//
//    @Override
//    @Transactional
//    public void handleCreateMessage(MessageCreateRequest request) {
//        Conversation conversation = this.conversationRepository.findById(request.getConversationId()).orElse(null);
//
//        Messages messages = new Messages();
//        messages.setRole(request.getRole());
//        messages.setContent(request.getContent());
//        messages.setConversation(conversation);
//
//        this.messageRepository.save(messages);
//    }
//
//    @Override
//    public MessageResponse convertMessageResponse(Messages messages) {
//        MessageResponse response = new MessageResponse();
//        response.setMessageId(messages.getMessageId());
//        response.setRole(messages.getRole());
//        response.setContent(messages.getContent());
//        response.setCreatedAt(messages.getCreatedAt());
//        return response;
//    }
}
