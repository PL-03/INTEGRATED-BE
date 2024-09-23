package com.pl03.kanban.controllers;

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
@CrossOrigin(origins = {"http://localhost:5173",
        "http://intproj23.sit.kmutt.ac.th",
        "http://intproj23.sit.kmutt.ac.th/pl3",
        "http://intproj23.sit.kmutt.ac.th/pl3/status",
        "http://ip23pl3.sit.kmutt.ac.th"})
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

        Claims claims = jwtTokenUtils.getClaimsFromToken(token);
        String ownerOid = claims.get("oid", String.class);
        String ownerName = claims.get("name", String.class);

        BoardResponse response = boardService.createBoard(request, ownerOid, ownerName);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoardResponse> getBoardById(@PathVariable String id, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        if (!jwtTokenUtils.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Claims claims = jwtTokenUtils.getClaimsFromToken(token);
        String ownerName = claims.get("name", String.class);

        BoardResponse response = boardService.getBoardById(id, ownerName);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<BoardResponse>> getAllBoards(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        if (!jwtTokenUtils.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Claims claims = jwtTokenUtils.getClaimsFromToken(token);
        String ownerName = claims.get("name", String.class);

        List<BoardResponse> responseList = boardService.getAllBoards(ownerName);
        return ResponseEntity.ok(responseList);
    }
}
