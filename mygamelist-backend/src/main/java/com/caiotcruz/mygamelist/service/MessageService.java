package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.dto.ActivityUserDTO;
import com.caiotcruz.mygamelist.dto.ConversationSummaryDTO;
import com.caiotcruz.mygamelist.dto.MessageDTO;
import com.caiotcruz.mygamelist.dto.MessageThreadDTO;
import com.caiotcruz.mygamelist.dto.SendMessageDTO;
import com.caiotcruz.mygamelist.model.Conversation;
import com.caiotcruz.mygamelist.model.Message;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.repository.ConversationRepository;
import com.caiotcruz.mygamelist.repository.MessageRepository;
import com.caiotcruz.mygamelist.repository.UserFollowRepository;
import com.caiotcruz.mygamelist.repository.UserRepository;

import jakarta.transaction.Transactional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MessageService {
    private final UserLocks locks;

    private final CommunitySafety safety;


    private static final int PREVIEW_MAX_LENGTH = 60;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final UserFollowRepository followRepository;

    public MessageService(ConversationRepository conversationRepository, MessageRepository messageRepository,
                           UserRepository userRepository, UserFollowRepository followRepository, CommunitySafety safety, UserLocks locks) {
        this.locks = locks;
        this.safety = safety;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.followRepository = followRepository;
    }

    public List<ConversationSummaryDTO> getConversations() {
        User me = getCurrentUser();
        List<Conversation> conversations = conversationRepository.findAllForUser(me).stream().filter(c -> !safety.blocked(me.getId(), partner(c,me).getId())).toList();

        Map<Long, Long> unreadByConversation = new HashMap<>();
        for (Object[] row : messageRepository.countUnreadByConversations(conversations, me)) {
            unreadByConversation.put((Long) row[0], (Long) row[1]);
        }

        return conversations.stream()
                .map(c -> toSummaryDTO(c, me, unreadByConversation.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Transactional
    public MessageThreadDTO getThread(Long otherUserId){return getThread(otherUserId,null);}

    @Transactional
    public MessageThreadDTO getThread(Long otherUserId,Long before) {
        if(otherUserId==null||otherUserId<1||before!=null&&before<1)throw new IllegalArgumentException("Invalid cursor");
        User me=getCurrentUser();
        User other=userRepository.findById(otherUserId).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Usuário não encontrado"));
        safety.requireVisible(me.getId(),other.getId());
        var conversation=conversationRepository.findBetween(me,other);
        List<Message> page=conversation.map(c->messageRepository.findTop51ByConversationAndIdLessThanOrderByIdDesc(c,before==null?Long.MAX_VALUE:before)).orElse(List.of());
        Long next=page.size()>50?page.get(49).getId():null;
        var visible=page.stream().limit(50).sorted(java.util.Comparator.comparing(Message::getId)).map(m->toMessageDTO(m,me)).toList();
        if(before==null)conversation.ifPresent(c->messageRepository.markAllAsRead(c,me));
        return new MessageThreadDTO(ActivityUserDTO.from(other),visible,safety.canSend(me,other),next);
    }

    @Transactional
    public MessageDTO sendMessage(Long otherUserId, SendMessageDTO dto) {
        User me = getCurrentUser();

        if (me.getId().equals(otherUserId)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Você não pode enviar mensagens para si mesmo.");
        }

        User other = userRepository.findById(otherUserId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Usuário não encontrado"));

        locks.lock(me.getId(),other.getId());
        if (!safety.canSend(me, other)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"As preferências de privacidade não permitem esta mensagem.");
        }

        Conversation conversation = conversationRepository.findBetween(me, other)
                .orElseGet(() -> createConversation(me, other));

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(me);
        message.setContent(dto.content().trim());
        Message saved = messageRepository.save(message);

        conversation.setLastMessagePreview(truncate(saved.getContent()));
        conversation.setLastMessageSender(me);
        conversation.setLastMessageAt(saved.getSentAt());
        conversationRepository.save(conversation);

        return toMessageDTO(saved, me);
    }

    public long getUnreadTotal() {
        User me = getCurrentUser();
        List<Conversation> conversations = conversationRepository.findAllForUser(me).stream().filter(c -> !safety.blocked(me.getId(), partner(c,me).getId())).toList();

        long total = 0;
        for (Object[] row : messageRepository.countUnreadByConversations(conversations, me)) {
            total += (Long) row[1];
        }
        return total;
    }

    private User partner(Conversation c,User me){return c.getUserA().getId().equals(me.getId())?c.getUserB():c.getUserA();}

    private Conversation createConversation(User a, User b) {
        Conversation conversation = new Conversation();
        if (a.getId() < b.getId()) {
            conversation.setUserA(a);
            conversation.setUserB(b);
        } else {
            conversation.setUserA(b);
            conversation.setUserB(a);
        }
        return conversationRepository.save(conversation);
    }

    private ConversationSummaryDTO toSummaryDTO(Conversation c, User me, long unreadCount) {
        User other = c.getUserA().getId().equals(me.getId()) ? c.getUserB() : c.getUserA();
        boolean lastFromMe = c.getLastMessageSender() != null
                && c.getLastMessageSender().getId().equals(me.getId());

        return new ConversationSummaryDTO(
                c.getId(),
                ActivityUserDTO.from(other),
                c.getLastMessagePreview(),
                c.getLastMessageAt(),
                lastFromMe,
                unreadCount
        );
    }

    private MessageDTO toMessageDTO(Message m, User me) {
        return new MessageDTO(
                m.getId(),
                m.getSender().getId(),
                m.getContent(),
                m.getSentAt(),
                m.getSender().getId().equals(me.getId())
        );
    }

    private boolean usersCanMessage(User me,Long id){return userRepository.findById(id).map(u->safety.canSend(me,u)).orElse(false);}

    private String truncate(String text) {
        if (text.length() <= PREVIEW_MAX_LENGTH) return text;
        return text.substring(0, PREVIEW_MAX_LENGTH) + "...";
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return (User) userRepository.findByEmail(email);
    }

    public List<ActivityUserDTO> getMessageableContacts() {
        User me = getCurrentUser();

        Set<Long> conversationPartnerIds = conversationRepository.findAllForUser(me).stream()
                .map(c -> c.getUserA().getId().equals(me.getId()) ? c.getUserB().getId() : c.getUserA().getId())
                .collect(Collectors.toSet());

        List<ActivityUserDTO> following = followRepository.findFollowingOf(me);
        List<ActivityUserDTO> followers = followRepository.findFollowersOf(me);

        Map<Long, ActivityUserDTO> distinct = new LinkedHashMap<>();
        following.forEach(u -> distinct.put(u.id(), u));
        followers.forEach(u -> distinct.putIfAbsent(u.id(), u));

        return distinct.values().stream()
                .filter(u -> !conversationPartnerIds.contains(u.id()))
                .filter(u -> usersCanMessage(me,u.id()))
                .toList();
    }
}