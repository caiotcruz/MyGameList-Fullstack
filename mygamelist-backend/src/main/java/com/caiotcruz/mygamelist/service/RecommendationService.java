package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.client.RawgClient;
import com.caiotcruz.mygamelist.model.enums.GameStatus;
import com.caiotcruz.mygamelist.repository.UserGameListRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class RecommendationService {
    public record Response(String state,int pendingGenres,List<RecommendationEngine.Preference> genres,List<RecommendationEngine.Pick> games) {}
    private final UserGameListRepository lists;
    private final RawgClient rawg;
    @Value("${api.rawg.key}") private String apiKey;
    public RecommendationService(UserGameListRepository lists,RawgClient rawg){this.lists=lists;this.rawg=rawg;}

    public Response recommend(Long userId) {
        var entries=lists.recommendationHistory(userId);
        var played=entries.stream().filter(e->e.getStatus()!=null && e.getStatus()!=GameStatus.PLAN_TO_PLAY).toList();
        if(played.isEmpty()) return new Response("EMPTY_HISTORY",0,List.of(),List.of());
        int pending=(int)played.stream().filter(e->e.getGame().getGenreSlugs()==null).count();
        var history=played.stream().map(e->new RecommendationEngine.History(e.getGame().getRawgId(),
            GenreMetadata.split(e.getGame().getGenreSlugs()),true,e.getScore()==null?0:e.getScore())).toList();
        var preferences=RecommendationEngine.preferences(history);
        if(preferences.isEmpty()) return new Response(pending>0?"PREPARING":"NO_GENRES",pending,List.of(),List.of());
        List<RecommendationEngine.Candidate> candidates=new ArrayList<>();
        for(var preference:preferences) {
            // Bounded, public candidate pools are shared in Redis; personalized results are not cached.
            var page=rawg.recommendationCandidates(apiKey,preference.genre(),40,1,"-added");
            if(page==null || page.results()==null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Recomendações indisponíveis. Tente novamente.");
            for(var c:page.results()) if(c!=null && c.id()!=null && c.name()!=null && c.genres()!=null) {
                candidates.add(new RecommendationEngine.Candidate(c.id(),c.name(),c.cover(),
                    GenreMetadata.split(GenreMetadata.slugs(c.genres())),c.rating()==null?0:c.rating(),c.ratingsCount()==null?0:c.ratingsCount()));
            }
        }
        // Re-read after upstream work, including PLAN_TO_PLAY and additions made while we were loading.
        Set<Long> excluded=new HashSet<>();
        for(var e:lists.recommendationHistory(userId)) excluded.add(e.getGame().getRawgId());
        var picks=RecommendationEngine.rank(preferences,candidates,excluded,12);
        return new Response(pending>0?"PREPARING":picks.isEmpty()?"EXHAUSTED":"READY",pending,preferences,picks);
    }
}
