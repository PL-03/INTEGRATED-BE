package com.pl03.kanban.services.impl;

import com.pl03.kanban.dtos.BoardRequest;
import com.pl03.kanban.dtos.BoardResponse;
import com.pl03.kanban.kanban_entities.Board;
import com.pl03.kanban.kanban_entities.BoardRepository;
import com.pl03.kanban.services.BoardService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public BoardServiceImpl(BoardRepository boardRepository, ModelMapper modelMapper) {
        this.boardRepository = boardRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public Board createBoard(String boardName, String oid) {
        Board board = Board.builder()
                .name(boardName)
                .oid(oid)
                .build();
        return boardRepository.save(board);
    }


    @Override
    public List<Board> getAllBoards() {
        return boardRepository.findAll();
    }

    @Override
    public Board getBoardById(String id) {
        Optional<Board> board = boardRepository.findById(id);
        return board.orElse(null);
    }

}
