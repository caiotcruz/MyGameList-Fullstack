package com.caiotcruz.mygamelist.client;

import com.caiotcruz.mygamelist.dto.GameDetailsDTO;
import com.caiotcruz.mygamelist.dto.RawgSearchResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
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
        @org.springframework.web.bind.annotation.PathVariable("id") Long id
    );
}