package com.kori.application.port.out.query;

import com.kori.application.query.model.AgentQueryModels;

import java.util.List;

public interface AgentSearchReadPort {
    List<AgentQueryModels.AgentSearchItem> searchByPhone(String phone, int limit);

    List<AgentQueryModels.AgentSearchItem> searchByCardUid(String cardUid, int limit);

    List<AgentQueryModels.AgentSearchItem> searchByTerminalUid(String terminalUid, int limit);
}
