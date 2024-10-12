package com.pl03.kanban.services.impl;

import com.pl03.kanban.dtos.BoardRequest;
import com.pl03.kanban.dtos.BoardResponse;
import com.pl03.kanban.dtos.CollaboratorRequest;
import com.pl03.kanban.dtos.CollaboratorResponse;
import com.pl03.kanban.exceptions.*;
import com.pl03.kanban.kanban_entities.*;
import com.pl03.kanban.services.BoardService;
import com.pl03.kanban.services.StatusService;
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
    private final BoardCollaboratorsRepository boardCollaboratorsRepository;
    private final ModelMapper modelMapper;
    private final StatusService statusService;

    private static final int MAX_BOARD_NAME_LENGTH = 120;

    @Autowired
    public BoardServiceImpl(BoardRepository boardRepository, UsersRepository usersRepository, BoardCollaboratorsRepository boardCollaboratorsRepository, ModelMapper modelMapper, StatusService statusService) {
        this.boardRepository = boardRepository;
        this.usersRepository = usersRepository;
        this.boardCollaboratorsRepository = boardCollaboratorsRepository;
        this.modelMapper = modelMapper;
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
        getBoardAndCheckAccess(id, requesterOid, boardRepository, boardCollaboratorsRepository);

        // Once validation passes, fetch the board and return the response
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board not found with id: " + id));

        return createBoardResponse(board, board.getUser().getName());
    }

    @Override
    public List<BoardResponse> getAllBoards(String requesterOid) {
        List<Board> boards = boardRepository.findAll();
        return boards.stream()
                .filter(board -> board.getVisibility() == Board.Visibility.PUBLIC || //filter for public board and requesters board
                        (board.getUser().getOid().equals(requesterOid)))
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
    @Override
    public List<CollaboratorResponse> getBoardCollaborators(String boardId, String requesterOid) {
        getBoardAndCheckAccess(boardId, requesterOid, boardRepository, boardCollaboratorsRepository);

        List<BoardCollaborators> collaborators = boardCollaboratorsRepository.findByBoardId(boardId);
        return collaborators.stream()
                .map(this::mapToCollaboratorResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CollaboratorResponse getBoardCollaboratorByOid(String boardId, String collabOid, String requesterOid) {
        getBoardAndCheckAccess(boardId, requesterOid, boardRepository, boardCollaboratorsRepository);

        BoardCollaborators collaborator = boardCollaboratorsRepository.findByBoardIdAndUserOid(boardId, collabOid)
                .orElseThrow(() -> new ItemNotFoundException("Collaborator not found"));

        return mapToCollaboratorResponse(collaborator);
    }

    @Override
    public CollaboratorResponse addBoardCollaborator(String boardId, CollaboratorRequest request, String ownerOid) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board not found"));

        if (!board.getUser().getOid().equals(ownerOid)) {
            throw new UnauthorizedAccessException("Only the board owner can add collaborators", null);
        }

        Users user = usersRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ItemNotFoundException("User not found with email: " + request.getEmail()));

        if (user.getOid().equals(ownerOid)) {
            throw new ConflictException("Cannot add board owner as a collaborator");
        }

        if (boardCollaboratorsRepository.existsByBoardIdAndUserOid(boardId, user.getOid())) {
            throw new ConflictException("User is already a collaborator");
        }

        BoardCollaborators.AccessLevel accessLevel;
        try {
            accessLevel = BoardCollaborators.AccessLevel.valueOf(request.getAccessRight().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidBoardFieldException("Invalid access right. Must be READ or WRITE", null);
        }

        BoardCollaborators collaborator = new BoardCollaborators();
        collaborator.setId(new BoardCollaboratorsId(board.getId(), user.getOid()));
        collaborator.setBoard(board);
        collaborator.setUser(user);
        collaborator.setAccessLevel(accessLevel);
        collaborator.setName(user.getName());
        collaborator.setEmail(user.getEmail());

        BoardCollaborators savedCollaborator = boardCollaboratorsRepository.save(collaborator);
        return mapToCollaboratorResponse(savedCollaborator);
    }

//    private Board getBoardAndCheckAccess(String boardId, String requesterOid) {
//        Board board = boardRepository.findById(boardId)
//                .orElseThrow(() -> new ItemNotFoundException("Board not found"));
//
//        if (board.getVisibility() != Board.Visibility.PUBLIC &&
//                !board.getUser().getOid().equals(requesterOid) &&
//                !boardCollaboratorsRepository.existsByBoardIdAndUserOid(boardId, requesterOid)) { //check is a collaborator or not
//            throw new UnauthorizedAccessException("Access to this board is restricted", null);
//        }
//
//        return board;
//    }

    static void getBoardAndCheckAccess(String boardId, String userId, BoardRepository boardRepository, BoardCollaboratorsRepository boardCollaboratorsRepository) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        if (board.getVisibility() != Board.Visibility.PUBLIC &&
                !board.getUser().getOid().equals(userId) && //check ownership
                !boardCollaboratorsRepository.existsByBoardIdAndUserOid(boardId, userId)) { //check is a collaborator or not
            throw new UnauthorizedAccessException("Access to this board is restricted", null);
        }
    }

    private CollaboratorResponse mapToCollaboratorResponse(BoardCollaborators collaborator) {
        return CollaboratorResponse.builder()
                .oid(collaborator.getUser().getOid())
                .name(collaborator.getName())
                .email(collaborator.getEmail())
                .accessRight(collaborator.getAccessLevel().name())
                .addedOn(collaborator.getAddedOn())
                .build();
    }
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