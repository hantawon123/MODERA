package com.ssafy.modera.api.domain.notification.service;

import com.ssafy.modera.api.domain.notification.dto.PushSendResult;

import java.util.List;
import java.util.Map;

public interface PushMessageSender {
    PushSendResult send(List<String> tokens, Map<String, String> data);
}
