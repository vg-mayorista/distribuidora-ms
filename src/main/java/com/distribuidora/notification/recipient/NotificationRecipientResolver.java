package com.distribuidora.notification.recipient;

import com.distribuidora.model.BusinessConfig;
import com.distribuidora.model.User;
import com.distribuidora.repository.UserRepository;
import com.distribuidora.service.BusinessConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationRecipientResolver {

    private final UserRepository userRepository;
    private final BusinessConfigService businessConfigService;

    public List<User> resolveRecipients() {
        BusinessConfig config = businessConfigService.getOrInitConfig();
        Map<UUID, User> recipientsMap = new LinkedHashMap<>();

        // 1. Destinatarios por roles configurados (default: ROLE_ADMIN, ROLE_DISTRIBUTOR)
        List<String> roles = config.getNotifyRolesList();
        if (!roles.isEmpty()) {
            List<User> usersByRole = userRepository.findByRole_NameInAndActiveTrue(roles);
            for (User u : usersByRole) {
                if (u.getActive() != null && u.getActive()) {
                    recipientsMap.put(u.getId(), u);
                }
            }
        }

        // 2. Destinatarios extras por IDs opcionales (notifyUserIdsCsv)
        String extraIdsCsv = config.getNotifyUserIdsCsv();
        if (extraIdsCsv != null && !extraIdsCsv.isBlank()) {
            List<UUID> extraUuids = Arrays.stream(extraIdsCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> {
                        try {
                            return UUID.fromString(s);
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(id -> id != null)
                    .toList();

            if (!extraUuids.isEmpty()) {
                List<User> extraUsers = userRepository.findAllByIdInAndActiveTrue(extraUuids);
                for (User u : extraUsers) {
                    if (u.getActive() != null && u.getActive()) {
                        recipientsMap.put(u.getId(), u);
                    }
                }
            }
        }

        return new ArrayList<>(recipientsMap.values());
    }
}
