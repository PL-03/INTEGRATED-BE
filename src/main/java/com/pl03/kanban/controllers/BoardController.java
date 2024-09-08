package com.pl03.kanban.controllers;

import com.pl03.kanban.configs.JwtTokenUtils;
import com.pl03.kanban.dtos.BoardRequest;
import com.pl03.kanban.dtos.BoardResponse;
import com.pl03.kanban.kanban_entities.Board;
import com.pl03.kanban.services.BoardService;
import com.pl03.kanban.services.StatusService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v3/boards")
public class BoardController {

    private final BoardService boardService;
    private final JwtTokenUtils jwtTokenUtils;
    private final StatusService statusService;

    @Autowired
    public BoardController(BoardService boardService, JwtTokenUtils jwtTokenUtils, StatusService statusService) {
        this.boardService = boardService;
        this.jwtTokenUtils = jwtTokenUtils;
        this.statusService = statusService;
    }

    @PostMapping
    public ResponseEntity<BoardResponse> createBoard(@RequestBody BoardRequest request, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer " prefix

        if (!jwtTokenUtils.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Claims claims = jwtTokenUtils.getClaimsFromToken(token);
        String ownerOid = claims.get("oid", String.class);
        String ownerName = claims.get("name", String.class);

        // Create the board
        Board board = boardService.createBoard(request.getName(), ownerOid);

        // Add default status to the board
        statusService.addDefaultStatus(board.getBoardId());

        BoardResponse response = convertToBoardResponse(board, ownerName);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoardResponse> getBoardById(@PathVariable String id, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);

        if (!jwtTokenUtils.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Claims claims = jwtTokenUtils.getClaimsFromToken(token);
        String ownerName = claims.get("name", String.class);

        Board board = boardService.getBoardById(id);

        if (board == null) {
            return ResponseEntity.notFound().build();
        }

        BoardResponse response = convertToBoardResponse(board, ownerName);
        return ResponseEntity.ok(response);
    }


    @GetMapping
    public ResponseEntity<List<BoardResponse>> getAllBoards(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);

//        if (!jwtToken.validateToken(token)) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        }

        Claims claims = jwtTokenUtils.getClaimsFromToken(token);
        String ownerName = claims.get("name", String.class);

        List<Board> boards = boardService.getAllBoards();

        List<BoardResponse> responseList = boards.stream()
                .map(board -> convertToBoardResponse(board, ownerName))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    private BoardResponse convertToBoardResponse(Board board, String ownerName) {
        BoardResponse response = new BoardResponse();
        response.setId(board.getBoardId());
        response.setName(board.getName());

        BoardResponse.OwnerResponse ownerResponse = new BoardResponse.OwnerResponse();
        ownerResponse.setOid(board.getOid());
        ownerResponse.setName(ownerName);

        response.setOwner(ownerResponse);

        return response;
    }


}
