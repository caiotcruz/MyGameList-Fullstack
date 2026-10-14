package com.caiotcruz.mygamelist.service;
import com.caiotcruz.mygamelist.dto.rawg.RawgGenre;
import com.caiotcruz.mygamelist.repository.GameRepository;
import com.caiotcruz.mygamelist.client.RawgClient;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.PageRequest;
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.recommendations.metadata-enabled",havingValue="true",matchIfMissing=true)
public class GenreMetadata {
    private final GameRepository games;
    private final RawgClient rawg;
    @Value("${api.rawg.key}") private String apiKey;
    public GenreMetadata(GameRepository games,RawgClient rawg){this.games=games;this.rawg=rawg;}
    public static String slugs(List<RawgGenre> genres){
        return genres.stream().filter(Objects::nonNull).map(RawgGenre::slug).filter(Objects::nonNull)
            .filter(s->s.matches("[a-z0-9-]+")).distinct().sorted().collect(java.util.stream.Collectors.joining(","));
    }
    public static List<String> split(String slugs){return slugs==null || slugs.isBlank()?List.of():List.of(slugs.split(","));}
    @Scheduled(initialDelay=5000,fixedDelay=30000,scheduler="genreScheduler")
    public void enrich(){
        for(var game:games.pendingGenres(java.time.LocalDateTime.now(),PageRequest.of(0,8))){
            try {
                var details=rawg.getGameDetails(apiKey,game.getRawgId());
                if(details==null || details.genres()==null) throw new IllegalStateException("Missing genres");
                games.updateGenres(game.getId(),slugs(details.genres()),null);
            } catch(Exception e){
                games.updateGenres(game.getId(),null,java.time.LocalDateTime.now().plusMinutes(15));
                org.slf4j.LoggerFactory.getLogger(GenreMetadata.class).warn("Genres pending for game {}; retry in 15 minutes",game.getId());
            }
        }
    }
}
