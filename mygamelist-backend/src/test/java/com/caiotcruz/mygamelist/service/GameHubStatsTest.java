package com.caiotcruz.mygamelist.service;
import com.caiotcruz.mygamelist.model.Game;
import com.caiotcruz.mygamelist.model.enums.GameStatus;
import com.caiotcruz.mygamelist.repository.*;
import com.caiotcruz.mygamelist.client.RawgClient;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class GameHubStatsTest {
 @Test void hubReportsDroppedSeparatelyFromOtherStatuses(){
  var games=mock(GameRepository.class);var lists=mock(UserGameListRepository.class);
  var game=new Game();game.setId(10L);game.setRawgId(20L);game.setTitle("Fixture");
  when(games.findByRawgId(20L)).thenReturn(Optional.of(game));
  when(lists.countByGameIdAndStatus(10L,GameStatus.DROPPED)).thenReturn(7L);
  when(lists.countByGameIdAndStatus(10L,GameStatus.PLAYING)).thenReturn(3L);
  var service=new GameService(games,lists,mock(RawgClient.class),mock(ReviewVoteRepository.class),mock(UserRepository.class),mock(ActivityRepository.class),mock(CommunitySafety.class),mock(UserLocks.class));
  var result=service.getGameHubData(20L,null);
  assertEquals(7L,result.droppedCount());assertEquals(3L,result.playingCount());assertEquals(0L,result.platinumCount());
 }
}
