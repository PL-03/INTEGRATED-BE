package com.pl03.kanban.controllers;

import com.pl03.kanban.exceptions.ErrorResponse;
import com.pl03.kanban.exceptions.ItemNotFoundException;
import com.pl03.kanban.exceptions.UnauthorizedAccessException;
import com.pl03.kanban.kanban_entities.Board;
import com.pl03.kanban.kanban_entities.BoardRepository;
import com.pl03.kanban.utils.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
@RestController
public abstract class BaseController {
    @Autowired
    protected JwtTokenUtils jwtTokenUtils;

    @Autowired
    protected BoardRepository boardRepository;

    public String validateTokenAndGetUserId(String authHeader) {
        String token = authHeader.substring(7);
        if (!jwtTokenUtils.validateToken(token)) {
            throw new UnauthorizedAccessException("Invalid token", null);
        }
        return getUserIdFromToken(token);
    }

    protected String getUserIdFromToken(String token) {
        Map<String, Object> claims = jwtTokenUtils.getClaimsFromToken(token);
        return (String) claims.get("oid");
    }

    public void validateBoardOwnership(String boardId, String userId, boolean allowPublicAccess) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board not found with id: " + boardId));

        if (!board.getUser().getOid().equals(userId) && (!allowPublicAccess || board.getVisibility() != Board.Visibility.PUBLIC)) {
            ErrorResponse errorResponse = new ErrorResponse(
                    HttpStatus.FORBIDDEN.value(),
                    "Only the board owner can perform this action",
                    "Authorization error"
            );
            throw new UnauthorizedAccessException("Unauthorized access", errorResponse.getErrors());
        }
    }
}
