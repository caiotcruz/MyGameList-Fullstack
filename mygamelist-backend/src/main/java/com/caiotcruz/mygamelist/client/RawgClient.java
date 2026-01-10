package com.caiotcruz.mygamelist.client;

import com.caiotcruz.mygamelist.dto.GameDetailsDTO;
import com.caiotcruz.mygamelist.dto.RawgSearchResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

// url: Pega a URL base do application.properties
@FeignClient(name = "rawgClient", url = "${api.rawg.url}")
public interface RawgClient {

    @GetMapping("/games")
    RawgSearchResponse searchGames(
        @RequestParam("key") String apiKey,  // A API exige a chave em toda requisição
        @RequestParam("search") String query, // O termo da busca (ex: "Mario")
        @RequestParam("page_size") int pageSize, // Quantos resultados trazer
        @RequestParam("page") int page
    );

    @GetMapping("/games/{id}")
    GameDetailsDTO getGameDetails(
        @RequestParam("key") String apiKey,
        @org.springframework.web.bind.annotation.PathVariable("id") Long id
    );
}