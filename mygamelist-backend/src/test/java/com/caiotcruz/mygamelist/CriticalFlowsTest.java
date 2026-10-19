package com.caiotcruz.mygamelist;

import com.caiotcruz.mygamelist.model.*;
import com.caiotcruz.mygamelist.model.enums.*;
import com.caiotcruz.mygamelist.repository.*;
import com.caiotcruz.mygamelist.infra.security.TokenService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

// Explicit opt-in; never targets DATABASE_URL or an existing production database.
@EnabledIfEnvironmentVariable(named="RUN_CRITICAL_TESTS",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
 "spring.datasource.url=jdbc:postgresql://localhost:5433/mygamelist_critical_tests",
 "spring.datasource.username=postgres", "spring.datasource.password=${DATABASE_PASSWORD}",
 "app.security.require-runtime-role=false", "app.community.migrate=true",
 "spring.jpa.hibernate.ddl-auto=create-drop", "app.mail.worker-enabled=false",
 "spring.rabbitmq.listener.simple.auto-startup=false", "spring.rabbitmq.dynamic=false",
 "management.server.port=0", "api.security.token.secret=local-integration-test-key-with-more-than-32-bytes",
 "app.outbox-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
 "api.rawg.key=unused-test-key", "spring.mail.username=test@example.test", "spring.mail.password=unused",
 "app.recommendations.metadata-enabled=false",
 "app.moderator-ids="
})
class CriticalFlowsTest {
 @org.springframework.test.context.bean.override.mockito.MockitoBean
 com.caiotcruz.mygamelist.infra.security.RateLimits isolatedRateLimits;
 @Autowired UserRepository users; @Autowired GameRepository games;
 @Autowired UserGameListRepository lists; @Autowired NotificationRepository notifications;
 @Autowired JdbcTemplate db; @Autowired TokenService tokens; @Autowired JsonMapper json; @Autowired Environment env;
 private final HttpClient http=HttpClient.newHttpClient();
 private User alice,bob,charlie; private Game game1,game2;
 @BeforeEach void fixtures(){alice=user();bob=user();charlie=user();game1=game();game2=game();}
 User user(){String uid=UUID.randomUUID().toString();User u=new User();u.setName("u"+uid);u.setEmail(uid+"@example.test");u.setPassword("unused-password");u.setEnabled(true);return users.saveAndFlush(u);}
 Game game(){Game g=new Game();g.setRawgId(Math.abs(UUID.randomUUID().getMostSignificantBits()));g.setTitle("Integration fixture");return games.saveAndFlush(g);}
 record Result(int status,String body){}
 Result call(User user,String method,String path,Object body)throws Exception{
  var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+env.getProperty("local.server.port")+path)).timeout(java.time.Duration.ofSeconds(20)).header("Authorization","Bearer "+tokens.generateToken(user));
  if(body==null)builder.method(method,HttpRequest.BodyPublishers.noBody());else builder.header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
  var response=http.send(builder.build(),HttpResponse.BodyHandlers.ofString());return new Result(response.statusCode(),response.body());
 }
 Result add(User u,Game g,boolean favorite)throws Exception{return call(u,"POST","/my-games",Map.of("rawgId",g.getRawgId(),"status","COMPLETED","isFavorite",favorite,"review","Uma review de teste"));}
 void ok(Result r){assertTrue(r.status>=200&&r.status<300,()->r.status+" "+r.body);}
 long xp(User user){User fresh=users.findById(user.getId()).orElseThrow();return (fresh.getLevel()-1L)*1000+fresh.getExperience();}
 List<Result> parallel(List<Callable<Result>> tasks)throws Exception{
  ExecutorService pool=Executors.newFixedThreadPool(8);CountDownLatch start=new CountDownLatch(1);
  try{var futures=new ArrayList<Future<Result>>();for(var task:tasks)futures.add(pool.submit(()->{start.await();return task.call();}));start.countDown();var results=new ArrayList<Result>();for(var future:futures)results.add(future.get(30,TimeUnit.SECONDS));return results;}finally{pool.shutdownNow();}
 }
 @Test void concurrentSameGameAwardsXpOnce()throws Exception{
  var tasks=new ArrayList<Callable<Result>>();for(int i=0;i<12;i++)tasks.add(()->add(alice,game1,false));
  parallel(tasks).forEach(this::ok);
  assertEquals(1,lists.findByUserId(alice.getId()).size());assertEquals(300,xp(alice));
 }
 @Test void concurrentDifferentGamesDoNotLoseXp()throws Exception{
  var tasks=new ArrayList<Callable<Result>>();for(int i=0;i<8;i++){Game game=game();tasks.add(()->add(alice,game,false));}
  parallel(tasks).forEach(this::ok);assertEquals(2400,xp(alice));
 }
 @Test void concurrentFavoritesLeaveExactlyOneAndDbRejectsTwo()throws Exception{
  ok(add(alice,game1,false));ok(add(alice,game2,false));
  var tasks=new ArrayList<Callable<Result>>();for(int i=0;i<12;i++){Game g=i%2==0?game1:game2;tasks.add(()->call(alice,"POST","/my-games",Map.of("rawgId",g.getRawgId(),"isFavorite",true)));}
  parallel(tasks).forEach(this::ok);
  assertEquals(1,lists.findByUserId(alice.getId()).stream().filter(UserGameList::isFavorite).count());assertEquals(600,xp(alice));
  assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.update("UPDATE tb_user_game_list SET is_favorite=true WHERE user_id=?",alice.getId()));
 }
 @Test void anotherUserCannotDeleteEntryOrReadPrivateConversation()throws Exception{
  ok(add(alice,game1,false));long id=lists.findByUserId(alice.getId()).get(0).getId();
  assertEquals(403,call(bob,"DELETE","/my-games/"+id,null).status);assertTrue(lists.existsById(id));assertEquals(0,xp(bob));
  ok(call(bob,"PUT","/safety/preferences",Map.of("messagePolicy","EVERYONE")));
  ok(call(alice,"POST","/messages/conversations/"+bob.getId(),Map.of("content","Private fixture")));
  var result=call(charlie,"GET","/messages/conversations/"+bob.getId(),null);ok(result);assertFalse(result.body.contains("Private fixture"));
 }
 @Test void notificationOwnershipIsEnforced()throws Exception{
  Notification n=new Notification(alice,bob,NotificationType.FOLLOW,null);notifications.saveAndFlush(n);
  ok(call(bob,"PUT","/notifications/"+n.getId()+"/read",null));assertFalse(notifications.findById(n.getId()).orElseThrow().isRead());
  ok(call(alice,"PUT","/notifications/"+n.getId()+"/read",null));assertTrue(notifications.findById(n.getId()).orElseThrow().isRead());
 }
 @Test void preferencesRequireRecipientsConsentAndKeepHistory()throws Exception{
  assertEquals(403,call(alice,"POST","/messages/conversations/"+bob.getId(),Map.of("content","hello")).status);
  ok(call(bob,"PUT","/safety/preferences",Map.of("messagePolicy","EVERYONE")));
  ok(call(alice,"POST","/messages/conversations/"+bob.getId(),Map.of("content","Consented history")));
  ok(call(bob,"PUT","/safety/preferences",Map.of("messagePolicy","NOBODY")));
  assertEquals(403,call(alice,"POST","/messages/conversations/"+bob.getId(),Map.of("content","denied")).status);
  var history=call(alice,"GET","/messages/conversations/"+bob.getId(),null);ok(history);assertTrue(history.body.contains("Consented history"));assertTrue(history.body.contains("\"canSend\":false"));
 }
 @Test void blockingIsBidirectionalAndPreventsInteractions()throws Exception{
  ok(add(bob,game1,false));long activity=db.queryForObject("SELECT MAX(id) FROM tb_activities WHERE user_id=?",Long.class,bob.getId());
  ok(call(alice,"PUT","/safety/blocks/"+bob.getId(),null));
  assertEquals(403,call(alice,"GET","/users/"+bob.getId(),null).status);
  assertEquals(403,call(bob,"GET","/community/users/"+alice.getId()+"/list",null).status);
  assertEquals(403,call(alice,"POST","/community/activities/"+activity+"/like",null).status);
  assertEquals(403,call(alice,"POST","/community/activities/"+activity+"/comments",Map.of("text","denied")).status);
  assertEquals(403,call(bob,"POST","/messages/conversations/"+alice.getId(),Map.of("content","denied")).status);
  ok(call(alice,"DELETE","/safety/blocks/"+bob.getId(),null));ok(call(alice,"GET","/users/"+bob.getId(),null));
 }
 @Test void reportsDoNotExposeThirdPartyMessagesOrModeratorQueue()throws Exception{
  ok(call(bob,"PUT","/safety/preferences",Map.of("messagePolicy","EVERYONE")));
  ok(call(alice,"POST","/messages/conversations/"+bob.getId(),Map.of("content","Private evidence")));
  long message=db.queryForObject("SELECT MAX(id) FROM tb_messages WHERE sender_id=?",Long.class,alice.getId());
  var body=Map.of("targetType","MESSAGE","targetId",message,"reason","SPAM","details","");
  assertEquals(404,call(charlie,"POST","/safety/reports",body).status);
  ok(call(bob,"POST","/safety/reports",body));ok(call(bob,"POST","/safety/reports",body));
  assertEquals(1L,db.queryForObject("SELECT COUNT(*) FROM tb_content_reports WHERE reporter_id=?",Long.class,bob.getId()).longValue());
  assertEquals(403,call(bob,"GET","/safety/moderation/reports",null).status);
  assertEquals(400,call(bob,"PUT","/safety/preferences",Map.of("messagePolicy","ADMIN")).status);
 }

 @Test void longReviewAndThemePersistAndOverflowIsRejected()throws Exception{
  String review="r".repeat(50000);
  ok(call(alice,"POST","/my-games",Map.of("rawgId",game1.getRawgId(),"status","DROPPED","review",review)));
  assertEquals(review,lists.findByUserId(alice.getId()).get(0).getReview());
  assertEquals(400,call(alice,"POST","/my-games",Map.of("rawgId",game1.getRawgId(),"review",review+"x")).status);
  assertEquals(review,lists.findByUserId(alice.getId()).get(0).getReview());
  ok(call(alice,"PUT","/users/me",Map.of("bio","b".repeat(5000),"theme","TEAL")));
  User saved=users.findById(alice.getId()).orElseThrow();
  assertEquals(5000,saved.getBio().length());assertEquals(ThemePreference.TEAL,saved.getTheme());
  assertEquals(400,call(alice,"PUT","/users/me",Map.of("theme","ARBITRARY")).status);
  assertEquals(ThemePreference.TEAL,users.findById(bob.getId()).orElseThrow().getTheme());
 }
}
