package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.message.MessageCreateRequest;

public interface MessageService {

    void handleCreateMessage(MessageCreateRequest request);
}
