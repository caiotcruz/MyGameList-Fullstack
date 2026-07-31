package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.dto.ActivityUserDTO;
import com.caiotcruz.mygamelist.dto.ConversationSummaryDTO;
import com.caiotcruz.mygamelist.dto.MessageDTO;
import com.caiotcruz.mygamelist.dto.MessageThreadDTO;
import com.caiotcruz.mygamelist.dto.SendMessageDTO;
import com.caiotcruz.mygamelist.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/conversations")
    public List<ConversationSummaryDTO> getConversations() {
        return messageService.getConversations();
    }

    @GetMapping("/conversations/{userId}")
    public MessageThreadDTO getThread(@PathVariable Long userId) {
        return messageService.getThread(userId);
    }

    @PostMapping("/conversations/{userId}")
    public MessageDTO sendMessage(@PathVariable Long userId, @Valid @RequestBody SendMessageDTO dto) {
        return messageService.sendMessage(userId, dto);
    }

    @GetMapping("/unread-count")
    public long getUnreadCount() {
        return messageService.getUnreadTotal();
    }

    @GetMapping("/contacts")
    public List<ActivityUserDTO> getMessageableContacts() {
        return messageService.getMessageableContacts();
    }
}