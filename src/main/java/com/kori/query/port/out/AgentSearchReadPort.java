package com.kori.query.port.out;

import com.kori.query.model.me.AgentQueryModels;

import java.util.List;

public interface AgentSearchReadPort {
    List<AgentQueryModels.AgentSearchItem> searchByPhone(String phone, int limit);

    List<AgentQueryModels.AgentSearchItem> searchByCardUid(String cardUid, int limit);

    List<AgentQueryModels.AgentSearchItem> searchByTerminalUid(String terminalUid, int limit);
}
