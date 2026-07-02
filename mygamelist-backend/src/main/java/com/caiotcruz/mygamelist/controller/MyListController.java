package com.caiotcruz.mygamelist.controller;

import com.caiotcruz.mygamelist.dto.AddGameDTO;
import com.caiotcruz.mygamelist.model.UserGameList;
import com.caiotcruz.mygamelist.service.UserGameListService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/my-games")
public class MyListController {

    private final UserGameListService listService;

    public MyListController(UserGameListService listService) {
        this.listService = listService;
    }

    @PostMapping
    public UserGameList addGame(@RequestBody AddGameDTO dto) {
        return listService.addGameToList(dto);
    }

    @GetMapping
    public List<UserGameList> getMyList() {
        return listService.getMyList();
    }

    @DeleteMapping("/{id}")
    public void deleteGame(@PathVariable Long id) {
        listService.removeItem(id);
    }
}