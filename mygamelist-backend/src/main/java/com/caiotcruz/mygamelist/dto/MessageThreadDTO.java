package com.caiotcruz.mygamelist.dto;

import java.util.List;

public record MessageThreadDTO(
        ActivityUserDTO partner,
        List<MessageDTO> messages
) {}