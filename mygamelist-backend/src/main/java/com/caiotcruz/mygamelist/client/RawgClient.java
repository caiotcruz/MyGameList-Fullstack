package com.caiotcruz.mygamelist.client;

import com.caiotcruz.mygamelist.dto.GameDetailsDTO;
import com.caiotcruz.mygamelist.dto.RawgSearchResponse;
import com.caiotcruz.mygamelist.dto.rawg.RawgAchievementsResponse;
import com.caiotcruz.mygamelist.dto.rawg.RawgGameListResponse;
import com.caiotcruz.mygamelist.dto.rawg.RawgMoviesResponse;
import com.caiotcruz.mygamelist.dto.rawg.RawgScreenshotsResponse;
import com.caiotcruz.mygamelist.dto.rawg.RawgStoresResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "rawgClient", url = "${api.rawg.url}")
public interface RawgClient {

    @GetMapping("/games")
    RawgSearchResponse searchGames(
        @RequestParam("key") String apiKey,
        @RequestParam("search") String query,
        @RequestParam("page_size") int pageSize,
        @RequestParam("page") int page
    );

    @GetMapping("/games/{id}")
    GameDetailsDTO getGameDetails(
        @RequestParam("key") String apiKey,
        @PathVariable("id") Long id
    );

    @GetMapping("/games/{id}/additions")
    RawgGameListResponse getAdditions(
        @RequestParam("key") String apiKey,
        @PathVariable("id") Long id
    );

    @GetMapping("/games/{id}/game-series")
    RawgGameListResponse getGameSeries(
        @RequestParam("key") String apiKey,
        @PathVariable("id") Long id
    );

    @GetMapping("/games/{id}/screenshots")
    RawgScreenshotsResponse getScreenshots(
        @RequestParam("key") String apiKey,
        @PathVariable("id") Long id
    );

    @GetMapping("/games/{id}/stores")
    RawgStoresResponse getStores(
        @RequestParam("key") String apiKey,
        @PathVariable("id") Long id
    );

    @GetMapping("/games/{id}/achievements")
    RawgAchievementsResponse getAchievements(
        @RequestParam("key") String apiKey,
        @PathVariable("id") Long id,
        @RequestParam("page") int page,
        @RequestParam("page_size") int pageSize
    );
    @GetMapping("/games/{id}/movies")
    RawgMoviesResponse getMovies(
        @RequestParam("key") String apiKey,
        @PathVariable("id") Long id
    );
}