package com.pl03.kanban.services.impl;

import com.pl03.kanban.dtos.BoardRequest;
import com.pl03.kanban.dtos.BoardResponse;
import com.pl03.kanban.exceptions.ItemNotFoundException;
import com.pl03.kanban.kanban_entities.Board;
import com.pl03.kanban.kanban_entities.BoardRepository;
import com.pl03.kanban.services.BoardService;
import com.pl03.kanban.services.StatusService;
import com.pl03.kanban.utils.ListMapper;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final ModelMapper modelMapper;
    private final ListMapper listMapper;
    private final StatusService statusService;

    @Autowired
    public BoardServiceImpl(BoardRepository boardRepository, ModelMapper modelMapper, ListMapper listMapper, StatusService statusService) {
        this.boardRepository = boardRepository;
        this.modelMapper = modelMapper;
        this.listMapper = listMapper;
        this.statusService = statusService;
    }

    @Override
    public BoardResponse createBoard(BoardRequest request, String ownerOid, String ownerName) {
        Board board = modelMapper.map(request, Board.class);
        board.setOid(ownerOid);

        // Ensure unique boardId
        do {
            board.generateUniqueId();
        } while (boardRepository.existsByBoardId(board.getBoardId()));

        board = boardRepository.save(board);

        // Add default status to the board
        statusService.addDefaultStatus(board.getBoardId());

        return createBoardResponse(board, ownerName);
    }

    @Override
    public BoardResponse getBoardById(String id, String ownerName) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board not found with id: " + id));
        return createBoardResponse(board, ownerName);
    }

    @Override
    public List<BoardResponse> getAllBoards(String ownerName) {
        List<Board> boards = boardRepository.findAll();
        return boards.stream()
                .map(board -> createBoardResponse(board, ownerName))
                .collect(Collectors.toList());
    }

    private BoardResponse createBoardResponse(Board board, String ownerName) {
        BoardResponse response = modelMapper.map(board, BoardResponse.class);
        response.setOwner(new BoardResponse.OwnerResponse(board.getOid(), ownerName));
        return response;
    }
}
