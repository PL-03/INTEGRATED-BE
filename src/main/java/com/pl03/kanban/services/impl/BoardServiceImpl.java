package com.pl03.kanban.services.impl;

import com.pl03.kanban.dtos.BoardRequest;
import com.pl03.kanban.dtos.BoardResponse;
import com.pl03.kanban.exceptions.*;
import com.pl03.kanban.kanban_entities.Board;
import com.pl03.kanban.kanban_entities.BoardRepository;
import com.pl03.kanban.kanban_entities.Users;
import com.pl03.kanban.kanban_entities.UsersRepository;
import com.pl03.kanban.services.BoardService;
import com.pl03.kanban.services.StatusService;
import com.pl03.kanban.utils.ListMapper;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final UsersRepository usersRepository;
    private final ModelMapper modelMapper;
    private final ListMapper listMapper;
    private final StatusService statusService;

    private static final int MAX_BOARD_NAME_LENGTH = 120;

    @Autowired
    public BoardServiceImpl(BoardRepository boardRepository, UsersRepository usersRepository, ModelMapper modelMapper, ListMapper listMapper, StatusService statusService) {
        this.boardRepository = boardRepository;
        this.usersRepository = usersRepository;
        this.modelMapper = modelMapper;
        this.listMapper = listMapper;
        this.statusService = statusService;
    }

    @Override
    public BoardResponse createBoard(BoardRequest request, String ownerOid, String ownerName) {
        // Validate the board name
        ErrorResponse errorResponse = validateBoardFields(request);
        if (errorResponse != null && !errorResponse.getErrors().isEmpty()) {
            throw new InvalidBoardFieldException("Validation error. Check 'errors' field for details", errorResponse.getErrors());
        }

        // Trim the board name after validation
        String trimmedBoardName = request.getName().trim();
        request.setName(trimmedBoardName);

        // Fetch the user by OID
        Users users = usersRepository.findByOid(ownerOid)
                .orElseThrow(() -> new ItemNotFoundException("User with oid " + ownerOid + " does not exist"));

        // Map the request to the Board entity
        Board board = modelMapper.map(request, Board.class);
        board.setUser(users);  // Associate the user with the board

        // Ensure unique boardId
        do {
            board.generateUniqueId();
        } while (boardRepository.existsById(board.getId()));

        // Set default visibility to PRIVATE
        board.setVisibility(Board.Visibility.PRIVATE);

        // Save the board
        board = boardRepository.save(board);

        // Add default status to the board
        statusService.addDefaultStatus(board.getId());

        // Return the BoardResponse
        return createBoardResponse(board, ownerName);
    }


    @Override
    public BoardResponse getBoardById(String id, String requesterOid) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board not found with id: " + id));

        // If the board is public or the requester is the owner, return the board
        if (board.getVisibility() == Board.Visibility.PUBLIC ||
                (requesterOid != null && board.getUser().getOid().equals(requesterOid))) {
            return createBoardResponse(board, board.getUser().getName());
        }

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Access to this private board is restricted",
                "Authorization error"
        );
        throw new UnauthorizedAccessException(errorResponse.getMessage(), errorResponse.getErrors());
    }


    @Override
    public List<BoardResponse> getAllBoards(String requesterOid) {
        List<Board> boards = boardRepository.findAll();
        return boards.stream()
                .filter(board -> board.getVisibility() == Board.Visibility.PUBLIC ||
                        (requesterOid != null && board.getUser().getOid().equals(requesterOid)))
                .map(board -> createBoardResponse(board, board.getUser().getName()))
                .collect(Collectors.toList());
    }

    @Override
    public BoardResponse updateBoardVisibility(String boardId, Map<String, String> updateRequest, String ownerOid) {
        // Fetch the board by id
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board not found with id: " + boardId));

        // Verify if the requester is the board owner
        if (!board.getUser().getOid().equals(ownerOid)) {
            throw new UnauthorizedAccessException("Only the board owner can change the visibility", null);
        }

        // Check if updateRequest is null or empty
        if (updateRequest == null || updateRequest.isEmpty()) {
            throw new InvalidBoardFieldException("Request body must not be empty", null);
        }

        String visibility = updateRequest.get("visibility");
        if (visibility == null || visibility.isEmpty()) {
            throw new InvalidBoardFieldException("Body must have 'visibility' value to update board's visibility", null);
        }

        // Convert visibility to uppercase for comparison, but keep original for error message if needed
        String uppercaseVisibility = visibility.toUpperCase();
        if (uppercaseVisibility.equals("PRIVATE") || uppercaseVisibility.equals("PUBLIC")) {
            board.setVisibility(Board.Visibility.valueOf(uppercaseVisibility));
        } else {
            ErrorResponse errorResponse = new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "Invalid visibility value",
                    "Validation error"
            );
            errorResponse.addValidationError("visibility", "Visibility must be 'PRIVATE' or 'PUBLIC'");
            throw new InvalidBoardFieldException("Validation error. Check 'errors' field for details", errorResponse.getErrors());
        }

        // Save and return the updated board
        Board updatedBoard = boardRepository.save(board);
        return createBoardResponse(updatedBoard, updatedBoard.getUser().getName());
    }

//    @Override
//    public boolean isOwner(String boardId, String userOid) {
//        Board board = boardRepository.findById(boardId)
//                .orElseThrow(() -> new ItemNotFoundException("Board not found with id: " + boardId));
//        return board.getUser().getOid().equals(userOid);
//    }

//    private BoardResponse createBoardResponse(Board board, String ownerName) {
//        BoardResponse response = modelMapper.map(board, BoardResponse.class);
//        response.setOwner(new BoardResponse.OwnerResponse(board.getUser().getOid(), ownerName));  // Get OID from user
//        return response;
//    }
    private BoardResponse createBoardResponse(Board board, String ownerName) {
        BoardResponse response = new BoardResponse(); // using manual mapping because of avoiding potential issue
        response.setId(board.getId());                  // with enum value
        response.setName(board.getName());
        response.setVisibility(BoardResponse.Visibility.valueOf(board.getVisibility().name()));
        response.setOwner(new BoardResponse.OwnerResponse(board.getUser().getOid(), ownerName));
        return response;
}

    // Validation method for the board name
    private ErrorResponse validateBoardFields(BoardRequest boardRequest) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation error. Check 'errors' field for details", "");

        // Validate board name
        if (boardRequest.getName() == null || boardRequest.getName().trim().isEmpty()) {
            errorResponse.addValidationError(BoardRequest.Fields.name, "Board name must not be null or empty");
        } else if (boardRequest.getName().trim().length() > MAX_BOARD_NAME_LENGTH) {
            errorResponse.addValidationError(BoardRequest.Fields.name, "Board name must be between 1 and " + MAX_BOARD_NAME_LENGTH + " characters");
        }

        // If there are no validation errors, return null
        if (errorResponse.getErrors().isEmpty()) {
            return null;
        }

        return errorResponse;
    }
}

