package com.pl03.kanban.services.impl;

import com.pl03.kanban.dtos.BoardRequest;
import com.pl03.kanban.dtos.BoardResponse;
import com.pl03.kanban.exceptions.*;
import com.pl03.kanban.kanban_entities.*;
import com.pl03.kanban.kanban_entities.repositories.BoardCollaboratorsRepository;
import com.pl03.kanban.kanban_entities.repositories.BoardRepository;
import com.pl03.kanban.kanban_entities.repositories.UsersRepository;
import com.pl03.kanban.services.BoardService;
import com.pl03.kanban.services.StatusService;
import jakarta.validation.constraints.NotNull;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.pl03.kanban.services.impl.CollaboratorServiceImpl.tempAccessRights;

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
    @Transactional(transactionManager = "kanbanTransactionManager")
    public BoardResponse createBoard(BoardRequest request, String ownerOid, String ownerName) {
        // Check if user already has a board
        if (boardRepository.existsByUserOid(ownerOid)) {
            throw new InvalidBoardFieldException("User can create only 1 board", null);
        }
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
    @Transactional(readOnly = true, transactionManager = "kanbanTransactionManager")
    public BoardResponse getBoardById(String id, String requesterOid) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board not found with id: " + id));

        // Check access conditions for the board
        boolean isPublic = board.getVisibility() == Board.Visibility.PUBLIC;
        boolean isOwner = requesterOid != null && board.getUser().getOid().equals(requesterOid);
        boolean isCollaborator = requesterOid != null &&
                boardCollaboratorsRepository.existsByBoardIdAndUserOid(
                        board.getId(),
                        requesterOid
                ); //check for collaborator (allow pending because the invitation link)

        // If the board is public, the requester is the owner, or the requester is a collaborator, return the board
        if (isPublic || isOwner || isCollaborator) {
            return createBoardResponse(board, board.getUser().getName());
        }

        // If access is denied, throw UnauthorizedAccessException
        throw new UnauthorizedAccessException("Access to this private board is restricted", null);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "kanbanTransactionManager")
    public List<BoardResponse> getAllBoards(String requesterOid) {
        return boardRepository.findAll().stream()
                .filter(board ->
                        board.getVisibility() == Board.Visibility.PUBLIC || // Include public boards
                                (requesterOid != null && (
                                        board.getUser().getOid().equals(requesterOid) || // Include boards owned by the requester
                                                boardCollaboratorsRepository.existsByBoardIdAndUserOid(
                                                        board.getId(), requesterOid)
                                )) // Include boards where the requester is also PENDING collab
                )
                .map(board -> createBoardResponse(board, board.getUser().getName())) // Map to response
                .collect(Collectors.toList()); // Collect to list
    }

    @Override
        @Transactional(transactionManager = "kanbanTransactionManager")
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

    static void getBoardAndCheckAccess(String boardId, String userId, BoardRepository boardRepository, BoardCollaboratorsRepository boardCollaboratorsRepository) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        if (board.getVisibility() != Board.Visibility.PUBLIC &&
                !board.getUser().getOid().equals(userId) && // Check ownership
                !boardCollaboratorsRepository.existsByBoardIdAndUserOidAndAccessRightNot(
                        boardId, userId, BoardCollaborators.AccessRight.PENDING)) { // Check is a valid collaborator
            throw new UnauthorizedAccessException("Access to this board is restricted", null);
        }
    }

    @NotNull
    public static Board validateBoardAccessAndOwnerShip(String boardId, String userId, BoardRepository boardRepository, BoardCollaboratorsRepository boardCollaboratorsRepository) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        if (!board.getUser().getOid().equals(userId)) {
            // Check if the user is a collaborator with write access
            Optional<BoardCollaborators> collaborator = boardCollaboratorsRepository.findByBoardIdAndUserOid(boardId, userId);
            if (collaborator.isEmpty() || collaborator.get().getAccessRight() != BoardCollaborators.AccessRight.WRITE) {
                throw new UnauthorizedAccessException("Only the board owner or collaborators with WRITE access can perform this operation", null);
            }
        }

        return board;
    }

    private BoardResponse createBoardResponse(Board board, String ownerName) {
        BoardResponse response = new BoardResponse();
        response.setId(board.getId());
        response.setName(board.getName());
        response.setVisibility(BoardResponse.Visibility.valueOf(board.getVisibility().name()));
        response.setOwner(new BoardResponse.OwnerResponse(board.getUser().getOid(), ownerName));

        List<BoardResponse.CollaboratorResponse> collaborators;
        if (board.getCollaborators() != null) {
            collaborators = board.getCollaborators().stream()
                    .map(collab -> {
                        // Construct the key for tempAccessRights
                        String tempAccessKey = board.getId() + "-" + collab.getUser().getOid();

                        // Determine assignedAccessRight
                        BoardCollaborators.AccessRight assignedRight = null;
                        if (collab.getAccessRight() == BoardCollaborators.AccessRight.PENDING) {
                            assignedRight = tempAccessRights.get(tempAccessKey);
                        }

                        // Create the collaborator response
                        return new BoardResponse.CollaboratorResponse(
                                collab.getUser().getOid(),
                                collab.getName(),
                                collab.getEmail(),
                                collab.getAccessRight(), // This will be PENDING
                                assignedRight, //  from tempAccessRights if exists
                                collab.getAddedOn()
                        );
                    })
                    .collect(Collectors.toList());
        } else {
            collaborators = new ArrayList<>(); // Empty list if there are no collaborators
        }
        response.setCollaborators(collaborators);

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