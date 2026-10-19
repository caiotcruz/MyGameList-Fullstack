package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.model.*;
import com.caiotcruz.mygamelist.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessagePaginationTest {
    final ConversationRepository conversations=mock(ConversationRepository.class);
    final MessageRepository messages=mock(MessageRepository.class);
    final UserRepository users=mock(UserRepository.class);
    final CommunitySafety safety=mock(CommunitySafety.class);
    final MessageService service=new MessageService(conversations,messages,users,mock(UserFollowRepository.class),safety,mock(UserLocks.class));
    final User me=new User(),other=new User();
    final Conversation conversation=new Conversation();
    @BeforeEach void setup(){
        me.setId(1L);me.setEmail("me@example.test");other.setId(2L);other.setName("Other");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(me.getEmail(),null,List.of()));
        when(users.findByEmail(me.getEmail())).thenReturn(me);
        when(users.findById(2L)).thenReturn(Optional.of(other));
        when(conversations.findBetween(me,other)).thenReturn(Optional.of(conversation));
        when(safety.canSend(me,other)).thenReturn(true);
    }
    @AfterEach void cleanup(){SecurityContextHolder.clearContext();}
    @Test void pageIsBoundedAndCursorDoesNotSkipTheExtraRow(){
        var page=new ArrayList<Message>();
        for(long id=100;id>=50;id--){var m=new Message();m.setId(id);m.setSender(other);m.setContent("text");page.add(m);}
        when(messages.findTop51ByConversationAndIdLessThanOrderByIdDesc(conversation,Long.MAX_VALUE)).thenReturn(page);
        var result=service.getThread(2L,null);
        assertEquals(50,result.messages().size());assertEquals(51L,result.nextBefore());
        assertEquals(51L,result.messages().get(0).id());assertEquals(100L,result.messages().get(49).id());
        verify(messages).markAllAsRead(conversation,me);
    }
    @Test void previousPageUsesOnlyAuthenticatedUsersConversation(){
        when(messages.findTop51ByConversationAndIdLessThanOrderByIdDesc(conversation,51L)).thenReturn(List.of());
        assertNull(service.getThread(2L,51L).nextBefore());
        verify(conversations).findBetween(me,other);
        verify(messages).findTop51ByConversationAndIdLessThanOrderByIdDesc(conversation,51L);
        verify(messages,never()).markAllAsRead(any(),any());
    }
    @Test void blockIsCheckedBeforeAnyMessagesAreRead(){
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(safety).requireVisible(1L,2L);
        assertThrows(ResponseStatusException.class,()->service.getThread(2L,51L));
        verifyNoInteractions(messages,conversations);
    }
    @Test void invalidCursorNeverQueriesDatabase(){
        assertThrows(IllegalArgumentException.class,()->service.getThread(2L,0L));
        verifyNoInteractions(messages,conversations,users);
    }
}
