package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.dto.AddGameDTO;
import com.caiotcruz.mygamelist.model.Game;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.UserGameList;
import com.caiotcruz.mygamelist.model.enums.GameStatus;
import com.caiotcruz.mygamelist.repository.UserGameListRepository;
import com.caiotcruz.mygamelist.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserGameListService {

    @Autowired
    private UserGameListRepository listRepository;
    @Autowired
    private GameService gameService;
    @Autowired
    private UserRepository userRepository;

    public UserGameList addGameToList(AddGameDTO dto) {
        // 1. Pegar usuário logado do Token
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = (User) userRepository.findByEmail(email);

        // 2. Garantir que o jogo existe no nosso banco (usando a lógica híbrida da Fase 2)
        Game game = gameService.getGameContent(dto.rawgId());

        // 3. Verificar se já existe na lista (se sim, atualiza; se não, cria)
        UserGameList item = listRepository.findByUserAndGame(user, game)
                .orElse(new UserGameList());

        // 4. Preencher dados
        if (item.getId() == null) {
            item.setUser(user);
            item.setGame(game);
        }
        
        // Atualiza os campos se vierem no DTO
        if (dto.status() != null) item.setStatus(dto.status());
        if (dto.score() != null) item.setScore(dto.score());
        if (dto.review() != null) item.setReview(dto.review());
        
        item.setUpdatedAt(LocalDateTime.now());

        return listRepository.save(item);
    }
    
    // Método para listar os jogos do usuário logado
    public java.util.List<UserGameList> getMyList() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = (User) userRepository.findByEmail(email);
        return listRepository.findByUser(user);
    }

    public void removeItem(Long listId) {
        // 1. Pega o usuário logado
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = (User) userRepository.findByEmail(email);

        // 2. Busca o item no banco
        UserGameList item = listRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Item não encontrado"));

        // 3. SEGURANÇA: Verifica se o item pertence mesmo a esse usuário
        if (!item.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Você não tem permissão para deletar este item.");
        }

        // 4. Deleta
        listRepository.delete(item);
    }
}