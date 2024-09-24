package com.pl03.kanban.services;


import com.pl03.kanban.dtos.BoardRequest;
import com.pl03.kanban.dtos.BoardResponse;

import java.util.List;

public interface BoardService {
    BoardResponse createBoard(BoardRequest request, String ownerOid, String ownerName);
    BoardResponse getBoardById(String id, String ownerName);
    List<BoardResponse> getAllBoards(String requesterOid);
    BoardResponse updateBoardVisibility(String boardId, String visibility, String ownerOid);
}
