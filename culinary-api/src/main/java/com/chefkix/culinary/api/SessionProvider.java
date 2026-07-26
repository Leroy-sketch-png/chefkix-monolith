package com.chefkix.culinary.api;

import com.chefkix.culinary.api.dto.SessionInfo;
import com.chefkix.identity.api.dto.BasicProfileInfo;

import java.util.List;

/**
 */
public interface SessionProvider {

    /**
     *
     */
    SessionInfo getSession(String sessionId);

    /**
     *
     */
    List<BasicProfileInfo> getCoChefs(String roomCode, String excludeUserId);
}
