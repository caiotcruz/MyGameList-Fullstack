package com.caiotcruz.mygamelist.service;
import com.caiotcruz.mygamelist.dto.AchievementDTO;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.UserGameList;
import com.caiotcruz.mygamelist.model.enums.GameStatus;
import com.caiotcruz.mygamelist.repository.UserFollowRepository;
import com.caiotcruz.mygamelist.repository.UserGameListRepository;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class AchievementService {
    private final UserGameListRepository games;
    private final UserFollowRepository follows;
    public AchievementService(UserGameListRepository games, UserFollowRepository follows) {
        this.games = games; this.follows = follows;
    }
    public List<AchievementDTO> forUser(User user) {
        List<UserGameList> entries = games.findByUserId(user.getId());
        long total = entries.size();
        long completed = entries.stream().filter(e -> e.getStatus() == GameStatus.COMPLETED || e.getStatus() == GameStatus.PLATINUM).count();
        long platinum = entries.stream().filter(e -> e.getStatus() == GameStatus.PLATINUM).count();
        long reviews = entries.stream().filter(e -> e.getReview() != null && !e.getReview().isBlank()).count();
        long ratings = entries.stream().filter(e -> e.getScore() != null && e.getScore() > 0).count();
        long followers = follows.countByFollowed(user);
        long following = follows.countByFollower(user);
        List<AchievementDTO> result = new ArrayList<>();
        add(result,"first-game","Primeiro registro","Adicione seu primeiro jogo.","fa-gamepad","bronze",total,1);
        add(result,"collector-10","Colecionador","Tenha 10 jogos na biblioteca.","fa-layer-group","silver",total,10);
        add(result,"collector-50","Arquivo vivo","Tenha 50 jogos na biblioteca.","fa-box-archive","gold",total,50);
        add(result,"first-complete","Primeiros créditos","Conclua um jogo.","fa-flag-checkered","bronze",completed,1);
        add(result,"complete-10","Dez jornadas","Conclua 10 jogos.","fa-trophy","silver",completed,10);
        add(result,"complete-50","Lenda dos créditos","Conclua 50 jogos.","fa-crown","gold",completed,50);
        add(result,"first-platinum","Caçador de platina","Registre sua primeira platina.","fa-gem","silver",platinum,1);
        add(result,"platinum-10","Perfeccionista","Registre 10 platinas.","fa-medal","gold",platinum,10);
        add(result,"first-review","Primeira opinião","Escreva uma review.","fa-pen-nib","bronze",reviews,1);
        add(result,"review-10","Voz da comunidade","Escreva 10 reviews.","fa-comments","silver",reviews,10);
        add(result,"review-50","Crítico veterano","Escreva 50 reviews.","fa-feather-pointed","gold",reviews,50);
        add(result,"rating-10","Curador de notas","Avalie 10 jogos.","fa-star","silver",ratings,10);
        add(result,"following-5","Em boa companhia","Siga 5 pessoas.","fa-user-plus","bronze",following,5);
        add(result,"followers-10","Ponto de encontro","Alcance 10 seguidores.","fa-users","silver",followers,10);
        return result;
    }
    private static void add(List<AchievementDTO> list, String id, String title, String description,
            String icon, String tier, long progress, long target) {
        list.add(new AchievementDTO(id,title,description,icon,tier,Math.min(progress,target),target,progress >= target));
    }
}
