package com.caiotcruz.mygamelist.dto;
import java.util.List;
public record MessageThreadDTO(ActivityUserDTO partner,List<MessageDTO> messages,boolean canSend,Long nextBefore) {
    public MessageThreadDTO(ActivityUserDTO partner,List<MessageDTO> messages,boolean canSend){this(partner,messages,canSend,null);}
}
