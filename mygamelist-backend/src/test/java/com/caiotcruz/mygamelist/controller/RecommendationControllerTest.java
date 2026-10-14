package com.caiotcruz.mygamelist.controller;
import com.caiotcruz.mygamelist.service.RecommendationService;
import com.caiotcruz.mygamelist.repository.UserRepository;
import com.caiotcruz.mygamelist.model.User;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class RecommendationControllerTest {
 @Test void derivesAccountOnlyFromAuthenticatedPrincipal(){
  var service=mock(RecommendationService.class);var users=mock(UserRepository.class);
  var user=new User();user.setId(7L);when(users.findByEmail("fixture@example.test")).thenReturn(user);
  var controller=new RecommendationController(service,users);controller.get(()->"fixture@example.test");
  verify(service).recommend(7L);
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->controller.get(null));
  verifyNoMoreInteractions(service);
 }
}
