package com.pl03.kanban.services;


import com.pl03.kanban.dtos.BoardRequest;
import com.pl03.kanban.dtos.BoardResponse;
import com.pl03.kanban.kanban_entities.Board;

import java.util.List;

public interface BoardService {
    BoardResponse createBoard(BoardRequest request, String ownerOid, String ownerName);
    BoardResponse getBoardById(String id, String ownerName);
    List<BoardResponse> getAllBoards(String ownerName);
}
