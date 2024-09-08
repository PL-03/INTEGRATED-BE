package com.pl03.kanban.services;


import com.pl03.kanban.kanban_entities.Board;

import java.util.List;

public interface BoardService {
    Board createBoard(String boardName, String ownerOid);
    Board getBoardById(String id);

    List<Board> getAllBoards();
}
