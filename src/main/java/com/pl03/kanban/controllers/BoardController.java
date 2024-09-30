package com.pl03.kanban.controllers;

import com.pl03.kanban.exceptions.UnauthorizedAccessException;
import com.pl03.kanban.utils.JwtTokenUtils;
import com.pl03.kanban.dtos.BoardRequest;
import com.pl03.kanban.dtos.BoardResponse;
import com.pl03.kanban.services.BoardService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"http://localhost:5173",
        "http://intproj23.sit.kmutt.ac.th",
        "http://intproj23.sit.kmutt.ac.th/pl3",
        "http://intproj23.sit.kmutt.ac.th/pl3/status",
        "http://ip23pl3.sit.kmutt.ac.th",
        "https://intproj23.sit.kmutt.ac.th",
        "https://intproj23.sit.kmutt.ac.th/pl3",
        "https://intproj23.sit.kmutt.ac.th/pl3/status",
        "https://ip23pl3.sit.kmutt.ac.th"})
@RestController
@RequestMapping("/v3/boards")
public class BoardController {

    private final BoardService boardService;
    private final JwtTokenUtils jwtTokenUtils;

    @Autowired
    public BoardController(BoardService boardService, JwtTokenUtils jwtTokenUtils) {
        this.boardService = boardService;
        this.jwtTokenUtils = jwtTokenUtils;
    }

    @PostMapping
    public ResponseEntity<BoardResponse> createBoard(@RequestBody BoardRequest request, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        if (!jwtTokenUtils.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Claims claims = (Claims) jwtTokenUtils.getClaimsFromToken(token);
        String ownerOid = claims.get("oid", String.class);
        String ownerName = claims.get("name", String.class);

        BoardResponse response = boardService.createBoard(request, ownerOid, ownerName);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoardResponse> getBoardById(@PathVariable String id,
                                                      @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String requesterOid = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtTokenUtils.validateToken(token)) {
                requesterOid = getUserIdFromToken(token);
            }
        }

        BoardResponse response = boardService.getBoardById(id, requesterOid);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<BoardResponse>> getAllBoards(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String requesterOid = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtTokenUtils.validateToken(token)) {
                requesterOid = getUserIdFromToken(token);
            }
        }

        List<BoardResponse> responseList = boardService.getAllBoards(requesterOid);
        return ResponseEntity.ok(responseList);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BoardResponse> updateBoardVisibility(
            @PathVariable String id,
            @RequestBody Map<String, String> updateRequest,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        if (!jwtTokenUtils.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String ownerOid = getUserIdFromToken(token);

        String visibility = updateRequest.get("visibility");
        if (visibility == null) {
            return ResponseEntity.badRequest().body(null);
        }

        BoardResponse response = boardService.updateBoardVisibility(id, visibility, ownerOid);
        return ResponseEntity.ok(response);
    }

    private String getUserIdFromToken(String token) {
        Map<String, Object> claims = jwtTokenUtils.getClaimsFromToken(token);
        return (String) claims.get("oid");
    }
}
