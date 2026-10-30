package com.campus.secondhand.service;
import com.campus.secondhand.common.ApiResponse;
import com.campus.secondhand.config.CurrentUser;
import com.campus.secondhand.util.JwtUtil;
import org.springframework.stereotype.Service;
@Service
public class SessionApplicationService {
    private final SessionRegistry sessions;
    private final JwtUtil jwt;
    public SessionApplicationService(SessionRegistry sessions, JwtUtil jwt) { this.sessions = sessions; this.jwt = jwt; }
    private long userId() { return CurrentUser.get().orElseThrow().getId(); }
    public ApiResponse<?> list(String header) { return ApiResponse.success("获取成功", sessions.list(userId(), jwt.tokenId(header))); }
    public ApiResponse<?> revoke(String id) {
        return sessions.revoke(userId(), id) ? ApiResponse.success("会话已退出") : ApiResponse.error(404, "会话不存在或已退出");
    }
    public ApiResponse<?> logout(String header) { sessions.revoke(userId(), jwt.tokenId(header)); return ApiResponse.success("已退出登录"); }
    public ApiResponse<?> logoutAll() { sessions.revokeAll(userId()); return ApiResponse.success("所有会话已退出"); }
}
