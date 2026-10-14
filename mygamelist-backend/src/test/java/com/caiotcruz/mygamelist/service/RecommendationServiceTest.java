package com.caiotcruz.mygamelist.service;
import com.caiotcruz.mygamelist.client.RawgClient;
import com.caiotcruz.mygamelist.repository.UserGameListRepository;
import com.caiotcruz.mygamelist.model.*;
import com.caiotcruz.mygamelist.model.enums.GameStatus;
import com.caiotcruz.mygamelist.dto.rawg.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
class RecommendationServiceTest {
 private UserGameList entry(long id,GameStatus status,String genres){
  var game=new Game();game.setRawgId(id);game.setGenreSlugs(genres);
  var entry=new UserGameList();entry.setGame(game);entry.setStatus(status);entry.setScore(9);return entry;
 }
 @Test void excludesPlannedGamesAndConcurrentAdditions(){
  var lists=mock(UserGameListRepository.class);var rawg=mock(RawgClient.class);
  var played=entry(1,GameStatus.COMPLETED,"action");var plan=entry(2,GameStatus.PLAN_TO_PLAY,"puzzle");
  when(lists.recommendationHistory(7L)).thenReturn(List.of(played,plan),List.of(played,plan,entry(3,GameStatus.PLAN_TO_PLAY,"action")));
  var genre=List.of(new RawgGenre(4L,"Action","action"));
  when(rawg.recommendationCandidates(isNull(),eq("action"),eq(40),eq(1),eq("-added"))).thenReturn(new RawgRecommendationPage(List.of(
   new RawgRecommendationPage.Candidate(1L,"Played",null,4.0,100,genre),new RawgRecommendationPage.Candidate(2L,"Planned",null,4.0,100,genre),
   new RawgRecommendationPage.Candidate(3L,"Added during request",null,4.0,100,genre),new RawgRecommendationPage.Candidate(4L,"New",null,4.0,100,genre))));
  var result=new RecommendationService(lists,rawg).recommend(7L);
  assertEquals(List.of(4L),result.games().stream().map(RecommendationEngine.Pick::rawgId).toList());
  assertEquals("action",result.genres().get(0).genre());verify(lists,times(2)).recommendationHistory(7L);
 }
 @Test void pendingMetadataAndOtherAccountNeverReusePersonalResults(){
  var lists=mock(UserGameListRepository.class);var rawg=mock(RawgClient.class);
  when(lists.recommendationHistory(1L)).thenReturn(List.of(entry(1,GameStatus.PLAYING,null)));
  when(lists.recommendationHistory(2L)).thenReturn(List.of());
  var service=new RecommendationService(lists,rawg);
  assertEquals("PREPARING",service.recommend(1L).state());assertEquals("EMPTY_HISTORY",service.recommend(2L).state());
  verifyNoInteractions(rawg);
 }
}
