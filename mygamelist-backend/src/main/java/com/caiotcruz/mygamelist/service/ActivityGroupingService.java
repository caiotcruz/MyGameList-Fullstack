package com.caiotcruz.mygamelist.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.caiotcruz.mygamelist.dto.GroupedActivityDTO;
import com.caiotcruz.mygamelist.model.Activity;

@Service
public class ActivityGroupingService {

    public List<GroupedActivityDTO> groupAndMap(List<Activity> activities, Long currentUserId) {
        Map<String, List<Activity>> byGroup = activities.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getGroupId() != null ? a.getGroupId() : "legacy-" + a.getId(),
                        LinkedHashMap::new, Collectors.toList()));

        return byGroup.values().stream()
                .map(g -> GroupedActivityDTO.from(g, currentUserId))
                .sorted(Comparator.comparing(GroupedActivityDTO::timestamp).reversed())
                .toList();
    }
}